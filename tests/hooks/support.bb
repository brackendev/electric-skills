;; Helpers shared by the hook test suites: hook input, running a hook, temporary
;; directories, and pass and fail counting. Each suite loads this file with
;; load-file.
(ns hook-test-support
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [cheshire.core :as json]
            [clojure.string :as str]))

(defn claude-input
  "The JSON Claude Code sends to a hook after a Bash command. For
  PostToolUseFailure the output is in error, and for PostToolUse it is in
  tool_response."
  [session event command output]
  (json/generate-string (cond-> {"session_id" session
                                 "hook_event_name" event
                                 "tool_name" "Bash"
                                 "tool_input" {"command" command}}
                          (= event "PostToolUseFailure") (assoc "error" (str "Exit code 1\n" output))
                          (= event "PostToolUse") (assoc "tool_response" {"stdout" output "stderr" ""}))))

(defn run-hook
  "Runs a hook script with input on standard input. Returns :exit, :out, and :err."
  [script input & {:keys [env path args]}]
  (let [env (cond-> (merge (into {} (System/getenv)) env)
              path (assoc "PATH" path))]
    (apply process/sh {:in input :env env :err :string} (str script) args)))

(defn temp-directory []
  (str (fs/create-temp-dir)))

(defn git [dir & arguments]
  (let [{:keys [exit err]} (apply process/sh {:dir dir :err :string} "git" arguments)]
    (when-not (zero? exit)
      (throw (ex-info (str "git " (str/join " " arguments) " failed: " err) {})))))

(defn write-file
  "Writes content to a path under dir, creating parent directories. Returns the absolute path."
  [dir relative content]
  (let [path (fs/path dir relative)]
    (fs/create-dirs (fs/parent path))
    (spit (str path) content)
    (str (fs/absolutize path))))

(def results (atom {:pass 0 :fail 0}))

(defn result
  "Records one case. Prints the description when got differs from want."
  [want got description]
  (if (= want got)
    (swap! results update :pass inc)
    (do (swap! results update :fail inc)
        (println (str "FAIL want=" (pr-str want) " got=" (pr-str got) "  " description)))))

(defn check [ok? description]
  (result true (boolean ok?) description))

(defn report
  "Prints the totals for a suite and exits 0 only when every case passed."
  [suite]
  (let [{:keys [pass fail]} @results]
    (println (str suite ": " pass " pass, " fail " fail"))
    (System/exit (if (and (pos? pass) (zero? fail)) 0 1))))
