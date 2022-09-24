package io.jenkins.plugins.autify;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import io.jenkins.plugins.autify.model.UrlReplacement;

public class AutifyCli {

    static final String INSTALL_SCRIPT_URL = "https://autify-cli-assets.s3.amazonaws.com/autify-cli/channels/stable/install-cicd.bash";

    private final FilePath workspace;
    private final Launcher launcher;
    private final PrintStream logger;
    private final String autifyPath;
    private final String shellInstallerUrl;
    private String webAccessToken = "";
    private String mobileAccessToken = "";

    public AutifyCli(FilePath workspace, Launcher launcher, TaskListener listener, String autifyPath,
            String shellInstallerUrl) {
        this.workspace = workspace;
        this.launcher = launcher;
        this.logger = listener.getLogger();
        this.autifyPath = StringUtils.isEmpty(autifyPath) ? "autify" : autifyPath;
        this.shellInstallerUrl = StringUtils.isEmpty(shellInstallerUrl) ? INSTALL_SCRIPT_URL : shellInstallerUrl;
    }

    public int install() {
        URL url;
        try {
            url = new URL(shellInstallerUrl);
            InputStream scriptStream = url.openStream();
            return runBashScript(scriptStream);
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }
    }

    public int webTestRun(String autifyUrl, boolean wait, String timeout, List<UrlReplacement> urlReplacements,
            String testExecutionName, String browser, String device, String deviceType, String os, String osVersion,
            String autifyConnect) {
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
        builder.addFlag("--autify-connect", autifyConnect);
        return execute(builder);
    }

    public int mobileTestRun(String autifyUrl, String buildId, String buildPath, boolean wait, String timeout) {
        Builder builder = new Builder("mobile", "test", "run");
        builder.add(autifyUrl);
        if (StringUtils.isNotBlank(buildId) && StringUtils.isNotBlank(buildPath)) {
            logger.println("Cannot specify both buildId and buildPath.");
            return 1;
        } else if (StringUtils.isNotBlank(buildId)) {
            builder.addFlag("--build-id", buildId);
        } else if (StringUtils.isNotBlank(buildPath)) {
            builder.addFlag("--build-path", buildPath);
        } else {
            logger.println("Either buildId or buildPath is required.");
            return 1;
        }
        builder.addFlag("--wait", wait);
        builder.addFlag("--timeout", timeout);
        return execute(builder);
    }

    public int mobileBuildUpload(String workspaceId, String buildPath) {
        Builder builder = new Builder("mobile", "build", "upload");
        builder.add(buildPath);
        builder.addFlag("--workspace-id", workspaceId);
        return execute(builder);
    }

    public void webAuthLogin(String webAccessToken) {
        this.webAccessToken = webAccessToken;
    }

    public void mobileAuthLogin(String mobileAccessToken) {
        this.mobileAccessToken = mobileAccessToken;
    }

    private int runCommand(InputStream stdin, ArgumentListBuilder builder) {
        return runCommand(stdin, builder.toCommandArray());
    }

    private int runCommand(InputStream stdin, String... command) {
        try {
            ProcStarter procStarter = launcher.launch()
                    .pwd(workspace)
                    .envs(getEnvs())
                    .stdout(logger)
                    .stderr(logger)
                    .cmds(command);
            if (stdin != null) {
                procStarter = procStarter.stdin(stdin);
            }
            return procStarter.start().join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(logger);
            return 1;
        }
    }

    private Map<String, String> getEnvs() {
        Map<String, String> envs = new HashMap<>(System.getenv());
        envs.put("AUTIFY_PATH", autifyPath);
        envs.put("AUTIFY_CLI_INSTALL_USE_CACHE", "1");
        envs.put("AUTIFY_WEB_ACCESS_TOKEN", webAccessToken);
        envs.put("AUTIFY_MOBILE_ACCESS_TOKEN", mobileAccessToken);
        envs.put("XDG_CACHE_HOME", workspace + "/.cache");
        envs.put("XDG_CONFIG_HOME", workspace + "/.config");
        envs.put("XDG_DATA_HOME", workspace + "/.data");
        return envs;
    }

    private int execute(ArgumentListBuilder builder) {
        try {
            InputStream scriptStream = AutifyCli.class
                    .getResourceAsStream("/io/jenkins/plugins/autify/AutifyCli/execute.bash");
            int ret = runBashScript(scriptStream, builder);
            scriptStream.close();
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }
    }

    private int runBashScript(InputStream scriptStream) {
        return runBashScript(scriptStream, new ArgumentListBuilder());
    }

    private int runBashScript(InputStream scriptStream, ArgumentListBuilder builder) {
        try {
            int ret;
            if (SystemUtils.IS_OS_WINDOWS) {
                String command = "bash" + " -xe -s - " + builder.toString();
                ret = runCommand(scriptStream, "cmd.exe", "/C", command);
            } else {
                builder.prepend("bash", "-xe", "-s", "-");
                ret = runCommand(scriptStream, builder);
            }
            scriptStream.close();
            return ret;
        } catch (IOException e) {
            e.printStackTrace(logger);
            return 1;
        }
    }

    private static class Builder extends ArgumentListBuilder {

        public Builder(String... arguments) {
            super();
            this.add(arguments);
        }

        public Builder addFlag(String flag, String value) {
            if (StringUtils.isNotBlank(value))
                add(flag, value);
            return this;
        }

        public Builder addFlag(String flag, boolean value) {
            if (value)
                add(flag);
            return this;
        }

    }
}
