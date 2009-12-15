/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.serverdbbackuprunner;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.cli.CommandLineBuilder;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.object.ServerDBBackupMBean;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;

/**
 * Application that runs server backup by interacting with ServerDBBackupMBean. Expects 2 args: (1) hostname of machine
 * running DSO server (2) jmx server port number (3) path where the back up needs to be performed
 */

public class ServerDBBackupRunner {
  private String             host;
  private int                port;
  private final String       username;
  private final String       password;
  public static final String DEFAULT_HOST = "localhost";
  public static final int    DEFAULT_PORT = 9520;
  private JMXConnector       jmxConnector;

  public static void main(String[] args) {
    CommandLineBuilder commandLineBuilder = new CommandLineBuilder(ServerDBBackupRunner.class.getName(), args);

    commandLineBuilder.addOption("n", "hostname", true, "Terracotta Server instance hostname", String.class, false,
                                 "hostname");
    commandLineBuilder.addOption("p", "jmxport", true, "Terracotta Server instance JMX port", Integer.class, false,
                                 "jmx-port");
    commandLineBuilder.addOption("u", "username", true, "username", String.class, false);
    commandLineBuilder.addOption("w", "password", true, "password", String.class, false);
    commandLineBuilder.addOption("d", "directory", true, "Directory to back up to", String.class, false);
    commandLineBuilder.addOption("h", "help", String.class, false);

    commandLineBuilder.parse();

    String[] arguments = commandLineBuilder.getArguments();
    String host = null;
    int port = -1;

    if (arguments.length > 2) {
      commandLineBuilder.usageAndDie("backup-data.bat/backup-data.sh");
    }
    if (commandLineBuilder.hasOption('h')) {
      commandLineBuilder.usageAndDie("backup-data.bat/backup-data.sh");
    }

    String username = null;
    String password = null;
    if (commandLineBuilder.hasOption('u')) {
      username = commandLineBuilder.getOptionValue('u');
      if (commandLineBuilder.hasOption('w')) {
        password = commandLineBuilder.getOptionValue('w');
      } else {
        password = CommandLineBuilder.readPassword();
      }
    }

    String path = null;
    if (commandLineBuilder.hasOption('d')) {
      path = commandLineBuilder.getOptionValue('d');
    }

    String hostValue = commandLineBuilder.getOptionValue('n');
    String portValue = commandLineBuilder.getOptionValue('p');

    host = hostValue == null ? DEFAULT_HOST : hostValue;
    port = DEFAULT_PORT;

    if (portValue != null) {
      try {
        port = Integer.parseInt(portValue);
      } catch (NumberFormatException e) {
        System.err.println("Invalid port number specified. Using default port '" + port + "'");
      }
    }

    System.out.println("Invoking Backup Runner on Terracotta Server instance at '" + host + "', port " + port);

    ServerDBBackupRunner serverDBBackupRunner = null;
    try {
      serverDBBackupRunner = new ServerDBBackupRunner(host, port, username, password);
      serverDBBackupRunner.runBackup(path);
    } catch (Exception se) {
      System.err.println(se.getMessage());
      commandLineBuilder.usageAndDie("backup-data.bat/backup-data.sh");
    }

    if (path == null) path = serverDBBackupRunner.getDefaultBackupPath();

    System.out.println("The back up was successfully taken at " + path);
  }

  public ServerDBBackupRunner(String host, int port) {
    this(host, port, null, null);
  }

  public ServerDBBackupRunner(String host, int port, String username, String password) {
    this.host = host;
    this.port = port;
    this.username = username;
    this.password = password;
  }

  public void runBackup(String path) throws IOException {
    runBackup(path, null, null, null, true);
  }

  public void runBackup(String path, NotificationListener listener, NotificationFilter filter, Object obj,
                        boolean closeJMXAndListener) throws IOException {
    jmxConnector = CommandLineBuilder.getJMXConnector(username, password, host, port);
    MBeanServerConnection mbs = getMBeanServerConnection(jmxConnector, host, port);
    if (mbs == null) throw new RuntimeException("");
    ServerDBBackupMBean mbean = getServerDBBackupMBean(mbs);

    try {
      if (listener != null) {
        mbs.addNotificationListener(L2MBeanNames.SERVER_DB_BACKUP, listener, filter, obj);
      }
      mbean.runBackUp(path);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      String message = null;
      if (e.getCause() != null) {
        message = e.getCause().getMessage();
      } else {
        message = e.getMessage();
      }
      throw new RuntimeException(message);
    } finally {
      if (closeJMXAndListener) {
        removeListenerAndCloseJMX(listener, jmxConnector, mbs);
        jmxConnector = null;
      }
    }
  }

  public static ServerDBBackupMBean getServerDBBackupMBean(MBeanServerConnection mbs) {
    return MBeanServerInvocationProxy.newMBeanProxy(mbs, L2MBeanNames.SERVER_DB_BACKUP, ServerDBBackupMBean.class,
                                                    false);
  }

  public static MBeanServerConnection getMBeanServerConnection(final JMXConnector jmxConnector, String host, int port) {
    MBeanServerConnection mbs;
    try {
      mbs = jmxConnector.getMBeanServerConnection();
    } catch (IOException e1) {
      System.err.println("Unable to connect to host '" + host + "', port " + port
                         + ". Are you sure there is a Terracotta Server instance running there?");
      return null;
    }
    return mbs;
  }

  public void removeListenerAndCloseJMX(NotificationListener listener) {
    MBeanServerConnection mbs = getMBeanServerConnection(jmxConnector, host, port);
    removeListenerAndCloseJMX(listener, jmxConnector, mbs);
  }

  public static void removeListenerAndCloseJMX(NotificationListener listener, final JMXConnector jmxConnector,
                                               MBeanServerConnection mbs) {
    removeListener(listener, mbs);
    closeJMXConnector(jmxConnector);
  }

  private static void closeJMXConnector(final JMXConnector jmxConnector) {
    try {
      jmxConnector.close();
    } catch (IOException e) {
      System.err.println("Unable to close the JMX connector " + e.getMessage());
    }
  }

  private static void removeListener(NotificationListener listener, MBeanServerConnection mbs) {
    try {
      if (listener != null) mbs.removeNotificationListener(L2MBeanNames.SERVER_DB_BACKUP, listener);
    } catch (Exception e) {
      System.err.println("Unable to remove Listener " + e.getMessage());
    }
  }

  public String getDefaultBackupPath() {
    final JMXConnector jmxConn = CommandLineBuilder.getJMXConnector(username, password, host, port);
    MBeanServerConnection mbs = getMBeanServerConnection(jmxConn, host, port);
    if (mbs == null) return null;
    ServerDBBackupMBean mbean = getServerDBBackupMBean(mbs);

    String backupPath = null;
    try {
      backupPath = mbean.getDefaultPathForBackup();
    } finally {
      removeListenerAndCloseJMX(null, jmxConn, mbs);
    }
    return backupPath;
  }
}
