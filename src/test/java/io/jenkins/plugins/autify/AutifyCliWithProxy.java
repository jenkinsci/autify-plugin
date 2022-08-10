package io.jenkins.plugins.autify;

import java.util.Map;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;

public class AutifyCliWithProxy extends AutifyCli {

    private final String originalAutifyPath;

    public AutifyCliWithProxy(FilePath workspace, Launcher launcher, TaskListener listener) {
        super(workspace, launcher, listener);
        this.originalAutifyPath = autifyPath;
        this.autifyPath = "./node_modules/.bin/autify-with-proxy";
    }

    public int install() {
        runCommand("npm", "install", "@autifyhq/autify-cli-integration-test");
        return super.install();
    }

    protected Map<String, String> getEnvs() {
        Map<String, String> envs = super.getEnvs();
        envs.put("AUTIFY_CLI_PATH", originalAutifyPath);
        return envs;
    }

    public static class Factory extends AutifyCli.Factory {
        public AutifyCli get(FilePath workspace, Launcher launcher, TaskListener listener) {
            return new AutifyCliWithProxy(workspace, launcher, listener);
        }
    }
}
