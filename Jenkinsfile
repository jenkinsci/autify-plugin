/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/

infra.ensureInNode('docker-windows') {
  bat 'choco install git.portable -v -d -y -f'
  bat 'bash.exe --version'
}

buildPlugin(
  useContainerAgent: true,
  jdkVersions: [11, 17],
  platforms: ['linux', 'docker-windows']
)
