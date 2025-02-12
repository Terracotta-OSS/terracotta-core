/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.l2.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.ServerID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;


public class PlatformInfoRequest extends AbstractGroupMessage {
  // Factory methods.
  /**
   * Called by the active when a new passive joins the cluster or when an active is selected, in order to ask all the
   *  passives for their information.
   * 
   * @return The message instance.
   */
  public static PlatformInfoRequest createEmptyRequest() {
    return new PlatformInfoRequest(REQUEST, -1, null, null, null, null, null, null, null);
  }

  public static PlatformInfoRequest createAddNode(long consumerID, String[] parents, String name, Serializable value) {
    return new PlatformInfoRequest(RESPONSE_ADD, consumerID, parents, name, value, null, null, null, null);
  }

  public static PlatformInfoRequest createRemoveNode(long consumerID, String[] parents, String name) {
    return new PlatformInfoRequest(RESPONSE_REMOVE, consumerID, parents, name, null, null, null, null, null);
  }

  public static PlatformInfoRequest createServerInfoMessage(Serializable serverInfo) {
    return new PlatformInfoRequest(RESPONSE_INFO, -1, null, null, null, serverInfo, null, null, null);
  }
  
  public static PlatformInfoRequest createServerInfoRemoveMessage(ServerID server) {
    return new PlatformInfoRequest(INFO_REMOVE, -1, null, null, null, server, null, null, null);
  }
  
  public static PlatformInfoRequest createBestEffortsBatch(long[] consumerIDs, String[] keys, Serializable[] values) {
    // This should not be called if there is no data.
    Assert.assertTrue(consumerIDs.length > 0);
    // Note that this would, ideally, use a different mechanism but the SEDA implementation routes based on type so we need
    //  to extend this class, for now.
    return new PlatformInfoRequest(BEST_EFFORTS_BATCH, -1, null, null, null, null, consumerIDs, keys, values);
  }


//  message types  
  public static final int ERROR               = 0;
  public static final int REQUEST               = 1;
  public static final int RESPONSE_INFO               = 2;
  public static final int RESPONSE_ADD               = 3;
  public static final int RESPONSE_REMOVE               = 4;
  public static final int BEST_EFFORTS_BATCH               = 5;
  public static final int INFO_REMOVE                     = 6;
  
  // Info related to RESPONSE_ADD and RESPONSE_REMOVE.
  private long changeConsumerID;
  private String[] nodeParents;
  private String nodeName;
  private Serializable nodeValue;
  
  // Info related only to RESPONSE_INFO.
  private Serializable serverInfo;
  
  // Info specific to BEST_EFFORTS_BATCH.
  private long[] consumerIDs;
  private String[] keys;
  private Serializable[] values;


  // Must be public for serialization initializer.
  public PlatformInfoRequest() {
    super(ERROR);
  }

