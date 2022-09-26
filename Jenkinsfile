/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/

infra.ensureInNode('docker-windows') {
  stage("Install git-windows") {
    bat 'choco install git -v -d -y -f --params="/GitAndUnixToolsOnPath"'
    withEnv(["Path=C:\\Program Files\\Git\\bin\\;${env.PATH}"]) {
      bat 'echo %Path%'
      bat 'echo %PATH%'
    }
    bat '"C:\\Program Files\\Git\\bin\\bash.exe" --version'
    bat 'bash.exe --version'
  }
}

infra.ensureInNode('linux') {
  stage("Install git-windows") {
    withEnv(["FOO=bar"]) {
      sh 'env | sort'
    }
  }
}

buildPlugin(
  useContainerAgent: true,
  jdkVersions: [11],
  platforms: ['linux', 'docker-windows']
)
