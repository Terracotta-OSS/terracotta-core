/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.remote.connect;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.remote.protocol.ProtocolProvider;
import com.tc.management.remote.protocol.terracotta.ClientProvider;
import com.tc.management.remote.protocol.terracotta.L1ConnectionMessage;
import com.tc.management.remote.protocol.terracotta.TunneledDomainsChanged;
import com.tc.management.remote.protocol.terracotta.TunnelingMessageConnection;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.statistics.StatisticsGateway;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class ClientConnectEventHandler extends AbstractEventHandler {
  private static final TCLogger                 LOGGER         = TCLogging.getLogger(ClientConnectEventHandler.class);

  private final StatisticsGateway               statisticsGateway;

  final ConcurrentMap<ChannelID, ClientBeanBag> clientBeanBags = new ConcurrentHashMap<ChannelID, ClientBeanBag>();

  public ClientConnectEventHandler(final StatisticsGateway statisticsGateway) {
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
    private final ClientBeanBag bag;

    ConnectorClosedListener(ClientBeanBag bag) {
      this.bag = bag;
    }

    final public void handleNotification(final Notification notification, final Object context) {
      bag.unregisterBeans();
    }
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof L1ConnectionMessage) {
      L1ConnectionMessage msg = (L1ConnectionMessage) context;
      if (msg.isConnectingMsg()) {
        addJmxConnection(msg);
      } else {
        removeJmxConnection(msg);
      }
    } else if (context instanceof TunneledDomainsChanged) {
      synchronized (clientBeanBags) {
        TunneledDomainsChanged msg = (TunneledDomainsChanged) context;
        ClientBeanBag bag = clientBeanBags.get(msg.getChannelID());
        if (bag != null) {
          try {
            bag.setTunneledDomains(msg.getTunneledDomains());
            bag.updateRegisteredBeans();
          } catch (IOException e) {
            LOGGER
                .error("Unable to create tunneled JMX connection to all the tunneled domains on the DSO client on host["
                           + msg.getChannel().getRemoteAddress()
                           + "], not all the JMX beans on the client will show up in monitoring tools!!", e);
          }
        }
      }
    } else {
      LOGGER.error("Unknown event context : " + context + " (" + context.getClass() + ")");
    }
  }

  private void addJmxConnection(final L1ConnectionMessage msg) {
    final MessageChannel channel = msg.getChannel();
    final TCSocketAddress remoteAddress = channel != null ? channel.getRemoteAddress() : null;
    if (remoteAddress == null) { return; }

    final MBeanServer l2MBeanServer = msg.getMBeanServer();
    final ConcurrentMap<ChannelID, JMXConnector> channelIdToJmxConnector = msg.getChannelIdToJmxConnector();
    final ConcurrentMap<ChannelID, TunnelingMessageConnection> channelIdToMsgConnector = msg
        .getChannelIdToMsgConnector();

    synchronized (clientBeanBags) {
      if (!channelIdToJmxConnector.containsKey(channel.getChannelID())) {
        JMXServiceURL serviceURL;
        try {
          serviceURL = new JMXServiceURL("terracotta", remoteAddress.getAddress().getHostAddress(),
                                         remoteAddress.getPort());
        } catch (MalformedURLException murle) {
          LOGGER.error("Unable to construct a JMX service URL using DSO client channel from host["
                           + channel.getRemoteAddress() + "]; tunneled JMX connection will not be established", murle);
          return;
        }
        Map environment = new HashMap();
        ProtocolProvider.addTerracottaJmxProvider(environment);
        environment.put(ClientProvider.JMX_MESSAGE_CHANNEL, channel);
        environment.put(ClientProvider.CONNECTION_LIST, channelIdToMsgConnector);
        environment.put("jmx.remote.x.request.timeout", Long.valueOf(Long.MAX_VALUE));
        environment.put("jmx.remote.x.client.connection.check.period", Long.valueOf(0));
        environment.put("jmx.remote.x.server.connection.timeout", Long.valueOf(Long.MAX_VALUE));

        final JMXConnector jmxConnector;
        try {
          jmxConnector = JMXConnectorFactory.connect(serviceURL, environment);

          final MBeanServerConnection l1MBeanServerConnection = jmxConnector.getMBeanServerConnection();

          statisticsGateway.addStatisticsAgent(channel.getChannelID(), l1MBeanServerConnection);

          ClientBeanBag bag = new ClientBeanBag(l2MBeanServer, channel, msg.getUUID(), msg.getTunneledDomains(),
                                                l1MBeanServerConnection);
          clientBeanBags.put(channel.getChannelID(), bag);

          if (bag.updateRegisteredBeans()) {
            try {
              jmxConnector.addConnectionNotificationListener(new ConnectorClosedListener(bag),
                                                             new ConnectorClosedFilter(), null);
            } catch (Exception e) {
              LOGGER.error("Unable to register a JMX connection listener for the DSO client["
                               + channel.getRemoteAddress()
                               + "], if the DSO client disconnects the then its (dead) beans will not be unregistered",
                           e);
            }
          }

        } catch (IOException ioe) {
          LOGGER.error("Unable to create tunneled JMX connection to the DSO client on host["
                           + channel.getRemoteAddress() + "], this DSO client will not show up in monitoring tools!!",
                       ioe);
          return;
        }
        channelIdToJmxConnector.put(channel.getChannelID(), jmxConnector);
      } else {
        LOGGER.warn("We are trying to create a new tunneled JMX connection but already have one for channel["
                    + channel.getRemoteAddress() + "], ignoring new connection message");
      }
    }
  }

  private void removeJmxConnection(final L1ConnectionMessage msg) {
    final MessageChannel channel = msg.getChannel();
    ConcurrentMap<ChannelID, JMXConnector> channelIdToJmxConnector = msg.getChannelIdToJmxConnector();
    ConcurrentMap<ChannelID, TunnelingMessageConnection> channelIdToMsgConnector = msg.getChannelIdToMsgConnector();

    try {
      final TunnelingMessageConnection tmc = channelIdToMsgConnector.remove(channel.getChannelID());
      if (tmc != null) {
        tmc.close();
      }
    } catch (Throwable t) {
      LOGGER.error("unhandled exception closing TunnelingMessageConnection for " + channel, t);
    }

    try {
      final JMXConnector jmxConnector = channelIdToJmxConnector.remove(channel.getChannelID());
      if (jmxConnector != null) {
        statisticsGateway.removeStatisticsAgent(channel.getChannelID());

        try {
          jmxConnector.close();
        } catch (IOException ioe) {
          LOGGER.debug("Unable to close JMX connector to DSO client[" + channel + "]", ioe);
        }
      } else {
        LOGGER.debug("DSO client channel closed without a corresponding tunneled JMX connection");
      }
    } catch (Throwable t) {
      LOGGER.error("unhandled exception closing JMX connector for " + channel, t);
    }
  }
}
