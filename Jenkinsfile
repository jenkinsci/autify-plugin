/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/

buildPlugin(
  configurations: [
    [ platform: 'linux', jdk: '11' ],
    [ platform: 'linux', jdk: '17' ],
    // Windows agent doesn't have bash...
    // [ platform: 'docker-windows', jdk: '11' ],
  ]
)
