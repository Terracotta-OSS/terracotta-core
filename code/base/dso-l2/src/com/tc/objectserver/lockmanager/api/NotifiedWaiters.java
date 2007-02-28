/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.LockContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NotifiedWaiters {

  private final Map notifiedSets = new HashMap();

  public String toString() {
    synchronized (notifiedSets) {
      return "NotifiedWaiters[" + notifiedSets + "]";
    }
  }

  public boolean isEmpty() {
    return notifiedSets.isEmpty();
  }

  public void addNotification(LockContext context) {
    synchronized (notifiedSets) {
      getOrCreateSetFor(context.getChannelID()).add(context);
    }
  }

  public Set getNotifiedFor(ChannelID channelID) {
    synchronized (notifiedSets) {
      Set rv = getSetFor(channelID);
      return (rv == null) ? Collections.EMPTY_SET : rv;
    }
  }

  private Set getSetFor(ChannelID channelID) {
    return (Set) notifiedSets.get(channelID);
  }

  private Set getOrCreateSetFor(ChannelID channelID) {
    Set rv = getSetFor(channelID);
    if (rv == null) {
      rv = new HashSet();
      notifiedSets.put(channelID, rv);
    }
    return rv;
  }


}
