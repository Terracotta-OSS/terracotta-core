/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.remote.connect;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.TerracottaMBean;
import com.tc.management.TerracottaManagement;
import com.tc.management.remote.protocol.ProtocolProvider;
import com.tc.management.remote.protocol.terracotta.ClientProvider;
import com.tc.management.remote.protocol.terracotta.TunnelingMessageConnection;
import com.tc.management.remote.protocol.terracotta.ClientTunnelingEventHandler.L1ConnectionMessage;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.MessageChannel;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class ClientConnectEventHandler extends AbstractEventHandler {

  private static final class ConnectorClosedFilter implements NotificationFilter {

    public boolean isNotificationEnabled(final Notification notification) {
      boolean enabled = false;
      if (notification instanceof JMXConnectionNotification) {
        final JMXConnectionNotification jmxcn = (JMXConnectionNotification) notification;
        enabled = jmxcn.getType().equals(JMXConnectionNotification.CLOSED);
      }
      return enabled;
    }

  }

  private static final class ConnectorClosedListener implements NotificationListener {

    private static final TCLogger       logger = TCLogging.getLogger(ConnectorClosedListener.class);

    private final MBeanServerConnection mBeanServerConnection;

    ConnectorClosedListener(final MBeanServerConnection mBeanServerConnection) {
      this.mBeanServerConnection = mBeanServerConnection;
    }

    final public void handleNotification(final Notification notification, final Object context) {
      logger.info("Tunneled JMX connection to a DSO client has terminated, unregistering its proxied beans:");
      for (Iterator beanNames = ((List) context).iterator(); beanNames.hasNext();) {
        final ObjectName dsoClientBeanName = (ObjectName) beanNames.next();
        try {
          logger.info("\tUnregistering proxied DSO client bean[" + dsoClientBeanName + "] from the L2");
          mBeanServerConnection.unregisterMBean(dsoClientBeanName);
        } catch (Exception e) {
          logger.warn("Unable to unregister DSO client bean[" + dsoClientBeanName + "]", e);
        }
      }
      logger.info("All proxied beans have been unregistered");
    }
  }

  private static final TCLogger logger = TCLogging.getLogger(ClientConnectEventHandler.class);

  public void handleEvent(EventContext context) throws EventHandlerException {
    L1ConnectionMessage msg = (L1ConnectionMessage) context;
    if (msg.isConnectingMsg()) {
      addJmxConnection(msg);
    } else {
      removeJmxConnection(msg);
    }
  }

  private void addJmxConnection(final L1ConnectionMessage msg) {
    final MessageChannel channel = msg.getChannel();
    final TCSocketAddress remoteAddress = channel != null ? channel.getRemoteAddress() : null;
    if (remoteAddress == null) { return; }

    final MBeanServer l2MBeanServer = msg.getMBeanServer();
    final Map channelIdToJmxConnector = msg.getChannelIdToJmxConnector();
    final Map channelIdToMsgConnection = msg.getChannelIdToMsgConnector();
    synchronized (channelIdToJmxConnector) {
      if (!channelIdToJmxConnector.containsKey(channel.getChannelID())) {
        JMXServiceURL serviceURL;
        try {
          serviceURL = new JMXServiceURL("terracotta", remoteAddress.getAddress().getHostName(), remoteAddress
              .getPort());
        } catch (MalformedURLException murle) {
          logger.error("Unable to construct a JMX service URL using DSO client channel from host["
                       + channel.getRemoteAddress() + "]; tunneled JMX connection will not be established", murle);
          return;
        }
        Map environment = new HashMap();
        ProtocolProvider.addTerracottaJmxProvider(environment);
        environment.put(ClientProvider.JMX_MESSAGE_CHANNEL, channel);
        environment.put(ClientProvider.CONNECTION_LIST, channelIdToMsgConnection);
        final JMXConnector jmxConnector;
        try {
          logger.info("An L1 has established a connection to the L2, attempting to connect to the client JMX server["
                      + serviceURL.toString() + "]");
          jmxConnector = JMXConnectorFactory.connect(serviceURL, environment);

          final MBeanServerConnection l1MBeanServerConnection = jmxConnector.getMBeanServerConnection();
          Set mBeans = l1MBeanServerConnection.queryNames(null, TerracottaManagement.matchAllTerracottaMBeans());
          List modifiedObjectNames = new ArrayList();
          for (Iterator iter = mBeans.iterator(); iter.hasNext();) {
            ObjectName objName = (ObjectName) iter.next();
            logger.info("Found L1 bean[" + objName.getCanonicalName() + "], attempting to register an L2 proxy for it");
            try {
              TerracottaMBean mBeanProxy = (TerracottaMBean) MBeanServerInvocationHandler
                  .newProxyInstance(l1MBeanServerConnection, objName, TerracottaMBean.class, false);
              ObjectName modifiedObjName = TerracottaManagement.addNodeInfo(objName, channel.getRemoteAddress());
              Class interfaceClass = Class.forName(mBeanProxy.getInterfaceClassName());
              logger.info("\tRegistration details: L1(" + interfaceClass.getName() + ") -> L2: ["
                          + objName.getCanonicalName() + "] -> [" + modifiedObjName.getCanonicalName() + "]");
              Object obj = MBeanServerInvocationHandler.newProxyInstance(l1MBeanServerConnection, objName,
                                                                         interfaceClass, mBeanProxy
                                                                             .isNotificationBroadcaster());
              l2MBeanServer.registerMBean(new StandardMBean(obj, interfaceClass), modifiedObjName);
              modifiedObjectNames.add(modifiedObjName);
              logger.info("Registration[" + objName.getCanonicalName() + "] successful");
            } catch (Exception e) {
              logger.error("Unable to register remote DSO client MBean[" + objName.getCanonicalName() + "] for host["
                           + channel.getRemoteAddress() + "], this bean will not show up in monitoring tools!!", e);
            }
          }
          try {
            jmxConnector.addConnectionNotificationListener(new ConnectorClosedListener(l2MBeanServer),
                                                           new ConnectorClosedFilter(), modifiedObjectNames);
          } catch (Exception e) {
            logger.error("Unable to register a JMX connection listener for the DSO client["
                         + channel.getRemoteAddress()
                         + "], if the DSO client disconnects the then its (dead) beans will not be unregistered", e);
          }
        } catch (IOException ioe) {
          logger.error("Unable to create tunneled JMX connection to the DSO client on host["
                       + channel.getRemoteAddress() + "], this DSO client will not show up in monitoring tools!!", ioe);
          return;
        }
        channelIdToJmxConnector.put(channel.getChannelID(), jmxConnector);
      } else {
        logger.warn("We are trying to create a new tunneled JMX connection but already have one for channel["
                    + channel.getRemoteAddress() + "], ignoring new connection message");
      }
    }
  }

  private void removeJmxConnection(final L1ConnectionMessage msg) {
    final MessageChannel channel = msg.getChannel();
    final Map channelIdToJmxConnector = msg.getChannelIdToJmxConnector();
    final Map channelIdToMsgConnection = msg.getChannelIdToMsgConnector();

    logger.info("DSO client channel[" + channel.getChannelID().toLong() + "] removed, closing tunneled JMX connection");

    synchronized (channelIdToMsgConnection) {
      final TunnelingMessageConnection tmc = (TunnelingMessageConnection) channelIdToMsgConnection.remove(channel
          .getChannelID());
      if (tmc != null) {
        try {
          tmc.close();
        } catch (IOException ioe) {
          logger.debug("Unable to close JMX tunneling message connection to DSO client[" + channel + "]", ioe);
        }
      }
    }

    synchronized (channelIdToJmxConnector) {
      if (channelIdToJmxConnector.containsKey(channel.getChannelID())) {
        final JMXConnector jmxConnector = (JMXConnector) channelIdToJmxConnector.remove(channel.getChannelID());
        if (jmxConnector != null) {
          try {
            jmxConnector.close();
          } catch (IOException ioe) {
            logger.debug("Unable to close JMX connector to DSO client[" + channel + "]", ioe);
          }
        }
      } else {
        logger.debug("DSO client channel closed without a corresponding tunneled JMX connection");
      }
    }
  }

}
