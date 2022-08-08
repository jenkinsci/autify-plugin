package io.jenkins.plugins.autify;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;

public class AutifyCliWithProxy extends AutifyCli {

    public AutifyCliWithProxy(FilePath workspace, Launcher launcher, TaskListener listener) {
        super(workspace, launcher, listener);
        this.autifyPath = "./node_modules/.bin/autify-with-proxy";
    }

    public int install() {
        runCommand("npm", "install", "@autifyhq/autify-cli-integration-test");
        return super.install();
    }

    public static class Factory extends AutifyCli.Factory {
        public AutifyCli get(FilePath workspace, Launcher launcher, TaskListener listener) {
            return new AutifyCliWithProxy(workspace, launcher, listener);
        }
    }
}
