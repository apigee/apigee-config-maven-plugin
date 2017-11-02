package com.apigee.edge.config.mavenplugin.kvm;

import org.apache.maven.plugin.MojoFailureException;

import java.io.IOException;

public interface Kvm {

    public void update(KvmValueObject kvmValueObject)
            throws IOException, MojoFailureException;

}
