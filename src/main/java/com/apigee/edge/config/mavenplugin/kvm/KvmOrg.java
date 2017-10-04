package com.apigee.edge.config.mavenplugin.kvm;

import com.apigee.edge.config.rest.RestUtil;
import com.google.api.client.http.HttpResponse;
import org.apache.maven.plugin.MojoFailureException;

import java.io.IOException;

public class KvmOrg extends KvmOperations implements Kvm {

    @Override
    public HttpResponse getEntriesForKvm(KvmValueObject kvmValueObject, String kvmEntryName) throws IOException {
        return RestUtil.getKvmEntriesForOrg(kvmValueObject.getProfile(),
                kvmValueObject.getKvmName(),
                kvmEntryName);
    }

    @Override
    public HttpResponse updateKvmEntries(KvmValueObject kvmValueObject, String kvmEntryName, String kvmEntryValue) throws IOException {
        return RestUtil.updateKvmEntriesForOrg(kvmValueObject.getProfile(),
                kvmValueObject.getKvmName(),
                kvmEntryName,
                kvmEntryValue);
    }

    @Override
    public HttpResponse createKvmEntries(KvmValueObject kvmValueObject, String kvmEntryValue) throws IOException {
        return RestUtil.createKvmEntriesForOrg(kvmValueObject.getProfile(),
                kvmValueObject.getKvmName(),
                kvmEntryValue);
    }

    @Override
    public void update(KvmValueObject kvmValueObject)
            throws IOException, MojoFailureException {
        super.update(kvmValueObject);
    }
}
