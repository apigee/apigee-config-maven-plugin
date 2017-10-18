package com.apigee.edge.config.mavenplugin.kvm;

import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import org.apache.maven.plugin.MojoFailureException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class KvmOperations {

    private static Logger logger = LoggerFactory.getLogger(KvmOperations.class);

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


            if(doesEntryAlreadyExistForOrg(kvmValueObject, entryName)){
                response = updateKvmEntries(kvmValueObject, entryName, entryJson.toJSONString());
            }else{
                response = createKvmEntries(kvmValueObject, entryJson.toJSONString());
            }

            try {

                logger.info("Response " + response.getContentType() + "\n" +
                        response.parseAsString());

                if (response.isSuccessStatusCode())
                    logger.info("KVM Entry Update Success: " + entryName);

            } catch (HttpResponseException e) {
                logger.error("KVM update error " + e.getMessage());
                throw new IOException(e.getMessage());
            }
        }
        logger.info("KVM Update Success: " + kvmValueObject.getKvmName());
    }

    private void updateKvmForNonCpsOrg(KvmValueObject kvmValueObject)
            throws IOException {

        HttpResponse response = updateKvmEntriesForNonCpsOrg(kvmValueObject);
        try {

            logger.info("Response " + response.getContentType() + "\n" +
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

            logger.info("Response " + response.getContentType() + "\n" +
                    response.parseAsString());

            if (response.isSuccessStatusCode()) {
                return true;
            }

        } catch (IOException e) {
            logger.error("Get KVM Entry error " + e.getMessage());
        }

        return false;
    }
}
