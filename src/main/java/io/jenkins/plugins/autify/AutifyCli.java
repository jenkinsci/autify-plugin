package io.jenkins.plugins.autify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.module.ModuleDescriptor.Version;

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
    private final String userAgentSuffix;
    private String webAccessToken = "";
    private String mobileAccessToken = "";

    public AutifyCli(FilePath workspace, Launcher launcher, TaskListener listener, String autifyPath,
            String shellInstallerUrl, String userAgentSuffix) {
        this.workspace = workspace;
        this.launcher = launcher;
        this.logger = listener.getLogger();
        this.autifyPath = StringUtils.isEmpty(autifyPath) ? "autify" : autifyPath;
        this.shellInstallerUrl = StringUtils.isEmpty(shellInstallerUrl) ? INSTALL_SCRIPT_URL : shellInstallerUrl;
        this.userAgentSuffix = StringUtils.trimToEmpty(userAgentSuffix);
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
            String autifyConnect, boolean autifyConnectClient, String autifyConnectClientExtraArguments) {
        Builder builder = new Builder("web", "test", "run");
        builder.add(autifyUrl);
        builder.addFlag("--wait", wait);
        builder.addFlag("--timeout", timeout);
        if (urlReplacements != null) {
            Version version = getVersion();
            if (version == null) {
                return 1;
            }
            for (UrlReplacement urlReplacement : urlReplacements) {
                builder.addFlag("--url-replacements", urlReplacement.toCliString(version));
            }
        }
        builder.addFlag("--name", testExecutionName);
        builder.addFlag("--browser", browser);
        builder.addFlag("--device", device);
        builder.addFlag("--device-type", deviceType);
        builder.addFlag("--os", os);
        builder.addFlag("--os-version", osVersion);
        builder.addFlag("--autify-connect", autifyConnect);
        if (autifyConnectClient) {
            if (!wait) {
                logger.println("Wait option must be set when running with Autify Connect Client.");
                return 1;
            }
            if (execute(new Builder("connect", "client", "install")) != 0) {
                logger.println("Failed to install Autify Connect Client.");
                return 1;
            }
            builder.addFlag("--autify-connect-client", autifyConnectClient);
            builder.addFlag("--autify-connect-client-extra-arguments", autifyConnectClientExtraArguments);
        }
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

    public Version getVersion() {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        int result = execute(new Builder("--version"), stdout);
        if (result == 0) {
            Pattern pattern = Pattern.compile("@autifyhq/autify-cli/(.*?) ");
            String stdoutStr = stdout.toString(Charset.defaultCharset());
            Matcher matcher = pattern.matcher(stdoutStr);
            if (matcher.find()) {
                return Version.parse(matcher.group(1));
            } else {
                logger.println(String.format("Failed to parse `%s` as version.", result));

                return null;
            }
        } else {
            logger.println("Failed to execute --version command.");

            return null;
        }
    }

    private int runCommand(InputStream stdin, OutputStream stdout, ArgumentListBuilder builder) {
        return runCommand(stdin, stdout, builder.toCommandArray());
    }

    private int runCommand(InputStream stdin, OutputStream stdout, String... command) {
        try {
            ProcStarter procStarter = launcher.launch()
                    .pwd(workspace)
                    .envs(getEnvs())
                    .stdout(stdout)
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
        envs.putIfAbsent("AUTIFY_CLI_USER_AGENT_SUFFIX", userAgentSuffix);
        envs.put("AUTIFY_CLI_INSTALL_USE_CACHE", "1");
        envs.put("AUTIFY_WEB_ACCESS_TOKEN", webAccessToken);
        envs.put("AUTIFY_MOBILE_ACCESS_TOKEN", mobileAccessToken);
        envs.put("XDG_CACHE_HOME", workspace + "/.cache");
        envs.put("XDG_CONFIG_HOME", workspace + "/.config");
        envs.put("XDG_DATA_HOME", workspace + "/.data");
        return envs;
    }

    private int execute(ArgumentListBuilder builder) {
        return execute(builder, logger);
    }

    private int execute(ArgumentListBuilder builder, OutputStream stdout) {
        try {
            InputStream scriptStream = AutifyCli.class
                    .getResourceAsStream("/io/jenkins/plugins/autify/AutifyCli/execute.bash");
            int ret = runBashScript(scriptStream, stdout, builder);
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
        return runBashScript(scriptStream, logger, builder);
    }

    private int runBashScript(InputStream scriptStream, OutputStream stdout, ArgumentListBuilder builder) {
        try {
            int ret;
            if (SystemUtils.IS_OS_WINDOWS) {
                // GitHub Actions windows runner can't recognize `bash` for some reasons.
                // This is a workaround to call `bash` by passing the command to `cmd.exe`.
                String command = "bash -xe -s - " + builder.toString();
                ret = runCommand(scriptStream, stdout, "cmd.exe", "/C", command);
            } else {
                builder.prepend("bash", "-xe", "-s", "-");
                ret = runCommand(scriptStream, stdout, builder);
            }
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
