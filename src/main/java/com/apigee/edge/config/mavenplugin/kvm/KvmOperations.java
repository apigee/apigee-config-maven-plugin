package com.apigee.edge.config.mavenplugin.kvm;

import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;

import net.javacrumbs.jsonunit.core.Configuration;
import net.javacrumbs.jsonunit.core.internal.Diff;

import org.apache.maven.plugin.MojoFailureException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.core.internal.Diff.create;

import java.io.IOException;

public abstract class KvmOperations {

    private static Logger logger = LoggerFactory.getLogger(KvmOperations.class);

    public abstract HttpResponse getKvm(KvmValueObject kvmValueObject) throws IOException; 
    
    public abstract HttpResponse getEntriesForKvm(KvmValueObject kvmValueObject, String kvmEntryName) throws IOException;

    public abstract HttpResponse updateKvmEntries(KvmValueObject kvmValueObject, String kvmEntryName, String kvmEntryValue) throws IOException;

    public abstract HttpResponse updateKvmEntriesForNonCpsOrg(KvmValueObject kvmValueObject) throws IOException;

    public abstract HttpResponse createKvmEntries(KvmValueObject kvmValueObject, String kvmEntryValue) throws IOException;


    public void update(KvmValueObject kvmValueObject)
            throws IOException, MojoFailureException {

        if(isOrgCpsEnabled(kvmValueObject)){
            updateKvmForCpsOrg(kvmValueObject);
        }else {
            updateKvmForNonCpsOrg(kvmValueObject);
        }

    }

    private Boolean isOrgCpsEnabled(KvmValueObject kvmValueObject) throws MojoFailureException {
        return kvmValueObject.getProfile().getCpsEnabled();
    }

    private void updateKvmForCpsOrg(KvmValueObject kvmValueObject) throws MojoFailureException, IOException {
        JSONArray entries = getEntriesConfig(kvmValueObject.getKvm());
        HttpResponse response;

        for (Object entry: entries){

            JSONObject entryJson = ((JSONObject) entry);
            String entryName = (String) entryJson.get("name");
            String entryValue = (String) entryJson.get("value");
            if(!kvmValueObject.getProfile().getKvmOverride() && compareKVMEntries(kvmValueObject, entryName, entryValue)) {
            	logger.info("No change to KVM - "+ kvmValueObject.getKvmName()+"-"+entryName +". Skipping !");
            	continue;
            }else if(doesEntryAlreadyExistForOrg(kvmValueObject, entryName)){
                response = updateKvmEntries(kvmValueObject, entryName, entryJson.toJSONString());
            }else{
                response = createKvmEntries(kvmValueObject, entryJson.toJSONString());
            }

            try {

                logger.debug("Response " + response.getContentType() + "\n" +
                        response.parseAsString());

                if (response.isSuccessStatusCode())
                    logger.info("KVM Entry Update Success: " + entryName);

            } catch (HttpResponseException e) {
                logger.error("KVM update error " + e.getMessage());
                throw new IOException(e.getMessage());
            }
        }
    }

    private void updateKvmForNonCpsOrg(KvmValueObject kvmValueObject)
            throws IOException {
    	if(!kvmValueObject.getProfile().getKvmOverride()) {
    		logger.info("Override is set to false");
    		HttpResponse response = getKvm(kvmValueObject);
    		String responseString = response.parseAsString();
    		logger.debug("Get KVM Response " + response.getContentType() + "\n" + responseString);
    		if(compareJSON(responseString, kvmValueObject.getKvm())) {
    			logger.info("No change to KVM - "+ kvmValueObject.getKvmName()+". Skipping !");
    			return;
    		}
    	}
        HttpResponse response = updateKvmEntriesForNonCpsOrg(kvmValueObject);
        try {

            logger.debug("Response " + response.getContentType() + "\n" +
                    response.parseAsString());
            if (response.isSuccessStatusCode())
                logger.info("Update Success.");

        } catch (HttpResponseException e) {
            logger.error("KVM update error " + e.getMessage());
            throw new IOException(e.getMessage());
        }
    }

    private static JSONArray getEntriesConfig(String kvm) throws MojoFailureException {
        JSONParser parser = new JSONParser();
        JSONObject entry;
        try {
            entry = (JSONObject) parser.parse(kvm);
            return (JSONArray) entry.get("entry");
        } catch(ParseException ex) {
            logger.info(ex.getMessage());
            throw new MojoFailureException("Error parsing " +
                    ex.getMessage());
        }
    }

    private boolean doesEntryAlreadyExistForOrg(KvmValueObject kvmValueObject, String kvmEntryName)  {
        try {

            HttpResponse response = getEntriesForKvm(kvmValueObject, kvmEntryName);

            if (response == null) {
                return false;
            }

            logger.debug("Response " + response.getContentType() + "\n" +
                    response.parseAsString());

            if (response.isSuccessStatusCode()) {
                return true;
            }

        } catch (IOException e) {
            logger.error("Get KVM Entry error " + e.getMessage());
        }

        return false;
    }
    
    /**
     * This method will compare the config data and the value in Apigee. If they match will return true
     * @param kvmValueObject
     * @param kvmEntryName
     * @param kvmEntryValue
     * @return
     */
    private boolean compareKVMEntries(KvmValueObject kvmValueObject, String kvmEntryName, String kvmEntryValue)  {
        try {

            HttpResponse response = getEntriesForKvm(kvmValueObject, kvmEntryName);
            if (response == null) {
                return false;
            }
            String responseValue = parseValuefromKVM(response.parseAsString());
            if (responseValue.equals(kvmEntryValue)) {
                return true;
            }

        } catch (IOException e) {
            logger.error("Get KVM Entry error " + e.getMessage());
        }

        return false;
    }
    
    /**
     * Parse Value from KVM Response
     * @param kvmEntryResponse
     * @return
     */
    private String parseValuefromKVM (String kvmEntryResponse) {
    	JSONParser parser = new JSONParser();
		String value = null ;
        try {
        	JSONObject obj = (JSONObject) parser.parse(kvmEntryResponse);
        	value = (String) obj.get("value");
        } catch(ParseException ex) {
        	logger.error("Error while parsing KVM Entry response " + ex.getMessage());
        }
        return value;
    }
    
    /**
     * Compare two JSON 
     * @param jsonString1
     * @param jsonString2
     * @return
     */
    private static boolean compareJSON (String jsonString1, String jsonString2) {	
		Configuration configuration = Configuration.empty();
		Diff diff = create(jsonString1, jsonString2, "fullJson", "", configuration.withOptions(IGNORING_ARRAY_ORDER));
		if(diff.similar()) {
			return true;
		}else {
			System.out.println(diff.differences());
			return false;
		}
	}
}
