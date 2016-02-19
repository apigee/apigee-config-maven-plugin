## Getting Started
  - edit samples/src/edge/shared-pom.xml, and update org and env to point to your Apigee org
    ```
                    <apigee.org>myorg</apigee.org>
                    <apigee.env>test</apigee.env>
    ```
  - to run mvn to create the config
    ```
    cd /samples/src/edge/HelloWorld
    export APIGEE_USER <your-apigee-username>
    export APIGEE_PASS <your-apigee-password>
    mvn install -Ptest -Dusername=${APIGEE_USER} -Dpassword=${APIGEE_PASS} -Dapigee.config.options=create
    ```

The example project performs the following steps in sequence. This sequence is
inherent to the platform and is managed using the sequencing of goals in pom.xml
  - Creates Caches
  - Creates Target servers
  - Deploy API
  - Creates API products
