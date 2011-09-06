/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.logging.TCLogger;
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

  private final TCLogger          logger;
  private int                     dataBytesNeeded;
  private AbstractTCNetworkHeader header;
  private TCByteBuffer[]          dataBuffers;
  private int                     bufferIndex       = -1;
  private int                     mode;

  public AbstractTCProtocolAdaptor(TCLogger logger) {
    this.logger = logger;
    init();
  }

  public void addReadData(TCConnection source, TCByteBuffer[] data, int length) throws TCProtocolException {
    processIncomingData(source, data, length);
  }

  public final TCByteBuffer[] getReadBuffers() {
    if (mode == MODE_HEADER) { return new TCByteBuffer[] { header.getDataBuffer() }; }

    Assert.eval(mode == MODE_DATA);

    if (dataBuffers == null) {
      dataBuffers = createDataBuffers(dataBytesNeeded);
      Assert.eval(dataBuffers.length > 0);
      bufferIndex = 0;
    }

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
  abstract protected TCNetworkMessage createMessage(TCConnection source, TCNetworkHeader hdr, TCByteBuffer[] data)
      throws TCProtocolException;

  abstract protected int computeDataLength(TCNetworkHeader hdr);

  protected final void init() {
    mode = MODE_HEADER;
    dataBuffers = null;
    header = getNewProtocolHeader();
  }

  protected final TCNetworkMessage processIncomingData(TCConnection source, TCByteBuffer[] data, final int length)
      throws TCProtocolException {
    if (mode == MODE_HEADER) { return processHeaderData(source, data); }

    Assert.eval(mode == MODE_DATA);
    if (length > dataBytesNeeded) { throw new TCProtocolException("More data read then expected: (" + length + " > "
                                                                  + dataBytesNeeded + ")"); }
    return processPayloadData(source, data);
  }

  protected final TCLogger getLogger() {
    return logger;
  }

  private TCByteBuffer[] createDataBuffers(int length) {
    Assert.eval(mode == MODE_DATA);
    return TCByteBufferFactory.getFixedSizedInstancesForLength(false, length);
  }

  private TCNetworkMessage processHeaderData(TCConnection source, final TCByteBuffer[] data) throws TCProtocolException {
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
        this.dataBuffers = null;

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

  private TCNetworkMessage processPayloadData(TCConnection source, final TCByteBuffer[] data) throws TCProtocolException {
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

      // message is complete!
      TCNetworkMessage msg = createMessage(source, header, dataBuffers);

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