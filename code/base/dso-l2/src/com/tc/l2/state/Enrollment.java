/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.NodeID;
import com.tc.util.Assert;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class Enrollment implements Externalizable {

  private static final TCLogger logger = TCLogging.getLogger(Enrollment.class);
  private NodeID                nodeID;
  private int[]                 weights;
  private boolean               isNew;

  public Enrollment() {
    // To make serialization happy
  }

  public Enrollment(NodeID nodeID, boolean isNew, int[] weights) {
    this.nodeID = nodeID;
    this.isNew = isNew;
    Assert.assertNotNull(weights);
    this.weights = weights;
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

      // XXX:: Both are the same weight. This should happen once we fix the weights to
      // be unique (based on hardware,ip,process id etc.) But now it is possible and we
      // handle it. If two nodes dont agree because of this there will be a re-election
      logger.warn("Two Enrollments with same weights : " + this + " == " + other);
      return false;
    }
  }

  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    this.nodeID = (NodeID) in.readObject();
    this.isNew = in.readBoolean();
    this.weights = new int[in.readInt()];
    for (int i = 0; i < weights.length; i++) {
      weights[i] = in.readInt();
    }
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(this.nodeID);
    out.writeBoolean(this.isNew);
    out.writeInt(weights.length);
    for (int i = 0; i < weights.length; i++) {
      out.writeInt(weights[i]);
    }
  }

  public int hashCode() {
    return nodeID.hashCode();
  }

  public boolean equals(Object o) {
    if (o instanceof Enrollment) {
      Enrollment oe = (Enrollment) o;
      return nodeID.equals(oe.nodeID);
    }
    return false;
  }

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
