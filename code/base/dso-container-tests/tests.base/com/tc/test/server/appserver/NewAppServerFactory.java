/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver;

import com.tc.config.Directories;
import com.tc.exception.ImplementMe;
import com.tc.test.TempDirectoryHelper;
import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.glassfishv1.GlassfishV1AppServerFactory;
import com.tc.test.server.appserver.jboss4x.JBoss4xAppServerFactory;
import com.tc.test.server.appserver.tomcat5x.Tomcat5xAppServerFactory;
import com.tc.test.server.appserver.war.War;
import com.tc.test.server.appserver.wasce1x.Wasce1xAppServerFactory;
import com.tc.test.server.appserver.weblogic8x.Weblogic8xAppServerFactory;
import com.tc.test.server.tcconfig.StandardTerracottaAppServerConfig;
import com.tc.util.Assert;
import com.tc.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/**
 * This factory is meant to be used by the general public. The properties file supplied in obtaining an instance may be
 * blank, which will fall on the default appserver implementations. This class should be the only point for reference in
 * creating a working appserver. Never instantiate specific appserver classes explicitly.
 */
public abstract class NewAppServerFactory {

  protected final TestConfigObject config;
  private boolean                  licenseIsSet;

  protected NewAppServerFactory(ProtectedKey protectedKey, TestConfigObject config) {
    Assert.assertNotNull(protectedKey);
    Assert.assertNotNull(config);
    copyLicenseIfAvailable();
    this.config = config;
  }

  public abstract AppServerParameters createParameters(String instanceName, Properties props);

  public AppServerParameters createParameters(String instanceName) {
    return createParameters(instanceName, new Properties());
  }

  public abstract AppServer createAppServer(AppServerInstallation installation);

  public abstract AppServerInstallation createInstallation(URL host, File serverDir, File workingDir) throws Exception;

  public abstract AppServerInstallation createInstallation(File home, File workingDir) throws Exception;

  public abstract War createWar(String appName);

  public abstract StandardTerracottaAppServerConfig createTcConfig(File baseDir);

  public static final NewAppServerFactory createFactoryFromProperties(TestConfigObject config) {
    Assert.assertNotNull(config);
    String factoryName = config.appserverFactoryName();
    String majorVersion = config.appserverMajorVersion();

    if ("tomcat".equals(factoryName)) {
      if ("5".equals(majorVersion)) return new Tomcat5xAppServerFactory(new ProtectedKey(), config);
    } else if ("weblogic".equals(factoryName)) {
      if ("8".equals(majorVersion)) return new Weblogic8xAppServerFactory(new ProtectedKey(), config);
    } else if ("wasce".equals(factoryName)) {
      if ("1".equals(majorVersion)) return new Wasce1xAppServerFactory(new ProtectedKey(), config);
    } else if ("jboss".equals(factoryName)) {
      if ("4".equals(majorVersion)) return new JBoss4xAppServerFactory(new ProtectedKey(), config);
    } else if ("glassfish".equals(factoryName)) {
      if ("v1".equals(majorVersion)) return new GlassfishV1AppServerFactory(new ProtectedKey(), config);
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
      FileUtils.copyFile(licenseFile, toFile);
      this.licenseIsSet = true;
    } catch (IOException ioe) {
      throw new RuntimeException("Can't set up license file", ioe);
    }
  }

  protected static class ProtectedKey {
    // ensure that only this class may invoke it's children
  }
}
