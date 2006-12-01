/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.schema;


import java.util.Arrays;

/**
 * Represents the <code>spring</code> config element hierarchy
 */
public class SpringApp {

  private boolean             sessionSupport;
  private Lock[]              locks;
  private InstrumentedClass[] includes;
  private AppContext[]        appContexts;
  private String              name;
  private boolean             fastProxy;  
  private String[] transientFields;

  public SpringApp(boolean sessionSupport, Lock[] locks, InstrumentedClass[] includes, AppContext[] appContexts,
      String name, boolean fastProxy, String[] transientFields) {

    this.sessionSupport = sessionSupport;
    this.locks = locks;
    this.includes = includes;
    this.appContexts = appContexts;
    this.name = name;
    this.fastProxy = fastProxy;
    this.transientFields = transientFields;
  }

  public boolean sessionSupport() {
    return sessionSupport;
  }

  public Lock[] locks() {
    return locks;
  }

  public InstrumentedClass[] includes() {
    return includes;
  }

  public AppContext[] appContexts() {
    return appContexts;
  }

  public String name() {
    return name;
  }
  
  public boolean fastProxy() {
    return fastProxy;
  }
  
  public String[] transientFields() {
    return transientFields;
  }

  public String toString() {
    return "SPRING: " + name + "\nSESSION: " + sessionSupport + "\nLOCKS:\n\n" + Arrays.asList(locks)
        + "\nINCLUDES:\n\n" + Arrays.asList(includes) + "\n" + Arrays.asList(appContexts) + "\nFASTPROXY: " + fastProxy
        + "\nTRANSIENT FIELDS:\n\n" + Arrays.asList(transientFields);
  }
}
