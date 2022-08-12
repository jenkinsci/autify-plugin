package io.jenkins.plugins.autify;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;

public class AutifyCliStub extends AutifyCli {

    public AutifyCliStub(FilePath workspace, Launcher launcher, TaskListener listener) {
        super(workspace, launcher, listener);
        this.autifyPath = "echo";
    }

    public int install() {
        logger.println("Executing script from " + INSTALL_SCRIPT_URL);
        return 0;
    }

    public static class Factory extends AutifyCli.Factory {
        public AutifyCli get(FilePath workspace, Launcher launcher, TaskListener listener) {
            return new AutifyCliStub(workspace, launcher, listener);
        }
    }
}
