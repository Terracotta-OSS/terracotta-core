/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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

import com.tc.bytes.TCReference;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.TCNetworkHeader;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.TCNetworkMessageImpl;
import com.tc.net.protocol.tcm.TCActionNetworkMessage;
import java.util.Optional;

/**
 * Wire protocol message. All network communications in the TC world are conducted with wire protocol messages. Wire
 * protocol is a lot like TCP/IP in the sense that is meant to carry other "application" level protocols on top/within
 * it
 * 
 * @author teck
 */
public class WireProtocolMessageImpl extends TCNetworkMessageImpl implements WireProtocolMessage {
  private final TCConnection sourceConnection;
  private final Optional<TCActionNetworkMessage> message;

  /**
   * Wrap the given network message with a wire protocol message instance. The header for the returned instance will
   * have it's total length
   * 
   * @param msgPayload the network message to wrap
   * @return a new wire protocol message instance that contains the given message as it's payload.
   */
  public static WireProtocolMessage wrapMessage(TCActionNetworkMessage msgPayload, TCConnection source) {
    WireProtocolHeader header = new WireProtocolHeader();
    header.setProtocol(WireProtocolHeader.getProtocolForMessageClass(msgPayload));

    WireProtocolMessage rv = new WireProtocolMessageImpl(source, header, msgPayload);
    return rv;
  }
  
  protected WireProtocolMessageImpl(TCConnection source, TCNetworkHeader header, TCReference buffers) {
    super(header);
    this.sourceConnection = source;
    setPayload(buffers);
    message = Optional.empty();
  }
  
  protected WireProtocolMessageImpl(TCConnection source, TCNetworkHeader header, TCActionNetworkMessage msg) {
    super(header);
    this.sourceConnection = source;
    this.message = Optional.of(msg);
  }

  @Override
  public boolean prepareToSend() {
    if (!this.message.isPresent()) {
      getWireProtocolHeader().finalizeHeader(this.getTotalLength());
      return true;
    } else if (this.message.get().commit()) {
      // duplicate because the original message is closed as well
      setPayload(this.message.get().getEntireMessageData().duplicate());
      getWireProtocolHeader().finalizeHeader(this.getTotalLength());
      return true;
    } else {
      return false;
    }
  }
  
  @Override
  public short getMessageProtocol() {
    return ((WireProtocolHeader) getHeader()).getProtocol();
  }

  @Override
  public WireProtocolHeader getWireProtocolHeader() {
    return ((WireProtocolHeader) getHeader());
  }

  @Override
  public TCConnection getSource() {
    return sourceConnection;
  }

  @Override
  public void complete() {
    this.message.ifPresent(TCNetworkMessage::complete);
    super.complete();
  }

  @Override
  public boolean isValid() {
    return !this.message.map(TCActionNetworkMessage::isCancelled).orElse(Boolean.FALSE);
  }
}
