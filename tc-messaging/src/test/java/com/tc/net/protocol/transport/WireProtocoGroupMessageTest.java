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
import com.tc.bytes.TCReferenceSupport;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.tcm.TCActionNetworkMessage;
import com.tc.net.protocol.tcm.TCMessageHeader;
import java.net.InetSocketAddress;
import java.util.Arrays;
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
    adaptor.getReadBuffers()[0].put(grp.getHeader().getDataBuffer());
    adaptor.addReadData(src, new TCByteBuffer[] {adaptor.getReadBuffers()[0]}, grp.getHeaderLength());
    TCByteBuffer m = adaptor.getReadBuffers()[0];
    for (TCByteBuffer buffer : grp.getPayload()) {
      m.put(buffer);
    }
    adaptor.addReadData(src, new TCByteBuffer[] {m}, grp.getDataLength());    
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
