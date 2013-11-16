/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.SearchRequestID;
import com.tc.object.session.SessionID;

import java.io.IOException;

public class SearchResultsCloseMessageImpl extends DSOMessageBase implements SearchResultsCloseMessage {

  private static final byte SEARCH_REQUEST_ID = 0;
  private static final byte CACHE_NAME        = 1;

  private SearchRequestID   reqId;
  private String            cacheName;

  @Override
  protected void dehydrateValues() {
    putNVPair(CACHE_NAME, cacheName);
    putNVPair(SEARCH_REQUEST_ID, reqId.toLong());
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case CACHE_NAME:
        cacheName = getStringValue();
        return true;
      case SEARCH_REQUEST_ID:
        reqId = new SearchRequestID(getLongValue());
        return true;
      default:
        return false;
    }
  }

  public SearchResultsCloseMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                              MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public SearchResultsCloseMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                              TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  public ClientID getClientID() {
    return (ClientID) getSourceNodeID();
  }

  @Override
  public SearchRequestID getRequestID() {
    return reqId;
  }

  @Override
  public String getCacheName() {
    return cacheName;
  }

  @Override
  public void initialize(final String name, SearchRequestID requestId) {
    this.cacheName = name;
    this.reqId = requestId;
  }
}
