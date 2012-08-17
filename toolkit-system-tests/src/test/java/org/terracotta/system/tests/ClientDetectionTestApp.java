/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.system.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.test.util.JMXUtils;
import org.terracotta.toolkit.Toolkit;

import com.tc.management.beans.L2MBeanNames;
import com.tc.stats.api.DSOClientMBean;
import com.tc.stats.api.DSOMBean;

import java.util.HashSet;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import junit.framework.Assert;

public class ClientDetectionTestApp extends ClientBase implements NotificationListener {
  private JMXConnector          jmxc;
  private MBeanServerConnection mbsc;
  private DSOMBean              dsoMBean;

  private final Set             channelIdSet = new HashSet();

  public ClientDetectionTestApp(String[] args) {
    super(args);
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    int jmxPort = Integer.valueOf(getTestControlMbean().getGroupsData()[0].getJmxPort(0));
    jmxc = JMXUtils.getJMXConnector("localhost", jmxPort);
    mbsc = jmxc.getMBeanServerConnection();
    dsoMBean = MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.DSO, DSOMBean.class, false);
    getCurrentChannelIds();
    mbsc.addNotificationListener(L2MBeanNames.DSO, this, null, null);

    System.out.println("@@@@@@@ I'm online.... id = " + getClientID());

    System.out.println("Waiting For All Clients to come up");
    while (getClientsConnected(getDSOClientMBeans()) != 4) {
      Thread.sleep(1000);
      System.out.println("num of clients: " + getDSOClientMBeans().length);
    }

    Assert.assertEquals(ClientDetectionL1Client.NUM_OF_CLIENTS + 1, this.channelIdSet.size());

    System.out.println("Waiting For All Client Termination");
    while (getClientsConnected(getDSOClientMBeans()) != 1) {
      Thread.sleep(1000);
      System.out.println("num of clients: " + getDSOClientMBeans().length);
    }
  }

  private DSOClientMBean[] getDSOClientMBeans() {
    ObjectName[] clientObjectNames = dsoMBean.getClients();
    DSOClientMBean[] clients = new DSOClientMBean[clientObjectNames.length];
    for (int i = 0; i < clients.length; i++) {
      clients[i] = MBeanServerInvocationHandler.newProxyInstance(mbsc, clientObjectNames[i], DSOClientMBean.class,
                                                                 false);
    }
    return clients;
  }

  private void getCurrentChannelIds() {
    for (DSOClientMBean c : getDSOClientMBeans()) {
      channelIdSet.add(String.valueOf(c.getChannelID().toLong()));
    }
  }

  private int getClientsConnected(DSOClientMBean[] clientMBeans) {
    Set set = new HashSet<String>();
    for (DSOClientMBean bean : clientMBeans) {
      try {
        System.out.println("got channel Id from dso bean: " + bean.getChannelID().toLong());
        set.add(bean.getChannelID().toString() + bean.getRemoteAddress());
      } catch (Exception e) {
        // client might have disconnected
        continue;
      }
    }
    System.out.println("ClientPresent: " + set);
    return set.size();
  }

  public void handleNotification(Notification notification, Object handback) {
    if ("dso.client.attached".equalsIgnoreCase(notification.getType())) {
      // extract channel id
      String source = notification.getSource().toString();
      int index = source.lastIndexOf('=');
      String channelId = source.substring(index + 1);

      System.out.println(">>>>> notification of channelId: " + channelId);
      Assert.assertFalse("duplicate clients notification found", channelIdSet.contains(channelId));
      channelIdSet.add(channelId);
    }

  }

}
