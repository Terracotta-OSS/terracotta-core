/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.tcconfig;

import com.tc.config.schema.builder.DSOApplicationConfigBuilder;
import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;
import com.tc.config.schema.builder.WebApplicationConfigBuilder;
import com.tc.config.schema.test.ApplicationConfigBuilder;
import com.tc.config.schema.test.DSOApplicationConfigBuilderImpl;
import com.tc.config.schema.test.InstrumentedClassConfigBuilderImpl;
import com.tc.config.schema.test.L1ConfigBuilder;
import com.tc.config.schema.test.L2ConfigBuilder;
import com.tc.config.schema.test.L2SConfigBuilder;
import com.tc.config.schema.test.LockConfigBuilderImpl;
import com.tc.config.schema.test.RootConfigBuilderImpl;
import com.tc.config.schema.test.SystemConfigBuilder;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.config.schema.test.WebApplicationConfigBuilderImpl;
import com.tc.util.PortChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class delegates to {@link TerracottaConfigBuilder} laying out the elements for a minimal config used to run
 * appserver modules such as sessions. The build() method must be called to update the internal object tree to it's
 * current state. The toString() method is used to obtain the actual XML text.
 */
public class StandardTerracottaAppServerConfig {

  private static final String               MODE         = "development";
  private static final String               DOMAIN       = "localhost";
  private static final String               LOGS         = "stderr:";
  private static final String               DATALOCATION = "server-data";
  private static final String               LOGSLOCATION = "logs/client-logs-%(NODE)";

  private final List                        instrumented = new ArrayList();
  private final List                        webapps      = new ArrayList();
  private final List                        roots        = new ArrayList();
  private final List                        locks        = new ArrayList();
  private final DSOApplicationConfigBuilder dacb;
  private final TerracottaConfigBuilder     configBuilder;
  private final int                         dsoPort;
  private final int                         jmxPort;
  private L1ConfigBuilder                   l1cb         = new L1ConfigBuilder();

  public StandardTerracottaAppServerConfig(File baseDir) {
    PortChooser portChooser = new PortChooser();

    this.jmxPort = portChooser.chooseRandomPort();
    this.dsoPort = portChooser.chooseRandomPort();

    this.configBuilder = new TerracottaConfigBuilder();

    SystemConfigBuilder scb = new SystemConfigBuilder();
    scb.setConfigurationModel(MODE);

    configBuilder.setSystem(scb);

    L2ConfigBuilder[] l2cbArray = new L2ConfigBuilder[1];
    L2ConfigBuilder l2cb = new L2ConfigBuilder();
    l2cb.setName(DOMAIN);
    l2cb.setData(baseDir + File.separator + DATALOCATION);
    l2cb.setLogs(LOGS);
    l2cb.setDSOPort(dsoPort);
    l2cb.setJMXPort(jmxPort);

    l2cbArray[0] = l2cb;

    L2SConfigBuilder l2scb = new L2SConfigBuilder();
    l2scb.setL2s(l2cbArray);

    configBuilder.setServers(l2scb);

    l1cb.setLogs(baseDir + File.separator + LOGSLOCATION);

    configBuilder.setClient(l1cb);

    dacb = new DSOApplicationConfigBuilderImpl();

    ApplicationConfigBuilder acb = new ApplicationConfigBuilder();
    acb.setDSO(dacb);

    configBuilder.setApplication(acb);
  }

  public int getDsoPort() {
    return dsoPort;
  }

  public int getJmxPort() {
    return jmxPort;
  }

  public void addModule(String name, String version) {
    l1cb.addModule(name, version);
  }

  public void addWebApplication(String appName) {
    WebApplicationConfigBuilder wacbImpl = new WebApplicationConfigBuilderImpl();
    wacbImpl.setWebApplicationName(appName);
    webapps.add(wacbImpl);
  }

  public void addWebApplication(String appName, boolean isSynchronousWrite) {
    WebApplicationConfigBuilder builder = new WebApplicationConfigBuilderImpl();
    builder.setWebApplicationName(appName);
    Map attributes = new HashMap();
    attributes.put(WebApplicationConfigBuilder.ATTRIBUTE_NAME, "" + isSynchronousWrite);
    builder.setWebApplicationAttributes(attributes);
    webapps.add(builder);
  }

  public final void addExclude(String exclude) {
    InstrumentedClassConfigBuilder iccbImpl = new InstrumentedClassConfigBuilderImpl();
    iccbImpl.setIsInclude(false);
    iccbImpl.setClassExpression(exclude);
    instrumented.add(iccbImpl);
  }

  public final void addRoot(String fieldName, String rootName) {
    RootConfigBuilder rootConfigBuilder = new RootConfigBuilderImpl();
    rootConfigBuilder.setFieldName(fieldName);
    rootConfigBuilder.setRootName(rootName);
    roots.add(rootConfigBuilder);
  }

  public final void addLock(boolean isAutolock, String methodExp, String lockLevel, String lockName) {
    String tag = isAutolock ? LockConfigBuilder.TAG_AUTO_LOCK : LockConfigBuilder.TAG_NAMED_LOCK;
    LockConfigBuilder lockConfigBuilder = new LockConfigBuilderImpl(tag);
    lockConfigBuilder.setMethodExpression(methodExp);
    lockConfigBuilder.setLockLevel(lockLevel);
    if (!isAutolock && lockName != null) {
      lockConfigBuilder.setLockName(lockName);
    }
    locks.add(lockConfigBuilder);
  }

  /**
   * Generates the current object tree representation in {@link TerracottaConfigBuilder}
   */
  public void build() {
    if (instrumented.size() > 0) {
      dacb.setInstrumentedClasses((InstrumentedClassConfigBuilder[]) instrumented
          .toArray(new InstrumentedClassConfigBuilder[0]));
    }
    if (webapps.size() > 0) {
      dacb.setWebApplications((WebApplicationConfigBuilder[]) webapps.toArray(new WebApplicationConfigBuilder[0]));
    }
    if (roots.size() > 0) {
      dacb.setRoots((RootConfigBuilder[]) roots.toArray(new RootConfigBuilder[0]));
    }
    if (locks.size() > 0) {
      dacb.setLocks((LockConfigBuilder[]) locks.toArray(new LockConfigBuilder[0]));
    }
  }

  public String toString() {
    return configBuilder.toString();
  }

  public TerracottaConfigBuilder getConfigBuilder() {
    return configBuilder;
  }

  public void addInclude(String classExpression) {
    InstrumentedClassConfigBuilder iccb = new InstrumentedClassConfigBuilderImpl();
    iccb.setClassExpression(classExpression);
    instrumented.add(iccb);
  }
}
