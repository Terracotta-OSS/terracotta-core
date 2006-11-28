package com.tctest.spring.integrationtests.framework;

import org.springframework.remoting.rmi.RmiServiceExporter;

public class RemoteService {

  private final String remoteName;
  private final Class  interfaceType;
  private final String beanName;
  private final Class exporterType;

  public RemoteService(String remoteName, String beanName, Class interfaceType) {
    this(RmiServiceExporter.class, remoteName, beanName, interfaceType);
  }

  public RemoteService(Class exporterType, String remoteName, String beanName, Class interfaceType) {
    this.exporterType = exporterType;
    this.remoteName = remoteName;
    this.beanName = beanName;
    this.interfaceType = interfaceType;
  }

  public String getBeanName() {
    return beanName;
  }

  public Class getInterfaceType() {
    return interfaceType;
  }

  public String getRemoteName() {
    return remoteName;
  }

  public Class getExporterType() {
    return exporterType;
  }

}
