package io.jenkins.plugins.autify;

import hudson.Launcher;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;

public class AutifyWebBuilder extends Builder implements SimpleBuildStep {

    private final String autifyUrl;
    private boolean wait;

    @DataBoundConstructor
    public AutifyWebBuilder(String autifyUrl) {
        this.autifyUrl = autifyUrl;
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
        if (wait) {
            listener.getLogger().println(autifyUrl + "wait");
        } else {
            listener.getLogger().println(autifyUrl);
        }
    }

    @Symbol("autifyWeb")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckAutifyUrl(@QueryParameter String value, @QueryParameter boolean wait) throws IOException, ServletException {
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
