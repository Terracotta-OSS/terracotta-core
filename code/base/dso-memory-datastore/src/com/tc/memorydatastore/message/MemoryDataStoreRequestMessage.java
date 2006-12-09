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

public class MemoryDataStoreRequestMessage extends DSOMessageBase {
  private static final byte       TYPE            = 1;
  private static final byte       DATA_STORE_NAME = 2;
  private static final byte       THREAD_ID       = 3;
  private static final byte       DATA            = 4;
  private static final byte       REMOVE_ALL_FLAG = 5;
  private static final byte       GET_ALL_FLAG    = 6;

  public static final int         PUT             = 1;
  public static final int         GET             = 2;
  public static final int         REMOVE          = 3;

  private int                     type;
  private boolean                 removeAll;
  private boolean                 getAll;
  private String                  dataStoreName;
  private ThreadID                threadID;
  private TCMemoryDataStoreMessageData data;

  public MemoryDataStoreRequestMessage(MessageMonitor monitor, TCByteBufferOutput out, MessageChannel channel,
      TCMessageType type) {
    super(monitor, out, channel, type);
  }

  public MemoryDataStoreRequestMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
      TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    putNVPair(TYPE, this.type);
    putNVPair(THREAD_ID, this.threadID.toLong());
    putNVPair(DATA_STORE_NAME, this.dataStoreName);
    if (isRemoveAll()) {
      putNVPair(REMOVE_ALL_FLAG, this.removeAll);
    }
    if (isGetAll()) {
      putNVPair(GET_ALL_FLAG, this.getAll);
    }
    putNVPair(DATA, data);
  }

  protected String describePayload() {
    StringBuffer rv = new StringBuffer();
    rv.append("Type : ");

    if (isPut()) {
      rv.append("PUT \n");
    } else if (isGet()) {
      rv.append("GET \n");
    } else if (isRemove()) {
      rv.append("REMOVE \n");
    } else {
      rv.append("UNKNOWN \n");
    }

    rv.append(threadID.toLong()).append(" \n");
    rv.append(data).append(" from ").append(dataStoreName).append('\n');

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
    case REMOVE_ALL_FLAG:
      this.removeAll = getBooleanValue();
      return true;
    case GET_ALL_FLAG:
      this.getAll = getBooleanValue();
      return true;
    case DATA_STORE_NAME:
      this.dataStoreName = getStringValue();
      return true;
    case DATA:
      data = new TCMemoryDataStoreMessageData(type);
      getObject(data);
      return true;
    default:
      return false;
    }
  }

  public void initializePut(ThreadID threadID, String dataStoreName, byte[] key, byte[] value) {
    this.type = PUT;  
    this.threadID = threadID;
    this.dataStoreName = dataStoreName;
    this.data = new TCMemoryDataStoreMessageData(type, key, value);
  }

  public void initializeGet(ThreadID threadID, String dataStoreName, byte[] key, boolean getAll) {
    this.type = GET;
    this.threadID = threadID;
    this.dataStoreName = dataStoreName;
    this.getAll = getAll;
    this.data = new TCMemoryDataStoreMessageData(type, key);
  }
  
  public void initializeRemove(ThreadID threadID, String dataStoreName, byte[] key, boolean removeAll) {
    this.type = REMOVE;
    this.threadID = threadID;
    this.dataStoreName = dataStoreName;
    this.removeAll = removeAll;
    this.data = new TCMemoryDataStoreMessageData(type, key);
  }

  public byte[] getKey() {
    return this.data.getKey();
  }

  public byte[] getValue() {
    return this.data.getValue();
  }

  public int getType() {
    return this.type;
  }

  public ThreadID getThreadID() {
    return this.threadID;
  }

  public String getDataStoreName() {
    return this.dataStoreName;
  }

  public boolean isRemoveAll() {
    return removeAll;
  }
  
  public boolean isGetAll() {
    return getAll;
  }
  
  private boolean isPut() {
    return this.type == PUT;
  }

  private boolean isGet() {
    return this.type == GET;
  }
  
  private boolean isRemove() {
    return this.type == REMOVE;
  }
}