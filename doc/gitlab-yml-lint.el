
(defvar gitlab-yml-lint-path
  "~/repos/gitlab-ci-lint/gitlab_lint.clj")

(defvar gitlab-yml-lint-dir
  "~/repos/gitlab-ci-lint/")

(defun gitlab-yml-lint ()
  (if-let (file (buffer-file-name))
      (let ((process-environment (append
                                  process-environment
                                  (list
                                   (concat
                                    "GITLAB_LINT_CONFIG_FILE="
                                    (expand-file-name
                                     "config.edn"
                                     gitlab-yml-lint-dir)))) ))
        (async-shell-command
         (concat gitlab-yml-lint-path " " (expand-file-name file))))
    (user-error "Your buffer is not visiting a file.")))


