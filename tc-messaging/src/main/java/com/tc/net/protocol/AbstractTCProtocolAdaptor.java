/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.protocol;

import org.slf4j.Logger;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCReference;
import com.tc.net.core.TCConnection;
import com.tc.util.Assert;

/**
 * Base class for protocol adaptors
 *
 * @author teck
 */
public abstract class AbstractTCProtocolAdaptor implements TCProtocolAdaptor {
  protected static final int      MODE_HEADER       = 1;
  protected static final int      MODE_DATA         = 2;

  private final Logger logger;
  private AbstractTCNetworkHeader header;
  private int dataBytesNeeded;
  private int                     mode;

  public AbstractTCProtocolAdaptor(Logger logger) {
    this.logger = logger;
    init();
  }

  @Override
  public int getExpectedBytes() {
    if (mode == MODE_HEADER) {
      return header.getDataBuffer().limit();
    } else {
      return dataBytesNeeded;
    }
  }
  
  abstract protected AbstractTCNetworkHeader getNewProtocolHeader();

  // subclasses override this method to return specific message types
  abstract protected TCNetworkMessage createMessage(TCConnection source, TCNetworkHeader hdr, TCReference data)
      throws TCProtocolException;

  abstract protected int computeDataLength(TCNetworkHeader hdr);

  protected final void init() {
    mode = MODE_HEADER;

    header = getNewProtocolHeader();
  }

  protected final TCNetworkMessage processIncomingData(TCConnection source, TCReference data)
      throws TCProtocolException {
    if (mode == MODE_HEADER) { return processHeaderData(source, data); }

    Assert.eval(mode == MODE_DATA);

    return processPayloadData(source, data);
  }

  protected final Logger getLogger() {
    return logger;
  }

  private TCNetworkMessage processHeaderData(TCConnection source, TCReference data) throws TCProtocolException {
    final int headerLength = this.header.getHeaderByteLength();
    final int bufferLength = header.getDataBuffer().limit();
    
    Assert.assertEquals(data.available(), bufferLength);
    // copy header length into header buffer
    TCByteBuffer headerBuf = header.getDataBuffer();
    for (TCByteBuffer b : data) {
      headerBuf.put(b);
    }
    
    if (!this.header.isHeaderLengthAvail()) { return null; }

    if (headerLength == AbstractTCNetworkHeader.LENGTH_NOT_AVAIL) { return null; }

    if ((headerLength < header.minLength) || (headerLength > header.maxLength) || (headerLength < bufferLength)) {
      // header data is screwed
      throw new TCProtocolException("Invalid Header Length: " + headerLength + ", min: " + header.minLength + ", max: "
                                    + header.maxLength + ", bufLen: " + bufferLength);
    }

    if (bufferLength != headerLength) {
      // maybe we should support a way to swap out the header buffer for a larger sized one
      // instead of always mandating that the backing buffer behind a header have
      // enough capacity for the largest possible header for the given protocol. Just a thought

      // protocol header is bigger than min length, adjust buffer limit and continue
      header.getDataBuffer().limit(headerLength);
      return null;
    } else {
      Assert.eval(bufferLength == headerLength);

      if (header.getDataBuffer().position() == headerLength) {
        this.header.validate();

        this.mode = MODE_DATA;
        
        dataBytesNeeded = computeDataLength(this.header);
        // compact out the header data, leave residual bytes to be read
        
        if (dataBytesNeeded < 0) { throw new TCProtocolException("Negative data size detected: "
                                                                      + dataBytesNeeded); }
        
        // allow for message types with zero length data payload
        if (0 == dataBytesNeeded) {
          return createMessage(source, this.header, null);
        } else if (dataBytesNeeded < data.available()) {
          data.limit(dataBytesNeeded);
          return processPayloadData(source, data);
        }

        return null;
      } else {
        // protocol header not completely read yet, do nothing
        return null;
      }
    }
  }
  
  private TCNetworkMessage processPayloadData(TCConnection source, TCReference data) throws TCProtocolException {
    TCNetworkMessage msg = createMessage(source, header, data.duplicate());

    if (logger.isDebugEnabled()) {
      logger.debug("Message complete on connection " + source + ": " + msg.toString());
    }

    return msg;
  }
}
