/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/

/*
Windows agents on Jenkins CI don't have Git Bash installed (Looks like MiGit only).
Thankfully, since C:\tools\git which is the default install directory of `choco install git.portable`
are already included in PATH (actually some of subdriectories), we don't have to worry about PATH modification.
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
