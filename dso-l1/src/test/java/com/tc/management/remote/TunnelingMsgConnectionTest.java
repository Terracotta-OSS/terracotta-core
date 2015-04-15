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
package com.tc.management.remote;


import com.tc.bytes.TCByteBuffer;
import com.tc.management.remote.protocol.terracotta.JmxRemoteTunnelMessage;
import com.tc.management.remote.protocol.terracotta.TunnelingEventHandler;
import com.tc.management.remote.protocol.terracotta.TunnelingMessageConnection;
import com.tc.net.core.SecurityInfo;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.tcm.ClientMessageChannelImpl;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageFactoryImpl;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageHeaderImpl;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.config.DSOMBeanConfig;
import com.tc.object.session.NullSessionManager;
import com.tc.object.session.SessionID;
import com.tc.test.TCTestCase;
import com.tc.util.UUID;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.management.remote.generic.MessageConnection;

public class TunnelingMsgConnectionTest extends TCTestCase {

  public void testTMC() throws Exception {
    final ClientMessageChannelImpl mc = new MockClientMessageChannelForTMC();
    mc.addClassMapping(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE, JmxRemoteTunnelMessage.class);

    MockTunnelingEventHandler teh = new MockTunnelingEventHandler(mc, new DSOMBeanConfig() {
      @Override
      public String[] getTunneledDomains() {
        return null;
      }
    });

    /*
     * JmxRemoteTunnelMessage messageEnvelope = (JmxRemoteTunnelMessage) mc
     * .createMessage(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE); messageEnvelope.setInitConnection();
     * teh.handleEvent(messageEnvelope);
     */

    // simulating a client connection
    MockJmxRemoteTunnelMessage jmxTunnelMsg = new MockJmxRemoteTunnelMessage(
                                                                             new SessionID(1),
                                                                             new NullMessageMonitor(),
                                                                             mc,
                                                                             new MockTCMsgHeader(
                                                                                                 TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE),
                                                                             new TCByteBuffer[] {});
    jmxTunnelMsg.setInitConnection();
    teh.handleEvent(jmxTunnelMsg);

    final TunnelingMessageConnection tmc = (TunnelingMessageConnection) teh.accept();

    System.out.println("XXX tmc = " + tmc);
    Thread t1 = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          tmc.writeMessage(null);
        } catch (IOException e) {
          throw new RuntimeException("not able to send message");
        }
      }
    });

    Thread t2 = new Thread(new Runnable() {
      @Override
      public void run() {
        mc.notifyTransportDisconnected(null, false);
      }
    });

    // t1 will try to send message but since the queue is full it gets blocked at send
    t1.start();
    System.out.println("T1 started - sending message");
    ThreadUtil.reallySleep(3000);

    // meanwhile channel gets notified about connection disconnect.
    t2.start();
    System.out.println("T2 started - channel notfied about disconnect");

    // channel notify disconnect should not get blocked by any tunnel message connection problem
    t2.join();
    System.out.println("T2 done");
  }

  private class MockClientMessageChannelForTMC extends ClientMessageChannelImpl {
    private final BlockingQueue<Object> queue;

    MockClientMessageChannelForTMC() {
      super(new TCMessageFactoryImpl(new NullSessionManager(), new NullMessageMonitor()), null,
            new NullSessionManager(), null, new SecurityInfo(), null, null, null);
      queue = new ArrayBlockingQueue<Object>(10);
      for (int i = 0; i < 10; i++) {
        try {
          queue.put(new Object());
        } catch (InterruptedException e) {
          throw new RuntimeException("not able to init queue");
        }
      }
    }

    @Override
    public void send(TCNetworkMessage message) {
      //
      System.out.println("XXX sending start");
      try {
        queue.put(message);
      } catch (InterruptedException e) {
        throw new RuntimeException("not able to add to queue. suppose to block during the prev put");
      }
      throw new AssertionError("Not suppose to reach here.");
    }
  }

  private class MockTunnelingEventHandler extends TunnelingEventHandler {
    public MockTunnelingEventHandler(MessageChannel channel, DSOMBeanConfig config) {
      super(channel, config, UUID.NULL_ID);
    }

    @Override
    public synchronized MessageConnection accept() throws IOException {
      return super.accept();
    }
  }

  private class MockJmxRemoteTunnelMessage extends JmxRemoteTunnelMessage {

    public MockJmxRemoteTunnelMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                      TCMessageHeader header, TCByteBuffer[] data) {
      super(sessionID, monitor, channel, header, data);
    }

    @Override
    public synchronized void setInitConnection() {
      super.setInitConnection();
    }
  }

  private class MockTCMsgHeader extends TCMessageHeaderImpl {
    public MockTCMsgHeader(TCMessageType type) {
      super(type);
    }
  }

}
