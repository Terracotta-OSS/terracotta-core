/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.memorydatastore.message;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.util.Collection;

public class MemoryDataStoreResponseMessage extends DSOMessageBase {
  private static final byte       TYPE                   = 1;
  private static final byte       THREAD_ID              = 2;
  private static final byte       REQUEST_COMPLETED_FLAG = 3;
  private static final byte       VALUE                  = 4;
  private static final byte       NUM_OF_REMOVE          = 5;

  public static final int         PUT_RESPONSE           = 4;
  public static final int         GET_RESPONSE           = 5;
  public static final int         GET_ALL_RESPONSE       = 6;
  public static final int         REMOVE_RESPONSE        = 7;
  public static final int         REMOVE_ALL_RESPONSE    = 8;

  private int                     type;
  private int                     numOfRemove;
  private boolean                 requestCompletedFlag;
  private ThreadID                threadID;
  private TCMemoryDataStoreMessageData value;

  public MemoryDataStoreResponseMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutput out, MessageChannel channel,
      TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public MemoryDataStoreResponseMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
      TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    putNVPair(TYPE, this.type);
    putNVPair(THREAD_ID, threadID.toLong());
    putNVPair(REQUEST_COMPLETED_FLAG, this.requestCompletedFlag);
    if (isGetResponse() || isGetAllResponse() || isRemoveResponse()) {
      putNVPair(VALUE, value);
    }
    if (isRemoveAllResponse()) {
      putNVPair(NUM_OF_REMOVE, this.numOfRemove);
    }
  }

  protected String describePayload() {
    StringBuffer rv = new StringBuffer();
    rv.append("Type : ");

    if (isPutResponse()) {
      rv.append("PUT RESPONSE \n");
    } else if (isGetResponse()) {
      rv.append("GET RESPONSE \n");
    } else if (isRemoveResponse()) {
      rv.append("REMOVE RESPONSE \n");
    } else {
      rv.append("UNKNOWN \n");
    }
    rv.append("Request Completed Flag: ");
    rv.append(this.requestCompletedFlag);
    rv.append(" \n");

    rv.append(value).append('\n');

    return rv.toString();
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
    case TYPE:
      this.type = getIntValue();
      return true;
    case THREAD_ID:
      this.threadID = new ThreadID(getLongValue());
      return true;
    case REQUEST_COMPLETED_FLAG:
      this.requestCompletedFlag = getBooleanValue();
      return true;
    case VALUE:
      value = new TCMemoryDataStoreMessageData(type);
      getObject(value);
      return true;
    case NUM_OF_REMOVE:
      this.numOfRemove = getIntValue();
      return true;
    default:
      return false;
    }
  }

  public void initializePutResponse(ThreadID threadID, boolean requestCompletedFlag) {
    this.type = PUT_RESPONSE;
    this.threadID = threadID;
    this.requestCompletedFlag = requestCompletedFlag;
  }

  public void initializeGetResponse(ThreadID threadID, byte[] value, boolean requestCompletedFlag) {
    this.type = GET_RESPONSE;
    this.threadID = threadID;
    this.requestCompletedFlag = requestCompletedFlag;
    this.value = new TCMemoryDataStoreMessageData(type, null, value);
  }
  
  public void initializeGetAllResponse(ThreadID threadID, Collection values, boolean requestCompletedFlag) {
    this.type = GET_ALL_RESPONSE;
    this.threadID = threadID;
    this.requestCompletedFlag = requestCompletedFlag;
    this.value = new TCMemoryDataStoreMessageData(type, null, values);
  }
  
  public void initializeRemoveResponse(ThreadID threadID, byte[] value, boolean requestCompletedFlag) {
    this.type = REMOVE_RESPONSE;
    this.threadID = threadID;
    this.requestCompletedFlag = requestCompletedFlag;
    this.value = new TCMemoryDataStoreMessageData(type, null, value);
  }
  
  public void initializeRemoveAllResponse(ThreadID threadID, int numOfRemove, boolean requestCompletedFlag) {
    this.type = REMOVE_ALL_RESPONSE;
    this.threadID = threadID;
    this.numOfRemove = numOfRemove;
    this.requestCompletedFlag = requestCompletedFlag;
  }

  public boolean isRequestCompletedFlag() {
    return requestCompletedFlag;
  }

  public byte[] getValue() {
    return this.value.getValue();
  }
  
  public Collection getValues() {
    return this.value.getValues();
  }

  public ThreadID getThreadID() {
    return this.threadID;
  }

  public int getType() {
    return this.type;
  }

  public int getNumOfRemove() {
    return numOfRemove;
  }
  
  public boolean isGetResponse() {
    return this.type == GET_RESPONSE || this.type == GET_ALL_RESPONSE;
  }
  
  private boolean isPutResponse() {
    return this.type == PUT_RESPONSE;
  }

  private boolean isGetAllResponse() {
    return this.type == GET_ALL_RESPONSE;
  }
  
  private boolean isRemoveResponse() {
    return this.type == REMOVE_RESPONSE;
  }
  
  private boolean isRemoveAllResponse() {
    return this.type == REMOVE_ALL_RESPONSE;
  }
}