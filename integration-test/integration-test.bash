#!/bin/bash
set -ex

SCRIPT_DIR="$(dirname -- "${BASH_SOURCE[0]}")"
ROOT_DIR=$(dirname "$SCRIPT_DIR");
TARGET_TMP_DIR="$ROOT_DIR/target/tmp"

export JENKINS_HOME="$TARGET_TMP_DIR/jenkins_home"
export PATH="$ROOT_DIR/node:$PATH"
export AUTIFY_CLI_INSTALL_USE_CACHE=1
export AUTIFY_CLI_INTEGRATION_TEST_INSTALL=1
export AUTIFY_TEST_WAIT_INTERVAL_SECOND=0
export AUTIFY_CONNECT_CLIENT_MODE=fake

JENKINS_PLUGINS_DIR="$JENKINS_HOME/plugins"
JENKINS_WAR_URL="https://get.jenkins.io/war-stable/2.361.1/jenkins.war"
JENKINS_PLUGIN_CLI_JAR_URL="https://github.com/jenkinsci/plugin-installation-manager-tool/releases/download/2.12.9/jenkins-plugin-manager-2.12.9.jar"

JENKINS_PID=
JENKINS_PASSWORD=

function download() {
  local file="$1"
  local url="$2"
  if ! [ -f "$file" ]; then
    curl -L -o "$file" "$url"
  fi
}

function exit-handler() {
  kill "$JENKINS_PID"
}
trap exit-handler EXIT

function jenkins-plugin-cli() {
  java -jar "$TARGET_TMP_DIR/jenkins-plugin-manager.jar" --verbose --war jenkins.war --plugin-download-directory "$JENKINS_PLUGINS_DIR" "$@"
}

function jenkins-cli() {
  java -jar "$TARGET_TMP_DIR/jenkins-cli.jar" -s http://localhost:8080 -auth admin:"$JENKINS_PASSWORD" "$@"
}

function wait-jenkins() {
  while ! curl -s -f -o /dev/null "http://localhost:8080/login"; do
    sleep 1
  done
}

function setup() {
  local autify_hpi=$1
  download "$TARGET_TMP_DIR/jenkins.war" "$JENKINS_WAR_URL"
  download "$TARGET_TMP_DIR/jenkins-plugin-manager.jar" "$JENKINS_PLUGIN_CLI_JAR_URL"
  jenkins-plugin-cli --plugins structs credentials plain-credentials workflow-aggregator
  if [ -n "$autify_hpi" ]; then
    cp "$autify_hpi" "$JENKINS_PLUGINS_DIR/autify.jpi"
  else
    jenkins-plugin-cli --plugins autify
  fi
  java -jar "$TARGET_TMP_DIR/jenkins.war" &
  JENKINS_PID=$!
  wait-jenkins
  JENKINS_PASSWORD=$(cat "$JENKINS_HOME/secrets/initialAdminPassword" | tr -d '\n' | tr -d '\r')
  download "$TARGET_TMP_DIR/jenkins-cli.jar" "http://localhost:8080/jnlpJars/jenkins-cli.jar"
  jenkins-cli import-credentials-as-xml system::system::jenkins < "$SCRIPT_DIR/credentials.xml"
}

function test-job() {
  local job=$1
  jenkins-cli delete-job integration-test || true
  sed "s|<!--SHELL_INSTALLER_URL-->|$INPUT_SHELL_INSTALLER_URL|g" "$SCRIPT_DIR/jobs/$job.xml" | \
  jenkins-cli create-job integration-test
  jenkins-cli build integration-test -s -v
}

function main() {
  local autify_hpi=$1
  setup "$autify_hpi"

  test-job autifyWeb
  test-job autifyWebPipeline
  test-job generateMobileFakeApp
  test-job autifyMobileUpload
  test-job autifyMobileUploadPipeline
  test-job autifyMobile
  test-job autifyMobilePipeline
}

main "$@"
