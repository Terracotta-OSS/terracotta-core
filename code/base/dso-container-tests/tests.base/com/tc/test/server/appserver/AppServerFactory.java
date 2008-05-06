/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver;

import com.tc.config.Directories;
import com.tc.exception.ImplementMe;
import com.tc.test.AppServerInfo;
import com.tc.test.TempDirectoryHelper;
import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.glassfishv1.GlassfishV1AppServerFactory;
import com.tc.test.server.appserver.jboss3x.JBoss3xAppServerFactory;
import com.tc.test.server.appserver.jboss4x.JBoss4xAppServerFactory;
import com.tc.test.server.appserver.jetty6x.Jetty6xAppServerFactory;
import com.tc.test.server.appserver.resin3x.Resin3xAppServerFactory;
import com.tc.test.server.appserver.tomcat5x.Tomcat5xAppServerFactory;
import com.tc.test.server.appserver.was6x.Was6xAppServerFactory;
import com.tc.test.server.appserver.wasce1x.Wasce1xAppServerFactory;
import com.tc.test.server.appserver.weblogic8x.Weblogic8xAppServerFactory;
import com.tc.test.server.appserver.weblogic9x.Weblogic9xAppServerFactory;
import com.tc.util.Assert;
import com.tc.util.io.TCFileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * This factory is meant to be used by the general public. The properties file supplied in obtaining an instance may be
 * blank, which will fall on the default appserver implementations. This class should be the only point for reference in
 * creating a working appserver. Never instantiate specific appserver classes explicitly.
 */
public abstract class AppServerFactory {
  private boolean licenseIsSet;

  protected AppServerFactory(ProtectedKey protectedKey) {
    Assert.assertNotNull(protectedKey);
    copyLicenseIfAvailable();
  }

  public abstract AppServerParameters createParameters(String instanceName, Properties props);

  public AppServerParameters createParameters(String instanceName) {
    return createParameters(instanceName, new Properties());
  }

  public abstract AppServer createAppServer(AppServerInstallation installation);

  public abstract AppServerInstallation createInstallation(File home, File workingDir, AppServerInfo appServerInfo)
      throws Exception;

  public static final AppServerFactory createFactoryFromProperties() {
    AppServerInfo appServerInfo = TestConfigObject.getInstance().appServerInfo();
    String factoryName = appServerInfo.getName();
    String majorVersion = appServerInfo.getMajor();
    System.out.println("APPSERVERINFO: " + appServerInfo);

    switch (appServerInfo.getId()) {
      case AppServerInfo.TOMCAT:
        if ("5".equals(majorVersion) || "6".equals(majorVersion)) return new Tomcat5xAppServerFactory(
                                                                                                      new ProtectedKey());
        break;
      case AppServerInfo.WEBLOGIC:
        if ("8".equals(majorVersion)) return new Weblogic8xAppServerFactory(new ProtectedKey());
        if ("9".equals(majorVersion)) return new Weblogic9xAppServerFactory(new ProtectedKey());
        break;
      case AppServerInfo.WASCE:
        if ("1".equals(majorVersion)) return new Wasce1xAppServerFactory(new ProtectedKey());
        break;
      case AppServerInfo.JBOSS:
        if ("3".equals(majorVersion)) return new JBoss3xAppServerFactory(new ProtectedKey());
        if ("4".equals(majorVersion)) return new JBoss4xAppServerFactory(new ProtectedKey());
        break;
      case AppServerInfo.GLASSFISH:
        if ("v1".equals(majorVersion)) return new GlassfishV1AppServerFactory(new ProtectedKey());
        break;
      case AppServerInfo.JETTY:
        if ("6".equals(majorVersion)) return new Jetty6xAppServerFactory(new ProtectedKey());
        break;
      case AppServerInfo.WEBSPHERE:
        if ("6".equals(majorVersion)) return new Was6xAppServerFactory(new ProtectedKey());
        break;
      case AppServerInfo.RESIN:
        if ("3".equals(majorVersion)) return new Resin3xAppServerFactory(new ProtectedKey());
        break;
    }

    throw new ImplementMe("App server named '" + factoryName + "' with major version " + majorVersion
                          + " is not yet supported.");
  }

  private final synchronized void copyLicenseIfAvailable() {
    if (this.licenseIsSet) return;

    try {
      File licenseFile = new File(Directories.getLicenseLocation(), "license.lic");

      if (!licenseFile.exists()) {
        this.licenseIsSet = true;
        return;
      }

      TempDirectoryHelper helper = new TempDirectoryHelper(getClass());
      File toDir = helper.getDirectory();
      File toFile = new File(toDir, licenseFile.getName());
      TCFileUtils.copyFile(licenseFile, toFile);
      this.licenseIsSet = true;
    } catch (IOException ioe) {
      throw new RuntimeException("Can't set up license file", ioe);
    }
  }

  protected static class ProtectedKey {
    // ensure that only this class may invoke it's children
  }
}
