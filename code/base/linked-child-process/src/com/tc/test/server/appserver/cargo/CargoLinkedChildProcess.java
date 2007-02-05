/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.cargo;

import com.tc.process.LinkedJavaProcessPollingAgent;
import com.tc.process.StartupAppender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Properties;

/**
 * This class serves three purposes. It delegates to {@link LinkedJavaProcessPollingAgent} to page the parent process.
 * And it loads a properties file which was written by the parent process (in the same directory as the log) and sets
 * all name value pairs as system properties for the appserver's JVM. This makes these available to servlets running in
 * the container.
 */
public final class CargoLinkedChildProcess {

  private static File instanceDir;

  private CargoLinkedChildProcess() {
    // cannot instantiate
  }

  public static void main(String[] args) throws Exception {
    String className = args[0];
    int port = Integer.parseInt(args[1]);
    instanceDir = new File(args[2]);

    String[] serverArgs = new String[0];
    if (args.length > 3) {
      serverArgs = new String[args.length - 3];
      for (int i = 3; i < args.length; i++) {
        serverArgs[i - 3] = args[i];
      }
    }

    System.out.println("JAVA VERSION: " + System.getProperty("java.version"));

    LinkedJavaProcessPollingAgent.startClientWatchdogService(port, className, true);
    loadProperties();

    invokeStartupAppender();

    try {
      Class startServer = Class.forName(className);
      Method main = startServer.getMethod("main", new Class[] { String[].class });
      main.invoke(null, new Object[] { serverArgs });

    } catch (ClassNotFoundException cnfe) {
      System.err.println("unable to locate server class: " + className);
      cnfe.printStackTrace();
    } catch (NoSuchMethodException nsme) {
      System.err.println("unable to access method: main()");
      nsme.printStackTrace();
    }
  }

  private static void loadProperties() {
    File sandbox = instanceDir.getParentFile();
    Properties props = new Properties();
    try {
      props.load(new FileInputStream(new File(sandbox + File.separator + instanceDir.getName() + ".properties")));
    } catch (IOException ioe) {
      throw new RuntimeException("Unable to load properties file: " + sandbox + File.separator + instanceDir.getName()
                                 + ".properties");
    }
    String name = null;
    for (Enumeration e = props.propertyNames(); e.hasMoreElements();) {
      name = (String) e.nextElement();
      System.setProperty(name, props.getProperty(name));
    }
  }

  private static void invokeStartupAppender() throws Exception {
    File startupAppenderJar = new File(instanceDir.getParent() + File.separator + StartupAppender.FILE_NAME);
    if (!startupAppenderJar.exists()) return;
    URL url = new URL("jar:file:" + startupAppenderJar.getAbsolutePath() + "!/");
    ClassLoader classLoader = new URLClassLoader(new URL[] { url }, CargoLinkedChildProcess.class.getClassLoader());
    URL appenderTypeUrl = new URL("jar:file:" + startupAppenderJar.getAbsolutePath() + "!/"
                                  + StartupAppender.APPENDER_TYPE_FILENAME);
    BufferedReader reader = new BufferedReader(new InputStreamReader(appenderTypeUrl.openStream()));
    String appenderType = reader.readLine();
    reader.close();
    Class appenderTypeClass = classLoader.loadClass(appenderType);
    StartupAppender appender = (StartupAppender) appenderTypeClass.newInstance();
    appender.append();
  }
}
