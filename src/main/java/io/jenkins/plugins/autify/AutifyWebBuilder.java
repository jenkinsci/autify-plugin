package io.jenkins.plugins.autify;

import hudson.Launcher;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import io.jenkins.plugins.autify.model.UrlReplacement;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;

import javax.annotation.CheckForNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundSetter;

public class AutifyWebBuilder extends Builder implements SimpleBuildStep {

    private final String credentialsId;
    private final String autifyUrl;
    private boolean wait;
    private String timeout;
    private List<UrlReplacement> urlReplacements;
    private String testExecutionName;
    private String browser;
    private String device;
    private String deviceType;
    private String os;
    private String osVersion;
    private String autifyConnect;

    private static AutifyCli.Factory autifyCliFactory = new AutifyCli.Factory();
    public static void setAutifyCliFactory(AutifyCli.Factory factory) {
        autifyCliFactory = factory;
    }
    public static void resetAutifyCliFactory() {
        autifyCliFactory = new AutifyCli.Factory();
    }

    @DataBoundConstructor
    public AutifyWebBuilder(String credentialsId, String autifyUrl) {
        this.credentialsId = credentialsId;
        this.autifyUrl = autifyUrl;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getAutifyUrl() {
        return autifyUrl;
    }

    public boolean isWait() {
        return wait;
    }

    @DataBoundSetter
    public void setWait(boolean wait) {
        this.wait = wait;
    }

    public String getTimeout() {
        return StringUtils.trimToEmpty(timeout);
    }

    @DataBoundSetter
    public void setTimeout(@CheckForNull String value) {
        this.timeout = value;
    }

    public List<UrlReplacement> getUrlReplacements() {
        return urlReplacements == null ? Collections.emptyList() : urlReplacements;
    }

    @DataBoundSetter
    public void setUrlReplacements(@CheckForNull List<UrlReplacement> value) {
        this.urlReplacements = value;
    }

    public String getTestExecutionName() {
        return StringUtils.trimToEmpty(testExecutionName);
    }

    @DataBoundSetter
    public void setTestExecutionName(@CheckForNull String value) {
        this.testExecutionName = value;
    }

    public String getBrowser() {
        return StringUtils.trimToEmpty(browser);
    }

    @DataBoundSetter
    public void setBrowser(@CheckForNull String value) {
        this.browser = value;
    }

    public String getDevice() {
        return StringUtils.trimToEmpty(device);
    }

    @DataBoundSetter
    public void setDevice(@CheckForNull String value) {
        this.device = value;
    }

    public String getDeviceType() {
        return StringUtils.trimToEmpty(deviceType);
    }

    @DataBoundSetter
    public void setDeviceType(@CheckForNull String value) {
        this.deviceType = value;
    }

    public String getOs() {
        return StringUtils.trimToEmpty(os);
    }

    @DataBoundSetter
    public void setOs(@CheckForNull String value) {
        this.os = value;
    }

    public String getOsVersion() {
        return StringUtils.trimToEmpty(osVersion);
    }

    @DataBoundSetter
    public void setOsVersion(@CheckForNull String value) {
        this.osVersion = value;
    }

    public String getAutifyConnect() {
        return StringUtils.trimToEmpty(autifyConnect);
    }

    @DataBoundSetter
    public void setAutifyConnect(@CheckForNull String value) {
        this.autifyConnect = value;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        StringCredentials credentials = CredentialsProvider.findCredentialById(credentialsId, StringCredentials.class, run, Collections.emptyList());
        if (credentials == null) {
            listener.getLogger().println("Cannot find any credentials for "+ credentialsId);
            run.setResult(Result.FAILURE);
            return;
        }
        String webAccessToken = Secret.toString(credentials.getSecret());
        AutifyCli autifyCli = autifyCliFactory.get(workspace, launcher, listener);
        if (autifyCli.install() != 0) {
            listener.getLogger().println("Failed to install autify-cli");
            run.setResult(Result.FAILURE);
            return;
        }
        autifyCli.webAuthLogin(webAccessToken);
        if (autifyCli.webTestRun(autifyUrl, wait, timeout, urlReplacements, testExecutionName, browser, device, deviceType, os, osVersion, autifyConnect) != 0) {
            listener.getLogger().println("Failed to execute autify web test run");
            run.setResult(Result.FAILURE);
            return;
        }
    }

    @Symbol("autifyWeb")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private final static Pattern TEST_SCENARIO_URL_PATTERN = Pattern.compile("^https://app.autify.com/projects/\\d+/scenarios/\\d+/?$");
        private final static Pattern TEST_PLAN_URL_PATTERN = Pattern.compile("^https://app.autify.com/projects/\\d+/test_plans/\\d+/?$");

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String credentialsId) {
            StandardListBoxModel result = new StandardListBoxModel();
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId);
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(credentialsId);
                }
            }
            return result
            .includeEmptyValue()
            .includeMatchingAs(ACL.SYSTEM, item, StringCredentials.class, Collections.emptyList(), CredentialsMatchers.always())
            .includeCurrentValue(credentialsId);
        }

        public FormValidation doCheckCredentialsId(@AncestorInPath Item item, @QueryParameter String value) {
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return FormValidation.ok();
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return FormValidation.ok();
                }
            }
            if (StringUtils.isBlank(value)) {
                return FormValidation.error(Messages.AutifyWebBuilder_CannotBeEmpty());
            }
            if (CredentialsProvider.listCredentials(
                StringCredentials.class,
                item,
                ACL.SYSTEM,
                Collections.emptyList(),
                CredentialsMatchers.withId(value)
            ).isEmpty()) {
                return FormValidation.error(Messages.AutifyWebBuilder_CannotFindCurrentlySelectedCredentials());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckAutifyUrl(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error(Messages.AutifyWebBuilder_CannotBeEmpty());
            }
            if (!isTestScenarioUrl(value) && !isTestPlanUrl(value)) {
                return FormValidation.error(Messages.AutifyWebBuilder_InvalidUrl());
            }
            return FormValidation.ok();
        }

        private boolean isTestScenarioUrl(String value) {
            return TEST_SCENARIO_URL_PATTERN.matcher(value).find();
        }

        private boolean isTestPlanUrl(String value) {
            return TEST_PLAN_URL_PATTERN.matcher(value).find();
        }

        public FormValidation doCheckTimeout(@QueryParameter String value, @QueryParameter boolean wait) {
            if (StringUtils.isNotBlank(value) && !wait) {
                return FormValidation.warning(Messages.AutifyWebBuilder_NoEffectWhenWaitIsUnchecked());
            }
            return FormValidation.ok();
        }

        private FormValidation checkEffectiveOnlyForTestScenarioUrl(String value, String autifyUrl) {
            if (StringUtils.isNotBlank(value) && !isTestScenarioUrl(autifyUrl)) {
                return FormValidation.warning(Messages.AutifyWebBuilder_EffectiveOnlyForTestScenarioUrl());
            }
            return FormValidation.ok(Messages.AutifyWebBuilder_EffectiveOnlyForTestScenarioUrl());
        }

        public FormValidation doCheckTestExecutionName(@QueryParameter String value, @QueryParameter String autifyUrl) {
            return checkEffectiveOnlyForTestScenarioUrl(value, autifyUrl);
        }

        public FormValidation doCheckBrowser(@QueryParameter String value, @QueryParameter String autifyUrl) {
            return checkEffectiveOnlyForTestScenarioUrl(value, autifyUrl);
        }

        public FormValidation doCheckDevice(@QueryParameter String value, @QueryParameter String autifyUrl) {
            return checkEffectiveOnlyForTestScenarioUrl(value, autifyUrl);
        }

        public FormValidation doCheckDeviceType(@QueryParameter String value, @QueryParameter String autifyUrl) {
            return checkEffectiveOnlyForTestScenarioUrl(value, autifyUrl);
        }

        public FormValidation doCheckOs(@QueryParameter String value, @QueryParameter String autifyUrl) {
            return checkEffectiveOnlyForTestScenarioUrl(value, autifyUrl);
        }

        public FormValidation doCheckOsVersion(@QueryParameter String value, @QueryParameter String autifyUrl) {
            return checkEffectiveOnlyForTestScenarioUrl(value, autifyUrl);
        }

        public FormValidation doCheckAutifyConnect(@QueryParameter String value, @QueryParameter String autifyUrl) {
            return checkEffectiveOnlyForTestScenarioUrl(value, autifyUrl);
        }
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.AutifyWebBuilder_DescriptorImpl_DisplayName();
        }

    }

}
