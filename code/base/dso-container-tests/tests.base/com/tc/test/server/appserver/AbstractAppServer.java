/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver;

import org.apache.commons.io.FileUtils;
import org.codehaus.cargo.util.internal.log.AbstractLogger;
import org.codehaus.cargo.util.log.LogLevel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Handles installation and dynamic port assignment for an appserver instance.
 */
public abstract class AbstractAppServer implements AppServer {

  private final AppServerStartupEnvironment installation;
  private File                              instance;

  public AbstractAppServer(AppServerInstallation installation) {
    this.installation = (AppServerStartupEnvironment) installation;
  }

  protected final synchronized File createInstance(AppServerParameters params) throws Exception {
    instance = new File(installation.sandboxDirectory() + File.separator + params.instanceName());
    if (instance.exists()) {
      FileUtils.deleteDirectory(instance);
    }
    instance.mkdir();
    return instance;
  }

  /**
   * The specific appserver implementation uses the server install directory to locate the immutable appserver
   * installation files used to start running instances in the working directory.
   */
  protected final File serverInstallDirectory() {
    if (!installation.isRepoInstall()) return installation.serverInstallDirectory();
    return new File(installation.serverInstallDirectory() + File.separator + serverType() + "-" + majorVersion() + "."
                    + minorVersion());

  }

  /**
   * The server name is used to create a parent directory for the server install directory which the appserver
   * implementation refers to as it's home directory.
   */
  protected final String serverType() {
    return installation.serverType();
  }

  protected final String majorVersion() {
    return installation.majorVersion();
  }

  protected final String minorVersion() {
    return installation.minorVersion();
  }

  protected final File serverBaseDirectory() {
    return installation.serverBaseDir();
  }

  protected final File sandboxDirectory() {
    return installation.sandboxDirectory();
  }

  /**
   * Implementing classes call this method to assign a series of properties to be available as system properties to the
   * appserver's JVM. Properties are optionally set by calling {@link StandardAppServerParameters}, passing a
   * <tt>Properties</tt> object to it's overloaded constructor. These instance specific properties are written to disk
   * and read by the appserver JVM. Two properties are always set by default: {@link AppServerConstants.APP_INSTANCE}
   * and {@link AppServerConstants.APP_PORT}.
   */
  protected final void setProperties(AppServerParameters params, int port, File instance) {
    Properties props = params.properties();
    if (props == null) props = new Properties();
    props.setProperty(AppServerConstants.APP_INSTANCE, params.instanceName());
    props.setProperty(AppServerConstants.APP_PORT, Integer.toString(port));
    File propsFile = new File(instance + ".properties");
    FileOutputStream fos = null;
    try {
      propsFile.createNewFile();
      fos = new FileOutputStream(propsFile, false);
      props.store(fos, "Available Application System Properties");
    } catch (IOException ioe) {
      throw new RuntimeException("Unable to write properties file to: " + propsFile, ioe);
    } finally {
      if (fos != null) {
        try {
          fos.close();
        } catch (IOException ioe) {
          throw new RuntimeException("Unable to write properties file to: " + propsFile, ioe);
        }
      }
    }
  }

  public final static class ConsoleLogger extends AbstractLogger {

    private static final DateFormat FORMAT = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss.SSS");

    private final String            instance;

    public ConsoleLogger(String instance) {
      this.instance = instance;
    }

    protected void doLog(LogLevel level, String message, String category) {
      String msg = "[" + FORMAT.format(new Date()) + "]" + "[" + level.getLevel() + "][" + instance + "] " + message;
      System.out.println(msg);
    }
  }
}
