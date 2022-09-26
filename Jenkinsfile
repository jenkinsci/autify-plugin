/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/
infra.ensureInNode('docker-windows') {
  stage("Install git-windows") {
    environment {
      PATH = ".\\PortableGit\\bin;${env.PATH}"
    }
    bat 'curl -L -o .\\PortableGit.7z.exe https://github.com/git-for-windows/git/releases/download/v2.37.3.windows.1/PortableGit-2.37.3-64-bit.7z.exe'
    bat '.\\PortableGit.7z.exe -y'
    bat '%cd%\\PortableGit\\bin\\bash.exe --version'
    bat 'SETX /M PATH \"%cd%\\PortableGit\\bin;%PATH%\"'
    bat 'SETX PATH \"%cd%\\PortableGit\\bin;%PATH%\"'
    bat 'SET PATH=\"%cd%\\PortableGit\\bin;%PATH%\"'
    bat 'ECHO %PATH%'
  }
  stage("Verify bash.exe") {
    environment {
      PATH = ".\\PortableGit\\bin;${env.PATH}"
    }
    bat 'ECHO %PATH%'
    bat '%cd%\\PortableGit\\bin\\bash.exe --version'
    bat 'bash.exe --version'
  }
}

buildPlugin(
  useContainerAgent: true,
  jdkVersions: [11],
  platforms: ['docker-windows']
)
