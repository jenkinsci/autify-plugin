package io.jenkins.plugins.autify;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.tasks.Shell;
import hudson.util.Secret;

import java.io.IOException;

import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;

public class AutifyMobileBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    final String autifyUrl = "https://mobile-app.autify.com/projects/AAA/test_plans/BBB";
    final String credentialsId = "autifyMobile";
    final String accessToken = "token";
    final String timeout = "10";
    final String stub = "foo";

    @Before
    public void setup() throws IOException {
        StringCredentials credentials = new StringCredentialsImpl(CredentialsScope.GLOBAL, credentialsId, "", Secret.fromString(accessToken));
        SystemCredentialsProvider.getInstance().getCredentials().add(credentials);
        AutifyMobileBuilder.setAutifyCliFactory(new AutifyCliStub.Factory());
    }

    @After
    public void tearDown() {
        AutifyMobileBuilder.resetAutifyCliFactory();
    }

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new AutifyMobileBuilder(credentialsId, autifyUrl));
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new AutifyMobileBuilder(credentialsId, autifyUrl), project.getBuildersList().get(0));
    }

    @Test
    public void testConfigRoundtripFull() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        AutifyMobileBuilder builder = new AutifyMobileBuilder(credentialsId, autifyUrl);
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
    public void testBuildWithBuildId() throws Exception {
        AutifyMobileBuilder.setAutifyCliFactory(new AutifyCliWithProxy.Factory());
        FreeStyleProject project = jenkins.createFreeStyleProject();
        AutifyMobileBuilder builder = new AutifyMobileBuilder(credentialsId, autifyUrl);
        builder.setBuildId(stub);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Executing script from " + AutifyCli.INSTALL_SCRIPT_URL, build);
        jenkins.assertLogContains("mobile test run " + autifyUrl + " --build-id " + stub + "\n", build);
    }

    @Test
    public void testBuildWithBuildIdWait() throws Exception {
        AutifyMobileBuilder.setAutifyCliFactory(new AutifyCliWithProxy.Factory());
        FreeStyleProject project = jenkins.createFreeStyleProject();
        AutifyMobileBuilder builder = new AutifyMobileBuilder(credentialsId, autifyUrl);
        builder.setBuildId(stub);
        builder.setWait(true);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Executing script from " + AutifyCli.INSTALL_SCRIPT_URL, build);
        jenkins.assertLogContains("mobile test run " + autifyUrl + " --build-id " + stub + " --wait\n", build);
    }

    @Test
    public void testBuildWithAndroidBuild() throws Exception {
        AutifyMobileBuilder.setAutifyCliFactory(new AutifyCliWithProxy.Factory());
        FreeStyleProject project = jenkins.createFreeStyleProject();
        String repositoryRoot = System.getProperty("user.dir");
        project.getBuildersList().add(new Shell("cp -pr " + repositoryRoot + "/android.apk ."));
        AutifyMobileBuilder builder = new AutifyMobileBuilder(credentialsId, autifyUrl);
        builder.setBuildPath("android.apk");
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Executing script from " + AutifyCli.INSTALL_SCRIPT_URL, build);
        jenkins.assertLogContains("mobile test run " + autifyUrl + " --build-path android.apk\n", build);
    }

    @Test
    public void testBuildWithIosBuild() throws Exception {
        AutifyMobileBuilder.setAutifyCliFactory(new AutifyCliWithProxy.Factory());
        FreeStyleProject project = jenkins.createFreeStyleProject();
        String repositoryRoot = System.getProperty("user.dir");
        project.getBuildersList().add(new Shell("cp -pr " + repositoryRoot + "/ios.app ."));
        AutifyMobileBuilder builder = new AutifyMobileBuilder(credentialsId, autifyUrl);
        builder.setBuildPath("ios.app");
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Executing script from " + AutifyCli.INSTALL_SCRIPT_URL, build);
        jenkins.assertLogContains("mobile test run " + autifyUrl + " --build-path ios.app\n", build);
    }

    @Test
    public void testScriptedPipelineWithBuildId() throws Exception {
        AutifyMobileBuilder.setAutifyCliFactory(new AutifyCliWithProxy.Factory());
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = "node {\n"
                + "  autifyMobile credentialsId: '" + credentialsId + "', autifyUrl: '"+ autifyUrl +"', buildId: '" + stub + "'\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains("Executing script from " + AutifyCli.INSTALL_SCRIPT_URL, completedBuild);
        jenkins.assertLogContains("mobile test run " + autifyUrl + " --build-id " + stub + "\n", completedBuild);
    }

}