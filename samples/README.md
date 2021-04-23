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
  - Creates Virtual Hosts (needs truststores and keyRef's setup using ManagementUI)
  - Creates API products
  - Creates Developers
  - Creates Developer Apps
  - Creates Companies

To use, edit samples/EdgeConfig/shared-pom.xml, and update org and env elements in all profiles to point to your Apigee org, env. You can add more profiles corresponding to each env in your org.

      <apigee.org>myorg</apigee.org>
      <apigee.env>test</apigee.env>

To run the plugin and use edge.json jump to samples project `cd /samples/EdgeConfig` and run 

`mvn install -Ptest -Dusername=<your-apigee-username> -Dpassword=<your-apigee-password> -Dapigee.config.options=create`

To run the plugin and use a config file similar to edge.json in any directory jump to samples project `cd /samples/EdgeConfig` and run 

`mvn install -Ptest -Dusername=<your-apigee-username> -Dpassword=<your-apigee-password> -Dapigee.config.file=<path-to-config-file> -Dapigee.config.options=create`

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
  - Creates Virtual Hosts (needs truststores and keyRef's setup using ManagementUI)
  - Deploy API  (from deploy plugin)
  - Creates API products
  - Creates Developers
  - Creates Developer Apps
  - Creates Companies

To use, edit samples/APIandConfig/shared-pom.xml, and update org and env elements in all profiles to point to your Apigee org, env. You can add more profiles corresponding to each env in your org.

      <apigee.org>myorg</apigee.org>
      <apigee.env>test</apigee.env>

To run jump to samples project `cd /samples/APIandConfig/HelloWorld` and run 

`mvn install -Ptest -Dusername=<your-apigee-username> -Dpassword=<your-apigee-password> -Dapigee.config.options=create`
