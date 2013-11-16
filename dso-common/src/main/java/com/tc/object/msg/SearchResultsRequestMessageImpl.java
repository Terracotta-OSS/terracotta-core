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

public class SearchResultsRequestMessageImpl extends DSOMessageBase implements SearchResultsRequestMessage {

  private static final byte SEARCH_REQUEST_ID = 0;
  private static final byte CACHE_NAME        = 1;

  private static final byte START_OFFSET      = 2;
  private static final byte SIZE              = 3;

  private SearchRequestID   reqId;
  private int               startOffset;
  private int               pageSize;
  private String            cacheName;

  public SearchResultsRequestMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                         MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public SearchResultsRequestMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
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
  public int getStart() {
    return startOffset;
  }

  @Override
  public int getPageSize() {
    return pageSize;
  }

  @Override
  public String getCacheName() {
    return cacheName;
  }

  @Override
  public void initialize(String cache, SearchRequestID req, int start, int size) {
    this.cacheName = cache;
    this.reqId = req;
    this.startOffset = start;
    this.pageSize = size;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(CACHE_NAME, cacheName);
    putNVPair(SEARCH_REQUEST_ID, reqId.toLong());
    putNVPair(START_OFFSET, startOffset);
    putNVPair(SIZE, pageSize);
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
      case START_OFFSET:
        startOffset = getIntValue();
        return true;
      case SIZE:
        pageSize = getIntValue();
        return true;
      default:
        return false;
    }
  }

}
