package io.jenkins.plugins.autify;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;

public class AutifyCli {

    protected final FilePath workspace;
    protected final Launcher launcher;
    protected final PrintStream logger;
    protected String autifyPath = "./autify/bin/autify";
    private String webAccessToken = "";
    private String mobileAccessToken = "";

    public AutifyCli(FilePath workspace, Launcher launcher, TaskListener listener) {
        this.workspace = workspace;
        this.launcher = launcher;
        this.logger = listener.getLogger();
    }

    public int install() {
        return runShellScript("https://autify-cli-assets.s3.amazonaws.com/autify-cli/channels/stable/install-cicd.bash");
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
        return runCommand(null, command);
    }

    protected int runCommand(InputStream input, String... command) {
        try {
            ProcStarter procStarter = launcher.launch()
                .pwd(workspace)
                .envs(getEnvs())
                .stdout(logger)
                .stderr(logger)
                .cmds(command);
            if (input != null) {
                procStarter = procStarter.stdin(input);
            }
            return procStarter.start().join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(logger);
            return 1;
        }
    }

    protected Map<String, String> getEnvs() {
        Map<String, String> envs = new HashMap<String, String>();
        envs.put("AUTIFY_WEB_ACCESS_TOKEN", webAccessToken);
        envs.put("AUTIFY_MOBILE_ACCESS_TOKEN", mobileAccessToken);
        envs.put("XDG_DATA_HOME", workspace + "/.config");
        return envs;
    }

    protected int runShellScript(String scriptUrl) {
        try {
            URL url = new URL(scriptUrl);
            InputStream scriptStream = url.openStream();
            logger.println("Executing script from " + scriptUrl);
            int ret = runCommand(scriptStream, "bash", "-xe");
            scriptStream.close();
            return ret;
        } catch (IOException e) {
            e.printStackTrace(logger);
            return 1;
        }
    }

    public static class Factory {
        public AutifyCli get(FilePath workspace, Launcher launcher, TaskListener listener) {
            return new AutifyCli(workspace, launcher, listener);
        }
    }
}
