/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.TCInternalError;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;
import com.tc.util.HexDump;
import com.tc.util.StringUtil;
import com.tc.util.concurrent.SetOnceFlag;

/**
 * Base class for network messages
 * 
 * @author teck
 */
public class AbstractTCNetworkMessage implements TCNetworkMessage {
  protected static final TCLogger logger                = TCLogging.getLogger(TCNetworkMessage.class);
  private static final int        MESSAGE_DUMP_MAXBYTES = 4 * 1024;

  protected AbstractTCNetworkMessage(TCNetworkHeader header, boolean seal) {
    this(header, null, null, seal);
  }

  protected AbstractTCNetworkMessage(TCNetworkHeader header, TCNetworkMessage msgPayload) {
    this(header, msgPayload, null, true);
  }

  protected AbstractTCNetworkMessage(TCNetworkHeader header, TCByteBuffer[] payload) {
    this(header, null, payload, true);
  }

  private AbstractTCNetworkMessage(TCNetworkHeader header, TCNetworkMessage msgPayload, TCByteBuffer[] payload,
                                   boolean seal) {
    Assert.eval(header != null);

    this.header = header;
    this.messagePayload = msgPayload;
    this.payloadData = (payload == null) ? EMPTY_BUFFER_ARRAY : payload;

    if (msgPayload != null) {
      payloadData = msgPayload.getEntireMessageData();
    }

    if (seal) {
      seal();
    }
  }

  public final int getDataLength() {
    checkSealed();
    return dataLength;
  }

  public final int getHeaderLength() {
    checkSealed();
    return headerLength;
  }

  public final int getTotalLength() {
    checkSealed();
    return totalLength;
  }

  public final TCNetworkHeader getHeader() {
    checkNotRecycled();
    return header;
  }

  public final TCNetworkMessage getMessagePayload() {
    checkNotRecycled();
    return messagePayload;
  }

  public final TCByteBuffer[] getPayload() {
    checkNotRecycled();
    return payloadData;
  }

  protected final void setPayload(TCByteBuffer[] newPayload) {
    checkNotSealed();

    entireMessageData = null;

    if (newPayload == null) {
      payloadData = EMPTY_BUFFER_ARRAY;
    } else {
      payloadData = newPayload;
    }
  }

  protected final void setMessagePayload(TCNetworkMessage subMessage) {
    checkNotSealed();

    entireMessageData = null;

    if (subMessage == null) {
      payloadData = EMPTY_BUFFER_ARRAY;
      this.messagePayload = null;
    } else {
      if (!subMessage.isSealed()) { throw new IllegalStateException("Message paylaod is not yet sealed"); }
      this.messagePayload = subMessage;
      this.payloadData = subMessage.getEntireMessageData();
    }
  }

  public final TCByteBuffer[] getEntireMessageData() {
    checkSealed();

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
    StringBuffer buf = new StringBuffer();
    buf.append("Message Class: ").append(getClass().getName()).append("\n");
    buf.append("Sealed: ").append(sealed.isSet()).append(", ");
    buf.append("Header Length: ").append(getHeaderLength()).append(", ");
    buf.append("Data Length: ").append(getDataLength()).append(", ");
    buf.append("Total Length: ").append(getTotalLength()).append("\n");

    String extraMsgInfo = describeMessage();
    if (extraMsgInfo != null) {
      buf.append(extraMsgInfo);
    }

    if (header != null) {
      buf.append("Header (").append(header.getClass().getName()).append(")\n");
      buf.append(StringUtil.indentLines(header.toString()));
      if (buf.charAt(buf.length() - 1) != '\n') {
        buf.append('\n');
      }
    }

    buf.append("Payload:\n");
    if (messagePayload != null) {
      buf.append(StringUtil.indentLines(messagePayload.toString())).append("\n");
    } else {
      if (payloadData != null) {
        buf.append(StringUtil.indentLines(describePayload()));
      } else {
        buf.append(StringUtil.indentLines("*** No payoad data ***\n"));
      }
    }

    return buf.toString();
  }

  // override this method to add more information about your message
  protected String describeMessage() {
    return null;
  }

