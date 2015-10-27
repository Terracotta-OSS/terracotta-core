/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.management.remote.connect;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.remote.protocol.terracotta.ClientTunnelingEventHandler;
import com.tc.management.remote.protocol.terracotta.JMXConnectStateMachine;
import com.tc.management.remote.protocol.terracotta.L1ConnectionMessage;
import com.tc.management.remote.protocol.terracotta.TunneledDomainsChanged;
import com.tc.net.protocol.tcm.MessageChannel;

public class ClientConnectEventHandler {
  private static final TCLogger LOGGER = TCLogging.getLogger(ClientConnectEventHandler.class);

  public ClientConnectEventHandler() {
    //
  }

  private final AbstractEventHandler<L1ConnectionMessage.Connecting> connectingHandler = new AbstractEventHandler<L1ConnectionMessage.Connecting>() {
    @Override
    public void handleEvent(L1ConnectionMessage.Connecting connectingMessage) throws EventHandlerException {
      addJmxConnection(connectingMessage);
    }
  };
  public AbstractEventHandler<L1ConnectionMessage.Connecting> getJxmConnectHandler() {
    return this.connectingHandler;
  }
  
  private final AbstractEventHandler<L1ConnectionMessage.Disconnecting> diconnectingHandler = new AbstractEventHandler<L1ConnectionMessage.Disconnecting>() {
    @Override
    public void handleEvent(L1ConnectionMessage.Disconnecting disconnectingMessage) throws EventHandlerException {
      removeJmxConnection(disconnectingMessage);
    }
  };
  public AbstractEventHandler<L1ConnectionMessage.Disconnecting> getJxmDisconnectHandler() {
    return this.diconnectingHandler;
  }
  
  private final AbstractEventHandler<TunneledDomainsChanged> tunneledHandler = new AbstractEventHandler<TunneledDomainsChanged>() {
    @Override
    public void handleEvent(TunneledDomainsChanged tunneledMessage) throws EventHandlerException {
      JMXConnectStateMachine state = (JMXConnectStateMachine) tunneledMessage.getChannel()
          .getAttachment(ClientTunnelingEventHandler.STATE_ATTACHMENT);
      state.tunneledDomainsChanged(tunneledMessage.getTunneledDomains());
    }
  };
  public AbstractEventHandler<TunneledDomainsChanged> getJxmTunneledHandler() {
    return this.tunneledHandler;
  }

  private void addJmxConnection(L1ConnectionMessage msg) {
    final long start = System.currentTimeMillis();

    LOGGER.info("addJmxConnection(" + msg.getChannel().getChannelID() + ")");

    JMXConnectStateMachine state = (JMXConnectStateMachine) msg.getChannel()
        .getAttachment(ClientTunnelingEventHandler.STATE_ATTACHMENT);

    state.initClientBeanBag(msg);

    LOGGER.info("addJmxConnection(" + msg.getChannel().getChannelID() + ") completed in "
                + (System.currentTimeMillis() - start) + " millis");
  }

  private void removeJmxConnection(L1ConnectionMessage msg) {
    final MessageChannel channel = msg.getChannel();

    JMXConnectStateMachine state = (JMXConnectStateMachine) channel
        .getAttachment(ClientTunnelingEventHandler.STATE_ATTACHMENT);

    if (state != null) {
      state.disconnect();
    }
  }

}
