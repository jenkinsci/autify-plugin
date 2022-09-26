/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/
infra.ensureInNode('docker-windows') {
  stage("Install git-windows") {
    bat 'curl -L -o .\\PortableGit.7z.exe https://github.com/git-for-windows/git/releases/download/v2.37.3.windows.1/PortableGit-2.37.3-64-bit.7z.exe'
    bat '.\\PortableGit.7z.exe -y'
    bat 'SETX /M PATH \"%cd%\\PortableGit\\bin;%PATH%\"'
  }
  stage("Verify bash.exe") {
    bat 'ECHO %PATH%'
    bat 'bash.exe --version'
  }
}

buildPlugin(
  useContainerAgent: true,
  jdkVersions: [11],
  platforms: ['docker-windows']
)
