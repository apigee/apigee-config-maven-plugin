# Keystores
Supported from v1.4.2

From 1.4.2 and later, keystores are supported and aliases are supported partialy.
Tow goals are added : keystores and aliases


## Prerequisites
You will need the following to run the samples:
- Apigee Edge developer account. See [docs](http://docs.apigee.com/api-services/content/creating-apigee-edge-account) for more details on how to setup your account..
- [Java SDK >= 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
- [Maven 3.x](https://maven.apache.org/)

## Usage

- Add keystores ans aliases steps to the plugin's executions sequence to be used in install phase. See example in samples/EdgeConfig/shared-pom.xml file.

- You can also work with keystores and aliases as an individual config type using the  corresponding goal prefixed with apigee-config this way "mvn apigee-config:keystores -Ptest -Dapigee.config.options=create" or this way "mvn apigee-config:aliases -Ptest -Dapigee.config.options=create"

- Add keystores to create in the config, see samples/EdgeConfig/resources/edge/env/test/keystores.json example file.

- Add aliases to create in the config, see samples/EdgeConfig/resources/edge/env/test/aliases.json example file.

- There are tow supported aliases types : "certificateandkey" and "certificateandkey". JAR file aliases and PKCS12/PFX aliases are not supported in 1.4.2 version.


## ROADMAP for keystores
  - In keystore, JAR File and PKCS12/PFX alias types
