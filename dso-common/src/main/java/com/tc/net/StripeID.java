/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.Serializable;

/*
 * Unique among a mirror-group, and persist across group restart. Each group knows StripeID of other groups to maintain
 * cluster consistency. Clients also matches StripeID from coordinator vs the one from connected group.
 */
public class StripeID implements NodeID, Serializable {

  public static final StripeID NULL_ID       = new StripeID("NULL-ID");

  private static final String  UNINITIALIZED = "Uninitialized";

  private String               name;

  public StripeID() {
    // satisfy serialization
    this.name = UNINITIALIZED;
  }

  public StripeID(String name) {
    this.name = name;
  }

  public int hashCode() {
    return name.hashCode();
  }

  public boolean equals(Object o) {
    if (o instanceof StripeID) {
      StripeID that = (StripeID) o;
      return name.equals(that.name);
    }
    return false;
  }

  public String getName() {
    Assert.assertTrue(this.name != UNINITIALIZED);
    return name;
  }

  public String toString() {
    return "StripeID[" + getName() + "]";
  }

  public boolean isNull() {
    return NULL_ID.equals(this);
  }

  public Object deserializeFrom(TCByteBufferInput in) throws IOException {
    this.name = in.readString();
    return this;
  }

  public void serializeTo(TCByteBufferOutput out) {
    Assert.assertTrue(this.name != UNINITIALIZED);
    out.writeString(this.name);
  }

  public byte getNodeType() {
    return STRIPE_NODE_TYPE;
  }

  public int compareTo(Object o) {
    NodeID n = (NodeID) o;
    if (getNodeType() != n.getNodeType()) { return getNodeType() - n.getNodeType(); }
    StripeID target = (StripeID) o;
    return name.compareTo(target.name);
  }

}
