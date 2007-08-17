/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.gcrunner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;

import com.tc.admin.TCStop;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.management.JMXConnectorProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.object.ObjectManagementMonitorMBean;

import java.lang.reflect.Method;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.HashMap;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

/**
 * Application that runs gc by interacting with ObjectManagementMonitorMBean. Expects 2 args: (1) hostname of machine
 * running DSO server (2) jmx server port number
 */
public class GCRunner {
  private static final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();

  private String                m_host;
  private int                   m_port;
  private String                m_userName;

  public static final String    DEFAULT_HOST  = "localhost";
  public static final int       DEFAULT_PORT  = 9520;

  public static void main(String[] args) throws Exception {
    Options options = new Options();
    CommandLine commandLine = null;

    Option hostOption = new Option("n", "hostname", true, "Terracotta Server hostname");
    hostOption.setType(String.class);
    hostOption.setRequired(false);
    hostOption.setArgName("l2-hostname");
    options.addOption(hostOption);

    Option jmxPortOption = new Option("p", "jmxport", true, "Terracotta Server JMX port");
    jmxPortOption.setType(Integer.class);
    jmxPortOption.setRequired(false);
    jmxPortOption.setArgName("l2-jmx-port");
    options.addOption(jmxPortOption);

    Option userNameOption = new Option("u", "username", true, "user name");
    userNameOption.setType(String.class);
    userNameOption.setRequired(false);
    options.addOption(userNameOption);

    Option helpOption = new Option("h", "help");
    helpOption.setType(String.class);
    helpOption.setRequired(false);
    options.addOption(helpOption);

    try {
      commandLine = new GnuParser().parse(options, args);
    } catch (UnrecognizedOptionException e) {
      System.err.println(e.getMessage());
      usageAndDie(options);
    }

    System.err.println("args: " + Arrays.asList(commandLine.getArgs()));
    if (commandLine == null || commandLine.getArgs().length > 2) {
      usageAndDie(options);
    }

    if (commandLine.hasOption("h")) {
      new HelpFormatter().printHelp("java " + TCStop.class.getName(), options);
      System.exit(1);
    }

    String userName = null;
    if (commandLine.hasOption('u')) {
      userName = commandLine.getOptionValue('u');
    }

    String host = null;
    int port = -1;

    if (commandLine.getArgs().length == 0) {
      host = DEFAULT_HOST;
      port = DEFAULT_PORT;
      System.err.println("No host or port provided. Invoking GC on Terracotta server at '" + host + "', port " + port
                         + " by default.");
    } else if (commandLine.getArgs().length == 1) {
      host = DEFAULT_HOST;
      port = Integer.parseInt(commandLine.getArgs()[0]);
    } else {
      host = commandLine.getArgs()[0];
      port = Integer.parseInt(commandLine.getArgs()[1]);
    }

    try {
      new GCRunner(host, port, userName).runGC();
    } catch (ConnectException ce) {
      System.err.println("Unable to connect to host '" + host + "', port " + port
                         + ". Are you sure there is a Terracotta server running there?");
    } catch(SecurityException se) {
      System.err.println(se.getMessage());
      usageAndDie(options);
    }
  }

  private static void usageAndDie(Options options) throws Exception {
    new HelpFormatter().printHelp("java " + GCRunner.class.getName(), options);
    System.exit(1);
  }

  public GCRunner(String host, int port) {
    m_host = host;
    m_port = port;
  }

  public GCRunner(String host, int port, String userName) {
    this(host, port);
    m_userName = userName;
  }

  private void runGC() throws Exception {
    ObjectManagementMonitorMBean mbean = null;
    final JMXConnector jmxConnector = getJMXConnector();
    final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
    mbean = (ObjectManagementMonitorMBean) MBeanServerInvocationHandler
        .newProxyInstance(mbs, L2MBeanNames.OBJECT_MANAGEMENT, ObjectManagementMonitorMBean.class, false);

    try {
      mbean.runGC();
    } catch (RuntimeException re) {
      consoleLogger.error(re.getCause().getMessage());
    }
  }

  private static String getPassword() {
    try {
      Method m = System.class.getMethod("console", new Class[] {});
      Object console = m.invoke(null, (Object[])null);
      if (console != null) {
        m = console.getClass().getMethod("readPassword", new Class[] { String.class, Object[].class });
        if (m != null) {
          byte[] pw = (byte[]) m.invoke(console, new Object[] { "[%s]", "[console] Enter Password: " });
          return new String(pw);
        }
      }
    } catch (Exception e) {/**/
    }

    try {
      System.out.print("Enter password: ");
      return new jline.ConsoleReader().readLine(new Character('*'));
    } catch (Exception e) {/**/
    }

    return null;
  }

  private JMXConnector getJMXConnector() {
    HashMap env = null;

    if (m_userName != null) {
      env = new HashMap();
      String[] creds = { m_userName, getPassword() };
      env.put("jmx.remote.credentials", creds);
    }

    return new JMXConnectorProxy(m_host, m_port, env);
  }
}
