/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver;

import org.apache.commons.io.CopyUtils;
import org.apache.commons.io.IOUtils;

import com.tc.config.Directories;
import com.tc.test.TempDirectoryHelper;
import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.jboss4x.JBoss4xAppServerFactory;
import com.tc.test.server.appserver.tomcat5x.Tomcat5xAppServerFactory;
import com.tc.test.server.appserver.war.War;
import com.tc.test.server.appserver.wasce1x.Wasce1xAppServerFactory;
import com.tc.test.server.appserver.weblogic8x.Weblogic8xAppServerFactory;
import com.tc.test.server.tcconfig.StandardTerracottaAppServerConfig;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    if (Tomcat5xAppServerFactory.NAME.equals(factoryName)) {
      return new Tomcat5xAppServerFactory(new ProtectedKey(), config);
    } else if (Weblogic8xAppServerFactory.NAME.equals(factoryName)) {
      return new Weblogic8xAppServerFactory(new ProtectedKey(), config);
    } else if (Wasce1xAppServerFactory.NAME.equals(factoryName)) {
      return new Wasce1xAppServerFactory(new ProtectedKey(), config);
    } else if (JBoss4xAppServerFactory.NAME.equals(factoryName)) {
      return new JBoss4xAppServerFactory(new ProtectedKey(), config);
    }

    else throw new RuntimeException("The code doesn't know anything about an app server named '" + factoryName + "'.");
  }

  private final synchronized void copyLicenseIfAvailable() {
    if (this.licenseIsSet) return;

    InputStream original = null;
    OutputStream dest = null;

    try {
      File licenseFile = new File(Directories.getLicenseLocation(), "license.lic");
      
      if(!licenseFile.exists()) {
        this.licenseIsSet = true;
        return;
      }

      TempDirectoryHelper helper = new TempDirectoryHelper(getClass());
      File toDir = helper.getDirectory();
      File toFile = new File(toDir, licenseFile.getName());
      original = new FileInputStream(licenseFile);
      dest = new FileOutputStream(toFile);
      CopyUtils.copy(original, dest);
      original.close();
      original = null;
      this.licenseIsSet = true;
    } catch (IOException ioe) {
      throw new RuntimeException("Can't set up license file", ioe);
    } finally {
      IOUtils.closeQuietly(original);
      IOUtils.closeQuietly(dest);
    }
  }

  protected static class ProtectedKey {
    // ensure that only this class may invoke it's children
  }
}
