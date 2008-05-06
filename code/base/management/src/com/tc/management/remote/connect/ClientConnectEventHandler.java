/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.remote.connect;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.TerracottaMBean;
import com.tc.management.TerracottaManagement;
import com.tc.management.remote.protocol.ProtocolProvider;
import com.tc.management.remote.protocol.terracotta.ClientProvider;
import com.tc.management.remote.protocol.terracotta.ClientTunnelingEventHandler.L1ConnectionMessage;
import com.tc.management.remote.protocol.terracotta.TunnelingMessageConnection;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.statistics.StatisticsGateway;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.generic.ConnectionClosedException;

public class ClientConnectEventHandler extends AbstractEventHandler {

  private final StatisticsGateway statisticsGateway;

  public ClientConnectEventHandler(StatisticsGateway statisticsGateway) {
    this.statisticsGateway = statisticsGateway;
  }

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

    private final MBeanServer beanServer;

    ConnectorClosedListener(final MBeanServer mBeanServer) {
      beanServer = mBeanServer;
    }

    final public void handleNotification(final Notification notification, final Object context) {
      unregisterBeans(beanServer, (List) context);
    }
  }

  private static final TCLogger logger = TCLogging.getLogger(ClientConnectEventHandler.class);

  public void handleEvent(EventContext context) {
    L1ConnectionMessage msg = (L1ConnectionMessage) context;
    if (msg.isConnectingMsg()) {
      addJmxConnection(msg);
    } else {
      removeJmxConnection(msg);
    }
  }

  class ProxyStandardMBean extends StandardMBean implements NotificationBroadcaster {
    ProxyStandardMBean(Object proxy, Class interfaceClass) throws NotCompliantMBeanException {
      super(proxy, interfaceClass);
    }

    protected String getClassName(MBeanInfo info) {
      Object proxy = getImplementation();
      if(proxy instanceof NotificationBroadcaster) {
        return NotificationBroadcaster.class.getName();
      }
      return super.getClassName(info);
    }

    public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback)
        throws IllegalArgumentException {
      Object proxy = getImplementation();
      if(proxy instanceof NotificationBroadcaster) {
        ((NotificationBroadcaster)proxy).addNotificationListener(listener, filter, handback);
      }
    }

    public MBeanNotificationInfo[] getNotificationInfo() {
      Object proxy = getImplementation();
      if(proxy instanceof NotificationBroadcaster) {
        return ((NotificationBroadcaster)proxy).getNotificationInfo();
      }
      return new MBeanNotificationInfo[0];
    }

    public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
      Object proxy = getImplementation();
      if(proxy instanceof NotificationBroadcaster) {
        ((NotificationBroadcaster)proxy).removeNotificationListener(listener);
      }
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
          serviceURL = new JMXServiceURL("terracotta", remoteAddress.getAddress().getHostAddress(), remoteAddress
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
        environment.put("jmx.remote.x.request.timeout", new Long(Long.MAX_VALUE));
        environment.put("jmx.remote.x.client.connection.check.period", new Long(0));
        environment.put("jmx.remote.x.server.connection.timeout", new Long(Long.MAX_VALUE));

        final JMXConnector jmxConnector;
        try {
          jmxConnector = JMXConnectorFactory.connect(serviceURL, environment);

          final MBeanServerConnection l1MBeanServerConnection = jmxConnector.getMBeanServerConnection();

          statisticsGateway.addStatisticsAgent(channel.getChannelID(), l1MBeanServerConnection);

          Set mBeans = l1MBeanServerConnection.queryNames(null, TerracottaManagement.matchAllTerracottaMBeans());
          List modifiedObjectNames = new ArrayList();
          for (Iterator iter = mBeans.iterator(); iter.hasNext();) {
            ObjectName objName = (ObjectName) iter.next();
            try {
              TerracottaMBean mBeanProxy = (TerracottaMBean) MBeanServerInvocationHandler
                  .newProxyInstance(l1MBeanServerConnection, objName, TerracottaMBean.class, false);
              ObjectName modifiedObjName = TerracottaManagement.addNodeInfo(objName, channel.getRemoteAddress());
              Class interfaceClass = Class.forName(mBeanProxy.getInterfaceClassName());
              boolean isNotificationBroadcaster = mBeanProxy.isNotificationBroadcaster();
              Object obj = MBeanServerInvocationHandler.newProxyInstance(l1MBeanServerConnection, objName,
                                                                         interfaceClass, isNotificationBroadcaster);
              l2MBeanServer.registerMBean(new ProxyStandardMBean(obj, interfaceClass), modifiedObjName);
              modifiedObjectNames.add(modifiedObjName);
            } catch (Exception e) {
              if (isConnectionException(e)) {
                logger.warn("Client disconnected before all beans could be registered");
                unregisterBeans(l2MBeanServer, modifiedObjectNames);
                return;
              }

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

  private static void unregisterBeans(MBeanServer beanServer, List modifiedObjectNames) {
    for (Iterator i = modifiedObjectNames.iterator(); i.hasNext();) {
      ObjectName on = (ObjectName) i.next();
      try {
        beanServer.unregisterMBean(on);
      } catch (Exception e) {
        logger.warn("Unable to unregister DSO client bean[" + on + "]", e);
      }
    }
  }

  private boolean isConnectionException(Throwable e) {
    while (e.getCause() != null) {
      e = e.getCause();
    }

    if (e instanceof ConnectionClosedException) { return true; }
    if ((e instanceof IOException) || ("The connection has been closed.".equals(e.getMessage()))) { return true; }

    return false;

  }

  private void removeJmxConnection(final L1ConnectionMessage msg) {
    final MessageChannel channel = msg.getChannel();
    final Map channelIdToJmxConnector = msg.getChannelIdToJmxConnector();
    final Map channelIdToMsgConnection = msg.getChannelIdToMsgConnector();

    try {
      synchronized (channelIdToMsgConnection) {
        final TunnelingMessageConnection tmc = (TunnelingMessageConnection) channelIdToMsgConnection.remove(channel
            .getChannelID());
        if (tmc != null) {
          tmc.close();
        }
      }
    } catch (Throwable t) {
      logger.error("unhandled exception closing TunnelingMessageConnection for " + channel, t);
    }

    try {
      synchronized (channelIdToJmxConnector) {
        if (channelIdToJmxConnector.containsKey(channel.getChannelID())) {
          final JMXConnector jmxConnector = (JMXConnector) channelIdToJmxConnector.remove(channel.getChannelID());
          if (jmxConnector != null) {
            statisticsGateway.removeStatisticsAgent(channel.getChannelID());
            
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
    } catch (Throwable t) {
      logger.error("unhandled exception closing JMX connector for " + channel, t);
    }
  }

}
