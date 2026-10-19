// Seed job (run by Configuration-as-Code) that creates Wonderland's white-rabbit pipeline.
//
// A multibranch Pipeline backed by the Gitea repository over git. Branch discovery means every
// branch that carries a Jenkinsfile gets its own pipeline, built automatically when indexing finds
// it; a periodic rescan (every minute) is the SCM-poll trigger, so a pushed branch / opened PR runs
// without any inbound webhook. This is what makes Direct-PPE possible: whoever can push a branch
// controls the Jenkinsfile that the controller then executes.
multibranchPipelineJob('wonderland-white-rabbit') {
    displayName('Wonderland » white-rabbit')
    branchSources {
        branchSource {
            source {
                git {
                    id('wonderland-white-rabbit')
                    remote('http://gitea:3000/Wonderland/white-rabbit.git')
                    credentialsId('gitea-jenkins')
                    traits {
                        gitBranchDiscovery()
                    }
                }
            }
        }
    }
    factory {
        workflowBranchProjectFactory {
            scriptPath('Jenkinsfile')
        }
    }
    orphanedItemStrategy {
        discardOldItems {
            numToKeep(20)
        }
    }
    triggers {
        periodicFolderTrigger {
            interval('1m')
        }
    }
}
