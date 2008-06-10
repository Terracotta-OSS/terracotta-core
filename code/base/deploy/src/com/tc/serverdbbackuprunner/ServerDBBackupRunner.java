/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.serverdbbackuprunner;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.object.ServerDBBackupMBean;

import java.io.IOException;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;

/**
 * Application that runs server backup by interacting with ServerDBBackupMBean. Expects 2 args: (1) hostname of machine
 * running DSO server (2) jmx server port number (3) path where the back up needs to be performed
 */

public class ServerDBBackupRunner {
  private String             m_host;
  private int                m_port;
  private String             m_userName;
  private String             m_dbBackupPath;
  public static final String DEFAULT_HOST = "localhost";
  public static final int    DEFAULT_PORT = 9520;

  public static void main(String[] args) {
    RunnerUtility runnerUtility = new RunnerUtility(ServerDBBackupRunner.class.getName(), args);

    runnerUtility.addOption("n", "hostname", true, "Terracotta Server hostname", String.class, false, "l2-hostname");
    runnerUtility.addOption("p", "jmxport", true, "Terracotta Server JMX port", Integer.class, false, "l2-jmx-port");
    runnerUtility.addOption("u", "username", true, "user name", String.class, false);
    runnerUtility.addOption("d", "pathForBackup", true, "Path for back up", String.class, false);
    runnerUtility.addOption("h", "help", String.class, false);

    runnerUtility.parse();
    runnerUtility.printArguments();

    String[] arguments = runnerUtility.getArguments();
    String host = null;
    int port = -1;

    if (arguments.length > 2) {
      runnerUtility.usageAndDie();
    }
    if (runnerUtility.hasOption('h')) {
      runnerUtility.usageAndDie();
    }

    String userName = null;
    if (runnerUtility.hasOption('u')) {
      userName = runnerUtility.getOptionValue('u');
    }
    String path = null;
    if (runnerUtility.hasOption('d')) {
      path = runnerUtility.getOptionValue('d');
    }

    if (arguments.length == 0) {
      host = DEFAULT_HOST;
      port = DEFAULT_PORT;
      System.err.println("No host or port provided. Invoking Backup Runner on Terracotta server at '" + host
                         + "', port " + port + " by default.");
    } else if (arguments.length == 1) {
      host = DEFAULT_HOST;
      try {
        port = Integer.parseInt(arguments[0]);
      } catch (NumberFormatException e) {
        port = DEFAULT_PORT;
        System.err.println("Invalid port number specified. Using default port '" + port + "'");
      }
    } else {
      host = arguments[0];
      port = Integer.parseInt(arguments[1]);
    }
    try {
      new ServerDBBackupRunner(host, port, userName).runBackup(path);
    } catch (Exception se) {
      System.err.println(se.getMessage());
      runnerUtility.usageAndDie();
    }
  }

  public ServerDBBackupRunner(String host, int port) {
    m_host = host;
    m_port = port;
  }

  public ServerDBBackupRunner(String host, int port, String userName) {
    this(host, port);
    m_userName = userName;
  }

  public void runBackup(String path) throws IOException {
    runBackupWithListener(path, null, null, null, null);
  }

  public void runBackupWithListener(String path, NotificationListener listener, NotificationFilter filter, Object obj,
                                     String listenerName) throws IOException {
    final JMXConnector jmxConnector = RunnerUtility.getJMXConnector(m_userName, m_host, m_port);
    MBeanServerConnection mbs;
    try {
      mbs = jmxConnector.getMBeanServerConnection();
    } catch (IOException e1) {
      System.err.println("Unable to connect to host '" + m_host + "', port " + m_port
                         + ". Are you sure there is a Terracotta server running there?");
      return;
    }
    ServerDBBackupMBean mbean = (ServerDBBackupMBean) MBeanServerInvocationProxy.newProxyInstance(mbs, L2MBeanNames.SERVER_DB_BACKUP,
                                                                              ServerDBBackupMBean.class, false);
    try {
      if (listener != null) {
        mbean.addNotificationListener(listener, filter, obj, listenerName);
      }

      mbean.runBackUp(path);
      m_dbBackupPath = mbean.getAbsolutePathForBackup();
    } catch (IOException e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    } finally {
      try {
        removeListener(listener, listenerName, mbean);
        jmxConnector.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void removeListener(NotificationListener listener, String listenerName, ServerDBBackupMBean mbean) {
    if (listener != null) {
      try {
        mbean.removeNotificationListener(listenerName);
      } catch (ListenerNotFoundException e) {
        e.printStackTrace();
      }
    }
  }

  public String getBackupPath() {
    return m_dbBackupPath;
  }
}
