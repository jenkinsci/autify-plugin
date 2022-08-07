package io.jenkins.plugins.autify;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.util.Secret;

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

public class AutifyWebBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    final String autifyUrl = "https://app.autify.com/projects/743/scenarios/91437";
    final String credentialsId = "autifyWeb";
    final String accessToken = "token";


    @Before
    public void setup() {
        StringCredentials credentials = new StringCredentialsImpl(CredentialsScope.GLOBAL, credentialsId, "", Secret.fromString(accessToken));
        SystemCredentialsProvider.getInstance().getCredentials().add(credentials);
    }

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new AutifyWebBuilder(credentialsId, autifyUrl));
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new AutifyWebBuilder(credentialsId, autifyUrl), project.getBuildersList().get(0));
    }

    @Test
    public void testConfigRoundtripWait() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        AutifyWebBuilder builder = new AutifyWebBuilder(credentialsId, autifyUrl);
        builder.setWait(true);
        project.getBuildersList().add(builder);
        project = jenkins.configRoundtrip(project);

        AutifyWebBuilder lhs = new AutifyWebBuilder(credentialsId, autifyUrl);
        lhs.setWait(true);
        jenkins.assertEqualDataBoundBeans(lhs, project.getBuildersList().get(0));
    }

    @Test
    public void testBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        AutifyWebBuilder builder = new AutifyWebBuilder(credentialsId, autifyUrl);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(autifyUrl, build);
    }

    @Test
    public void testBuildWait() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        AutifyWebBuilder builder = new AutifyWebBuilder(credentialsId, autifyUrl);
        builder.setWait(true);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(autifyUrl, build);
        jenkins.assertLogContains("wait", build);
    }

    @Test
    public void testScriptedPipeline() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = "node {\n"
                + "  autifyWeb credentialsId: '" + credentialsId + "', autifyUrl: '"+ autifyUrl +"'\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        String expectedString = autifyUrl;
        jenkins.assertLogContains(expectedString, completedBuild);
    }

}