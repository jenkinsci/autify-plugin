/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/

/*
infra.ensureInNode('docker-windows') {
  stage("Install Git Bash") {
    bat 'choco install git.portable -v -d -y -f'
    bat 'bash.exe --version'
  }

  stage("Install Node.js") {
    bat 'choco install nodejs-lts -v -d -y -f'
    bat 'dir "C:\\Program Files"'
    bat 'dir "C:\\Program Files (x86)"'
  }

  withEnv(["Path=C:\\Program Files\\nodejs;${env.PATH}"]) {
    buildPlugin(
      useContainerAgent: true,
      jdkVersions: [11],
      platforms: ['docker-windows']
    )
  }
}
*/

buildPlugin(
  useContainerAgent: true,
  jdkVersions: [11],
  platforms: ['linux', 'docker-windows']
)
