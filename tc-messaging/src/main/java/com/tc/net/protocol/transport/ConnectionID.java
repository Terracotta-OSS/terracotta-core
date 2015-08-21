/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.io.TCDataInput;
import com.tc.io.TCDataOutput;
import com.tc.license.ProductID;
import com.tc.net.protocol.tcm.ChannelID;

import java.io.IOException;

public class ConnectionID {

  private static final char        DOT_PLACEHOLDER = '+';
  private static final char        DOT             = '.';

  private final long               channelID;
  private final String             serverID;
  private final String             jvmID;
  private final Exception          initEx;
  private final String             username;
  private final ProductID          productId;

  private volatile char[]          password;

  private static final String      NULL_SERVER_ID = "ffffffffffffffffffffffffffffffff";
  public static final String       NULL_JVM_ID    = "ffffffffffffffffffffffffffffffffffffffffffffffff";
  private static final ProductID   DEFAULT_PRODUCT_ID = ProductID.USER;
  public static final ConnectionID NULL_ID        = new ConnectionID(NULL_JVM_ID, ChannelID.NULL_ID.toLong(),
                                                                     NULL_SERVER_ID);

  private static final char        SEP            = '.';

  public ConnectionID(String jvmID, long channelID, String serverID) {
    this(jvmID, channelID, serverID, null, null, null);
  }

  public ConnectionID(String jvmID, long channelID, String serverID, String username, String password) {
    this(jvmID, channelID, serverID, username, password == null ? null : password.toCharArray(), null);
  }

  public ConnectionID(String jvmID, long channelID, String serverID, String username, char[] password, ProductID productId) {
    this.jvmID = jvmID;
    this.channelID = channelID;
    this.serverID = serverID;

    if (jvmID.equals(NULL_JVM_ID)) {
      initEx = new Exception("Created (" + getID() + ") by:-----------------------------------------------------------");
    } else {
      initEx = null;
    }
    this.username = username;
    this.password = password;

    if (productId == null) {
      this.productId = DEFAULT_PRODUCT_ID;
    } else {
      this.productId = productId;
    }
  }

  public void authenticated() {
    this.password = null;
  }

  public void setPassword(char[] password) {
    this.password = password;
  }

  public ConnectionID(String jvmID, long channelID, String username, char[] password, ProductID productId) {
    this(jvmID, channelID, NULL_SERVER_ID, username, password, productId);
  }

  public ConnectionID(String jvmID, long channelID) {
    this(jvmID, channelID, NULL_SERVER_ID);
  }

  @Override
  public String toString() {
    return "ConnectionID" + (isSecured() ? ".secured(" : "(") + getID() + ")[" + "]";
  }

  public boolean isNull() {
    return NULL_ID.equals(this);
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
    return getID(false);
  }

  public String getID(boolean withCredentials) {
    StringBuilder sb = new StringBuilder(withCredentials ? 128 : 64);
    sb.append(this.channelID).append(SEP).append(this.serverID).append(SEP).append(this.jvmID).append(SEP).append(productId);
    if (withCredentials) {
      sb.append(SEP);
      if(username != null) {
        sb.append(username.replace(DOT, DOT_PLACEHOLDER));
      }
      sb.append(SEP);
      if(password != null) {
        sb.append(password);
      }
    }
    return sb.toString();
  }

  public void writeTo(TCDataOutput out) {
    out.writeLong(channelID);
    out.writeString(serverID);
    out.writeString(jvmID);
    out.writeString(productId.name());
    out.writeBoolean(username != null);
    if (username != null) {
      out.writeString(username);
    }
    out.writeBoolean(password != null);
    if (password != null) {
      out.writeString(String.valueOf(password));
    }
  }

  public static ConnectionID readFrom(TCDataInput in) throws IOException {
    long channelID = in.readLong();
    String serverID = in.readString();
    String jvmID = in.readString();
    ProductID productId = ProductID.valueOf(in.readString());
    String username = null;
    char[] password = null;
    if (in.readBoolean()) {
      username = in.readString();
    }
    if (in.readBoolean()) {
      password = in.readString().toCharArray();
    }
    return new ConnectionID(jvmID, channelID, serverID, username, password, productId);
  }

  public String getUsername() {
    return username;
  }

  public char[] getPassword() {
    return password;
  }

  public boolean isSecured() {
    return username != null;
  }

  public ProductID getProductId() {
    return productId;
  }
}
