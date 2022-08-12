package io.jenkins.plugins.autify;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import io.jenkins.plugins.autify.model.UrlReplacement;

public class AutifyCli {

    public static final String INSTALL_SCRIPT_URL = "https://autify-cli-assets.s3.amazonaws.com/autify-cli/channels/stable/install-cicd.bash";

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
        return runShellScript(INSTALL_SCRIPT_URL);
    }

    public int webTestRun(String autifyUrl, boolean wait, String timeout, List<UrlReplacement> urlReplacements, String testExecutionName, String browser, String device, String deviceType, String os, String osVersion) {
        Builder builder = new Builder("web", "test", "run");
        builder.add(autifyUrl);
        builder.addFlag("--wait", wait);
        builder.addFlag("--timeout", timeout);
        if (urlReplacements != null) {
            for (UrlReplacement urlReplacement : urlReplacements) {
                builder.addFlag("--url-replacements", urlReplacement.toCliString());
            }
        }
        builder.addFlag("--name", testExecutionName);
        builder.addFlag("--browser", browser);
        builder.addFlag("--device", device);
        builder.addFlag("--device-type", deviceType);
        builder.addFlag("--os", os);
        builder.addFlag("--os-version", osVersion);
        return runCommand(builder);
    }

    public void webAuthLogin(String webAccessToken) {
        this.webAccessToken = webAccessToken;
    }

    public void mobileAuthLogin(String mobileAccessToken) {
        this.mobileAccessToken = mobileAccessToken;
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

    private class Builder extends ArgumentListBuilder {

        public Builder(String... arguments) {
            super(autifyPath);
            this.add(arguments);
        }

        public Builder addFlag(String flag, String value) {
            if (StringUtils.isNotBlank(value)) add(flag, value);
            return this;
        }

        public Builder addFlag(String flag, boolean value) {
            if (value) add(flag);
            return this;
        }

    }

    public static class Factory {
        public AutifyCli get(FilePath workspace, Launcher launcher, TaskListener listener) {
            return new AutifyCli(workspace, launcher, listener);
        }
    }
}
