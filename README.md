# apigee-config-maven-plugin (Apigee X or Apigee hybrid)


Maven plugin to create, manage Apigee config like KVM, Target Server, Resource Files, API Products, Developers, Developer Apps, Flow hooks and many more .

Help API teams follow API development best practices with Apigee.
  * Track Apigee Config (KVM, target servers, etc.) in source control
  * Deploy config changes along with the API in a CI pipeline
  * Simplify, automate config management during API development
  * Track config changes in prod environment as releases

Small API projects can use the single file format in edge.json to manage their config. Large, complex projects with several config entities can use the multi-file format to organize config in source control. Checkout samples for examples.

This plugin is available in public maven repo and can be used just by referring to it in pom.xml. This github repo is the plugin source code and unless you make changes to the code you do not have to build this repo to use the plugin. Read this document further for plugin usage instructions.

**NOTE:** Log4J libraries are upgraded to v2.17.1 in v2.2.2

## Prerequisites
You will need the following to run the samples:
- Apigee Edge developer account (in an Apigee hybrid org)
- [Java SDK >= 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
- [Maven 3.x](https://maven.apache.org/)

## Plugin Usage

### NOTE ###
- If you want to use this plugin for Apigee Edge SaaS or Private Cloud OPDK, please refer to this [link](https://github.com/apigee/apigee-config-maven-plugin/tree/1.x). You should be using the version 1.x
```xml
<dependency>
  <groupId>com.apigee.edge.config</groupId>
  <artifactId>apigee-config-maven-plugin</artifactId>
  <version>1.x</version>
</dependency>
```
- For Apigee X or Apigee Hybrid, the version of the plugin is 2.x
```xml
<dependency>
  <groupId>com.apigee.edge.config</groupId>
  <artifactId>apigee-config-maven-plugin</artifactId>
  <version>2.x</version>
</dependency>
```

```
mvn install -Ptest -Dapigee.config.options=create

  # Options

  -P<profile>
    Pick a profile in the parent pom.xml (shared-pom.xml in the example).
    Apigee org and env information comes from the profile.

  -Dapigee.config.options
    none   - No action (default)
    create - Create when not found. Pre-existing config is NOT updated even if it is different.
    update - Update when found; create when not found, updates individual entries for kvms. Refreshes all config to reflect edge.json.
    delete - Delete all config listed in edge.json.
    sync   - Delete and recreate.

  -Dapigee.config.file=<path-to-config>
     path containing the configuration.
  
  -Dapigee.config.dir=<dir>
     directory containing multi-file format config files.
     
  -Dapigee.config.exportDir=<dir>
     dir where the dev app keys are exported. This is only used for `exportAppKeys` goal. The file name is always devAppKeys.json
 
   -Dapigee.kvm.override=true|false
     is used only when -Dapigee.config.options=update. It overrides the KVM entries even if there are no changes. Default is true. If set to false, it skips the entry update.

  # Individual goals
  You can also work with an individual config type using the 
  corresponding goal directly. The goals available are,
 
  keyvaluemaps           #v2.4.0 or later supports entries    
  targetservers
  resourcefiles
  flowhooks
  apiproducts
  developers
  apps
  exportAppKeys			
  reports			
  references
  keystores
  aliases
  importKeys             #v2.1.3 or later
  apicategories          #v2.6.0 or later
  apidocs                #v2.6.0 or later
  appgroups              #v2.9.0 or later
  appgroupapps           #v2.9.0 or later
  importAppGroupAppKeys	 #v2.9.0 or later
  exportAppGroupAppKeys	 #v2.9.0 or later
  spaces				 #v2.9.3 or later

  For example, the apps goal is used below to only create apps and ignore all other config types.
  mvn apigee-config:apps -Ptest -Dapigee.config.options=create
  
  To export the dev app keys, use the following:
  mvn apigee-config:exportAppKeys -Ptest -Dapigee.config.exportDir=./target  
  
  To export the app group app keys, use the following:
  mvn apigee-config:exportAppGroupAppKeys -Ptest -Dapigee.config.exportDir=./target
  
```
The default "none" action is a NO-OP and it helps deploy APIs (using [apigee-deploy-maven-plugin](https://github.com/apigee/apigee-deploy-maven-plugin)) without affecting config.

## To configure a proxy 
Supported from v2.5.0

Please refer to this [doc](http://maven.apache.org/guides/mini/guide-proxies.html) that explains how to setup proxy settings in your settings.xml usually in your $HOME/.m2 directory. Only `https` protocol is supported

## Sample project
Refer to an example project at [/samples/EdgeConfig](./samples/EdgeConfig)

This project demonstrates the creation and management of Apigee Edge Config and performs the following steps in sequence.
  - Creates Target servers
  - Creates KVM
  - Creates Resource File
  - Creates API products
  - Creates Developers
  - Creates Developer Apps
  - Creates Custom Reports
  - Creates References
  - Creates Keystores
  - Creates Aliases
  - Creates Spaces
  and many more

To use, edit samples/EdgeConfig/shared-pom.xml, and update org, env to point to your Apigee org and env respectively and path of service account credentials elements in all profiles. You can add more profiles corresponding to each env in your org. 

      <apigee.org>myorg</apigee.org>
      <apigee.env>test</apigee.env>
      <apigee.serviceaccount.file>/dir/sa.json</apigee.serviceaccount.file>

To run the plugin and use edge.json jump to samples project `cd /samples/EdgeConfig` and run 

`mvn install -Ptest -Dusername=<your-apigee-username> -Dpassword=<your-apigee-password> -Dapigee.config.options=create`

To run the plugin and use a config file similar to edge.json in any directory jump to samples project `cd /samples/EdgeConfig` and run 

`mvn install -Ptest -Dusername=<your-apigee-username> -Dpassword=<your-apigee-password> -Dapigee.config.file=<path-to-config-file> -Dapigee.config.options=create`

To run the plugin and use the multi-file config format jump to samples project `cd /samples/EdgeConfig` and run 

`mvn install -Ptest -Dusername=<your-apigee-username> -Dpassword=<your-apigee-password> -Dapigee.config.options=create -Dapigee.config.dir=resources/edge`

Refer to [samples/APIandConfig/HelloWorld](./samples/APIandConfig/HelloWorld) for config management along with API deployment using [apigee-deploy-maven-plugin](https://github.com/apigee/apigee-deploy-maven-plugin). More info at [samples/README.md](./samples/README.md)

### Update Developer App, Group App and Company App (from v2.9.0 onwards)

When `apigee.config.options=update` is run on apps and if the payload passed includes the apiProducts, the Management server created a new credentials. To avoid this you can pass `-Dapigee.app.ignoreAPIProducts=true`. Please note this is applicable only for `apigee.config.options=update`. For more info on this, check [this](https://github.com/apigee/apigee-config-maven-plugin/issues/128) discussion.

### To use Portal APIs (from v2.6.0 onwards)

To use `apicategories` and `apidocs`, make sure you provide the Apigee Portal Site ID in the pom properties. For example
```xml
<apigee.portal.siteId>${siteId}</apigee.portal.siteId>
```

### To manage Spaces (from v2.9.3 onwards)

To use `spaces`, make sure its enabled in your Apigee project and you have the appropriate permissions. This plugin only supports space creation, updation and deletion. For associating and moving entities from/to spaces and assigning members to spaces, please follow our public documentation.

## Multi-file config
Projects with several config entities can utilize the multi-file structure to organize config while keeping individual file sizes within manageable limits. The plugin requires the use of specific file names and directories to organize config. 

The apigee.config.dir option must be used to identify the top most directory containing the following config structure.


      ├── api
      │   ├── forecastweatherapi
      │   │   ├── kvms.json
      │   │   ├── kvms-security.json
      │   └── oauth
      │       ├── kvms.json
      ├── env
      │   ├── prod
      │   │   ├── kvms.json
      │   │   ├── kvms-targets.json
      │   │   ├── flowhooks.json
      │   │   ├── targetServers.json
      │   │   ├── references.json
      │   ├── test
      │   │   ├── kvms.json
      │   │   ├── targetServers.json
      │   │   ├── targetServers-backend.json      
      │   │   ├── keystores.json
      │   │   ├── keystores-signed.json
      │   │   ├── aliases.json
      │   │   └── references.json   
      └── org
          ├── apiProducts.json
          ├── appGroups.json
          ├── appGroupApps.json          
          ├── developerApps.json
          ├── developers.json
          ├── kvms.json
          ├── reports.json
          └── importKeys.json


## Single file config structure - edge.json
Projects with fewer config entities can use the single file edge.json format to capture all config of an API project. The edge.json file organizes config into 3 scopes corresponding to the scopes of config entities that can be created in Edge. The plugin looks for edge.json in the current directory by default.
   ```
     envConfig
     orgConfig
     apiConfig
   ```
For example, API product is an orgConfig whereas Cache is an envConfig. The envConfig is further organized into individual environments as follows.
   ```
     envConfig.test
     envConfig.dev
     envConfig.pre-prod
   ```
The environment name is intended to be the same as the profile name in pom.xml (parent-pom.xml or shared-pom.xml).

The final level is the config entities. 
   ```
     orgConfig.apiProducts[]
     orgConfig.developers[]
     orgConfig.developerApps[]
     orgConfig.reports[]
     envConfig.test.targetServers[]
     envConfig.test.keystores[]
     envConfig.test.references[]
     envConfig.test.aliases[]
   ```

Each config entity is the payload of the corresponding management API. The example project may not show all the attributes of config entities, but any valid input of a management API would work.

Config entities like "developerApps" are grouped under the developerId (email) they belong to.

## Support
Issues filed on Github are not subject to service level agreements (SLAs) and responses should be assumed to be on an ad-hoc volunteer basis. The
[Apigee community board](https://community.apigee.com/) is recommended as for community support and is regularly checked by Apigee experts.
Apigee customers should use [formal support channels](https://cloud.google.com/apigee/support) for Apigee product related concerns.

* Post a question in [Apigee community](https://community.apigee.com/index.html)
* Create an [issue](https://github.com/apigee/apigee-config-maven-plugin/issues/new). Please specify if its for Apigee Hybrid/ Apigee X

## Disclaimer
This is not an officially supported Google product.