  private PlatformInfoRequest(int type, long changeConsumerID, String[] nodeParents, String nodeName, Serializable nodeValue, Serializable serverInfo, long[] consumerIDs, String[] keys, Serializable[] values) {
    super(type);
    // Info related to RESPONSE_ADD and RESPONSE_REMOVE.
    this.changeConsumerID = changeConsumerID;
    this.nodeParents = nodeParents;
    this.nodeName = nodeName;
    this.nodeValue = nodeValue;
    
    // Info related only to RESPONSE_INFO.
    this.serverInfo = serverInfo;
    
    // Info related to BEST_EFFORTS_BATCH.
    this.consumerIDs = consumerIDs;
    this.keys = keys;
    this.values = values;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    switch (getType()) {
    case REQUEST:
      // No additional data.
      break;
    case RESPONSE_INFO: {
      // Only the serverInfo.
      int valueSize = in.readInt();
      Assert.assertTrue(valueSize > 0);
      byte[] valueArray = new byte[valueSize];
      in.readFully(valueArray);
      this.serverInfo = deserializeByteArray(this.getClass().getClassLoader(), valueArray);
      break;
    }
    case RESPONSE_ADD: {
      // All fields but serverInfo.
      this.changeConsumerID = in.readLong();
      int parentCount = in.readInt();
      this.nodeParents = new String[parentCount];
      for (int i = 0; i < parentCount; ++i) {
        this.nodeParents[i] = in.readString();
      }
      this.nodeName = in.readString();
      int valueSize = in.readInt();
      if (valueSize > 0) {
        byte[] valueArray = new byte[valueSize];
        in.readFully(valueArray);
        this.nodeValue = valueArray;
      } else {
        this.nodeValue = null;
      }
      break;
    }
    case RESPONSE_REMOVE: {
      // All fields except nodeValue or serverInfo.
      this.changeConsumerID = in.readLong();
      int parentCount = in.readInt();
      this.nodeParents = new String[parentCount];
      for (int i = 0; i < parentCount; ++i) {
        this.nodeParents[i] = in.readString();
      }
      this.nodeName = in.readString();
      break;
    }
    case BEST_EFFORTS_BATCH: {
      // Need to read only the consumerIDs, keys, and values.
      // Find out how many tuples there are.
      int tupleCount = in.readInt();
      long[] consumerIDs = new long[tupleCount];
      String[] keys = new String[tupleCount];
      Serializable[] values = new Serializable[tupleCount];
      for (int i = 0; i < tupleCount; ++i) {
        consumerIDs[i] = in.readLong();
        keys[i] = in.readString();
        int valueSize = in.readInt();
        if (valueSize > 0) {
          byte[] valueArray = new byte[valueSize];
          in.readFully(valueArray);
          values[i] = valueArray;
        } else {
          values[i] = null;
        }
      }
      this.consumerIDs = consumerIDs;
      this.keys = keys;
      this.values = values;
      break;
    }
    default:
      Assert.fail();
    }
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    switch (getType()) {
    case REQUEST:
      // No additional data.
      break;
    case RESPONSE_INFO: {
      // Only the serverInfo.
      byte[] serializedValue = serialize(this.serverInfo);
      out.writeInt(serializedValue.length);
      out.write(serializedValue);
      break;
    }
    case RESPONSE_ADD: {
      // All fields but serverInfo.
      out.writeLong(this.changeConsumerID);
      out.writeInt(nodeParents.length);
      for (int i = 0; i < nodeParents.length; ++i) {
        out.writeString(this.nodeParents[i]);
      }
      out.writeString(this.nodeName);
      if (null != this.nodeValue) {
        byte[] serializedValue = serialize(this.nodeValue);
        out.writeInt(serializedValue.length);
        out.write(serializedValue);
      } else {
        out.writeInt(0);
      }
      break;
    }
    case RESPONSE_REMOVE: {
      // All fields except nodeValue or serverInfo.
      out.writeLong(this.changeConsumerID);
      out.writeInt(nodeParents.length);
      for (int i = 0; i < nodeParents.length; ++i) {
        out.writeString(this.nodeParents[i]);
      }
      out.writeString(this.nodeName);
      break;
    }
    case BEST_EFFORTS_BATCH: {
      // Need to read only the consumerIDs, keys, and values.
      // First, write an int of how many tuples there are.
      out.writeInt(this.consumerIDs.length);
      for (int i = 0; i < this.consumerIDs.length; ++i) {
        out.writeLong(this.consumerIDs[i]);
        out.writeString(this.keys[i]);
        if (null != this.values[i]) {
          byte[] serializedValue = serialize(this.values[i]);
          out.writeInt(serializedValue.length);
          out.write(serializedValue);
        } else {
          out.writeInt(0);
        }
      }
      break;
    }
    default:
      Assert.fail();
    }
  }

  public long getConsumerID() {
    return this.changeConsumerID;
  }

  public String[] getParents() {
    return this.nodeParents;
  }

  public String getNodeName() {
    return this.nodeName;
  }

  public Serializable getNodeValue(ClassLoader loader) {
    if (this.nodeValue instanceof byte[]) {
      this.nodeValue = deserializeByteArray(loader, (byte[])this.nodeValue);
    }
    return this.nodeValue;
  }

  public Serializable getServerInfo() {
    return this.serverInfo;
  }

  public long[] getConsumerIDs() {
    return this.consumerIDs;
  }

  public String[] getKeys() {
    return this.keys;
  }

  public Serializable[] getValues(ClassLoader loader) {
    for (int x=0;x<this.values.length;x++) {
      if (this.values[x] instanceof byte[]) {
        this.values[x] = deserializeByteArray(loader, (byte[])this.values[x]);
      }
    }
    return this.values;
  }

  private byte[] serialize(Serializable value) {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    byte[] result = null;
    try {
      ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
      objectStream.writeObject(value);
      objectStream.close();
      result = byteStream.toByteArray();
    } catch (IOException e) {
      // We don't expect to fail to serialize monitoring data.
      Assert.fail();
    }
    return result;
  }
  
  private Serializable deserializeByteArray(ClassLoader loader, byte[] valueArray) {
    ByteArrayInputStream byteStream = new ByteArrayInputStream(valueArray);
    try {
      ObjectInputStream objectStream = new ObjectInputStream(byteStream) {
        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
          String name = desc.getName();
          return Class.forName(name, true, loader);
        }
      };
      return (Serializable) objectStream.readObject();
    } catch (IOException e) {
      // We don't expect to fail to deserialize monitoring data.
      Assert.fail();
    } catch (ClassNotFoundException e) {
      // We don't expect to fail to deserialize monitoring data.
      throw Assert.failure("platform info request", e);
    }
    return null;
  }
}
