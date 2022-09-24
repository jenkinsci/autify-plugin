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

public class AutifyMobileUploadBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    final String rootDir = System.getProperty("user.dir");
    final String shellInstallerUrl;
    final String credentialsId = "autifyMobileAccessToken";
    final String accessToken = "token";
    final String workspaceId = "AAA";
    final String buildPath = "./android.apk";
    final String stub = "foo";

    AutifyMobileUploadBuilder builder;

    public AutifyMobileUploadBuilderTest() throws MalformedURLException {
        shellInstallerUrl = new File(rootDir + "/src/test/resources/install-stub.bash").toURI().toURL().toString();
    }

    @Before
    public void setup() throws IOException {
        StringCredentials credentials = new StringCredentialsImpl(CredentialsScope.GLOBAL, credentialsId, "",
                Secret.fromString(accessToken));
        SystemCredentialsProvider.getInstance().getCredentials().add(credentials);
        this.builder = new AutifyMobileUploadBuilder(credentialsId, workspaceId, buildPath);
        this.builder.setShellInstallerUrl(shellInstallerUrl);
    }

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(builder);
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new AutifyMobileUploadBuilder(credentialsId, workspaceId, buildPath),
                project.getBuildersList().get(0));
    }

    @Test
    public void testBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(builder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(
                "mobile build upload " + buildPath + " --workspace-id " + workspaceId + "\n", build);
    }

    @Test
    public void testScriptedPipeline() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        String pipelineScript = "node {\n"
                + "autifyMobileUpload"
                + "  credentialsId: '" + credentialsId + "', "
                + "  workspaceId: '" + workspaceId + "', "
                + "  buildPath: '" + buildPath + "', "
                + "  shellInstallerUrl: '" + shellInstallerUrl + "'\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun build = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains(
                "mobile build upload " + buildPath + " --workspace-id " + workspaceId + "\n", build);
    }

}
