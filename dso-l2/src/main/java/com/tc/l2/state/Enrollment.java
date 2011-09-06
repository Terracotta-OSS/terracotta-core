/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.NodeIDSerializer;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.Arrays;

public class Enrollment implements TCSerializable {

  private static final TCLogger logger = TCLogging.getLogger(Enrollment.class);
  private NodeID                nodeID;
  private long[]                weights;
  private boolean               isNew;

  public Enrollment() {
    // To make serialization happy
  }

  public Enrollment(NodeID nodeID, boolean isNew, long[] weights) {
    this.nodeID = nodeID;
    this.isNew = isNew;
    Assert.assertNotNull(weights);
    this.weights = weights;
  }

  public void serializeTo(TCByteBufferOutput out) {
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer(this.nodeID);
    nodeIDSerializer.serializeTo(out);
    out.writeBoolean(this.isNew);
    out.writeInt(weights.length);
    for (long weight : weights) {
      out.writeLong(weight);
    }
  }

  public Object deserializeFrom(TCByteBufferInput in) throws IOException {
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer();
    nodeIDSerializer = (NodeIDSerializer) nodeIDSerializer.deserializeFrom(in);
    this.nodeID = nodeIDSerializer.getNodeID();
    this.isNew = in.readBoolean();
    this.weights = new long[in.readInt()];
    for (int i = 0; i < weights.length; i++) {
      weights[i] = in.readLong();
    }
    return this;
  }

  public NodeID getNodeID() {
    return nodeID;
  }

  public boolean isANewCandidate() {
    return isNew;
  }

  public boolean wins(Enrollment other) {
    if (isNew != other.isNew) {
      // an old candidate always wins over a new candidate
      return !isNew;
    }
    int myLength = weights.length;
    int otherLength = other.weights.length;
    if (myLength > otherLength) {
      return true;
    } else if (myLength < otherLength) {
      return false;
    } else {
      for (int i = 0; i < myLength; i++) {
        if (weights[i] > other.weights[i]) {
          // wins
          return true;
        } else if (weights[i] < other.weights[i]) {
          // loses
          return false;
        }
      }

      // XXX:: Both are the same weight. This shouldn't happen once we fix the weights to
      // be unique (based on hardware,ip,process id etc.) But now it is possible and we
      // handle it. If two nodes dont agree because of this there will be a re-election
      logger.warn("Two Enrollments with same weights : " + this + " == " + other);
      return false;
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (isNew ? 1231 : 1237);
    result = prime * result + ((nodeID == null) ? 0 : nodeID.hashCode());
    result = prime * result + Arrays.hashCode(weights);
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Enrollment) {
      Enrollment oe = (Enrollment) o;
      return nodeID.equals(oe.nodeID) && Arrays.equals(weights, oe.weights) && isNew == oe.isNew;
    }
    return false;
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer("Enrollment [ ");
    sb.append(nodeID).append(", isNew = ").append(isNew);
    sb.append(", weights = ");
    int length = weights.length;
    for (int i = 0; i < length; i++) {
      sb.append(weights[i]);
      if (i < length - 1) {
        sb.append(",");
      }
    }
    sb.append(" ]");
    return sb.toString();
  }

}
