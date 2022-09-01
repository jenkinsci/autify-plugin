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

public class AutifyMobileUploadBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    final String credentialsId = "autifyMobile";
    final String accessToken = "token";
    final String workspaceId = "AAA";
    final String androidBuildPath = "./android.apk";
    final String iosBuildPath = "./ios.app";
    final String stub = "foo";

    @Before
    public void setup() throws IOException {
        StringCredentials credentials = new StringCredentialsImpl(CredentialsScope.GLOBAL, credentialsId, "", Secret.fromString(accessToken));
        SystemCredentialsProvider.getInstance().getCredentials().add(credentials);
        AutifyMobileUploadBuilder.setAutifyCliFactory(new AutifyCliStub.Factory());
    }

    @After
    public void tearDown() {
        AutifyMobileUploadBuilder.resetAutifyCliFactory();
    }

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new AutifyMobileUploadBuilder(credentialsId, workspaceId, androidBuildPath));
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new AutifyMobileUploadBuilder(credentialsId, workspaceId, androidBuildPath), project.getBuildersList().get(0));
    }

    @Test
    public void testBuildWithAndroid() throws Exception {
        AutifyMobileUploadBuilder.setAutifyCliFactory(new AutifyCliWithProxy.Factory());
        FreeStyleProject project = jenkins.createFreeStyleProject();
        String repositoryRoot = System.getProperty("user.dir");
        project.getBuildersList().add(new Shell("cp -pr " + repositoryRoot + "/android.apk ."));
        AutifyMobileUploadBuilder builder = new AutifyMobileUploadBuilder(credentialsId, workspaceId, androidBuildPath);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Executing script from " + AutifyCli.INSTALL_SCRIPT_URL, build);
        jenkins.assertLogContains("mobile build upload " + androidBuildPath + " --workspace-id " + workspaceId + "\n", build);
    }

    @Test
    public void testBuildWithIos() throws Exception {
        AutifyMobileUploadBuilder.setAutifyCliFactory(new AutifyCliWithProxy.Factory());
        FreeStyleProject project = jenkins.createFreeStyleProject();
        String repositoryRoot = System.getProperty("user.dir");
        project.getBuildersList().add(new Shell("cp -pr " + repositoryRoot + "/ios.app ."));
        AutifyMobileUploadBuilder builder = new AutifyMobileUploadBuilder(credentialsId, workspaceId, iosBuildPath);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Executing script from " + AutifyCli.INSTALL_SCRIPT_URL, build);
        jenkins.assertLogContains("mobile build upload " + iosBuildPath + " --workspace-id " + workspaceId + "\n", build);
    }

    @Test
    public void testScriptedPipeline() throws Exception {
        AutifyMobileUploadBuilder.setAutifyCliFactory(new AutifyCliWithProxy.Factory());
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String repositoryRoot = System.getProperty("user.dir");
        String pipelineScript
                = "node {\n"
                + "  sh 'cp -pr " + repositoryRoot + "/android.apk .'\n"
                + "  autifyMobileUpload credentialsId: '" + credentialsId + "', workspaceId: '"+ workspaceId +"', buildPath: '" + androidBuildPath + "'\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains("Executing script from " + AutifyCli.INSTALL_SCRIPT_URL, completedBuild);
        jenkins.assertLogContains("mobile build upload " + androidBuildPath + " --workspace-id " + workspaceId + "\n", completedBuild);
    }

}
