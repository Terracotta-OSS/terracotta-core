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
package com.tc.net.protocol.tcm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCReference;
import com.tc.io.TCByteBufferInputStream;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.util.HexDump;

/**
 * A class that knows how to parse TCMessages out of raw bytes
 */
class TCMessageParser {
   private static final Logger logger = LoggerFactory.getLogger(TCMessageParser.class);
  private final TCMessageFactory factory;

  TCMessageParser(TCMessageFactory factory) {
    this.factory = factory;
  }

  TCAction parseMessage(MessageChannel source, TCNetworkMessage msg) {
    TCByteBufferInputStream helper = new TCByteBufferInputStream(msg.getPayload());
    TCByteBuffer header = helper.read(TCMessageHeader.HEADER_LENGTH);
    TCMessageHeader hdr = new TCMessageHeaderImpl(header);
    final int headerLength = hdr.getHeaderByteLength();

    if (headerLength != TCMessageHeader.HEADER_LENGTH) {
      logger.error("Invalid header length ! length = " + headerLength);
      logger.error("error header = " + hdr);
      logger.error(" buffer data is " + toString(msg.getPayload()));
      throw new RuntimeException("Invalid header length: " + headerLength);
    }

    final int msgType = hdr.getMessageType();
    final TCMessageType type = TCMessageType.getInstance(hdr.getMessageType());

    if (type == null) {
      throw new RuntimeException("Can't find message type for type: " + msgType);
    }
    
    TCAction converted = factory.createMessage(source, type, hdr, helper);
    
    return converted;
  }

  private String toString(TCReference data) {
    if(data == null || !data.iterator().hasNext()) { return "null or size 0"; }
    StringBuffer sb = new StringBuffer();
    for (TCByteBuffer buf : data) {
      sb.append(buf);
      sb.append(" { ");
      byte[] read = new byte[buf.remaining()];
      buf.duplicate().get(read);
      HexDump.dump(read);
      sb.append(" } ");
    }
    return sb.toString();
  }
}
