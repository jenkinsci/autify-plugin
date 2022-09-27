/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/

/*
  Windows agents on Jenkins CI don't have Git Bash installed and they have MinGit only:
  https://github.com/jenkins-infra/packer-images/blob/088eb6eb7f7b37e037a14f8221ed555778559273/provisioning/windows-provision.ps1#L148-L162
  Therefore, we need to install it on-demand here.
  Thankfully, since C:\tools\git which is the default install directory of `choco install git.portable`
  are already included in PATH (actually some of subdriectories), we don't have to worry about PATH modification.
*/
infra.ensureInNode('docker-windows') {
  stage('Install Git for Bash') {
    bat 'choco install git.portable -v -d -y -f'
    bat 'bash.exe --version'
  }
}

buildPlugin(
  configurations: [
    [ platform: 'linux', jdk: '11' ],
    [ platform: 'linux', jdk: '17' ],
    [ platform: 'docker-windows', jdk: '11' ],
  ]
)
