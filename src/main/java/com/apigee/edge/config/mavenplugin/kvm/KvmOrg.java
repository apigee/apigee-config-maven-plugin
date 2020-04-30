package com.apigee.edge.config.mavenplugin.kvm;

import com.apigee.edge.config.rest.RestUtil;
import com.google.api.client.http.HttpResponse;
import org.apache.maven.plugin.MojoFailureException;

import java.io.IOException;

public class KvmOrg extends KvmOperations implements Kvm {

	@Override
    public HttpResponse getKvm(KvmValueObject kvmValueObject) throws IOException {
		RestUtil restUtil = new RestUtil(kvmValueObject.getProfile());
		return restUtil.getOrgConfig(kvmValueObject.getProfile(),
                "keyvaluemaps/"+kvmValueObject.getKvmName());
    }
	
	@Override
    public HttpResponse getEntriesForKvm(KvmValueObject kvmValueObject, String kvmEntryName) throws IOException {
		RestUtil restUtil = new RestUtil(kvmValueObject.getProfile());
		return restUtil.getOrgConfig(kvmValueObject.getProfile(),
                "keyvaluemaps",
                kvmValueObject.getKvmName(),
                "entries",
                kvmEntryName);
    }

    @Override
    public HttpResponse updateKvmEntries(KvmValueObject kvmValueObject, String kvmEntryName, String kvmEntryValue) throws IOException {
    	RestUtil restUtil = new RestUtil(kvmValueObject.getProfile());
    	return restUtil.updateOrgConfig(kvmValueObject.getProfile(),
                "keyvaluemaps",
                kvmValueObject.getKvmName(),
                "entries",
                kvmEntryName,
                kvmEntryValue);
    }

    @Override
    public HttpResponse updateKvmEntriesForNonCpsOrg(KvmValueObject kvmValueObject) throws IOException {
    	RestUtil restUtil = new RestUtil(kvmValueObject.getProfile());
    	return restUtil.updateOrgConfig(kvmValueObject.getProfile(),
                "keyvaluemaps",
                kvmValueObject.getKvmName(),
                kvmValueObject.getKvm());
    }

    @Override
    public HttpResponse createKvmEntries(KvmValueObject kvmValueObject, String kvmEntryValue) throws IOException {
    	RestUtil restUtil = new RestUtil(kvmValueObject.getProfile());
    	return restUtil.createOrgConfig(kvmValueObject.getProfile(),
                "keyvaluemaps",
                kvmValueObject.getKvmName(),
                "entries",
                kvmEntryValue);
    }

    @Override
    public void update(KvmValueObject kvmValueObject)
            throws IOException, MojoFailureException {
        super.update(kvmValueObject);
    }
}
