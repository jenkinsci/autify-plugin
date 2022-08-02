package io.jenkins.plugins.autify;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class AutifyWebBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    final String autifyUrl = "https://app.autify.com/projects/743/scenarios/91437";

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new AutifyWebBuilder(autifyUrl));
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new AutifyWebBuilder(autifyUrl), project.getBuildersList().get(0));
    }

    @Test
    public void testConfigRoundtripWait() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        AutifyWebBuilder builder = new AutifyWebBuilder(autifyUrl);
        builder.setWait(true);
        project.getBuildersList().add(builder);
        project = jenkins.configRoundtrip(project);

        AutifyWebBuilder lhs = new AutifyWebBuilder(autifyUrl);
        lhs.setWait(true);
        jenkins.assertEqualDataBoundBeans(lhs, project.getBuildersList().get(0));
    }

    @Test
    public void testBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        AutifyWebBuilder builder = new AutifyWebBuilder(autifyUrl);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(autifyUrl, build);
    }

    @Test
    public void testBuildWait() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        AutifyWebBuilder builder = new AutifyWebBuilder(autifyUrl);
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
                + "  autifyWeb '" + autifyUrl + "'\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        String expectedString = autifyUrl;
        jenkins.assertLogContains(expectedString, completedBuild);
    }

}