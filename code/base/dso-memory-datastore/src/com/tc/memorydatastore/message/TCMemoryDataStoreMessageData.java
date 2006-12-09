/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.memorydatastore.message;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class TCMemoryDataStoreMessageData implements TCSerializable {
  private final int  type;
  private byte[]     key;
  private byte[]     value;
  private Collection values;

  public TCMemoryDataStoreMessageData(int type) {
    this.type = type;
  }
  
  public TCMemoryDataStoreMessageData(int type, byte[] key) {
    this.type = type;
    this.key = key;
    this.value = null;
    this.values = null;
  }

  public TCMemoryDataStoreMessageData(int type, byte[] key, Collection values) {
    this.type = type;
    this.key = key;
    this.values = values;
  }

  public TCMemoryDataStoreMessageData(int type, byte[] key, byte[] value) {
    this.type = type;
    this.key = key;
    this.value = value;
  }

  public byte[] getKey() {
    return key;
  }

  public byte[] getValue() {
    return value;
  }

  public Collection getValues() {
    return values;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    if (key != null) {
      // serialOutput.writeByte(KEY);
      serialOutput.writeInt(key.length);
      serialOutput.write(key);
    }
    if (value != null) {
      // serialOutput.writeByte(VALUE);
      serialOutput.writeInt(value.length);
      serialOutput.write(value);
    } else {
      if (type == MemoryDataStoreResponseMessage.GET_RESPONSE || type == MemoryDataStoreResponseMessage.REMOVE_RESPONSE
          || type == MemoryDataStoreResponseMessage.REMOVE_ALL_RESPONSE) {
        serialOutput.writeInt(0);
      } else if (type == MemoryDataStoreResponseMessage.GET_ALL_RESPONSE) {
        serialOutput.writeInt(values.size());
        for (Iterator i = values.iterator(); i.hasNext();) {
          TCByteArrayKeyValuePair keyValuePair = (TCByteArrayKeyValuePair)i.next();
          keyValuePair.serializeTo(serialOutput);
        }
      }
    }
  }

  public Object deserializeFrom(TCByteBufferInputStream serialInput) throws IOException {
    int length = 0;
    switch (type) {
    case MemoryDataStoreRequestMessage.PUT:
      length = serialInput.readInt();
      this.key = new byte[length];
      serialInput.read(this.key);

      length = serialInput.readInt();
      this.value = new byte[length];
      serialInput.read(this.value);
      break;
    case MemoryDataStoreRequestMessage.GET:
      length = serialInput.readInt();
      this.key = new byte[length];
      serialInput.read(this.key);
      break;
    case MemoryDataStoreRequestMessage.REMOVE:
      length = serialInput.readInt();
      this.key = new byte[length];
      serialInput.read(this.key);
      break;
    case MemoryDataStoreResponseMessage.GET_RESPONSE:
      length = serialInput.readInt();
      if (length > 0) {
        this.value = new byte[length];
        serialInput.read(this.value);
      }
      break;
    case MemoryDataStoreResponseMessage.GET_ALL_RESPONSE:
      int size = serialInput.readInt();
      this.values = new ArrayList(size);
      for (int i = 0; i < size; i++) {
        TCByteArrayKeyValuePair keyValuePair = new TCByteArrayKeyValuePair();
        keyValuePair.deserializeFrom(serialInput);
        values.add(keyValuePair);
      }
      break;
    case MemoryDataStoreResponseMessage.REMOVE_RESPONSE:
      length = serialInput.readInt();
      if (length > 0) {
        this.value = new byte[length];
        serialInput.read(this.value);
      }
      break;
    }
    return this;
  }
}