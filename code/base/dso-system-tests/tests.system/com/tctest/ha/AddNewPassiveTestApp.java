/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest.ha;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.cli.CommandLineBuilder;
import com.tc.config.schema.L2Info;
import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.test.InstrumentedClassConfigBuilderImpl;
import com.tc.config.schema.test.LockConfigBuilderImpl;
import com.tc.config.schema.test.RootConfigBuilderImpl;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.management.JMXConnectorProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.object.EnterpriseTCClientMbean;
import com.tc.management.beans.object.EnterpriseTCServerMbean;
import com.tc.objectserver.control.ExtraProcessServerControl;
import com.tc.stats.DSOClientMBean;
import com.tc.stats.DSOMBean;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

public class AddNewPassiveTestApp {
  private static int                jmxPort1;
  private static int                jmxPort2;
  private static int                dsoPort1;
  private static int                dsoPort2;
  private static int                l2Port1;
  private static int                l2Port2;

  private static String             configFilePath;
  private static String             javaHomePath;

  private static ArrayList<Integer> mySharedArrayList = new ArrayList<Integer>();

  public static void main(String[] args) {
    getFromArgs(args);

    addToList(0);

    printServersInTheCluster();

    changeTcConfigFile();

    reloadConfig();
    ThreadUtil.reallySleep(1000);

    printServersInTheCluster();

    // start a new server
    startServer2();

    boolean isPassive = checkIfServer2IsPassive(jmxPort2);
    Assert.assertTrue(isPassive);

    killServer(jmxPort1);

    ThreadUtil.reallySleep(15000);

    boolean isActive = checkIfServer2IsActive(jmxPort2);
    Assert.assertTrue(isActive);

    addToList(1000);

    ThreadUtil.reallySleep(15000);

    doAsserts(2000);

    killServer(jmxPort2);
  }

  private static void getFromArgs(String[] args) {
    jmxPort1 = Integer.parseInt(args[0]);
    jmxPort2 = Integer.parseInt(args[1]);
    dsoPort1 = Integer.parseInt(args[2]);
    dsoPort2 = Integer.parseInt(args[3]);
    l2Port1 = Integer.parseInt(args[4]);
    l2Port2 = Integer.parseInt(args[5]);
    configFilePath = args[6];
    javaHomePath = args[7];
  }

  public static String[] createArgs(int dPort1, int dPort2, int jPort1, int jPort2, int l2GrpPort1, int l2GrpPort2,
                                    String cfgPath, String javaPath) {
    String[] args = new String[8];
    args[0] = jPort1 + "";
    args[1] = jPort2 + "";
    args[2] = dPort1 + "";
    args[3] = dPort2 + "";
    args[4] = l2GrpPort1 + "";
    args[5] = l2GrpPort2 + "";
    args[6] = cfgPath;
    args[7] = javaPath;

    return args;
  }

  private static boolean checkIfServer2IsPassive(int jmxPort) {
    final JMXConnector jmxConnector = CommandLineBuilder.getJMXConnector("localhost", jmxPort);
    MBeanServerConnection mbs = getMBeanServerConnection(jmxConnector, "localhost", jmxPort);
    TCServerInfoMBean serverInfoMbean = getTCServerInfoMbean(mbs);

    boolean isPassive = serverInfoMbean.isPassiveStandby();
    closeJMXConnector(jmxConnector);

    return isPassive;
  }

  public static boolean checkIfServer2IsActive(int jmxPort) {
    try {
      final JMXConnector jmxConnector = CommandLineBuilder.getJMXConnector("localhost", jmxPort);
      MBeanServerConnection mbs = getMBeanServerConnection(jmxConnector, "localhost", jmxPort);
      TCServerInfoMBean serverInfoMbean = getTCServerInfoMbean(mbs);

      boolean isActive = serverInfoMbean.isActive();
      closeJMXConnector(jmxConnector);
      return isActive;
    } catch (Exception e) {
      return false;
    }
  }

  private static void killServer(int jmxPort) {
    final JMXConnector jmxConnector = CommandLineBuilder.getJMXConnector("localhost", jmxPort);
    MBeanServerConnection mbs = getMBeanServerConnection(jmxConnector, "localhost", jmxPort);
    TCServerInfoMBean serverInfoMbean = getTCServerInfoMbean(mbs);

    serverInfoMbean.shutdown();

    closeJMXConnector(jmxConnector);
  }

  private static void addToList(int no) {
    synchronized (mySharedArrayList) {
      for (int i = 0; i < 1000; i++) {
        mySharedArrayList.add(i + no);
      }
    }
  }

  private static void doAsserts(int size) {
    synchronized (mySharedArrayList) {
      int expected = 0;
      for (Integer i : mySharedArrayList) {
        Assert.assertEquals(new Integer(expected), i);
        expected++;
      }
      System.out.println("Size === " + mySharedArrayList.size());
      Assert.assertEquals(size, mySharedArrayList.size());
    }
  }

