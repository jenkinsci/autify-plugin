/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/

infra.ensureInNode('docker-windows') {
  stage("Install git-windows") {
    bat 'choco install git -v -d -y -f --params="/GitAndUnixToolsOnPath" --install-arguments="/DIR=C:\\tools\\git"'
    bat 'SET'
    bat '"C:\\tools\\git\\bin\\bash.exe" --version'
    bat 'bash.exe --version'
  }
}

buildPlugin(
  useContainerAgent: true,
  jdkVersions: [11],
  platforms: ['docker-windows']
)
