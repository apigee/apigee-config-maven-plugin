package com.apigee.edge.config.mavenplugin.kvm;

import com.apigee.edge.config.utils.ServerProfile;

public class KvmValueObject {

    private ServerProfile profile;
    private String api;
    private String kvmName;
    private String kvm;

    public KvmValueObject(ServerProfile profile, String api, String kvmName, String kvm) {
        this.profile = profile;
        this.api = api;
        this.kvmName = kvmName;
        this.kvm = kvm;
    }

    public KvmValueObject(ServerProfile profile, String kvmName, String kvm) {

        this.profile = profile;
        this.kvmName = kvmName;
        this.kvm = kvm;

    }

    public ServerProfile getProfile() {
        return profile;
    }

    public void setProfile(ServerProfile profile) {
        this.profile = profile;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public String getKvmName() {
        return kvmName;
    }

    public String getKvm() {
        return kvm;
    }

}
