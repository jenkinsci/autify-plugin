package io.jenkins.plugins.autify.model;

import org.apache.commons.validator.routines.UrlValidator;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.jenkins.plugins.autify.Messages;
import hudson.Util;

public class UrlReplacement extends AbstractDescribableImpl<UrlReplacement> {

    private final String patternUrl;
    private final String replacementUrl;

    @DataBoundConstructor
    public UrlReplacement(final String patternUrl, final String replacementUrl) {
        this.patternUrl = Util.fixEmptyAndTrim(patternUrl);
        this.replacementUrl = Util.fixEmptyAndTrim(replacementUrl);
    }

    public String getPatternUrl() {
        return patternUrl;
    }

    public String getReplacementUrl() {
        return replacementUrl;
    }

    public String toCliString() {
        if (patternUrl == null || replacementUrl == null) return null;
        else return patternUrl + " " + replacementUrl;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<UrlReplacement> {

        private final UrlValidator urlValidator = new UrlValidator(new String[]{"http", "https"}, UrlValidator.ALLOW_LOCAL_URLS);

        public FormValidation doCheckPatternUrl(@QueryParameter String value, @QueryParameter String replacementUrl) {
            if (Util.fixEmptyAndTrim(value) == null) {
                return FormValidation.error(Messages.AutifyWebBuilder_CannotBeEmpty());
            }
            if (Util.fixEmptyAndTrim(replacementUrl) != null && value.equals(replacementUrl)) {
                return FormValidation.error(Messages.AutifyWebBuilder_CannotSetTheSameUrl());
            }
            if (!urlValidator.isValid(value)) {
                return FormValidation.warning(Messages.AutifyWebBuilder_InvalidUrl());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckReplacementUrl(@QueryParameter String value, @QueryParameter String patternUrl) {
            if (Util.fixEmptyAndTrim(value) == null) {
                return FormValidation.error(Messages.AutifyWebBuilder_CannotBeEmpty());
            }
            if (Util.fixEmptyAndTrim(patternUrl) != null && value.equals(patternUrl)) {
                return FormValidation.error(Messages.AutifyWebBuilder_CannotSetTheSameUrl());
            }
            if (!urlValidator.isValid(value)) {
                return FormValidation.warning(Messages.AutifyWebBuilder_InvalidUrl());
            }
            return FormValidation.ok();
        }

        @Override
        public String getDisplayName() {
            return "UrlReplacement";
        }

    }

}
