/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/

infra.ensureInNode('docker-windows') {
  stage("Install git-windows") {
    bat 'choco install git.portable -v -d -y -f'
    bat 'echo %PATH%'
    withEnv(["Path=C:\\ProgramData\\chocolatey\\lib\\git\\bin;${env.PATH}"]) {
      bat 'echo %PATH%'
    }
    bat 'bash.exe --version'
  }
}

buildPlugin(
  useContainerAgent: true,
  jdkVersions: [11],
  platforms: ['docker-windows']
)
