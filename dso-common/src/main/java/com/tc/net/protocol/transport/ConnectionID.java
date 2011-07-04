/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.protocol.tcm.ChannelID;

public class ConnectionID {

  private final long               channelID;
  private final String             serverID;

  private static final String      NULL_SERVER_ID = "ffffffffffffffffffffffffffffffff";
  public static final ConnectionID NULL_ID        = new ConnectionID(ChannelID.NULL_ID.toLong(), NULL_SERVER_ID);

  private static final char        SEP            = '.';

  public static ConnectionID parse(String compositeID) throws InvalidConnectionIDException {
    if (compositeID == null) { throw new InvalidConnectionIDException("NULL ConnectionID"); }

    int idx = compositeID.indexOf(SEP);
    if (idx <= 0 || idx >= compositeID.length() - 1) {
      // make formatter sane
      throw new InvalidConnectionIDException(compositeID, "Invalid format. Separator (.) found at : " + idx);
    }

    String channelID = compositeID.substring(0, idx);
    final long channel;
    try {
      channel = Long.parseLong(channelID);
    } catch (Exception e) {
      throw new InvalidConnectionIDException(compositeID, "parse exception for channelID " + channelID, e);
    }

    String server = compositeID.substring(idx + 1);
    if (server.length() != 32) { throw new InvalidConnectionIDException(compositeID, "invalid serverID length: "
                                                                                     + server.length()); }

    if (!validateCharsInServerID(server)) { throw new InvalidConnectionIDException(compositeID,
                                                                                   "invalid chars in serverID: "
                                                                                       + server); }

    return new ConnectionID(channel, server);
  }

  /**
   * This method does not use String.matches() for performance reason.
   */
  private static boolean validateCharsInServerID(String server) {
    for (int i = 0; i < server.length(); i++) {
      char c = server.charAt(i);
      if (!(((c >= '0') && (c <= '9')) || ((c >= 'a') && (c <= 'f')) || ((c >= 'A') && (c <= 'F')))) { return false; }
    }
    return true;
  }

  public ConnectionID(long channelID, String serverID) {
    this.channelID = channelID;
    this.serverID = serverID;
  }

  public ConnectionID(long channelID) {
    this(channelID, NULL_SERVER_ID);
  }

  @Override
  public String toString() {
    return "ConnectionID(" + getID() + ")";
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
    if (obj instanceof ConnectionID) {
      ConnectionID other = (ConnectionID) obj;
      return (this.channelID == other.channelID) && (this.serverID.equals(other.serverID));
    }
    return false;
  }

  public long getChannelID() {
    return this.channelID;
  }

  public String getID() {
    StringBuilder sb = new StringBuilder(64);
    sb.append(this.channelID).append(SEP).append(this.serverID);
    return sb.toString();
  }

}