  private static void changeTcConfigFile() {
    AddNewPassiveTest.writeConfigFile(false, configFilePath, dsoPort1, dsoPort2, jmxPort1, jmxPort2, l2Port1, l2Port2);
  }

  private static void reloadConfig() {
    reloadServerConfig();
    reloadClientConfig();
  }

  private static void reloadClientConfig() {
    JMXConnectorProxy jmxConnector = new JMXConnectorProxy("localhost", jmxPort1);
    MBeanServerConnection mbsc = getMBeanServerConnection(jmxConnector, "localhost", jmxPort1);
    DSOMBean dsoMBean = (DSOMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.DSO,
                                                                                 DSOMBean.class, false);

    ObjectName[] clientObjectNames = dsoMBean.getClients();
    DSOClientMBean[] clients = new DSOClientMBean[clientObjectNames.length];
    for (int i = 0; i < clients.length; i++) {
      clients[i] = (DSOClientMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc, clientObjectNames[i],
                                                                                  DSOClientMBean.class, false);
      EnterpriseTCClientMbean l1InfoMbean = (EnterpriseTCClientMbean) MBeanServerInvocationHandler
          .newProxyInstance(mbsc, clients[i].getEnterpriseTCClientBeanName(), EnterpriseTCClientMbean.class, false);
      try {
        l1InfoMbean.reloadConfiguration();
      } catch (ConfigurationSetupException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }
    closeJMXConnector(jmxConnector);
  }

  private static void reloadServerConfig() {
    final JMXConnector jmxConnector = CommandLineBuilder.getJMXConnector("localhost", jmxPort1);
    MBeanServerConnection mbs = getMBeanServerConnection(jmxConnector, "localhost", jmxPort1);
    EnterpriseTCServerMbean enterpriseServerMbean = getEnterpriseServerMbean(mbs);
    try {
      enterpriseServerMbean.reloadConfiguration();
    } catch (ConfigurationSetupException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }

    closeJMXConnector(jmxConnector);
  }

  private static void printServersInTheCluster() {
    final JMXConnector jmxConnector = CommandLineBuilder.getJMXConnector("localhost", jmxPort1);
    MBeanServerConnection mbs = getMBeanServerConnection(jmxConnector, "localhost", jmxPort1);
    TCServerInfoMBean serverInfoMbean = getTCServerInfoMbean(mbs);
    L2Info[] l2infos = serverInfoMbean.getL2Info();

    for (int i = 0; i < l2infos.length; i++) {
      System.out.println("Server " + i + " Info ---------------- ");
      System.out.println("Name=" + l2infos[i].name());
      System.out.println("Jmx=" + l2infos[i].jmxPort());
    }

    closeJMXConnector(jmxConnector);
  }

  private static MBeanServerConnection getMBeanServerConnection(final JMXConnector jmxConnector, String host, int port) {
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

  private static TCServerInfoMBean getTCServerInfoMbean(MBeanServerConnection mbs) {
    return MBeanServerInvocationProxy.newMBeanProxy(mbs, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, false);
  }

  private static EnterpriseTCServerMbean getEnterpriseServerMbean(MBeanServerConnection mbs) {
    return MBeanServerInvocationProxy.newMBeanProxy(mbs, L2MBeanNames.ENTERPRISE_TC_SERVER,
                                                    EnterpriseTCServerMbean.class, false);
  }

  private static void closeJMXConnector(final JMXConnector jmxConnector) {
    try {
      jmxConnector.close();
    } catch (IOException e) {
      System.err.println("Unable to close the JMX connector " + e.getMessage());
    }
  }

  private static void startServer2() {
    ExtraProcessServerControl processControl = new ExtraProcessServerControl("localhost", dsoPort2, jmxPort2,
                                                                             configFilePath, true, "1-localhost",
                                                                             Collections.EMPTY_LIST,
                                                                             new File(javaHomePath), true);

    try {
      processControl.start();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    ThreadUtil.reallySleep(1000 * 20);
  }

  public static TerracottaConfigBuilder getTerracottaConfigBuilder() {
    try {
      TerracottaConfigBuilder cb = new TerracottaConfigBuilder();

      InstrumentedClassConfigBuilder instrumented1 = new InstrumentedClassConfigBuilderImpl();
      instrumented1.setClassExpression(AddNewPassiveTestApp.class.getName());

      cb.getApplication().getDSO().setInstrumentedClasses(new InstrumentedClassConfigBuilder[] { instrumented1 });

      LockConfigBuilder lock1 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
      lock1.setLockLevel(LockConfigBuilder.LEVEL_WRITE);
      lock1.setMethodExpression("* " + AddNewPassiveTestApp.class.getName() + "*.*(..)");

      cb.getApplication().getDSO().setLocks(new LockConfigBuilder[] { lock1 });

      RootConfigBuilder root1 = new RootConfigBuilderImpl();
      root1.setFieldName(AddNewPassiveTestApp.class.getName() + ".mySharedArrayList");
      root1.setRootName("mySharedArrayList");

      cb.getApplication().getDSO().setRoots(new RootConfigBuilder[] { root1 });

      return cb;
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }
}
