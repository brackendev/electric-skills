;; Runs electric-rules.sh end to end against temporary projects and checks when it
;; delivers the rules: at session start in a matching project, once on the
;; first matching edit when the session start did not deliver them, again after
;; a clear or compaction, never twice for one start, never outside a matching
;; project, and never on a failure path. Also checks the launcher without
;; Babashka.
;;
;; Usage: bb electric-rules-hook.bb <hook scripts directory>
(require '[babashka.fs :as fs]
         '[babashka.process :as process]
         '[cheshire.core :as json]
         '[clojure.string :as str])

(def here (fs/parent (fs/absolutize *file*)))
(load-file (str (fs/path here "support.bb")))
(alias 'support 'hook-test-support)

(def scripts (or (first *command-line-args*)
                 (do (println "Usage: bb electric-rules-hook.bb <hook scripts directory>")
                     (System/exit 1))))

;; Package settings

(def hook-name "electric-rules")
(def first-law "ELEC-1:")
;; Project files that the hook's gate accepts, and a project it rejects.
(def matching-files {"deps.edn" "{:deps {com.hyperfiddle/electric {:mvn/version \"v3\"}}}\n"})
(def other-files {"deps.edn" "{:paths [\"src\"]}\n"})
(def edited-file "src/app/main.cljc")

;; Cases

(def hook (str (fs/path scripts (str hook-name ".sh"))))

(defn session [] (str "test-" (random-uuid)))

(defn project [files]
  (let [dir (support/temp-directory)]
    (doseq [[file content] files] (support/write-file dir file content))
    (support/write-file dir edited-file "(ns app.core)\n")
    dir))

(defn run
  "Runs the hook and returns {:exit :event :context :err}. :event is the
  hookEventName of the output, :none for no output, or :invalid."
  [input & opts]
  (let [{:keys [exit out err]} (apply support/run-hook hook (json/generate-string input) opts)
        out (str/trim (or out ""))]
    (merge {:exit exit :err err}
           (if (str/blank? out)
             {:event :none}
             (let [parsed (try (json/parse-string out) (catch Exception _ nil))]
               {:event (or (get-in parsed ["hookSpecificOutput" "hookEventName"]) :invalid)
                :context (get-in parsed ["hookSpecificOutput" "additionalContext"])})))))

(defn start [session-id cwd source]
  (cond-> {"hook_event_name" "SessionStart" "source" source "cwd" cwd}
    session-id (assoc "session_id" session-id)))

(defn edit [session-id cwd path]
  (cond-> {"hook_event_name" "PostToolUse" "cwd" cwd "tool_name" "Write"
           "tool_input" {"file_path" path} "tool_response" {}}
    session-id (assoc "session_id" session-id)))

(defn delivered?
  "True when the hook exited 0 and returned the rules for event."
  [{:keys [exit context] :as result} event]
  (and (zero? exit) (= event (:event result)) (string? context) (str/includes? context first-law)))

(defn silent? [{:keys [exit event]}]
  (and (zero? exit) (= :none event)))

(def matching (project matching-files))
(def other (project other-files))
(def outside (support/temp-directory))
(def matching-path (str (fs/path matching edited-file)))

;; A session that starts in a matching project receives the rules once.
(let [s (session)
      first-start (run (start s matching "startup"))]
  (support/check (delivered? first-start "SessionStart") "startup in a matching project delivers the rules")
  (support/check (silent? (run (edit s matching matching-path))) "an edit after a delivered start is silent")
  (support/check (silent? (run (start s matching "startup"))) "a second install's start for the same session start is silent")
  (let [compact (run (start s matching "compact"))]
    (support/check (delivered? compact "SessionStart") "compaction delivers the rules again"))
  (let [clear (run (start s matching "clear"))]
    (support/check (delivered? clear "SessionStart") "a clear delivers the rules again")))

;; A session that starts outside a project receives the rules on the first matching edit.
(let [s (session)]
  (support/check (silent? (run (start s outside "startup"))) "startup outside a project is silent")
  (let [first-edit (run (edit s outside matching-path))]
    (support/check (delivered? first-edit "PostToolUse") "the first matching edit delivers the rules"))
  (support/check (silent? (run (edit s outside matching-path))) "a second matching edit is silent")
  (support/check (silent? (run (start s outside "compact"))) "compaction outside a project is silent")
  (let [after-compact (run (edit s outside matching-path))]
    (support/check (delivered? after-compact "PostToolUse")
                   "the first matching edit after a compaction delivers the rules again")))

;; Edits that do not match stay silent.
(let [s (session)]
  (support/check (silent? (run (edit s matching (str (fs/path matching "README.md")))))
                 "an edit of a file with another extension is silent")
  (support/check (silent? (run (edit s other (str (fs/path other edited-file)))))
                 "an edit in a project the gate rejects is silent")
  (support/check (silent? (run (start s other "startup"))) "startup in a project the gate rejects is silent")
  (support/check (silent? (run {"session_id" s "hook_event_name" "PostToolUse" "cwd" matching
                                "tool_name" "Bash" "tool_input" {"command" "ls"} "tool_response" {}}))
                 "a shell command is silent"))

;; A Codex apply_patch edit delivers the rules.
(let [s (session)
      patch (str "*** Begin Patch\n*** Update File: " edited-file "\n@@\n-(ns app.core)\n+(ns app.core)\n*** End Patch\n")
      result (run {"session_id" s "hook_event_name" "PostToolUse" "cwd" matching
                   "tool_name" "apply_patch" "tool_input" {"command" patch} "tool_response" {}})]
  (support/check (delivered? result "PostToolUse") "an apply_patch edit delivers the rules"))

;; Without a session ID, a start still delivers, and an edit stays silent.
(let [no-session-start (run (start nil matching "startup"))]
  (support/check (delivered? no-session-start "SessionStart") "startup without a session ID delivers the rules")
  (support/check (silent? (run (edit nil matching matching-path))) "an edit without a session ID is silent"))

;; Failures never block the session.
(let [{:keys [exit out err]} (support/run-hook hook "{\"hook_event_name\": \"SessionStart\", not json")]
  (support/check (and (zero? exit) (str/blank? out) (str/includes? err (str hook-name " failed")))
                 "malformed input exits 0 and reports the failure on standard error"))

(let [path-without-bb (support/temp-directory)]
  (doseq [tool ["cat" "dirname"]] (fs/create-sym-link (fs/path path-without-bb tool) (fs/which tool)))
  (let [{:keys [exit err]} (process/sh {:in (json/generate-string (start (session) matching "startup")) :err :string
                                        :env (assoc (into {} (System/getenv)) "PATH" (str path-without-bb))}
                                       "/bin/bash" hook)]
    (support/check (and (zero? exit) (str/includes? err "is inactive: Babashka (bb) is not installed"))
                   "the launcher without bb reports the hook as inactive and exits 0"))
  (fs/delete-tree path-without-bb))

(run! fs/delete-tree [matching other outside])
(support/report (str hook-name " hook"))
