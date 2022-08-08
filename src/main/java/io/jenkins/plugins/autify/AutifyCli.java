package io.jenkins.plugins.autify;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;

public class AutifyCli {

    private final FilePath workspace;
    private final Launcher launcher;
    private final PrintStream logger;
    protected String autifyPath = "./bin/autify";
    private String webAccessToken = "";
    private String mobileAccessToken = "";

    public AutifyCli(FilePath workspace, Launcher launcher, TaskListener listener) {
        this.workspace = workspace;
        this.launcher = launcher;
        this.logger = listener.getLogger();
    }

    public int install() {
        return runShellScript("install.sh");
    }

    public int webTestRun(String autifyUrl, boolean wait) {
        ArgumentListBuilder builder = autifyBuilder("web", "test", "run");
        builder.add(autifyUrl);
        if (wait) builder.add("--wait");
        return runCommand(builder);
    }

    public void webAuthLogin(String webAccessToken) {
        this.webAccessToken = webAccessToken;
    }

    public void mobileAuthLogin(String mobileAccessToken) {
        this.mobileAccessToken = mobileAccessToken;
    }

    private ArgumentListBuilder autifyBuilder(String... arguments) {
        ArgumentListBuilder builder = new ArgumentListBuilder(autifyPath);
        builder.add(arguments);
        return builder;
    }

    protected int runCommand(ArgumentListBuilder builder) {
        return runCommand(builder.toCommandArray());
    }

    protected int runCommand(String... command) {
        try {
            return launcher.launch()
                .pwd(workspace)
                .envs(
                    "PATH+WORKSPACE=" + workspace + "/bin",
                    "AUTIFY_WEB_ACCESS_TOKEN=" + webAccessToken,
                    "AUTIFY_MOBILE_ACCESS_TOKEN=" + mobileAccessToken,
                    "XDG_DATA_HOME=" + workspace + "/.config"
                )
                .stdout(logger)
                .stderr(logger)
                .cmds(command)
                .start()
                .join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(logger);
            return 1;
        }
    }

    protected int runShellScript(String scriptName) {
        URL scriptUrl = getClass().getResource("AutifyCli/" + scriptName);
        if (scriptUrl == null) {
            logger.println("Cannot find the script '" + scriptName + "'");
            return 1;
        }
        String scriptFile = scriptUrl.getPath();
        return runCommand("bash", "-xe", scriptFile);
    }

    public static class Factory {
        public AutifyCli get(FilePath workspace, Launcher launcher, TaskListener listener) {
            return new AutifyCli(workspace, launcher, listener);
        }
    }
}
