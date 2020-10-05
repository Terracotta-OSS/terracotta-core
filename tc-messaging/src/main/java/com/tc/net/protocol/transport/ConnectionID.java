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
package com.tc.net.protocol.transport;

import com.tc.io.TCDataInput;
import com.tc.io.TCDataOutput;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.core.ProductID;

import java.io.IOException;

public class ConnectionID {

  private static final char        DOT_PLACEHOLDER = '+';
  private static final char        DOT             = '.';

  private final long               channelID;
  private final String             serverID;
  private final String             jvmID;
  private final Exception          initEx;
  private final ProductID          productId;

  private static final String      NULL_SERVER_ID = "ffffffffffffffffffffffffffffffff";
  public static final String       NULL_JVM_ID    = "ffffffffffffffffffffffffffffffffffffffffffffffff";
  private static final ProductID   DEFAULT_PRODUCT_ID = ProductID.PERMANENT;
  public static final ConnectionID NULL_ID        = new ConnectionID(NULL_JVM_ID, ChannelID.NULL_ID.toLong(),
                                                                     NULL_SERVER_ID);

  private static final char        SEP            = '.';

  public ConnectionID(String jvmID, long channelID, String serverID) {
    this(jvmID, channelID, serverID, null);
  }

  public ConnectionID(String jvmID, long channelID, String serverID, ProductID productId) {
    this.jvmID = jvmID;
    this.channelID = channelID;
    this.serverID = serverID;

    if (jvmID.equals(NULL_JVM_ID)) {
      initEx = new Exception("Created (" + getID() + ") by:-----------------------------------------------------------");
    } else {
      initEx = null;
    }

    if (productId == null) {
      this.productId = DEFAULT_PRODUCT_ID;
    } else {
      this.productId = productId;
    }
  }

  public ConnectionID(String jvmID, long channelID, ProductID productId) {
    this(jvmID, channelID, NULL_SERVER_ID, productId);
  }

  public ConnectionID(String jvmID, long channelID) {
    this(jvmID, channelID, NULL_SERVER_ID);
  }

  @Override
  public String toString() {
    return "ConnectionID(" + getID() + ")";
  }

  public boolean isNull() {
    return NULL_ID.equals(this);
  }
  
  public boolean isValid() {
    return channelID >= 0;
  }

  public boolean isNewConnection() {
    return (this.serverID.equals(NULL_SERVER_ID));
  }

  public String getServerID() {
    return this.serverID;
  }

  @Override
  public int hashCode() {
    int hc = 17;
    hc = 37 * hc + (int) (this.channelID ^ (this.channelID >>> 32));
    if (this.serverID != null) {
      hc = 37 * hc + this.serverID.hashCode();
    }

    return hc;
  }

  @Override
  public boolean equals(Object obj) {
    // equals does NOT take into account jvmID on purpose.
    // there are cases where we do not have a value (cases where a ConnectionID
    // can only be built with a channelID ... true identification is based
    // on channelId
    if (obj instanceof ConnectionID) {
      ConnectionID other = (ConnectionID) obj;
      return (this.channelID == other.channelID) && (this.serverID.equals(other.serverID));
    }
    return false;
  }

  public long getChannelID() {
    return this.channelID;
  }

  public String getJvmID() {
    if (this.jvmID.equals(NULL_JVM_ID)) { throw new IllegalStateException(
                                                                          "Attempt to get jvmID from pseudo-ConnectionID that was not initialized with one.",
                                                                          initEx); }
    return this.jvmID;
  }

  public boolean isJvmIDNull() {
    return this.jvmID.equals(NULL_JVM_ID);
  }

  public String getID() {
    StringBuilder sb = new StringBuilder(64);
    sb.append(this.channelID).append(SEP).append(this.serverID).append(SEP).append(this.jvmID).append(SEP).append(productId);
    return sb.toString();
  }

  public void writeTo(TCDataOutput out) {
    out.writeLong(channelID);
    out.writeString(serverID);
    out.writeString(jvmID);
    out.writeString(productId.name());
    out.writeBoolean(false);  // legacy username
    out.writeBoolean(false);  //  legacy password
  }

  public static ConnectionID readFrom(TCDataInput in) throws IOException {
    long channelID = in.readLong();
    String serverID = in.readString();
    String jvmID = in.readString();
    ProductID productId = ProductID.valueOf(in.readString());
    String username = null;
    char[] password = null;
    if (in.readBoolean()) {
      throw new AssertionError();
    }
    if (in.readBoolean()) {
      throw new AssertionError();
    }
    return new ConnectionID(jvmID, channelID, serverID, productId);
  }

  public ProductID getProductId() {
    return productId;
  }
  
  public ClientID getClientID() {
    return new ClientID(channelID);
  }
  
  public ConnectionID changeProductId(ProductID product) {
    return new ConnectionID(jvmID, channelID, serverID, product);
  }
}
