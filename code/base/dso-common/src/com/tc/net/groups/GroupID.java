/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.core.ConnectionAddressIterator;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;

public class GroupID extends NodeIDImpl {
  private static final String UNINITIALIZED = "Uninitialized";

  private String              name;
  private ArrayList           nodes         = new ArrayList();

  public GroupID() {
    // To make serialization happy
    name = UNINITIALIZED;
  }

  // satisfy serialization
  public GroupID(String name, ConnectionAddressProvider provider) {
    this.name = name;
    if (provider != null) {
      ConnectionAddressIterator it = provider.getIterator();
      while (it.hasNext()) {
        ConnectionInfo addr = it.next();
        NodeIDImpl node = new NodeIDImpl(name, addr.toString().getBytes());
        nodes.add(node);
      }
    }
  }

  public boolean isNull() {
    return (nodes.size() == 0);
  }

  public boolean equals(Object obj) {
    if (obj instanceof GroupID) {
      GroupID other = (GroupID) obj;
      return (this.getNodes().equals(other.getNodes()));
    }
    return false;
  }

  public ArrayList getNodes() {
    return this.nodes;
  }

  public int hashCode() {
    int hc = 0;
    for (int i = 0; i < nodes.size(); ++i)
      hc += ((NodeIDImpl) nodes.get(i)).hashCode();
    return hc;
  }

  public String toString() {
    return "GroupID[" + name + "]" + nodes;
  }

  /**
   * FIXME::Two difference serialization mechanisms are implemented since these classes are used with two different
   * implementation of comms stack.
   */

  public void readExternal(ObjectInput in) throws IOException {
    this.name = in.readUTF();
    int nodeSize = in.readInt();
    if (nodeSize > 0) {
      nodes = new ArrayList(nodeSize);
      for (int i = 0; i < nodeSize; ++i) {
        try {
          nodes.add(in.readObject());
        } catch (ClassNotFoundException e) {
          throw new IOException("ClassNotFound" + e.toString());
        }
      }
    }
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    Assert.assertTrue(this.name != UNINITIALIZED);
    out.writeUTF(this.name);
    if (nodes == null) {
      out.writeInt(0);
    } else {
      for (int i = 0; i < nodes.size(); ++i) {
        NodeIDImpl node = (NodeIDImpl) nodes.get(i);
        out.writeObject(node);
      }
    }
  }

  /**
   * FIXME::Two difference serialization mechanisms are implemented since these classes are used with two different
   * implementation of comms stack.
   */
  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    this.name = serialInput.readString();
    int nodeSize = serialInput.readInt();
    if (nodeSize > 0) {
      nodes = new ArrayList(nodeSize);
      for (int i = 0; i < nodeSize; ++i) {
        NodeIDImpl node = new NodeIDImpl();
        node.deserializeFrom(serialInput);
        nodes.add(node);
      }
    }
    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    Assert.assertTrue(this.name != UNINITIALIZED);
    serialOutput.writeString(this.name);
    if (nodes == null) {
      serialOutput.writeInt(0);
    } else {
      for (int i = 0; i < nodes.size(); ++i) {
        NodeIDImpl node = (NodeIDImpl) nodes.get(i);
        node.serializeTo(serialOutput);
      }
    }
  }

  public byte getType() {
    return L2_NODE_TYPE;
  }

}
