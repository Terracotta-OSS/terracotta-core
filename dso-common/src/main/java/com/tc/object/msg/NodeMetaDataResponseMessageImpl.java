/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.locks.ThreadID;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;

import java.io.IOException;

public class NodeMetaDataResponseMessageImpl extends DSOMessageBase implements NodeMetaDataResponseMessage {

  private final static byte THREAD_ID   = 1;
  private final static byte IP_ID       = 2;
  private final static byte HOSTNAME_ID = 3;

  private ThreadID          threadID;
  private String            ip;
  private String            hostname;

  public NodeMetaDataResponseMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                         final TCByteBufferOutputStream out, final MessageChannel channel,
                                         final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public NodeMetaDataResponseMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                         final MessageChannel channel, final TCMessageHeader header,
                                         final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public void initialize(final ThreadID tID, final String theIp, final String theHostname) {
    this.threadID = tID;
    this.ip = theIp;
    this.hostname = theHostname;
  }

  @Override
  protected void dehydrateValues() {
    Assert.assertNotNull(threadID);

    putNVPair(THREAD_ID, threadID.toLong());
    putNVPair(IP_ID, ip);
    putNVPair(HOSTNAME_ID, hostname);
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case THREAD_ID:
        threadID = new ThreadID(getLongValue());
        return true;
      case IP_ID:
        ip = getStringValue();
        return true;
      case HOSTNAME_ID:
        hostname = getStringValue();
        return true;
      default:
        return false;
    }
  }

  public ThreadID getThreadID() {
    return threadID;
  }

  public String getIp() {
    return ip;
  }

  public String getHostname() {
    return hostname;
  }
}
