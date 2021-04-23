# apigee-config-maven-plugin

Maven plugin to create, manage Apigee config like Cache, KVM, Target Server, Resource Files, API Products, Developers, Developer Apps, Flow hooks, Extensions, Mask Config, Custom Roles and API Spec.

> **NOTE** The `mvn apigee-config:specs` - uses API whose contract and designs are experimental and expected to change, so may > break at any time and without warning. There are no guarantees of reliability, performance, stability, or support -- use at > your own risk

Help API teams follow API development best practices with Apigee.
  * Track Apigee Config (KVM, cache, target servers, etc.) in source control
  * Deploy config changes along with the API in a CI pipeline
  * Simplify, automate config management during API development
  * Track config changes in prod environment as releases

Small API projects can use the single file format in edge.json to manage their config. Large, complex projects with several config entities can use the multi-file format to organize config in source control. Checkout samples for examples.

This plugin is available in public maven repo and can be used just by referring to it in pom.xml. This github repo is the plugin source code and unless you make changes to the code you do not have to build this repo to use the plugin. Read this document further for plugin usage instructions.

## Prerequisites
You will need the following to run the samples:
- Apigee Edge developer account. See [docs](http://docs.apigee.com/api-services/content/creating-apigee-edge-account) for more details on how to setup your account..
- [Java SDK >= 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
- [Maven 3.x](https://maven.apache.org/)

## Plugin Usage

### NOTE ###
- If you want to use this plugin for Apigee X or Apigee Hybrid, please refer to this [link](https://github.com/apigee/apigee-config-maven-plugin/tree/hybrid). You should be using the version 2.x
```xml
<dependency>
  <groupId>com.apigee.edge.config</groupId>
  <artifactId>apigee-config-maven-plugin</artifactId>
  <version>2.x</version>
</dependency>
```
- For Apigee SaaS/Private Cloud, the version of the plugin is 1.x
```xml
<dependency>
  <groupId>com.apigee.edge.config</groupId>
  <artifactId>apigee-config-maven-plugin</artifactId>
  <version>1.x</version>
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

  # Individual goals
  You can also work with an individual config type using the 
  corresponding goal directly. The goals available are,
 
  caches
  kvms                  # CPS support from v1.2.1
  targetservers
  resourcefiles
  flowhooks
  maskconfigs
  apiproducts
  developers
  apps
  virtualhosts
  exportAppKeys
  extensions			#v1.3.1 or later
  reports			    #v1.3.2 or later
  references			#v1.3.4 or later
  userroles  			#v1.3.7 or later 
  specs  			    #v1.3.8 or later (experimental)
  importKeys			#v1.4.1 or later
  keystores			    #v1.4.2 or later
  aliases			    #v1.4.2 or later
  companies			    #v1.4.5 or later
  companyapps			    #v1.4.5 or later
  

  For example, the apps goal is used below to only create apps and ignore all other config types.
  mvn apigee-config:apps -Ptest -Dapigee.config.options=create
  
  To export the dev app keys, use the following:
  mvn apigee-config:exportAppKeys -Ptest -Dapigee.config.exportDir=./target  
```
The default "none" action is a NO-OP and it helps deploy APIs (using [apigee-deploy-maven-plugin](https://github.com/apigee/apigee-deploy-maven-plugin)) without affecting config.

## To configure a proxy

Supported from v1.4

Please refer to this [doc](http://maven.apache.org/guides/mini/guide-proxies.html) that explains how to setup proxy settings in your settings.xml usually in your $HOME/.m2 directory. Only `https` proxy protocol is supported.

## Sample project
Refer to an example project at [/samples/EdgeConfig](https://github.com/apigee/apigee-config-maven-plugin/tree/master/samples/EdgeConfig)

This project demonstrates the creation and management of Apigee Edge Config and performs the following steps in sequence.
  - Creates Caches
  - Creates Target servers
  - Creates Virtual Hosts
  - Creates KVM
  - Creates Resource File
  - Creates API products
  - Creates Developers
  - Creates Developer Apps
  - Creates Custom Reports
  - Creates References
  - Create User Roles and add permissions to Role [GET,POST,DELETE]
  and many more
  - Create API Spec
  - Create Keystores/Truststores
  - Create Alias
  - Create Companies
  - Create Company Apps

To use, edit samples/EdgeConfig/shared-pom.xml, and update org and env elements in all profiles to point to your Apigee org, env. You can add more profiles corresponding to each env in your org.

      <apigee.org>myorg</apigee.org>
      <apigee.env>test</apigee.env>

To run the plugin and use edge.json jump to samples project `cd /samples/EdgeConfig` and run 

`mvn install -Ptest -Dusername=<your-apigee-username> -Dpassword=<your-apigee-password> -Dapigee.config.options=create`

To run the plugin and use a config file similar to edge.json in any directory jump to samples project `cd /samples/EdgeConfig` and run 

`mvn install -Ptest -Dusername=<your-apigee-username> -Dpassword=<your-apigee-password> -Dapigee.config.file=<path-to-config-file> -Dapigee.config.options=create`

To run the plugin and use the multi-file config format jump to samples project `cd /samples/EdgeConfig` and run 

`mvn install -Ptest -Dusername=<your-apigee-username> -Dpassword=<your-apigee-password> -Dapigee.config.options=create -Dapigee.config.dir=resources/edge`

Refer to [samples/APIandConfig/HelloWorld](https://github.com/apigee/apigee-config-maven-plugin/tree/master/samples/APIandConfig/HelloWorld) for config management along with API deployment using [apigee-deploy-maven-plugin](https://github.com/apigee/apigee-deploy-maven-plugin). More info at [samples/README.md](https://github.com/apigee/apigee-config-maven-plugin/blob/master/samples/README.md)

## Multi-file config
Projects with several config entities can utilize the multi-file structure to organize config while keeping individual file sizes within manageable limits. The plugin requires the use of specific file names and directories to organize config. 

The apigee.config.dir option must be used to identify the top most directory containing the following config structure.


      ├── api
      │   ├── forecastweatherapi
      │   │   ├── resourceFiles
      │   │   │   ├── jsc
      │   │   │   │    ├── test.js
      │   │   ├── kvms.json
      │   │   └── resourcefiles.json
      │   └── oauth
      │       ├── kvms.json
      │       └── maskconfigs.json
      ├── env
      │   ├── prod
      │   │   ├── caches.json
      │   │   └── flowhooks.json
      │   └── test
      │       ├── caches.json
      │       ├── kvms.json
      │       ├── targetServers.json
      │       └── virtualHosts.json   
      │       └── references.json
      │ 	  └── keystores.json
      │ 	  └── aliases.json   
      └── org
          ├── apiProducts.json
          ├── developerApps.json
          ├── developers.json
          ├── kvms.json
          ├── companies.json
          ├── companyApps.json
          ├── reports.json
          └── maskconfigs.json
          └── userroles.json

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
     envConfig.test.caches[]
     envConfig.test.targetServers[]
     envConfig.test.virtualHosts[]
     envConfig.test.references[]
   ```

Each config entity is the payload of the corresponding management API. The example project may not show all the attributes of config entities, but any valid input of a management API would work.

Config entities like "developerApps" are grouped under the developerId (email) they belong to.

## OAuth (supported from v1.2 or higher)
Apigee management APIs are secured using OAuth tokens as an alternative to the Basic Auth security. Additionally Two-Factor authentication (MFA) using TOTP can also be configured as an additional layer of security. This plugin has the capability to acquire OAuth tokens and invoke management API calls.

Refer to [How to get OAuth2 tokens](http://docs.apigee.com/api-services/content/using-oauth2-security-apigee-edge-management-api#howtogetoauth2tokens) and [Two-Factor authentication](http://docs.apigee.com/api-services/content/enable-two-factor-auth-your-apigee-account)for details.

### Using OAuth
OAuth capability when enabled is seamless and the plugin acquires OAuth tokens and uses it subsequently to call management APIs. 

To enable OAuth add the following options to all profiles as required. Refer to [shared-pom.xml](https://github.com/apigee/apigee-config-maven-plugin/blob/oauth/samples/EdgeConfig/shared-pom.xml) example.

    <apigee.tokenurl>${tokenurl}</apigee.tokenurl> <!-- optional: oauth -->
    <apigee.authtype>${authtype}</apigee.authtype> <!-- optional: oauth|basic(default) -->
    
You need to pass the OAuth ClientId and Secret to the plugin for it to generate the token and use that to invoke the Apigee Management APIs
    
    <apigee.clientid>${clientId}</apigee.clientid> <!-- optional: Oauth Client Id - Default is edgecli-->
    <apigee.clientsecret>${clientSecret}</apigee.clientsecret> <!-- optional: Oauth Client Secret Default is edgeclisecret-->


To invoke, add command line flags to enable OAuth.

    mvn install -Ptest -Dusername=$ae_username -Dpassword=$ae_password \
                        -Dorg=testmyapi -Dauthtype=oauth -Dapigee.config.options=create
                        
"tokenurl" is optional and defaults to the cloud version "https://login.apigee.com/oauth/token"

### Two-Factor Authentication
[Two-Factor authentication](http://docs.apigee.com/api-services/content/enable-two-factor-auth-your-apigee-account) is based on TOTP tokens. When the apigee account is enabled for Two-Factor Authentication it applies to management APIs as well.

The plugin can accept TOTP tokens generated by an external utility and use it to acquire OAuth tokens.

TOTP can be generated using command line tools for use in CI tools like Jenkins.

### Using Two-Factor Authentication token
**Note** OAuth needs to be enabled before Two-Factor Authentication can be used. 

To enable Two-Factor Authentication, add the following options to all profiles as required. Refer to [shared-pom.xml](https://github.com/apigee/apigee-config-maven-plugin/blob/oauth/samples/EdgeConfig/shared-pom.xml) example.

    <apigee.mfatoken>${mfatoken}</apigee.mfatoken> <!-- optional: mfa -->

Provide the token when invoking the plugin.

    mvn install -Ptest -Dusername=$ae_username -Dpassword=$ae_password \
                        -Dorg=testmyapi -Dauthtype=oauth -Dmfatoken=123456 -Dapigee.config.options=create
                        
If the API takes a long time to package up then  it is likely that the token would have expired before it is used.  To mitigate against this, from version 1.1.5, an initmfa goal can be called during the validate phase:

    <execution>
        <id>initialise-mfa</id>
        <phase>validate</phase>
        <goals>
            <goal>initmfa</goal>
        </goals>
    </execution>

Depending on where the plugin is in the order, and how much validation is required, it is possible that this may still result in token timeout.

### Passing the Bearer Token as a parameter
If you would like to generate the bearer token outside of this plugin and provide it as a command line parameter, you can add the following: 

    <apigee.bearer>${bearer}</apigee.bearer>

Provide the token when invoking the plugin.

    mvn install -Ptest -Dusername=$ae_username -Dorg=testmyapi \
                         -Dauthtype=oauth -Dbearer=c912eu1201c -Dapigee.config.options=create
                    
*NOTE: when using bearer token - please provide the username as well, as it is used for token validation*
                        
### Passing the Refresh Token as a parameter
If you would like to generate the refresh token outside of this plugin and provide it as a command line parameter, you can add the following: 

    <apigee.refresh>${refresh}</apigee.refresh>

Provide the token when invoking the plugin.

    mvn install -Ptest -Dusername=$ae_username -Dorg=testmyapi \
                         -Dauthtype=oauth -Dbearer=c912eu1201c -Drefresh=d023fv2312d -Dapigee.config.options=create
   
*NOTE: If you are providing refresh token, you need to provide the bearer token and username as well*
Apigee edge comes with several inbuilt role that provides several permission level 

### Update Developer App and Company App (from v1.4.5 onwards)

When `apigee.config.options=update` is run on apps and companyapps and if the payload passed includes the apiProducts, the Management server created a new credentials. To avoid this you can pass `-Dapigee.app.ignoreAPIProducts=true`. Please note this is applicable only for `apigee.config.options=update`

## ROADMAP
- API Monitoring
- Spec snapshot (experimental)

Please send feature requests using [issues](https://github.com/apigee/apigee-config-maven-plugin/issues)

## Support
Issues filed on Github are not subject to service level agreements (SLAs) and responses should be assumed to be on an ad-hoc volunteer basis. The
[Apigee community board](https://community.apigee.com/) is recommended as for community support and is regularly checked by Apigee experts.
Apigee customers should use [formal support channels](https://cloud.google.com/apigee/support) for Apigee product related concerns.

* Post a question in [Apigee community](https://community.apigee.com/index.html)
* Create an [issue](https://github.com/apigee/apigee-config-maven-plugin/issues/new)

## Disclaimer
This is not an officially supported Google product.
