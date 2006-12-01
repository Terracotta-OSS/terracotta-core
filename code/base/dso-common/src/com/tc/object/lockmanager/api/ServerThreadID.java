/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.api;

import com.tc.net.protocol.tcm.ChannelID;

public class ServerThreadID {
  public static final ServerThreadID NULL_ID = new ServerThreadID(ChannelID.NULL_ID, ThreadID.NULL_ID);

  private final ChannelID            channelID;
  private final ThreadID             threadID;
  private final int                  hashCode;

  public ServerThreadID(ChannelID channelID, ThreadID threadID) {
    this.channelID = channelID;
    this.threadID = threadID;

    int hash = 31;
    hash = (37 * hash) + channelID.hashCode();
    hash = (37 * hash) + threadID.hashCode();
    this.hashCode = hash;
  }

  public ChannelID getChannelID() {
    return channelID;
  }

  public ThreadID getClientThreadID() {
    return threadID;
  }

  public String toString() {
    return new StringBuffer().append("ServerThreadID{").append(channelID).append(',').append(threadID).append('}')
        .toString();
  }

  public int hashCode() {
    return this.hashCode;
  }

  public boolean equals(Object obj) {
    if (obj instanceof ServerThreadID) {
      ServerThreadID other = (ServerThreadID) obj;
      return this.channelID.equals(other.channelID) && this.threadID.equals(other.threadID);
    }
    return false;
  }
}
