/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.server.util;

import org.apache.commons.io.IOUtils;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.cli.CommandLineBuilder;
import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.management.TerracottaManagement;
import com.tc.management.beans.L1MBeanNames;
import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.util.Conversion;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

public class ClusterDumper {

  private final String           host;
  private final int              port;
  private final String           username;
  private final String           password;

  private static final String    FILENAME_FORMAT = "cluster-thread-dump-%s.zip";
  private static final int       ZIP_BUFFER_SIZE = 2048;
  private static final String    DEFAULT_HOST    = "localhost";
  private static final int       DEFAULT_PORT    = 9520;

  private final SimpleDateFormat dateFormat      = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

  public static void main(String[] args) throws Exception {
    CommandLineBuilder commandLineBuilder = new CommandLineBuilder(ClusterDumper.class.getName(), args);

    commandLineBuilder.addOption("n", "hostname", true, "Terracotta Server instance hostname.", String.class, false,
                                 "l2-hostname");
    commandLineBuilder.addOption("p", "jmxport", true, "Terracotta Server instance JMX port.", Integer.class, false,
                                 "l2-jmx-port");
    commandLineBuilder.addOption("c", "Take only client dumps.", String.class, false);
    commandLineBuilder.addOption("s", "Take only server dumps.", String.class, false);
    commandLineBuilder.addOption("u", "username", true, "username", String.class, false);
    commandLineBuilder.addOption("w", "password", true, "password", String.class, false);
    commandLineBuilder.addOption("d", "Take cluster state dump. Check server/client logs for the dump.", String.class,
                                 false);
    commandLineBuilder.addOption("h", "help", String.class, false);

    commandLineBuilder.parse();

    String[] arguments = commandLineBuilder.getArguments();
    if (arguments.length > 2) {
      commandLineBuilder.usageAndDie();
    }

    if (commandLineBuilder.hasOption('h')) {
      System.out.println("Debugger script to take cluster thread dumps (default) or cluster state dump.");
      commandLineBuilder.usageAndDie();
    }

    boolean server = true;
    boolean client = true;

    if (commandLineBuilder.hasOption('s')) {
      client = false;
      System.out.println("Taking dumps only for server(s).");
    } else if (commandLineBuilder.hasOption('c')) {
      server = false;
      System.out.println("Taking dumps only for client(s).");
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

    String host = commandLineBuilder.getOptionValue('n');
    String portString = commandLineBuilder.getOptionValue('p');
    int port = portString != null ? parsePort(commandLineBuilder.getOptionValue('p')) : DEFAULT_PORT;

    if (arguments.length == 1) {
      host = DEFAULT_HOST;
      port = parsePort(arguments[0]);
    } else if (arguments.length == 2) {
      host = arguments[0];
      port = parsePort(arguments[1]);
    }

    host = host == null ? DEFAULT_HOST : host;
    ClusterDumper dumper = new ClusterDumper(host, port, username, password);
    try {
      System.out.println("Connecting " + host + ":" + port + "...");
      if (commandLineBuilder.hasOption('d')) {
        dumper.takeClusterStateDump(server, client);
      } else {
        dumper.takeClusterThreadDump(server, client);
      }
    } catch (IOException ioe) {
      System.out.println("Unable to connect to host '" + host + "', port " + port
                         + ". Are you sure there is a Terracotta server instance running there?");
    } catch (SecurityException se) {
      System.out.println(se.getMessage());
      commandLineBuilder.usageAndDie();
    }
  }

  private static int parsePort(String portString) {
    int port = -1;
    try {
      port = Integer.parseInt(portString);
    } catch (NumberFormatException e) {
      port = DEFAULT_PORT;
      System.err.println("Invalid port number specified. Using default port '" + port + "'");
    }
    return port;
  }

  public ClusterDumper(String host, int port) {
    this(host, port, null, null);
  }

  public ClusterDumper(String host, int port, String userName, String password) {
    this.host = host;
    this.port = port;
    this.username = userName;
    this.password = password;
  }

  public void takeClusterStateDump(boolean server, boolean client) throws Exception {
    ServerGroupInfo[] serverGrpInfos = getServerGroupInfo();
    L2Info activeCoordinator = findActiveCoordinator(serverGrpInfos);

    if (server) doServerStateDumps(serverGrpInfos);
    if (client) doClientsStateDump(activeCoordinator.host(), activeCoordinator.jmxPort());
    System.out.println("\nCluster state dump taken successfully. ");
  }

  public void takeClusterThreadDump(boolean server, boolean client) throws Exception {
    ServerGroupInfo[] serverGrpInfos = getServerGroupInfo();
    L2Info activeCoordinator = findActiveCoordinator(serverGrpInfos);

    File file = new File(String.format(FILENAME_FORMAT, dateFormat.format(new Date())));
    FileOutputStream fos = new FileOutputStream(file);
    ZipOutputStream zout = new ZipOutputStream(fos);

    if (server) doServerThreadDump(serverGrpInfos, zout);
    if (client) doClientThreadDumps(activeCoordinator.host(), activeCoordinator.jmxPort(), zout);
    zout.close();
    System.out.println("\nAll thread dumps taken successfully. ");
    System.out.println("Zipped to " + file.getAbsolutePath());
  }

  private void doServerStateDumps(ServerGroupInfo[] serverGrpInfos) {
    System.out.println("\nTaking Server State dumps.");
    System.out.println("==========================\n");
    for (ServerGroupInfo serverGrpInfo : serverGrpInfos) {
      L2Info[] members = serverGrpInfo.members();
      for (L2Info member : members) {
        L2DumperMBean mbean = null;
        JMXConnector jmxConnector = null;

        try {
          String hostName = member.host();
          int jmxPort = member.jmxPort();
          System.out.println("Trying to take Server State Dump for " + hostName + ":" + jmxPort);
          jmxConnector = CommandLineBuilder.getJMXConnector(username, password, hostName, jmxPort);
          final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
          mbean = MBeanServerInvocationProxy.newMBeanProxy(mbs, L2MBeanNames.DUMPER, L2DumperMBean.class, false);
          mbean.doServerDump();
          System.out.println("Server State Dump taken for " + hostName + ":" + jmxPort);
        } catch (Exception e) {
          System.out.println((e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
        } finally {
          if (jmxConnector != null) {
            try {
              jmxConnector.close();
            } catch (Exception e) {
              // System.out.println("Exception while trying to close the JMX connector for port no: " + jmxPort);
            }
          }
        }
      }
    }
  }

  private void doServerThreadDump(ServerGroupInfo[] serverGrpInfos, ZipOutputStream zout) {
    System.out.println("\nTaking Server Thread dumps.");
    System.out.println("===========================\n");
    for (ServerGroupInfo serverGrpInfo : serverGrpInfos) {
      L2Info[] members = serverGrpInfo.members();
      for (L2Info member : members) {
        TCServerInfoMBean mbean = null;
        JMXConnector jmxConnector = null;

        try {
          String hostName = member.host();
          int jmxPort = member.jmxPort();
          System.out.println("Trying to take Server Thread Dump for " + hostName + ":" + jmxPort);
          jmxConnector = CommandLineBuilder.getJMXConnector(username, password, hostName, jmxPort);
          final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
          mbean = MBeanServerInvocationProxy.newMBeanProxy(mbs, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class,
                                                           false);
          byte[] zipped = mbean.takeCompressedThreadDump(0);
          String node = hostName + "/" + jmxPort;
          ZipEntry entry = new ZipEntry(String.format("server-%s/%s.log", node, dateFormat.format(new Date())));
          zout.putNextEntry(entry);
          zout.write(decompress(zipped));
          zout.closeEntry();

          System.out.println("Server Thread Dump taken for " + hostName + ":" + jmxPort);
        } catch (Exception e) {
          System.out.println((e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
        } finally {
          if (jmxConnector != null) {
            try {
              jmxConnector.close();
            } catch (Exception e) {
              // System.out.println("Exception while trying to close the JMX connector for port no: " + jmxPort);
            }
          }
        }
      }
    }
  }

  protected byte[] decompress(byte[] zippedByte) {
    ZipInputStream zIn = new ZipInputStream(new ByteArrayInputStream(zippedByte));
    StringBuilder sb = new StringBuilder();
    try {
      byte[] buffer = new byte[ZIP_BUFFER_SIZE];
      while (zIn.getNextEntry() != null) {
        int len = 0;
        while ((len = zIn.read(buffer)) > 0) {
          if (len < ZIP_BUFFER_SIZE) {
            sb.append(Conversion.bytes2String(buffer).substring(0, len));
          } else {
            sb.append(Conversion.bytes2String(buffer));
          }
        }
      }
      return sb.toString().getBytes();
    } catch (IOException e) {
      System.err.println("Error decompressing bytes: " + e.getMessage());
      return new byte[0];
    } finally {
      IOUtils.closeQuietly(zIn);
    }
  }

  private L2Info findActiveCoordinator(ServerGroupInfo[] serverGrpInfos) {
    L2Info[] l2Infos = null;
    for (ServerGroupInfo serverGrpInfo : serverGrpInfos) {
      if (serverGrpInfo.isCoordinator()) {
        l2Infos = serverGrpInfo.members();
        break;
      }
    }

    if (l2Infos == null) { throw new IllegalStateException("Active coordinator group not found,"
                                                           + " clients dump are not taken."); }

    for (L2Info l2Info : l2Infos) {
      String hostName = l2Info.host();
      int jmxPort = l2Info.jmxPort();
      if (isActive(hostName, jmxPort)) { return l2Info; }
    }

    throw new IllegalStateException("Active coordinator group not found, clients dump are not taken.");
  }

  private ServerGroupInfo[] getServerGroupInfo() throws Exception {
    ServerGroupInfo[] serverGrpInfos = null;
    TCServerInfoMBean mbean = null;
    final JMXConnector jmxConnector = CommandLineBuilder.getJMXConnector(username, password, host, port);
    final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
    mbean = MBeanServerInvocationProxy.newMBeanProxy(mbs, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, false);
    serverGrpInfos = mbean.getServerGroupInfo();
    jmxConnector.close();
    return serverGrpInfos;
  }

  private boolean isActive(String hostname, int jmxPort) {
    TCServerInfoMBean mbean = null;
    boolean isActive = false;
    JMXConnector jmxConnector = null;

    try {
      jmxConnector = CommandLineBuilder.getJMXConnector(username, password, hostname, jmxPort);
      final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
      mbean = MBeanServerInvocationProxy
          .newMBeanProxy(mbs, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, false);
      isActive = mbean.isActive();
    } catch (Exception e) {
      return false;
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (Exception e) {
          // System.out.println("Exception while trying to close the JMX connector for port no: " + jmxPort);
        }
      }
    }

    return isActive;
  }

  private void doClientThreadDumps(String hostName, int jmxPort, ZipOutputStream zout) {
    System.out.println("\nTaking Client Thread Dumps \nby connecting Active Coordinator " + hostName + ":" + jmxPort);
    System.out.println("=========================================\n");
    JMXConnector jmxConnector = null;
    try {
      jmxConnector = CommandLineBuilder.getJMXConnector(username, password, hostName, jmxPort);
      final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
      Set allL1DumperMBeans = mbs
          .queryNames(new ObjectName(L1MBeanNames.L1INFO_PUBLIC.getCanonicalName() + ",*"), null);

      if (allL1DumperMBeans.size() == 0) {
        System.out.println("*** No Clients connected to the cluster. ***");
        return;
      }

      for (Iterator iterator = allL1DumperMBeans.iterator(); iterator.hasNext();) {
        ObjectName l1DumperBean = (ObjectName) iterator.next();
        byte[] zipped = (byte[]) mbs.invoke(l1DumperBean, "takeCompressedThreadDump", new Object[] { 0 },
                                            new String[] { long.class.getName() });
        String node = l1DumperBean.getKeyProperty("node");
        ZipEntry entry = new ZipEntry(String.format("client-%s/%s.log", node, dateFormat.format(new Date())));
        zout.putNextEntry(entry);
        zout.write(decompress(zipped));
        zout.closeEntry();

        System.out.println("Client Thread Dump taken for " + node);
      }
    } catch (Exception e) {
      System.err.println((e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (Exception e) {
          // System.out.println("Exception while trying to close the JMX connector for port no: " + jmxPort);
        }
      }
    }
  }

  private void doClientsStateDump(String hostName, int jmxPort) {
    System.out.println("\nTaking Client State Dumps \nby connecting Active Coordinator " + hostName + ":" + jmxPort);
    System.out.println("=========================================\n");
    JMXConnector jmxConnector = null;
    try {
      jmxConnector = CommandLineBuilder.getJMXConnector(username, password, hostName, jmxPort);
      final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
      Set allL1DumperMBeans;
      allL1DumperMBeans = TerracottaManagement.getAllL1DumperMBeans(mbs);
      if (allL1DumperMBeans.size() == 0) {
        System.out.println("*** No Clients connected the cluster. ***");
        return;
      }

      for (Iterator iterator = allL1DumperMBeans.iterator(); iterator.hasNext();) {
        ObjectName l1DumperBean = (ObjectName) iterator.next();
        mbs.invoke(l1DumperBean, "doClientDump", new Object[] {}, new String[] {});
        String node = l1DumperBean.getKeyProperty("node");
        System.out.println("Client State Dump taken for " + node);
      }
    } catch (Exception e) {
      System.err.println((e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (Exception e) {
          // System.out.println("Exception while trying to close the JMX connector for port no: " + jmxPort);
        }
      }
    }
  }
}
