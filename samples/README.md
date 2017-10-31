# Samples
Two sample projects are provided here.
 * EdgeConfig - create and manage config for Edge
 * APIandConfig - create and manage config alongwith API deployment

## EdgeConfig
```
/samples/EdgeConfig
```

This project demonstrates the creation and management of Apigee Edge Config and performs the following steps in sequence.
  - Creates Caches
  - Creates Target servers
  - Creates API products
  - Creates Developers
  - Creates Developer Apps

To use, edit samples/EdgeConfig/shared-pom.xml, and update org and env elements in all profiles to point to your Apigee org, env. You can add more profiles corresponding to each env in your org.

      <apigee.org>myorg</apigee.org>
      <apigee.env>test</apigee.env>

To run the plugin and use edge.json jump to samples project `cd /samples/EdgeConfig` and run 

`mvn install -Ptest -Dusername=<your-apigee-username> -Dpassword=<your-apigee-password> -Dapigee.config.options=create`

To run the plugin and use the multi-file format jump to samples project `cd /samples/EdgeConfig` and run 

`mvn install -Ptest -Dusername=<your-apigee-username> -Dpassword=<your-apigee-password> -Dapigee.config.options=create -Dapigee.config.dir=resources/edge`

## APIandConfig

Create config and deploy API
```
/samples/APIandConfig/HelloWorld
```

This project demonstrates use of apigee-config-maven-plugin and [apigee-deploy-maven-plugin](https://github.com/apigee/apigee-deploy-maven-plugin) to create API as well as manage config. The example project performs the following steps in sequence. This sequence is inherent to the platform and is managed using the sequencing of goals in pom.xml
  - Creates Caches
  - Creates Target servers
  - Deploy API  (from deploy plugin)
  - Creates API products
  - Creates Developers
  - Creates Developer Apps

To use, edit samples/APIandConfig/shared-pom.xml, and update org and env elements in all profiles to point to your Apigee org, env. You can add more profiles corresponding to each env in your org.

      <apigee.org>myorg</apigee.org>
      <apigee.env>test</apigee.env>

To run jump to samples project `cd /samples/APIandConfig/HelloWorld` and run 

`mvn install -Ptest -Dusername=<your-apigee-username> -Dpassword=<your-apigee-password> -Dapigee.config.options=create`

## DevPortal

Create models and import OpenAPI specs to a developer portal instance
```
/samples/DevPortal
```

This project demonstrates use of apigee-config-maven-plugin and [apigee-deploy-maven-plugin](https://github.com/apigee/apigee-deploy-maven-plugin) to create models and import OpenAPI specs to a developer portal. The example project performs a data import defined in pom.xml

Note that to utilize this example, you will need a working developer portal instance with the [smartdocs_service module](https://github.com/apigeecs/smartdocs_service) installed and enabled.

To use, edit samples/DevPortal/pom.xml, and update portal values.

      <portal.username>${pusername}</portal.username><!-- Username for the developer portal. -->
      <portal.password>${ppassword}</portal.password><!-- Password for the developer portal. -->
      <portal.directory>${pdirectory}</portal.directory><!-- Directory whered OpenAPI specs are accessible. -->
      <portal.url>${purl}</portal.url><!-- URL of the developer portal. -->
      <portal.path>${ppath}</portal.path><!-- Servies path defined in the developer portal. -->
      <portal.format>json</portal.format><!-- Format of the OpenAPI specs. -->

To run jump to samples project `cd /samples/DevPortal` and run 

`mvn install -Pdev -Dapigee.config.options=create`