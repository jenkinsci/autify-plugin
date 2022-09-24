package io.jenkins.plugins.autify;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.util.Secret;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;

public class AutifyMobileBuilderTest {

        @Rule
        public JenkinsRule jenkins = new JenkinsRule();

        final String rootDir = System.getProperty("user.dir");
        final String shellInstallerUrl;
        final String autifyUrl = "https://mobile-app.autify.com/projects/AAA/test_plans/BBB";
        final String credentialsId = "autifyMobileAccessToken";
        final String accessToken = "token";
        final String timeout = "10";
        final String stub = "foo";

        AutifyMobileBuilder builder;

        public AutifyMobileBuilderTest() throws MalformedURLException {
                shellInstallerUrl = new File(rootDir + "/src/test/resources/install-stub.bash").toURI().toURL()
                                .toString();
        }

        @Before
        public void setup() throws IOException {
                StringCredentials credentials = new StringCredentialsImpl(CredentialsScope.GLOBAL, credentialsId, "",
                                Secret.fromString(accessToken));
                SystemCredentialsProvider.getInstance().getCredentials().add(credentials);
                this.builder = new AutifyMobileBuilder(credentialsId, autifyUrl);
                this.builder.setShellInstallerUrl(shellInstallerUrl);
        }

        @Test
        public void testConfigRoundtrip() throws Exception {
                FreeStyleProject project = jenkins.createFreeStyleProject();
                project.getBuildersList().add(builder);
                project = jenkins.configRoundtrip(project);
                jenkins.assertEqualDataBoundBeans(new AutifyMobileBuilder(credentialsId, autifyUrl),
                                project.getBuildersList().get(0));
        }

        @Test
        public void testConfigRoundtripFull() throws Exception {
                FreeStyleProject project = jenkins.createFreeStyleProject();
                builder.setBuildId(stub);
                builder.setBuildPath(stub);
                builder.setWait(true);
                builder.setTimeout(timeout);
                project.getBuildersList().add(builder);
                project = jenkins.configRoundtrip(project);

                AutifyMobileBuilder lhs = new AutifyMobileBuilder(credentialsId, autifyUrl);
                lhs.setBuildId(stub);
                lhs.setBuildPath(stub);
                lhs.setWait(true);
                lhs.setTimeout(timeout);
                jenkins.assertEqualDataBoundBeans(lhs, project.getBuildersList().get(0));
        }

        @Test
        public void testBuild() throws Exception {
                FreeStyleProject project = jenkins.createFreeStyleProject();
                builder.setBuildId(stub);
                project.getBuildersList().add(builder);
                FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
                jenkins.assertLogContains("mobile test run " + autifyUrl + " --build-id " + stub + "\n",
                                build);
        }

        @Test
        public void testBuildOtherFields() throws Exception {
                FreeStyleProject project = jenkins.createFreeStyleProject();
                builder.setBuildPath(stub);
                builder.setWait(true);
                builder.setTimeout(timeout);
                project.getBuildersList().add(builder);
                FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
                jenkins.assertLogContains(
                                "mobile test run " + autifyUrl + " --build-path " + stub + " --wait --timeout "
                                                + timeout + "\n",
                                build);
        }

        @Test
        public void testScriptedPipeline() throws Exception {
                String agentLabel = "my-agent";
                jenkins.createOnlineSlave(Label.get(agentLabel));
                WorkflowJob job = jenkins.createProject(WorkflowJob.class);
                String pipelineScript = "node {\n"
                                + "autifyMobile"
                                + "  credentialsId: '" + credentialsId + "', "
                                + "  autifyUrl: '" + autifyUrl + "', "
                                + "  buildId: '" + stub + "', "
                                + "  shellInstallerUrl: '" + shellInstallerUrl + "'\n"
                                + "}";
                job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
                WorkflowRun build = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
                jenkins.assertLogContains("mobile test run " + autifyUrl + " --build-id " + stub + "\n", build);
        }

        @Test
        public void testScriptedPipelineWithOtherFields() throws Exception {
                String agentLabel = "my-agent";
                jenkins.createOnlineSlave(Label.get(agentLabel));
                WorkflowJob job = jenkins.createProject(WorkflowJob.class);
                String pipelineScript = "node {\n"
                                + "autifyMobile"
                                + "  credentialsId: '" + credentialsId + "', "
                                + "  autifyUrl: '" + autifyUrl + "', "
                                + "  buildPath: '" + stub + "', "
                                + "  wait: true, "
                                + "  timeout: '" + timeout + "', "
                                + "  shellInstallerUrl: '" + shellInstallerUrl + "'\n"
                                + "}";
                job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
                WorkflowRun build = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
                jenkins.assertLogContains(
                                "mobile test run " + autifyUrl + " --build-path " + stub + " --wait --timeout "
                                                + timeout + "\n",
                                build);
        }

}