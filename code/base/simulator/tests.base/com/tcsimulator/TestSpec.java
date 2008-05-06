/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.simulator.app.ApplicationConfig;
import com.tcsimulator.distrunner.ServerSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TestSpec {
  // this is the Impl, not the interface because the portability restrictions are on the field type, not the
  // instance type.
  private ApplicationConfigImpl appConfig;
  private Map                   containerSpecsByHostname;
  private Map                   containerSpecsByVmName;
  private int                   globalVmCount;
  private Map                   serverSpecsByHostName;
  private Map                   serverSpecsByType;

  public static void visitDSOApplicationConfig(ConfigVisitor visitor, DSOApplicationConfig config) {
    String classname = TestSpec.class.getName();
    config.addIncludePattern(classname);
    config.addIncludePattern(ContainerSpec.class.getName());
    config.addWriteAutolock("* " + classname + ".*(..)");
    visitor.visitDSOApplicationConfig(config, ApplicationConfigImpl.class);
  }

  public TestSpec(String className, int intensity, Collection cSpecs, Collection sSpecs) {
    containerSpecsByHostname = new HashMap();
    containerSpecsByVmName = new HashMap();
    serverSpecsByHostName = new HashMap();
    serverSpecsByType = new HashMap();
    GlobalVmNameGenerator vmNameGenerator = new GlobalVmNameGenerator();
    int globalParticipantCount = 0;

    for (Iterator i = cSpecs.iterator(); i.hasNext();) {
      ClientSpec cSpec = (ClientSpec) i.next();
      String hostName = cSpec.getHostName();

      int vmCount = cSpec.getVMCount();
      globalVmCount += vmCount;
      int executionCount = cSpec.getExecutionCount();
      List jvmArgs = cSpec.getJvmOpts();
      globalParticipantCount += (vmCount * executionCount);

      Collection specList = (Collection) containerSpecsByHostname.get(hostName);
      if (specList == null) {
        specList = new ArrayList();
        this.containerSpecsByHostname.put(hostName, specList);
      }

      for (int j = 0; j < vmCount; j++) {
        String vmName = vmNameGenerator.nextVmName();
        ContainerSpec newSpec = new ContainerSpec(vmName, cSpec.getTestHome(), executionCount, jvmArgs);
        specList.add(newSpec);
        containerSpecsByVmName.put(vmName, newSpec);
      }
    }

    for (Iterator i = sSpecs.iterator(); i.hasNext();) {
      ServerSpec sSpec = (ServerSpec) i.next();
      String hostName = sSpec.getHostName();
      int type = sSpec.getType();

      Collection specList = (Collection) serverSpecsByHostName.get(hostName);
      if (specList == null) {
        specList = new ArrayList();
        this.serverSpecsByHostName.put(hostName, specList);
      }
      specList.add(sSpec);

      specList = (Collection) serverSpecsByType.get(new Integer(type));
      if (specList == null) {
        specList = new ArrayList();
        this.serverSpecsByType.put(new Integer(type), specList);
      }
      specList.add(sSpec);
    }

    this.appConfig = new ApplicationConfigImpl(className, intensity, globalParticipantCount);
  }

  public TestSpec() {
    // Use this only after DSO object TestSpec has been created.
  }

  public synchronized ApplicationConfig getTestConfig() {
    return appConfig.copy();
  }

  public Collection getContainerSpecsFor(String hostname) {
    Collection rv = new ArrayList();
    Collection containerSpecsByHostName = (Collection) containerSpecsByHostname.get(hostname);
    if (containerSpecsByHostName != null) {
      for (Iterator i = ((Collection) containerSpecsByHostname.get(hostname)).iterator(); i.hasNext();) {
        rv.add(((ContainerSpec) i.next()).copy());
      }
    }
    return rv;
  }

  public Collection getServerSpecsFor(String hostname) {
    Collection rv = new ArrayList();
    Collection serverSpecs = (Collection) serverSpecsByHostName.get(hostname);
    if (serverSpecs != null) {
      for (Iterator i = serverSpecs.iterator(); i.hasNext();) {
        rv.add(((ServerSpec) i.next()).copy());
      }
    }
    return rv;
  }

  public Collection getServerSpecsFor(int type) {
    Collection rv = new ArrayList();
    Collection serverSpecs = (Collection) serverSpecsByType.get(new Integer(type));
    if (serverSpecs != null) {
      for (Iterator i = serverSpecs.iterator(); i.hasNext();) {
        rv.add(((ServerSpec) i.next()).copy());
      }
    }
    return rv;
  }

  public ContainerSpec getContainerSpecFor(String vmName) {
    return ((ContainerSpec) containerSpecsByVmName.get(vmName)).copy();
  }

  public synchronized int getGlobalVmCount() {
    return globalVmCount;
  }

  public long getAppExecutionTimeout() {
    return 1000 * 60 * 24 * 365 * 10;
  }

  public synchronized String toString() {
    StringBuffer result = new StringBuffer();
    result.append(getClass().getName() + " [containerSpecsByVmName: ");
    result.append(containerSpecsByVmName);
    result.append(",  containerSpecsByHostname: ");
    result.append(containerSpecsByHostname);
    result.append(",  globalVmCount: ");
    result.append(globalVmCount + "]");
    return result.toString();
  }

}
