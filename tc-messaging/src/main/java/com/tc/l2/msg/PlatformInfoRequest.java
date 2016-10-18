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
package com.tc.l2.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
    return new PlatformInfoRequest(REQUEST, -1, null, null, null, null);
  }

  public static PlatformInfoRequest createAddNode(long consumerID, String[] parents, String name, Serializable value) {
    return new PlatformInfoRequest(RESPONSE_ADD, consumerID, parents, name, value, null);
  }

  public static PlatformInfoRequest createRemoveNode(long consumerID, String[] parents, String name) {
    return new PlatformInfoRequest(RESPONSE_REMOVE, consumerID, parents, name, null, null);
  }

  public static PlatformInfoRequest createServerInfoMessage(Serializable serverInfo) {
    return new PlatformInfoRequest(RESPONSE_INFO, -1, null, null, null, serverInfo);
  }


//  message types  
  public static final int ERROR               = 0;
  public static final int REQUEST               = 1;
  public static final int RESPONSE_INFO               = 2;
  public static final int RESPONSE_ADD               = 3;
  public static final int RESPONSE_REMOVE               = 4;
  
  // Info related to RESPONSE_ADD and RESPONSE_REMOVE.
  private long changeConsumerID;
  private String[] nodeParents;
  private String nodeName;
  private Serializable nodeValue;
  
  // Info related only to RESPONSE_INFO.
  private Serializable serverInfo;


  // Must be public for serialization initializer.
  public PlatformInfoRequest() {
    super(ERROR);
  }

  private PlatformInfoRequest(int type, long changeConsumerID, String[] nodeParents, String nodeName, Serializable nodeValue, Serializable serverInfo) {
    super(type);
    // Info related to RESPONSE_ADD and RESPONSE_REMOVE.
    this.changeConsumerID = changeConsumerID;
    this.nodeParents = nodeParents;
    this.nodeName = nodeName;
    this.nodeValue = nodeValue;
    
    // Info related only to RESPONSE_INFO.
    this.serverInfo = serverInfo;
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
      this.serverInfo = deserialize(valueArray);
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
        this.nodeValue = deserialize(valueArray);
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

  public Serializable getNodeValue() {
    return this.nodeValue;
  }

  public Serializable getServerInfo() {
    return this.serverInfo;
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

  private Serializable deserialize(byte[] valueArray) {
    ByteArrayInputStream byteStream = new ByteArrayInputStream(valueArray);
    Serializable object = null;
    try {
      ObjectInputStream objectStream = new ObjectInputStream(byteStream);
      object = (Serializable) objectStream.readObject();
    } catch (IOException e) {
      // We don't expect to fail to deserialize monitoring data.
      Assert.fail();
    } catch (ClassNotFoundException e) {
      // We don't expect to fail to deserialize monitoring data.
      Assert.fail();
    }
    return object;
  }
}
