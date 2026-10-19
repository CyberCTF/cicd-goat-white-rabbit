# Upstream

This lab reproduces one challenge — **White Rabbit** — from CI/CD Goat.

| What | Source | Licence |
| --- | --- | --- |
| The "White Rabbit" challenge (Direct Poisoned Pipeline Execution against a Jenkins pipeline) | [CI/CD Goat](https://github.com/cider-security-research/cicd-goat) by Cider Security (now Palo Alto Networks), v1.2.7 | Apache-2.0 |
| Security class | [OWASP CICD-SEC-4: Poisoned Pipeline Execution](https://owasp.org/www-project-top-10-ci-cd-security-risks/CICD-SEC-04-Poisoned-Pipeline-Execution) | — |

Only the *vulnerability* is reproduced, not upstream's eight-machine CI/CD stack. The lab is two
machines — a Jenkins controller and a Gitea git host — which is the minimum this attack needs: a
pipeline that builds a repository on push, and a Global-scope credential any pipeline can read.

Nothing is vendored from upstream. Upstream serves a copy of the `urllib3` project (MIT) as the
`white-rabbit` repository and bakes one fixed UUID flag into Jenkins. This lab instead ships an
**original** minimal sample package (`build/gitea/repo-src/`, authored here, no third-party code)
with an original `Jenkinsfile`, and injects a **per-player** flag into the Jenkins credential store
at run time, so each player exfiltrates their own flag and nothing but the challenge idea comes from
upstream.

## One faithful deviation (the trigger)

Upstream wires the trigger with the Jenkins **Gitea** plugin: a Gitea SCM source discovers *pull
requests* (`PR-*`) and a periodic folder trigger rescans every minute. This lab uses the Jenkins
**git** plugin's multibranch source with *branch* discovery and the same one-minute periodic rescan
(an SCM poll, no inbound webhook). The attack and the player's steps are identical — clone, poison
the `Jenkinsfile`, push a branch and open a pull request — but the build is triggered by the pushed
branch rather than by the PR object. This drops a Jenkins↔Gitea plugin version dependency for a
version-robust git poll, which matters for a lab family meant to run unattended. The solver still
opens the pull request, so the player-facing flow matches upstream.

## A note on masking (the exfiltration encoding)

Upstream's solution prints `echo $flag1 | base64`, relying on base64 slipping past credential
masking. On the modern Jenkins this lab ships, the `credentials-binding` plugin masks a bound
secret's raw value **and its plain base64**, so that exact trick no longer works. The lab keeps the
vulnerability identical (a Global-scope credential any pipeline can bind) and only updates the
encoding the attacker uses to one the masker does not precompute — reversing before base64
(`echo $flag1 | rev | base64`). This is arguably a truer lesson for current CI: masking is a
best-effort guardrail, not a control, and a chosen transform defeats it.
