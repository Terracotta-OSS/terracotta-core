/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.management.remote.protocol.terracotta;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.remote.connect.ClientBeanBag;
import com.tc.management.remote.protocol.ProtocolProvider;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.message.Message;

public class JMXConnectStateMachine {

  private static final long     CONNECT_TIMEOUT = TCPropertiesImpl.getProperties()
                                                    .getLong(TCPropertiesConsts.L2_REMOTEJMX_CONNECT_TIMEOUT, 30000L);
  private static final TCLogger LOGGER = TCLogging.getLogger(JMXConnectStateMachine.class);

  private enum State {
    INITIAL, CONNECTED, DISCONNECTED;
  }

  private final Object                        beanBagLock = new Object();
  private volatile TunnelingMessageConnection tmc;
  private State                               state;
  private JMXConnector                        jmxConnector;
  private ChannelID                           channelId;
  private ClientBeanBag                       clientBeanBag;

  public JMXConnectStateMachine() {
    this.state = JMXConnectStateMachine.State.INITIAL;
  }

  public synchronized boolean connect(ChannelID cid, TunnelingMessageConnection connection, JMXConnector connector) {
    if (state != State.INITIAL) { return false; }

    state = State.CONNECTED;

    close();

    this.channelId = cid;
    this.tmc = connection;
    this.jmxConnector = connector;

    return true;
  }

  public void tunneledDomainsChanged(String[] domains) {
    synchronized (beanBagLock) {
      final ClientBeanBag bag = clientBeanBag;
      if (bag != null) {
        try {
          bag.setTunneledDomains(domains);
          bag.updateRegisteredBeans();
        } catch (IOException e) {
          LOGGER.error("Unable to create tunneled JMX connection to all the tunneled domains on the DSO client ["
                       + channelId + "], not all the JMX beans on the client will show up in monitoring tools!!", e);
        }
      }
    }
  }

  public synchronized void disconnect() {
    if (state != State.DISCONNECTED) {
      close();
      state = State.DISCONNECTED;
    }
  }

  private void close() {
    if (tmc != null) {
      try {
        tmc.close();
      } catch (Throwable t) {
        LOGGER.error("unhandled exception closing TunnelingMessageConnection for " + channelId, t);
      } finally {
        tmc = null;
      }
    }

    if (jmxConnector != null) {
      try {
        jmxConnector.close();
      } catch (IOException ioe) {
        LOGGER.debug("Unable to close JMX connector to " + channelId, ioe);
      } catch (Throwable t) {
        LOGGER.error("unhandled exception closing JMX connector for " + channelId, t);
      } finally {
        jmxConnector = null;
      }
    }

    channelId = null;
  }

  public void incomingNetworkMessage(JmxRemoteTunnelMessage messageEnvelope) {
    final Message message = (Message) messageEnvelope.getTunneledMessage();
    final MessageChannel channel = messageEnvelope.getChannel();

    TunnelingMessageConnection conn = tmc; // volatile read

    if (conn != null) {
      conn.incomingNetworkMessage(message);
    } else {
      LOGGER.warn("Received tunneled JMX message with no associated message connection,"
                  + " sending close() to remote JMX server");
      final JmxRemoteTunnelMessage closeMessage = (JmxRemoteTunnelMessage) channel
          .createMessage(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE);
      closeMessage.setCloseConnection();
      closeMessage.send();
    }
  }

  public void initClientBeanBag(L1ConnectionMessage msg) {
    synchronized (beanBagLock) {
      if (clientBeanBag != null) {
        LOGGER.warn("We are trying to create a new tunneled JMX connection but already have one for channel["
                    + channelId + "], ignoring new connection message");
        return;

      }

      final MessageChannel channel = msg.getChannel();
      final TCSocketAddress remoteAddress = channel != null ? channel.getRemoteAddress() : null;
      if (remoteAddress == null) {
        LOGGER.error("Not adding JMX connection for " + (channel == null ? "null" : channel.getChannelID())
                     + ". remoteAddress=" + remoteAddress);
        return;
      }

      final MBeanServer l2MBeanServer = msg.getMBeanServer();

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
      environment.put("jmx.remote.x.request.timeout", Long.valueOf(Long.MAX_VALUE));
      environment.put("jmx.remote.x.client.connection.check.period", Long.valueOf(0));
      environment.put("jmx.remote.x.server.connection.timeout", Long.valueOf(Long.MAX_VALUE));

      final JMXConnector conn;
      try {
        conn = jmxConnect(channel, serviceURL, environment);

        final MBeanServerConnection l1MBeanServerConnection = conn.getMBeanServerConnection();

        clientBeanBag = new ClientBeanBag(l2MBeanServer, channel, msg.getUUID(), msg.getTunneledDomains(),
                                          l1MBeanServerConnection);

        if (clientBeanBag.updateRegisteredBeans()) {
          try {
            conn.addConnectionNotificationListener(new ConnectorClosedListener(clientBeanBag),
                                                   new ConnectorClosedFilter(), null);
          } catch (Exception e) {
            LOGGER
                .error("Unable to register a JMX connection listener for the DSO client[" + channel.getRemoteAddress()
                       + "], if the DSO client disconnects the then its (dead) beans will not be unregistered", e);
          }
        }
      } catch (IOException ioe) {
        LOGGER.info("Unable to create tunneled JMX connection to the DSO client on host[" + channel.getRemoteAddress()
                    + "], this DSO client will not show up in monitoring tools!!");
        return;
      }
    }
  }

  private static JMXConnector jmxConnect(MessageChannel channel, final JMXServiceURL serviceURL, final Map environment)
      throws IOException {
    final AtomicReference<Object> ref = new AtomicReference<Object>();

    Thread connectThread = new Thread("JMX Connect for " + channel.getChannelID()) {
      @Override
      public void run() {
        try {
          ref.set(JMXConnectorFactory.connect(serviceURL, environment));
        } catch (Throwable t) {
          ref.set(t);
        }
      }
    };

    // override the uncaught exception handler just in case
    connectThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread t, Throwable e) {
        //
      }
    });
    connectThread.setDaemon(true);
    connectThread.start();

    try {
      connectThread.join(CONNECT_TIMEOUT);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }

    Object o = ref.get();
    if (o instanceof Throwable) {
      if (o instanceof IOException) { throw (IOException) o; }
      throw new IOException((Throwable) o);
    } else if (o == null) {
      connectThread.interrupt();
      throw new IOException("timeout waiting for jmx connect on " + channel.getChannelID());
    } else if (o instanceof JMXConnector) {
      //
      return (JMXConnector) o;
    } else {
      //
      throw new IOException("Unexpected object type: " + o.getClass().getName());
    }
  }

  private static final class ConnectorClosedFilter implements NotificationFilter {
    @Override
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

    @Override
    final public void handleNotification(final Notification notification, final Object context) {
      bag.unregisterBeans();
    }
  }

}
