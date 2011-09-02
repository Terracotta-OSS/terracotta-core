/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.TestMessageChannel;
import com.tc.object.ApplicatorDNAEncodingImpl;
import com.tc.object.ObjectID;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.session.SessionID;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

/**
 * @author Abhishek Sanoujam
 */
public class ServerMapEvictionBroadcastMessageTest extends TestCase {

  private ServerMapEvictionBroadcastMessageImpl msg;
  private TCByteBufferOutputStream              out;
  private MessageMonitor                        monitor;
  private MessageChannel                        channel;
  private DNAEncoding                           decoder;

  @Override
  public void setUp() throws Exception {
    this.monitor = new NullMessageMonitor();
    this.channel = new TestMessageChannel();
    this.out = new TCByteBufferOutputStream(4, 4096, false);
    this.msg = new ServerMapEvictionBroadcastMessageImpl(new SessionID(0), this.monitor, this.out, this.channel,
                                                         TCMessageType.BROADCAST_TRANSACTION_MESSAGE);
    decoder = new ApplicatorDNAEncodingImpl(new MockClassProvider());
  }

  public void testBasics() throws Exception {

    ObjectID mapObjectID = new ObjectID(12345);
    Set evictedObjectKeys = new HashSet();
    for (int i = 0; i < 100; i++) {
      evictedObjectKeys.add("key-" + i);
    }

    this.msg.initializeEvictionBroadcastMessage(mapObjectID, evictedObjectKeys, 10);
    this.msg.dehydrate();

    TCByteBuffer[] data = this.out.toArray();
    TCMessageHeader header = (TCMessageHeader) this.msg.getHeader();

    ServerMapEvictionBroadcastMessage hydratedMsg = new ServerMapEvictionBroadcastMessageImpl(SessionID.NULL_ID,
                                                                                              this.monitor,
                                                                                              this.channel, header,
                                                                                              data, decoder);
    hydratedMsg.hydrate();
    int clientIndex = hydratedMsg.getClientIndex();
    ObjectID actualMapId = hydratedMsg.getMapID();
    Set actualKeys = hydratedMsg.getEvictedKeys();

    assertEquals(10, clientIndex);
    assertEquals(mapObjectID, actualMapId);
    assertTrue(actualKeys.size() == 100);

    for (int i = 0; i < 100; i++) {
      assertTrue(actualKeys.contains("key-" + i));
    }

  }

}
