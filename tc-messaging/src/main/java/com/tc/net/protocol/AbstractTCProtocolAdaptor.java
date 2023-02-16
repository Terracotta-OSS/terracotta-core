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
import com.tc.bytes.TCByteBufferFactory;
import com.tc.bytes.TCReferenceSupport;
import com.tc.bytes.TCReference;
import com.tc.net.core.TCConnection;
import com.tc.util.Assert;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * Base class for protocol adaptors
 *
 * @author teck
 */
public abstract class AbstractTCProtocolAdaptor implements TCProtocolAdaptor {
  protected static final int      MODE_HEADER       = 1;
  protected static final int      MODE_DATA         = 2;

  private final Logger logger;
  private int                     dataBytesNeeded;
  private AbstractTCNetworkHeader header;
  private TCByteBuffer[]          dataBuffers;
  private int                     bufferIndex       = -1;
  private int                     mode;

  public AbstractTCProtocolAdaptor(Logger logger) {
    this.logger = logger;
    init();
  }
//
//  @Override
//  public void addReadData(TCConnection source, TCByteBuffer[] data, int length) throws TCProtocolException {
//    processIncomingData(source, data, length);
//  }

  @Override
  public final TCByteBuffer[] getReadBuffers() {
    if (mode == MODE_HEADER) { return new TCByteBuffer[] { header.getDataBuffer() }; }

    Assert.eval(mode == MODE_DATA);

    // only return the subset of buffers that can actually receive more bytes
    final TCByteBuffer[] rv = new TCByteBuffer[dataBuffers.length - bufferIndex];
    System.arraycopy(dataBuffers, bufferIndex, rv, 0, rv.length);

    // Make sure we're not passing back a set of arrays with no space left in them
    boolean spaceAvail = false;
    for (int i = 0, n = rv.length; i < n; i++) {
      if (rv[i].hasRemaining()) {
        spaceAvail = true;
        break;
      }
    }

    Assert.assertTrue("No space in buffers to read more data", spaceAvail);
    return rv;
  }

  abstract protected AbstractTCNetworkHeader getNewProtocolHeader();

  // subclasses override this method to return specific message types
  abstract protected TCNetworkMessage createMessage(TCConnection source, TCNetworkHeader hdr, TCReference data)
      throws TCProtocolException;

  abstract protected int computeDataLength(TCNetworkHeader hdr);

  protected final void init() {
    mode = MODE_HEADER;
    dataBuffers = null;
    header = getNewProtocolHeader();
  }

  protected final TCNetworkMessage processIncomingData(TCConnection source, TCByteBuffer[] data, int length, Queue<TCByteBuffer> recycle)
      throws TCProtocolException {
    if (mode == MODE_HEADER) { return processHeaderData(source, data, recycle); }

    Assert.eval(mode == MODE_DATA);
    if (length > dataBytesNeeded) { throw new TCProtocolException("More data read then expected: (" + length + " > "
                                                                  + dataBytesNeeded + ")"); }
    return processPayloadData(source, data, recycle);
  }

  protected final Logger getLogger() {
    return logger;
  }

  private TCByteBuffer[] createDataBuffers(int length, Queue<TCByteBuffer> recycle) {
    Assert.eval(mode == MODE_DATA);
    if (recycle != null) {
      int run = 0;
      List<TCByteBuffer> bufs = new LinkedList<>();
      TCByteBuffer last = null;
      while (run < length) {
        TCByteBuffer buf = recycle.poll();
        if (buf == null) {
          buf = TCByteBufferFactory.getInstance(length - run);
        }
        bufs.add(buf);
        run += buf.remaining();
        last = buf;
      }
      if (last != null) {
        last.limit(last.limit() - (run - length));
      }
      return bufs.toArray(new TCByteBuffer[bufs.size()]);
    } else {
      return new TCByteBuffer[] {TCByteBufferFactory.getInstance(length)};
    }
  }

  private TCNetworkMessage processHeaderData(TCConnection source, TCByteBuffer[] data, Queue<TCByteBuffer> recycle) throws TCProtocolException {
    Assert.eval(data.length == 1);
    Assert.eval(data[0] == this.header.getDataBuffer());

    if (!this.header.isHeaderLengthAvail()) { return null; }

    final TCByteBuffer buf = data[0];
    final int headerLength = this.header.getHeaderByteLength();
    final int bufferLength = buf.limit();

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
      buf.limit(headerLength);
      return null;
    } else {
      Assert.eval(bufferLength == headerLength);

      if (buf.position() == headerLength) {
        this.header.validate();

        this.mode = MODE_DATA;
        this.dataBytesNeeded = computeDataLength(this.header);
        if (dataBuffers != null) {
          recycleBuffers(dataBuffers, recycle);
        }
        this.dataBuffers = createDataBuffers(dataBytesNeeded, recycle);
        this.bufferIndex = 0;

        if (this.dataBytesNeeded < 0) { throw new TCProtocolException("Negative data size detected: "
                                                                      + this.dataBytesNeeded); }

        // allow for message types with zero length data payload
        if (0 == this.dataBytesNeeded) {
          return createMessage(source, this.header, null);
        }

        return null;
      } else {
        // protocol header not completely read yet, do nothing
        return null;
      }
    }
  }

  private static void recycleBuffers(TCByteBuffer[] buffers, Queue<TCByteBuffer> recycle) {
    for (TCByteBuffer buf : buffers) {
      recycle.offer(buf.reInit());
    }
  }
  
  private TCNetworkMessage processPayloadData(TCConnection source, TCByteBuffer[] data, Queue<TCByteBuffer> recycle) throws TCProtocolException {
    for (int i = 0; i < data.length; i++) {
      final TCByteBuffer buffer = data[i];

      if (!buffer.hasRemaining()) {
        buffer.flip();
        dataBytesNeeded -= buffer.limit();
        bufferIndex++;

        if (dataBytesNeeded < 0) { throw new TCProtocolException("More data in buffers than expected"); }
      } else {
        break;
      }
    }

    if (0 == dataBytesNeeded) {
      if (bufferIndex != dataBuffers.length) { throw new TCProtocolException("Not all buffers consumed"); }
      TCByteBuffer[] localDataBuffers = this.dataBuffers;
      // message is complete!
      TCNetworkMessage msg = createMessage(source, header, TCReferenceSupport.createReference(Arrays.asList(localDataBuffers), recycle == null ? r->{} : recycle::add));

      if (logger.isDebugEnabled()) {
        logger.debug("Message complete on connection " + source + ": " + msg.toString());
      }

      return msg;
    }

    Assert.eval(dataBytesNeeded > 0);

    // data portion not done, try again later
    return null;
  }

}
