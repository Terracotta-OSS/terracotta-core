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
package com.tc.net.protocol;

import com.tc.bytes.TCByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.bytes.TCReference;
import com.tc.bytes.TCReferenceSupport;
import com.tc.exception.TCInternalError;
import com.tc.util.Assert;
import com.tc.util.HexDump;
import com.tc.util.concurrent.SetOnceFlag;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Base class for network messages
 * 
 * @author teck
 */
public class TCNetworkMessageImpl implements TCNetworkMessage {
  protected static final Logger logger = LoggerFactory.getLogger(TCNetworkMessage.class);
  private static final int        MESSAGE_DUMP_MAXBYTES = 4 * 1024;

  protected TCNetworkMessageImpl(TCNetworkHeader header) {
    Assert.eval(header != null);
    this.header = header;
  }

  @Override
  public final int getDataLength() {
    checkSealed();
    return dataLength;
  }

  @Override
  public final int getHeaderLength() {
    checkSealed();
    return headerLength;
  }

  @Override
  public final int getTotalLength() {
    checkSealed();
    return totalLength;
  }

  @Override
  public final TCNetworkHeader getHeader() {
    return header;
  }

  @Override
  public final TCReference getPayload() {
    Assert.eval(payloadData != null);

    return payloadData;
  }

  protected final void setPayload(TCReference newPayload) {
    // this array should have already been set in seal()
    Assert.eval(payloadData == null);
    if (newPayload == null) {
      payloadData = EMPTY_BUFFER;
    } else {
      payloadData = newPayload;
    }
    seal();
    this.complete.thenRun(payloadData::close);
  }

  @Override
  public final TCReference getEntireMessageData() {
    // this array should have already been set in seal()
    Assert.eval(entireMessageData != null);

    return entireMessageData;
  }

  @Override
  public final String toString() {
    try {
      return toString0();
    } catch (Exception e) {
      logger.warn("Exception in toString()", e);
      return "EXCEPTION in toString(): " + e.getMessage();
    }
  }

  protected final String toString0() {
    StringBuilder buf = new StringBuilder();
    buf.append("Message Class: ").append(getClass().getName()).append("\n");
    buf.append("Sealed: ").append((payloadData != null)).append(", ");
    buf.append("Header Length: ").append(getHeaderLength()).append(", ");
    buf.append("Data Length: ").append(getDataLength()).append(", ");
    buf.append("Total Length: ").append(getTotalLength()).append("\n");

    String extraMsgInfo = describeMessage();
    if (extraMsgInfo != null) {
      buf.append(extraMsgInfo).append("\n");
    }
    String payload = describePayload();
    if (payload != null) {
      buf.append(payload);
    }

    return buf.toString();
  }

  // override this method to add more information about your message
  protected String describeMessage() {
    return null;
  }
  
  protected String describePayload() {
    return null;
  }

  // override this method to add more description to your payload data
  protected String messageBytes() {
    StringBuilder buf = new StringBuilder();
    int totalBytesDumped = 0;
    if (payloadData != null) {
      for (TCByteBuffer i : payloadData) {
        buf.append("Buffer ").append(i).append(": ");
        buf.append(i.toString());
        buf.append("\n");

        if (totalBytesDumped < MESSAGE_DUMP_MAXBYTES) {
          int bytesFullBuf = i.limit();
          int bytesToDump = (((totalBytesDumped + bytesFullBuf) < MESSAGE_DUMP_MAXBYTES) ? bytesFullBuf
              : (MESSAGE_DUMP_MAXBYTES - totalBytesDumped));
          byte[] read = new byte[i.remaining()];
          i.duplicate().get(read);
          buf.append(HexDump.dump(read));
          totalBytesDumped += bytesToDump;
        }
      }
    } else {
      buf.append("No payload buffers present");
    }

    return buf.toString();
  }

  protected String dump() {
    StringBuilder toRet = new StringBuilder(toString());
    toRet.append("\n\n");
    if (entireMessageData != null) {
      int count = 0;
      for (TCByteBuffer data : entireMessageData) {
        toRet.append('[').append(count++).append(']').append('=').append(data.toString());
        toRet.append(" =  { ");
        byte ba[] = new byte[data.remaining()];
        data.duplicate().get(ba);
        HexDump.dump(ba);
        toRet.append(" }  \n\n");
      }
    }
    return toRet.toString();
  }

  private void seal() {
    Objects.requireNonNull(payloadData);
    try (TCReference headerRef = TCReferenceSupport.createGCReference(header.getDataBuffer())) {
      TCReference message = TCReferenceSupport.createAggregateReference(headerRef ,payloadData);

      long dataLen = payloadData.available();

      if (dataLen + header.getHeaderByteLength() > Integer.MAX_VALUE) { throw new TCInternalError("Message too big"); }

      this.dataLength = (int) dataLen;
      this.headerLength = header.getHeaderByteLength();
      this.totalLength = this.headerLength + this.dataLength;
      this.entireMessageData = message;
      complete.thenRun(entireMessageData::close);
    }
  }

  @Override
  public void complete() {
    fireCallbacks();
  }

  private void fireCallbacks() {
    if (callbackFired.attemptSet()) {
      complete.complete(null);
    }
  }

  @Override
  public void addCompleteCallback(Runnable r) {
    complete.thenRun(r);
  }
  
  private void checkSealed() {
    // this check is not thread safe
    if (payloadData == null) throw new IllegalStateException("Message is not sealed");
  }
  
  private final SetOnceFlag           callbackFired  = new SetOnceFlag();
  private static final TCReference EMPTY_BUFFER = TCReferenceSupport.createReference(Collections.emptyList(), null);
  private final TCNetworkHeader       header;
  private TCReference             payloadData;
  private TCReference              entireMessageData;
  private int                         totalLength;
  private int                         dataLength;
  private int                         headerLength;
  private final CompletableFuture<Void>       complete = new CompletableFuture<>();
}
