/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.protocol.transport;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.bytes.TCReference;
import com.tc.bytes.TCReferenceSupport;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.tcm.TCActionNetworkMessage;
import com.tc.net.protocol.tcm.TCMessageHeader;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Iterator;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 */
public class WireProtocoGroupMessageTest {

  @Test
  public void testGoodHeader() throws Exception {
    TCConnection src = mock(TCConnection.class);

    TCActionNetworkMessage msg = mock(TCActionNetworkMessage.class);
    when(msg.commit()).thenReturn(Boolean.TRUE);
    when(msg.getTotalLength()).thenReturn(TCMessageHeader.HEADER_LENGTH + 32);
    when(msg.getEntireMessageData()).thenReturn(
            TCReferenceSupport.createGCReference(TCByteBufferFactory.getInstance(TCMessageHeader.HEADER_LENGTH + 32)));
    
    TCActionNetworkMessage[] msgs = new TCActionNetworkMessage[2];
    msgs[0] = msg;
    msgs[1] = msg;
    WireProtocolGroupMessageImpl grp = WireProtocolGroupMessageImpl.wrapMessages(Arrays.asList(msgs), src);
    finalizeWireProtocolMessage(grp, 2);
    
    WireProtocolAdaptorImpl adaptor = new WireProtocolAdaptorImpl((message) -> {
      System.out.println(message);
    });
    TCByteBuffer headerData = TCByteBufferFactory.getInstance(grp.getHeader().getDataBuffer().capacity());
    headerData.limit(grp.getHeader().getDataBuffer().limit());
    
    try (TCReference headerRef = TCReferenceSupport.createGCReference(headerData)) {
      headerData.put(grp.getHeader().getDataBuffer());
      adaptor.addReadData(src, headerRef);
    }
    
    TCByteBuffer payloadData = TCByteBufferFactory.getInstance(grp.getDataLength());
    try (TCReference payloadRef = TCReferenceSupport.createGCReference(payloadData)) {
      for (TCByteBuffer buffer : grp.getPayload()) {
        while (buffer.hasRemaining()) {
          int lim = buffer.limit();
          buffer.limit(buffer.position() + Math.min(buffer.remaining(), payloadData.remaining()));
          payloadData.put(buffer);
          buffer.limit(lim);
        }
      }
      adaptor.addReadData(src, payloadRef);    
    }
  }
  
  private static TCReference testAllocator(int size) {
    return TCReferenceSupport.createGCReference(TCByteBufferFactory.getInstance(size));
  }
  @Test
  public void testCancellation() throws Exception {
    TCConnection src = mock(TCConnection.class);

    TCActionNetworkMessage msg = mock(TCActionNetworkMessage.class);
    when(msg.commit()).thenReturn(Boolean.TRUE);
    when(msg.getTotalLength()).thenReturn(TCMessageHeader.HEADER_LENGTH + 32);
    when(msg.getEntireMessageData()).thenReturn(TCReferenceSupport.createGCReference(TCByteBufferFactory.getInstance(TCMessageHeader.HEADER_LENGTH + 32)));
    
    TCActionNetworkMessage[] msgs = new TCActionNetworkMessage[2];
    msgs[0] = msg;
    msgs[1] = msg;
    WireProtocolGroupMessageImpl grp = WireProtocolGroupMessageImpl.wrapMessages(Arrays.asList(msgs), src);
    finalizeWireProtocolMessage(grp, 2);
    assertTrue(grp.isValid());
    
    when(msg.isCancelled()).thenReturn(Boolean.TRUE);

    assertFalse(grp.isValid());
  }
  
  private WireProtocolMessage finalizeWireProtocolMessage(WireProtocolMessage message, int messageCount) {
    assertTrue(message.prepareToSend());
    InetSocketAddress address = new InetSocketAddress(999);
    final WireProtocolHeader hdr = (WireProtocolHeader) message.getHeader();
    hdr.setSourceAddress(address.getAddress().getAddress());
    hdr.setSourcePort(address.getPort());
    hdr.setDestinationAddress(address.getAddress().getAddress());
    hdr.setDestinationPort(address.getPort());
    hdr.setMessageCount(messageCount);
    hdr.computeChecksum();
    return message;
  }
}
