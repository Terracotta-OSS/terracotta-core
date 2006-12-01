/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.remote.protocol.terracotta;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

import javax.management.remote.generic.MessageConnection;
import javax.management.remote.message.Message;

import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;

public final class TunnelingMessageConnection implements MessageConnection {

  private final LinkedList     inbox;
  private final MessageChannel channel;
  private boolean              connected;
  private final boolean        isJmxConnectionServer;

  /**
   * @param channel outgoing network channel, calls to {@link #writeMessage(Message)} will drop messages here and send
   *        to the other side
   */
  public TunnelingMessageConnection(final MessageChannel channel, boolean isJmxConnectionServer) {
    this.isJmxConnectionServer = isJmxConnectionServer;
    this.inbox = new LinkedList();
    this.channel = channel;
    connected = false;
  }

  public synchronized void close() throws IOException {
    checkConnected();
    connected = false;
    synchronized (inbox) {
      inbox.clear();
      inbox.notifyAll();
    }
  }

  public synchronized void connect(final Map environment) throws IOException {
    if (connected) { throw new IOException("Connection is already open"); }
    if (!isJmxConnectionServer) {
      JmxRemoteTunnelMessage connectMessage = (JmxRemoteTunnelMessage) channel
          .createMessage(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE);
      connectMessage.setInitConnection();
      connectMessage.send();
    }
    connected = true;
  }

  public String getConnectionId() {
    return channel.getRemoteAddress().getStringForm();
  }

  public Message readMessage() throws IOException, ClassNotFoundException {
    Message inboundMessage = null;
    while (inboundMessage == null) {
      checkConnected();
      synchronized (inbox) {
        if (inbox.isEmpty()) {
          try {
            inbox.wait();
          } catch (InterruptedException ie) {
            throw new IOException("Interrupted while waiting for inbound message");
          }
        } else {
          inboundMessage = (Message) inbox.removeFirst();
          inbox.notifyAll();
        }
      }
    }
    return inboundMessage;
  }

  public void writeMessage(final Message outboundMessage) throws IOException {
    JmxRemoteTunnelMessage messageEnvelope = (JmxRemoteTunnelMessage) channel
        .createMessage(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE);
    messageEnvelope.setTunneledMessage(outboundMessage);
    messageEnvelope.send();
  }

  /**
   * This should only be invoked from the SEDA event handler that receives incoming network messages.
   */
  void incomingNetworkMessage(final Message inboundMessage) {
    synchronized (this) {
      if (!connected) return;
    }
    synchronized (inbox) {
      inbox.addLast(inboundMessage);
      inbox.notifyAll();
    }
  }

  private synchronized void checkConnected() throws IOException {
    if (!connected) { throw new IOException("Connection has been closed"); }
  }

}
