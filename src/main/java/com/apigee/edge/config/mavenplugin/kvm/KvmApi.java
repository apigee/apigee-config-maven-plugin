package com.apigee.edge.config.mavenplugin.kvm;

import com.apigee.edge.config.rest.RestUtil;
import com.google.api.client.http.HttpResponse;
import org.apache.maven.plugin.MojoFailureException;

import java.io.IOException;

public class KvmApi extends KvmOperations implements Kvm {

    @Override
    public HttpResponse getEntriesForKvm(KvmValueObject kvmValueObject, String kvmEntryName) throws IOException {
        return RestUtil.getKvmEntriesForApi(kvmValueObject.getProfile(),
                kvmValueObject.getApi(),
                kvmValueObject.getKvmName(),
                kvmEntryName);
    }

    @Override
    public HttpResponse updateKvmEntries(KvmValueObject kvmValueObject, String kvmEntryName, String kvmEntryValue) throws IOException {
        return RestUtil.updateKvmEntriesForApi(kvmValueObject.getProfile(),
                kvmValueObject.getApi(),
                kvmValueObject.getKvmName(),
                kvmEntryName,
                kvmEntryValue);
    }

    @Override
    public HttpResponse createKvmEntries(KvmValueObject kvmValueObject, String kvmEntryValue) throws IOException {
        return RestUtil.createKvmEntriesForApi(kvmValueObject.getProfile(),
                kvmValueObject.getApi(),
                kvmValueObject.getKvmName(),
                kvmEntryValue);
    }

    @Override
    public void update(KvmValueObject kvmValueObject)
            throws IOException, MojoFailureException {
        super.update(kvmValueObject);
    }
}
