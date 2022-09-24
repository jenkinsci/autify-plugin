/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/
buildPlugin(
  useContainerAgent: true,
  jdkVersions: [11, 17],
  platforms: ['linux', 'docker-windows']
)

infra.ensureInNode('docker-windows') {
  //dir('.\\target\\tmp') {
    // sh 'curl -L -o .\PortableGit.7z.exe https://github.com/git-for-windows/git/releases/download/v2.37.3.windows.1/PortableGit-2.37.3-64-bit.7z.exe'
    //sh '.\PortableGit.7z.exe -y'
    //sh 'SETX /M PATH "%cd%\PortableGit\bin;%PATH%"'
  //}
  sh 'choco install git'
}