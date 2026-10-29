;; electric-rules.bb - Puts the condensed rules in electric-rules.md into the agent's
;; context in a matching project: when a session starts, and on the first edit of
;; a matching file when the session start did not deliver them. Started by
;; electric-rules.sh, which reports the hook as inactive when Babashka is missing.
;;
;; Agents rarely load an auto-triggered skill on their own, so the hook delivers
;; the rules as text and tells the agent to load the skill for the details.
;;
;; Shipped as an APM hook primitive and verified on Claude Code. APM registers
;; it for SessionStart and for PostToolUse with the matcher "*", and the script
;; picks the edit events itself:
;;
;;   SessionStart  Runs for every source (startup, resume, clear, compact,
;;                 fork), because clearing and compaction remove the earlier
;;                 rules from context. Output: hookSpecificOutput.additionalContext.
;;   PostToolUse   Claude Code Write, Edit, and MultiEdit (path in
;;                 tool_input.file_path), or an apply_patch command (paths in
;;                 its "*** Add File:" and "*** Update File:" lines). Delivers
;;                 the rules once per session, and only when no session start
;;                 delivered them since the last clear or compaction.
;;
;; The settings below are the only part that differs between the Clojure-family
;; packages. Every path exits 0, so a failure leaves the session without the
;; rules and never blocks it.
(ns electric-rules
  (:require [babashka.fs :as fs]
            [cheshire.core :as json]
            [clojure.string :as str]))

;; Package settings

(def hook-name "electric-rules")

(def rules-file (fs/path (fs/parent (fs/absolutize *file*)) "electric-rules.md"))

;; Extensions of the files whose first edit can deliver the rules.
(def extensions #{"clj" "cljs" "cljc" "edn"})

;; The project matches when any entry holds in its root: the file exists, it
;; contains :mentions when that is set, and it does not contain :excludes when
;; that is set.
(def gate
  [{:file "deps.edn" :mentions "com.hyperfiddle/electric"}])

;; Project detection

(def project-markers
  ["deps.edn" "project.clj" "bb.edn" "shadow-cljs.edn" "build.clj" "figwheel-main.edn"])

(defn project-root
  "The nearest directory, starting at dir and moving up, that holds a Clojure
  project marker, or nil when there is none."
  [dir]
  (->> (iterate fs/parent (fs/absolutize dir))
       (take-while some?)
       (filter (fn [d] (some #(fs/regular-file? (fs/path d %)) project-markers)))
       first))

(defn entry-holds? [root {:keys [file mentions excludes]}]
  (let [path (fs/path root file)]
    (and (fs/regular-file? path)
         (let [text (if (or mentions excludes) (slurp (str path)) "")]
           (and (or (nil? mentions) (str/includes? text mentions))
                (or (nil? excludes) (not (str/includes? text excludes))))))))

(defn matching-project? [dir]
  (when-let [root (some-> dir project-root)]
    (some #(entry-holds? root %) gate)))

;; Edit events

(defn patch-paths
  "Files that an apply_patch command adds, updates, or moves to."
  [patch]
  (->> (str/split-lines (or patch ""))
       (keep #(second (re-matches #"\*\*\* (?:Add File|Update File|Move to): (.+)" %)))
       (map str/trim)))

(defn edited-paths
  "The files an edit event changed, or nil when the event is not an edit."
  [input]
  (case (:tool_name input)
    ("Write" "Edit" "MultiEdit") (some-> (get-in input [:tool_input :file_path]) vector)
    "apply_patch" (seq (patch-paths (get-in input [:tool_input :command])))
    nil))

(defn absolute-path [cwd path]
  (fs/normalize (fs/absolutize (if (fs/absolute? path) path (fs/path (or cwd ".") path)))))

(defn extension [path]
  (some-> (re-find #"\.([^./]+)$" (str path)) second))

(defn matching-edit? [input]
  (some (fn [path]
          (let [path (absolute-path (:cwd input) path)]
            (and (extensions (extension path))
                 (matching-project? (fs/parent path)))))
        (edited-paths input)))

;; Session state

(defn state-dir
  "The directory that records deliveries for the session, or nil when the input
  has no session ID."
  [input]
  (when-let [id (or (:session_id input) (:sessionId input))]
    (let [dir (fs/path (fs/temp-dir) "clojure-family-rules" (str/replace id #"[^A-Za-z0-9_-]" "_"))]
      (fs/create-dirs dir)
      dir)))

(defn delivered-file [dir] (fs/file dir (str "delivered-" hook-name)))

;; A user-scope and a project-scope install both run for the same session
;; start. The first process to claim the start delivers, and a claim older
;; than this is a later start with the same source, such as a second compaction.
(def start-claim-ms 60000)

(defn claim-start!
  "True when this process is the first to deliver for this session start."
  [dir source]
  (let [claim (fs/file dir (str "start-" hook-name "-" (str/replace (str source) #"[^A-Za-z0-9_-]" "_")))]
    (or (.createNewFile claim)
        (when (> (- (System/currentTimeMillis) (.lastModified claim)) start-claim-ms)
          (.delete claim)
          (.createNewFile claim)))))

;; Output

(defn respond [event text]
  (println (json/generate-string {:hookSpecificOutput {:hookEventName event
                                                       :additionalContext text}})))

(defn rules-text [] (str/trim (slurp (str rules-file))))

(defn session-start
  "Delivers the rules in a matching project. Elsewhere it clears the delivery
  record, so the first matching edit after a clear or compaction delivers them
  again."
  [input]
  (let [dir (state-dir input)]
    (if (matching-project? (:cwd input))
      (when (or (nil? dir) (claim-start! dir (:source input)))
        (some-> dir delivered-file .createNewFile)
        (respond "SessionStart" (rules-text)))
      (some-> dir delivered-file fs/delete-if-exists))))

(defn after-edit
  "Delivers the rules on the first matching edit of a session that has not
  received them. Without a session ID there is no record, so it stays silent."
  [input]
  (when-let [dir (state-dir input)]
    (when (and (matching-edit? input) (.createNewFile (delivered-file dir)))
      (respond "PostToolUse" (rules-text)))))

(defn -main []
  (let [input (json/parse-string (slurp *in*) true)]
    (if (= "SessionStart" (:hook_event_name input))
      (session-start input)
      (after-edit input))))

(try
  (-main)
  (catch Exception e
    ;; A hook failure must not block the agent. Report it on standard error, which
    ;; the runtimes log without treating it as a denial.
    (binding [*out* *err*]
      (println (str hook-name " failed: " (ex-message e))))))
