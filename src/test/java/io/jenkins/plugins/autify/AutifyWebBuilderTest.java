package io.jenkins.plugins.autify;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.util.ArgumentListBuilder;
import hudson.util.Secret;
import io.jenkins.plugins.autify.model.UrlReplacement;

import java.util.Arrays;

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

public class AutifyWebBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    final String autifyUrl = "https://app.autify.com/projects/743/scenarios/91437";
    final String credentialsId = "autifyWeb";
    final String accessToken = "token";
    final String timeout = "10";
    final UrlReplacement urlReplacement = new UrlReplacement("https://foo.com", "https://bar.com");
    final String stub = "foo";
    final String webTestRunFullCommand = new ArgumentListBuilder("web", "test", "run")
        .add(autifyUrl)
        .add("--wait")
        .add("--timeout", timeout)
        .add("--url-replacements", urlReplacement.toCliString())
        .add("--name", stub)
        .add("--browser", stub)
        .add("--device", stub)
        .add("--device-type", stub)
        .add("--os", stub)
        .add("--os-version", stub)
        .toString() + "\n";

    @Before
    public void setup() {
        StringCredentials credentials = new StringCredentialsImpl(CredentialsScope.GLOBAL, credentialsId, "", Secret.fromString(accessToken));
        SystemCredentialsProvider.getInstance().getCredentials().add(credentials);
        AutifyWebBuilder.setAutifyCliFactory(new AutifyCliStub.Factory());
    }

    @After
    public void tearDown() {
        AutifyWebBuilder.resetAutifyCliFactory();
    }

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new AutifyWebBuilder(credentialsId, autifyUrl));
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new AutifyWebBuilder(credentialsId, autifyUrl), project.getBuildersList().get(0));
    }

    @Test
    public void testConfigRoundtripFull() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        AutifyWebBuilder builder = new AutifyWebBuilder(credentialsId, autifyUrl);
        builder.setWait(true);
        builder.setTimeout(timeout);
        builder.setUrlReplacements(Arrays.asList(urlReplacement));
        builder.setTestExecutionName(stub);
        builder.setBrowser(stub);
        builder.setDevice(stub);
        builder.setDeviceType(stub);
        builder.setOs(stub);
        builder.setOsVersion(stub);
        project.getBuildersList().add(builder);
        project = jenkins.configRoundtrip(project);

        AutifyWebBuilder lhs = new AutifyWebBuilder(credentialsId, autifyUrl);
        lhs.setWait(true);
        lhs.setTimeout(timeout);
        lhs.setUrlReplacements(Arrays.asList(urlReplacement));
        lhs.setTestExecutionName(stub);
        lhs.setBrowser(stub);
        lhs.setDevice(stub);
        lhs.setDeviceType(stub);
        lhs.setOs(stub);
        lhs.setOsVersion(stub);
        jenkins.assertEqualDataBoundBeans(lhs, project.getBuildersList().get(0));
    }

    @Test
    public void testBuild() throws Exception {
        AutifyWebBuilder.setAutifyCliFactory(new AutifyCliWithProxy.Factory());
        FreeStyleProject project = jenkins.createFreeStyleProject();
        AutifyWebBuilder builder = new AutifyWebBuilder(credentialsId, autifyUrl);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Executing script from " + AutifyCli.INSTALL_SCRIPT_URL, build);
        jenkins.assertLogContains("web test run " + autifyUrl + "\n", build);
    }

    @Test
    public void testBuildWait() throws Exception {
        AutifyWebBuilder.setAutifyCliFactory(new AutifyCliWithProxy.Factory());
        FreeStyleProject project = jenkins.createFreeStyleProject();
        AutifyWebBuilder builder = new AutifyWebBuilder(credentialsId, autifyUrl);
        builder.setWait(true);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Executing script from " + AutifyCli.INSTALL_SCRIPT_URL, build);
        jenkins.assertLogContains("web test run " + autifyUrl + " --wait\n", build);
    }

    @Test
    public void testBuildAllFields() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        AutifyWebBuilder builder = new AutifyWebBuilder(credentialsId, autifyUrl);
        builder.setWait(true);
        builder.setTimeout(timeout);
        builder.setUrlReplacements(Arrays.asList(urlReplacement));
        builder.setTestExecutionName(stub);
        builder.setBrowser(stub);
        builder.setDevice(stub);
        builder.setDeviceType(stub);
        builder.setOs(stub);
        builder.setOsVersion(stub);
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Executing script from " + AutifyCli.INSTALL_SCRIPT_URL, build);
        jenkins.assertLogContains(webTestRunFullCommand, build);
    }

    @Test
    public void testScriptedPipeline() throws Exception {
        AutifyWebBuilder.setAutifyCliFactory(new AutifyCliWithProxy.Factory());
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = "node {\n"
                + "  autifyWeb credentialsId: '" + credentialsId + "', autifyUrl: '"+ autifyUrl +"'\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains("Executing script from " + AutifyCli.INSTALL_SCRIPT_URL, completedBuild);
        jenkins.assertLogContains("web test run " + autifyUrl + "\n", completedBuild);
    }

    @Test
    public void testScriptedPipelineAllField() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = "node {\n"
                + "  autifyWeb "
                + "credentialsId: '" + credentialsId + "', "
                + "autifyUrl: '"+ autifyUrl +"', "
                + "wait: true, "
                + "timeout: '"+ timeout +"', "
                + "urlReplacements: [[patternUrl: '"+ urlReplacement.getPatternUrl() + "', replacementUrl: '"+ urlReplacement.getReplacementUrl() +"']], "
                + "testExecutionName: '"+ stub +"', "
                + "browser: '"+ stub +"', "
                + "device: '"+ stub +"', "
                + "deviceType: '"+ stub +"', "
                + "os: '"+ stub +"', "
                + "osVersion: '"+ stub +"'\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains("Executing script from " + AutifyCli.INSTALL_SCRIPT_URL, completedBuild);
        jenkins.assertLogContains(webTestRunFullCommand, completedBuild);
    }

}