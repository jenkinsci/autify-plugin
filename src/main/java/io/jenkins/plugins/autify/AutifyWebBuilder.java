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

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;

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

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        StringCredentials credentials = CredentialsProvider.findCredentialById(
            credentialsId,
            StringCredentials.class,
            run,
            Collections.emptyList()
        );
        if (credentials == null) {
            listener.getLogger().println("Cannot find any credentials for "+ credentialsId);
            run.setResult(Result.FAILURE);
            return;
        }
        String secretText = Secret.toString(credentials.getSecret());
        listener.getLogger().println(secretText);
        if (wait) {
            listener.getLogger().println(autifyUrl + "wait");
        } else {
            listener.getLogger().println(autifyUrl);
        }
    }

    @Symbol("autifyWeb")
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
                return FormValidation.error("Cannot be empty");
            }
            if (CredentialsProvider.listCredentials(
                StringCredentials.class,
                item,
                ACL.SYSTEM,
                Collections.emptyList(),
                CredentialsMatchers.withId(value)
            ).isEmpty()) {
                return FormValidation.error("Cannot find currently selected credentials");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckAutifyUrl(@QueryParameter String value, @QueryParameter boolean wait) throws IOException, ServletException {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Cannot be empty");
            }
            return FormValidation.ok();
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
