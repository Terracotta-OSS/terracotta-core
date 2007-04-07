/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver;

import org.apache.commons.io.FileUtils;
import org.codehaus.cargo.util.log.LogLevel;
import org.codehaus.cargo.util.log.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
    instance = new File(installation.getSandboxDirectory() + File.separator + params.instanceName());
    if (instance.exists()) {
      FileUtils.deleteDirectory(instance);
    }
    instance.mkdir();
    initiateStartupAppender(installation.getSandboxDirectory());
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

  /**
   * Subclasses may provide implementations to allow preprocessing to take place in the appserver child process JVM.
   * Appenders must implement <tt>StartupAppender</tt>.
   */
  protected void initiateStartupAppender(File sandboxDir) throws Exception {
    // not implemented
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

  public final static class ConsoleLogger implements Logger {

    private static final DateFormat FORMAT = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss.SSS");

    private final String            instance;
    private final boolean           debugEnabled;
    private LogLevel                logLevel;

    public ConsoleLogger(String instance, boolean debugEnabled) {
      this.instance = instance;
      this.debugEnabled = debugEnabled;
      this.logLevel = LogLevel.INFO;
    }

    public void info(String message, String category) {
      log("info", message, category);
    }

    public void debug(String message, String category) {
      if (debugEnabled) {
        log("debug", message, category);
      }
    }

    public void warn(String message, String category) {
      log("warn", message, category);
    }

    private void log(String severity, String message, String category) {
//      System.out.println(FORMAT.format(new Date()) + " [" + severity + "][" + category + "][" + instance + "] "
//                         + message);
      System.out.println("[" + severity + "]: " + message);
    }

    public LogLevel getLevel() {
      return logLevel;
    }

    public void setLevel(LogLevel level) {
      logLevel = level;
    }
  }
}
