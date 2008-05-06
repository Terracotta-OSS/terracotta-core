/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.remote.protocol.terracotta;

import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.util.concurrent.SetOnceFlag;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

import javax.management.remote.generic.MessageConnection;
import javax.management.remote.message.Message;

public final class TunnelingMessageConnection implements MessageConnection {

  private final LinkedList      inbox;
  private final MessageChannel  channel;
  private final boolean         isJmxConnectionServer;
  private final SetOnceFlag     connected = new SetOnceFlag();
  private final SetOnceFlag     closed    = new SetOnceFlag();

  /**
   * @param channel outgoing network channel, calls to {@link #writeMessage(Message)} will drop messages here and send
   *        to the other side
   */
  public TunnelingMessageConnection(final MessageChannel channel, boolean isJmxConnectionServer) {
    this.isJmxConnectionServer = isJmxConnectionServer;
    this.inbox = new LinkedList();
    this.channel = channel;
  }

  public synchronized void close() {
    if (closed.attemptSet()) {
      inbox.clear();
      notifyAll();
    }
  }

  public synchronized void connect(final Map environment) {
    if (connected.attemptSet()) {
      if (!isJmxConnectionServer) {
        JmxRemoteTunnelMessage connectMessage = (JmxRemoteTunnelMessage) channel
            .createMessage(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE);
        connectMessage.setInitConnection();
        connectMessage.send();
      }
    }
  }

  public String getConnectionId() {
    return channel.getRemoteAddress().getStringForm();
  }

  public synchronized Message readMessage() throws IOException {
    while (inbox.isEmpty()) {
      if (closed.isSet()) {
        throw new IOException("connection closed");
      }
      try {
        wait();
      } catch (InterruptedException ie) {
        throw new IOException("Interrupted while waiting for inbound message");
      }
    }

    return (Message) inbox.removeFirst();
  }

  public synchronized void writeMessage(final Message outboundMessage) throws IOException {
    if (closed.isSet()) {
      throw new IOException("connection closed");
    }


    JmxRemoteTunnelMessage messageEnvelope = (JmxRemoteTunnelMessage) channel
        .createMessage(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE);
    messageEnvelope.setTunneledMessage(outboundMessage);
    messageEnvelope.send();
  }

  /**
   * This should only be invoked from the SEDA event handler that receives incoming network messages.
   */
  synchronized void incomingNetworkMessage(final Message inboundMessage) {
    if (closed.isSet()) {
      return;
    }

    inbox.addLast(inboundMessage);
    notifyAll();
  }

}
