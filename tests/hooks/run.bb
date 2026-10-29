;; Runs every hook test suite against this repository:
;;   1. The electric-rules hook end to end, for session starts and edits.
;;   2. A local APM install at project and user scope, checking the deployed
;;      hook scripts.
;; Requires bb, git, and apm. A missing requirement fails the run. No
;; suite is skipped.
;;
;; Usage: bb tests/hooks/run.bb
(require '[babashka.fs :as fs]
         '[babashka.process :as process])

(def here (fs/parent (fs/absolutize *file*)))
(def root (str (fs/parent (fs/parent here))))
(def scripts (str (fs/path root ".apm" "hooks" "scripts")))

(def missing
  (for [tool ["git" "apm"] :when (not (fs/which tool))]
    (str "Missing requirement: " tool)))

(when (seq missing)
  (run! println missing)
  (System/exit 1))

(def suites
  [["electric-rules hook" "electric-rules-hook.bb" scripts]
   ["APM install" "install-check.bb" root]])

(def failed
  (doall
   (for [[suite-name file argument] suites
         :let [_ (println (str "\n=== " suite-name))
               {:keys [exit]} @(process/process {:out :inherit :err :inherit}
                                                "bb" (str (fs/path here file)) argument)]
         :when (not (zero? exit))]
     suite-name)))

(println)
(if (seq failed)
  (do (println "Failed suites:")
      (doseq [suite-name failed] (println (str "  " suite-name)))
      (System/exit 1))
  (println "All hook suites passed."))
