# Autify Plugin

![logo_Autify_yoko_c_RGB](https://user-images.githubusercontent.com/37822/183773852-ef120bb6-7c8d-42ca-831d-72ecc0e91c98.jpg)

## Introduction
This plugin allows you to integrate your [Autify](https://autify.com/) end-to-end tests with Jenkins.
You can simply plugin a build step for Autify after your software deployment step
and ensure your new deployment passes the end-to-end tests you recorded before.

## Getting started

### Prerequisites

First, we assume you already have Jenkins Project or Pipeline to deploy your software to somewhere e.g. staging, production. We'll add an Autify test step right after the deployment step to confirm the newly deployed software doesn't break the end-to-end experience.

Secondly, you need to create a test plan or test scenario on [Autify for Web](https://app.autify.com). The test scenario should be recorded against your target website where the software is deployed by your Jenkins. Please note the URL of your test plan or scenario e.g. `https://app.autify.com/projects/00/scenarios/000` and we'll use it later.

You also need to [create a personal access token](https://help.autify.com/docs/integrate-with-api#issue-a-personal-access-token) of Autify for Web. Please note the generated token somewhere so that we can store the value on Jenkins Secrets later.

*Note: Since the personal access token is associated with a single user, we recommend you to create a machine user in your organization and use its personal access token for CI/CD integration.*

Lastly, we currently only support Jenkins running on Linux or macOS platform. Windows is planned.

### Install plugin

Just install "Autify" plugin to your Jenkins. It will also install [Credentials](https://plugins.jenkins.io/credentials/) and [Plain Credentials](https://plugins.jenkins.io/plain-credentials/) plugins if not exist.

### Store personal access token on Jenkins Secrets

Go to "Manage Jenkins" > "Security | Manage Credentials" and you'll see the page like below:

![Screen Shot 2022-08-08 at 5 02 31 PM](https://user-images.githubusercontent.com/37822/183771797-0069b1ba-7a29-4c9b-b6d9-bc99a3914e02.png)


Click "(global)" domain of Jenkins store (or any appropriate domain if your Jenkins has something already). Then, click "Add Credentials" on the left pane, select "Secret text" for "Kind", input the generated personal access token to "Secret" and name some "ID" e.g. `autifyWebAccessToken`. Lastly, click "Create":

![Screen Shot 2022-08-08 at 5 05 04 PM](https://user-images.githubusercontent.com/37822/183771825-6af387cf-fd89-4734-84d2-22f1457081af.png)

### Add a build step to run Autify for Web (Jenkins Project)

Finally, let's add a new step to your existing Jenkins Project.

Go to configuration of your Project and click "Add build step" and select "Run test on Autify for Web":

![Screen Shot 2022-08-09 at 3 21 56 PM](https://user-images.githubusercontent.com/37822/183772297-59fa0e28-256f-4268-aefb-c25b72b825b1.png)

Then, select `autifyWebAccessToken` from the select box of "Autify Personal Access Token" and paste the URL e.g. `https://app.autify.com/projects/00/scenarios/000` to "Autify test URL":

![Screen Shot 2022-08-09 at 3 23 17 PM](https://user-images.githubusercontent.com/37822/183772258-2f294ff2-dddd-492a-bf23-8ab0cd4f5f64.png)

Note: Check "Wait?" if you want to wait until the test finishes and succeed only if it passes.

Then, put the Autify step right after your deployment step and click "Save".

### Add a build step to run Autify for Web (Jenkins Pipeline)

Finally, let's add a new step to your existing Jenkins Pipeline.

Open your `Jenkinsfile` or Pipeline configuration page and insert the step below right after your deployment step. 

Simply start a test and finish the step (no waiting for the finish of the test):
```groovy
autifyWeb credentialsId: 'autifyWebAccessToken', autifyUrl: 'https://app.autify.com/projects/00/scenarios/000'
```

Start a test and wait until the test finishes or timed out:
```groovy
autifyWeb credentialsId: 'autifyWebAccessToken', autifyUrl: 'https://app.autify.com/projects/00/scenarios/000', wait: true
```

## Issues

Report issues and enhancements in the [GitHub Issues](https://github.com/jenkinsci/autify-plugin/issues).

## Contributing

Refer to our [contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md)

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)