  // override this method to add more description to your payload data
  protected String describePayload() {
    StringBuffer buf = new StringBuffer();
    int totalBytesDumped = 0;
    if ((payloadData != null) && (payloadData.length != 0)) {
      for (int i = 0; i < payloadData.length; i++) {
        buf.append("Buffer ").append(i).append(": ");
        if (payloadData[i] != null) {

          buf.append(payloadData[i].toString());
          buf.append("\n");

          if (totalBytesDumped < MESSAGE_DUMP_MAXBYTES) {
            int bytesFullBuf = payloadData[i].limit();
            int bytesToDump = (((totalBytesDumped + bytesFullBuf) < MESSAGE_DUMP_MAXBYTES) ? bytesFullBuf
                : (MESSAGE_DUMP_MAXBYTES - totalBytesDumped));

            buf.append(HexDump.dump(payloadData[i].array(), payloadData[i].arrayOffset(), bytesToDump));
            totalBytesDumped += bytesToDump;
          }
        } else {
          buf.append("null");
        }
      }
    } else {
      buf.append("No payload buffers present");
    }

    return buf.toString();
  }

  protected String dump() {
    StringBuffer toRet = new StringBuffer(toString());
    toRet.append("\n\n");
    if (entireMessageData != null) {
      for (int i = 0; i < entireMessageData.length; i++) {
        toRet.append('[').append(i).append(']').append('=').append(entireMessageData[i].toString());
        toRet.append(" =  { ");
        byte ba[] = entireMessageData[i].array();
        for (byte element : ba) {
          toRet.append(Byte.toString(element)).append(' ');
        }
        toRet.append(" }  \n\n");
      }
    }
    return toRet.toString();
  }

  public final boolean isSealed() {
    return sealed.isSet();
  }

  public final void seal() {
    if (sealed.attemptSet()) {
      final int size = 1 + payloadData.length;
      entireMessageData = new TCByteBuffer[size];
      entireMessageData[0] = header.getDataBuffer();
      System.arraycopy(payloadData, 0, entireMessageData, 1, payloadData.length);

      long dataLen = 0;
      for (int i = 1; i < entireMessageData.length; i++) {
        dataLen += entireMessageData[i].limit();
      }

      if (dataLen > Integer.MAX_VALUE) { throw new TCInternalError("Message too big"); }

      this.dataLength = (int) dataLen;
      this.headerLength = header.getHeaderByteLength();
      this.totalLength = this.headerLength + this.dataLength;
    } else {
      throw new IllegalStateException("Message is sealed");
    }
  }

  public final void wasSent() {
    fireSentCallback();
    doRecycleOnWrite();
  }

  // Can be overloaded by sub classes to decide when to recycle differently.
  public void doRecycleOnWrite() {
    recycle();
  }

  public void recycle() {
    if (entireMessageData != null) {
      int i = 0;
      if (entireMessageData.length > 1 && entireMessageData[0].array() == entireMessageData[1].array()) {
        // This is done as TCMessageParser creates a dupilcate of the first buffer for the header.
        // @see TCMessageParser.parseMessage()
        // Can be done more elegantly, but it is done like this keeping performance in mind.
        i++;
      }
      for (; i < entireMessageData.length; i++) {
        entireMessageData[i].recycle();
      }
      entireMessageData = null;
    } else {
      logger.warn("Entire Message is null ! Probably recycle was called twice ! ");
      Thread.dumpStack();
    }
  }

  protected boolean isRecycled() {
    return isSealed() && entireMessageData == null;
  }

  private void fireSentCallback() {
    if (sentCallback != null) {
      if (sentCallbackFired.attemptSet()) {
        try {
          sentCallback.run();
        } catch (Exception e) {
          logger.error("Caught exception running sent callback", e);
        }
      }
    }
  }

  public final void setSentCallback(Runnable callback) {
    this.sentCallback = callback;
  }

  public final Runnable getSentCallback() {
    return this.sentCallback;
  }

  private void checkNotRecycled() {
    if (isRecycled()) { throw new IllegalStateException("Message is already Recycled"); }
  }

  private void checkSealed() {
    if (!isSealed()) { throw new IllegalStateException("Message is not sealed"); }
  }

  private void checkNotSealed() {
    if (sealed.isSet()) { throw new IllegalStateException("Message is sealed"); }
  }

  private final SetOnceFlag           sealed             = new SetOnceFlag();
  private final SetOnceFlag           sentCallbackFired  = new SetOnceFlag();
  private static final TCByteBuffer[] EMPTY_BUFFER_ARRAY = {};
  private final TCNetworkHeader       header;
  private TCByteBuffer[]              payloadData;
  private TCNetworkMessage            messagePayload;
  private TCByteBuffer[]              entireMessageData;
  private int                         totalLength;
  private int                         dataLength;
  private int                         headerLength;
  private Runnable                    sentCallback       = null;

}