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

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
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

  @Override
  public String toString() {
    return "StripeID[" + getName() + "]";
  }

  @Override
  public boolean isNull() {
    return NULL_ID.equals(this);
  }

  @Override
  public StripeID deserializeFrom(TCByteBufferInput in) throws IOException {
    this.name = in.readString();
    return this;
  }

  @Override
  public void serializeTo(TCByteBufferOutput out) {
    Assert.assertTrue(this.name != UNINITIALIZED);
    out.writeString(this.name);
  }

  @Override
  public byte getNodeType() {
    return STRIPE_NODE_TYPE;
  }

  @Override
  public int compareTo(NodeID n) {
    if (getNodeType() != n.getNodeType()) { return getNodeType() - n.getNodeType(); }
    StripeID target = (StripeID) n;
    return name.compareTo(target.name);
  }

}
