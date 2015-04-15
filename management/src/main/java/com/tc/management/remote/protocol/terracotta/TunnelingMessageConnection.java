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

import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.util.concurrent.SetOnceFlag;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.remote.generic.MessageConnection;
import javax.management.remote.message.Message;

public class TunnelingMessageConnection implements MessageConnection {

  private final LinkedBlockingQueue<Message> inbox;
  protected final MessageChannel             channel;
  private final AtomicBoolean                isJmxConnectionServer;
  private final SetOnceFlag                  connected = new SetOnceFlag();
  protected final SetOnceFlag                closed    = new SetOnceFlag();

  /**
   * @param channel outgoing network channel, calls to {@link #writeMessage(Message)} will drop messages here and send
   *        to the other side
   */
  public TunnelingMessageConnection(final MessageChannel channel, boolean isJmxConnectionServer) {
    this.isJmxConnectionServer = new AtomicBoolean(isJmxConnectionServer);
    this.inbox = new LinkedBlockingQueue<Message>();
    this.channel = channel;
  }

  public void close() {
    if (closed.attemptSet()) {
      inbox.clear();
    }
  }

  public void connect(final Map environment) {
    if (connected.attemptSet()) {
      if (!isJmxConnectionServer.get()) {
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

  public Message readMessage() throws IOException {
    Message m;
    do {
      try {
        m = inbox.poll(1000, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        throw new IOException("Interrupted while waiting for inbound message");
      }
      if (closed.isSet()) { throw new IOException("connection closed"); }
    } while (m == null);
    return m;
  }

  public void writeMessage(final Message outboundMessage) throws IOException {
    if (closed.isSet()) { throw new IOException("connection closed"); }

    JmxRemoteTunnelMessage messageEnvelope = (JmxRemoteTunnelMessage) channel
        .createMessage(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE);
    messageEnvelope.setTunneledMessage(outboundMessage);
    messageEnvelope.send();
  }

  /**
   * This should only be invoked from the SEDA event handler that receives incoming network messages.
   */
  void incomingNetworkMessage(final Message inboundMessage) {
    if (closed.isSet()) { return; }
    inbox.add(inboundMessage);
  }

}
