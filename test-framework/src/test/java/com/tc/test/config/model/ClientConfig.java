package com.tc.test.config.model;

import java.util.ArrayList;
import java.util.List;

public class ClientConfig {
  private boolean      parallelClients = true;
  private final List<String> extraClientJvmArgs;
  private Class<?>[]   classes;
  

  public ClientConfig() {
    extraClientJvmArgs = new ArrayList<String>();
  }

  public List<String> getExtraClientJvmArgs() {
    return extraClientJvmArgs;
  }

  public void addExtraClientJvmArg(String extraClientJvmArg) {
    extraClientJvmArgs.add(extraClientJvmArg);
  }

  public void setClientClasses(Class<?>[] classes) {
    this.classes = classes;
  }

  public void setClientClasses(Class clientClass, int count) {
    this.classes = new Class<?>[count];
    for (int i = 0; i < count; i++) {
      classes[i] = clientClass;
    }
  }

  public Class<?>[] getClientClasses() {
    return classes;
  }

  public void setParallelClients(boolean parallelClients) {
    this.parallelClients = parallelClients;
  }

  public boolean isParallelClients() {
    return parallelClients;
  }

}
