/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.env;

import java.io.PrintStream;
import java.lang.reflect.Method;

public class EnvironmentDetector {

  private final PrintStream m_stream;

  public EnvironmentDetector(PrintStream s) {
    m_stream = s;
  }

  public void printTomcatInfo() {
    String info;
    try {
      // TODO check if this class and method exists for all Tomcat versions - I have only looked at 5.x
      Class serverInfo = Class.forName(
          "org.apache.catalina.util.ServerInfo", 
          true,
          EnvironmentDetector.class.getClassLoader()
      );
      Method getServerInfo = serverInfo.getMethod("getServerInfo", new Class[]{});
      info = (String)getServerInfo.invoke(null, new Object[]{});
    } catch (Throwable e) {
      // ignore - not Tomcat
      return;
    } 
    m_stream.println("Tomcat Version:\t" + info);
  }

  public void printJavaInfo() {
    m_stream.println("JVM Vendor:\t\t" + System.getProperty("java.vendor").toUpperCase());
    m_stream.println("JVM Version:\t\tto" + System.getProperty("java.version").toUpperCase());
  }

  public void printOSInfo() {
    m_stream.println("OS Arch:\t\t" + System.getProperty("os.arch").toUpperCase());
    m_stream.println("OS Name:\t\t" + System.getProperty("os.name").toUpperCase());
    m_stream.println("OS Version:\t\t" + System.getProperty("os.version").toUpperCase());
  }

  public void printEnv() {
    EnvironmentDetector detector = new EnvironmentDetector(m_stream);
    m_stream.println("----------------------------------");
    detector.printOSInfo();
    detector.printJavaInfo();
    detector.printTomcatInfo();
  }

  public static void main(String[] args) {
    new EnvironmentDetector(System.out).printEnv();
  }
}