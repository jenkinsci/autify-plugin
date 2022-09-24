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
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;

import java.io.IOException;
import java.util.Collections;

import javax.annotation.CheckForNull;

import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

public class AutifyMobileUploadBuilder extends Builder implements SimpleBuildStep {

    private final String credentialsId;
    private final String workspaceId;
    private final String buildPath;
    private String autifyPath;
    private String shellInstallerUrl;

    @DataBoundConstructor
    public AutifyMobileUploadBuilder(String credentialsId, String workspaceId, String buildPath) {
        this.credentialsId = credentialsId;
        this.workspaceId = workspaceId;
        this.buildPath = buildPath;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getWorkspaceId() {
        return StringUtils.trimToEmpty(workspaceId);
    }

    public String getBuildPath() {
        return StringUtils.trimToEmpty(buildPath);
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
        if (autifyCli.mobileBuildUpload(workspaceId, buildPath) != 0) {
            listener.getLogger().println("Failed to execute autify mobile build upload");
            run.setResult(Result.FAILURE);
            return;
        }
    }

    @Symbol("autifyMobileUpload")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

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
                return FormValidation.error(Messages.AutifyMobileUploadBuilder_CannotBeEmpty());
            }
            if (CredentialsProvider.listCredentials(
                    StringCredentials.class,
                    item,
                    ACL.SYSTEM,
                    Collections.emptyList(),
                    CredentialsMatchers.withId(value)).isEmpty()) {
                return FormValidation
                        .error(Messages.AutifyMobileUploadBuilder_CannotFindCurrentlySelectedCredentials());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckWorkspaceId(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error(Messages.AutifyMobileUploadBuilder_CannotBeEmpty());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckBuildPath(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error(Messages.AutifyMobileUploadBuilder_CannotBeEmpty());
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.AutifyMobileUploadBuilder_DescriptorImpl_DisplayName();
        }

    }

}
