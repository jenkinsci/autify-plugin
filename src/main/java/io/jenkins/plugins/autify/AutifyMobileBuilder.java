package io.jenkins.plugins.autify;

import hudson.Launcher;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
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
import java.util.regex.Pattern;

import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundSetter;

public class AutifyMobileBuilder extends Builder implements SimpleBuildStep {

    private final String credentialsId;
    private final String autifyUrl;
    private String buildId;
    private String buildPath;
    private boolean wait;
    private String timeout;
    private String autifyPath;
    private String shellInstallerUrl;

    @DataBoundConstructor
    public AutifyMobileBuilder(String credentialsId, String autifyUrl) {
        this.credentialsId = credentialsId;
        this.autifyUrl = autifyUrl;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getAutifyUrl() {
        return autifyUrl;
    }

    public String getBuildId() {
        return StringUtils.trimToEmpty(buildId);
    }

    @DataBoundSetter
    public void setBuildId(String value) {
        this.buildId = value;
    }

    public String getBuildPath() {
        return StringUtils.trimToEmpty(buildPath);
    }

    @DataBoundSetter
    public void setBuildPath(String value) {
        this.buildPath = value;
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

    public String getAutifyPath() {
        return StringUtils.trimToEmpty(autifyPath);
    }

    @DataBoundSetter
    public void setAutifyPath(@CheckForNull String value) {
        this.autifyPath = value;
    }

    public String getShellInstallerUrl() {
        return StringUtils.trimToEmpty(shellInstallerUrl);
    }

    @DataBoundSetter
    public void setShellInstallerUrl(@CheckForNull String value) {
        this.shellInstallerUrl = value;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        StringCredentials credentials = CredentialsProvider.findCredentialById(credentialsId, StringCredentials.class,
                run, Collections.emptyList());
        if (credentials == null) {
            listener.getLogger().println("Cannot find any credentials for " + credentialsId);
            run.setResult(Result.FAILURE);
            return;
        }
        String mobileAccessToken = Secret.toString(credentials.getSecret());
        AutifyCli autifyCli = new AutifyCli(workspace, launcher, listener, autifyPath, shellInstallerUrl);
        if (autifyCli.install() != 0) {
            listener.getLogger().println("Failed to install autify-cli");
            run.setResult(Result.FAILURE);
            return;
        }
        autifyCli.mobileAuthLogin(mobileAccessToken);
        if (autifyCli.mobileTestRun(autifyUrl, buildId, buildPath, wait, timeout) != 0) {
            listener.getLogger().println("Failed to execute autify mobile test run");
            run.setResult(Result.FAILURE);
            return;
        }
    }

    @Symbol("autifyMobile")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private final static Pattern TEST_PLAN_URL_PATTERN = Pattern
                .compile("^https://mobile-app.autify.com/projects/[^/]+/test_plans/[^/]+/?$");

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
                    .includeMatchingAs(ACL.SYSTEM, item, StringCredentials.class, Collections.emptyList(),
                            CredentialsMatchers.always())
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
                return FormValidation.error(Messages.AutifyMobileBuilder_CannotBeEmpty());
            }
            if (CredentialsProvider.listCredentials(
                    StringCredentials.class,
                    item,
                    ACL.SYSTEM,
                    Collections.emptyList(),
                    CredentialsMatchers.withId(value)).isEmpty()) {
                return FormValidation.error(Messages.AutifyMobileBuilder_CannotFindCurrentlySelectedCredentials());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckAutifyUrl(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error(Messages.AutifyMobileBuilder_CannotBeEmpty());
            }
            if (!isTestPlanUrl(value)) {
                return FormValidation.error(Messages.AutifyMobileBuilder_InvalidUrl());
            }
            return FormValidation.ok();
        }

        private boolean isTestPlanUrl(String value) {
            return TEST_PLAN_URL_PATTERN.matcher(value).find();
        }

        public FormValidation doCheckBuildId(@QueryParameter String value, @QueryParameter String buildPath) {
            if (StringUtils.isNotBlank(value) && StringUtils.isNotBlank(buildPath)) {
                return FormValidation.error(Messages.AutifyMobileBuilder_CannotSpecifyBothBuildIdAndBuildPath());
            }
            if (StringUtils.isBlank(value) && StringUtils.isBlank(buildPath)) {
                return FormValidation.error(Messages.AutifyMobileBuilder_SpecifyEitherBuildIdOrBuildPath());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckBuildPath(@QueryParameter String value, @QueryParameter String buildId) {
            if (StringUtils.isNotBlank(value) && StringUtils.isNotBlank(buildId)) {
                return FormValidation.error(Messages.AutifyMobileBuilder_CannotSpecifyBothBuildIdAndBuildPath());
            }
            if (StringUtils.isBlank(value) && StringUtils.isBlank(buildId)) {
                return FormValidation.error(Messages.AutifyMobileBuilder_SpecifyEitherBuildIdOrBuildPath());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckTimeout(@QueryParameter String value, @QueryParameter boolean wait) {
            if (StringUtils.isNotBlank(value) && !wait) {
                return FormValidation.warning(Messages.AutifyMobileBuilder_NoEffectWhenWaitIsUnchecked());
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.AutifyMobileBuilder_DescriptorImpl_DisplayName();
        }

    }

}
