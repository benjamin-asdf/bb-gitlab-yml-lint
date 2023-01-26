#!/bin/bb

(ns gitlab-lint
  (:require
   [babashka.curl :as curl]
   [clojure.string :as str]
   [cheshire.core :as json]
   [babashka.fs :as fs]
   [clojure.java.shell :as shell]))

(def config-file (or (System/getenv "GITLAB_LINT_CONFIG_FILE") "config.edn"))

(when-not (fs/exists? config-file)
  (binding [*out* *err*]
    (println "I did not find a config.edn file.")
    (if (System/getenv "GITLAB_LINT_CONFIG_FILE")
      (println "I checked " (System/getenv "GITLAB_LINT_CONFIG_FILE") " but there was no file")
      (do
        (println "I checked " (str (fs/absolutize (fs/file "config.edn")))
                 " and there was  no file.")
        (println "You can also set GITLAB_LINT_CONFIG_FILE instead if you like. ")))
    (ex-info "Programmer was insufficiently polite" {:babashka/exit 1})))

(def config (read-string (slurp config-file)))

(def token
  (memoize
   (fn []
     (first
      (keep
       (fn [command]
         (let [out (-> (apply shell/sh command) :out str/trim)]
           (when-not (str/blank? out)
             out)))
       (config :token))))))

(def file (first *command-line-args*))
;; (def file "/home/benj/repos/UnityMethodPatcher/.gitlab-ci.yml")

(when-not (fs/exists? file)
  (binding [*out* *err*]
    (println "Give me as first arg is the gitlab yml you want to lint.")
    (throw (ex-info "Programmer was insufficiently polite" {:babashka/exit 1}))))

(when-not (token)
  (binding [*out* *err*]
    (println "You need to configure how I can get a gitlab token in " (str config-file))
    (throw (ex-info "Programmer was insufficiently polite" {:babashka/exit 1}))))

(let [resp (->>
            (curl/post
             "https://gitlab.botogames.com/api/v4/ci/lint"
             {:headers {"PRIVATE-TOKEN" (token)
                        "Content-Type" "application/json"}
              :as :json
              :body (json/encode {:content (slurp file)})})
            :body
            json/decode)]
  (when-let [errors (get resp "errors")]
    (run! println errors))
  (if-not (get resp "valid")
    (throw
     (ex-info "Yml invalid." {:babashka/exit 1}))
    (println "yml valid.")))

(comment
  {"valid" false, "errors" ["retry max must be less than or equal to 2"], "warnings" [], "includes" [], "status" "invalid"}
  
  ;; (def resp *1)
  )
