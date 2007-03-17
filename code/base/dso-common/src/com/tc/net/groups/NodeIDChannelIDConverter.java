/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

import com.tc.net.protocol.tcm.ChannelID;

import java.util.Map;

/**
 * HACK::FIXME::TODO This method is a quick hack to brick NodeIDs to ChannelIDs. This mapping is only valid for the
 * current VM. The ChannelIDs are given out in the range -100 to Integer.MIN_VALUE to not clash with the regular client
 * channelID. This definitely needs some cleanup
 */
public class NodeIDChannelIDConverter {

  private static final Map map           = new ConcurrentReaderHashMap();

  private static int       nextChannelID = -100;

  public static ChannelID getChannelIDFor(NodeID nodeID) {
    ChannelID cid = (ChannelID) map.get(nodeID);
    if (cid != null) return cid;
    synchronized (map) {
      if (!map.containsKey(nodeID)) {
        map.put(nodeID, (cid = new ChannelID(nextChannelID--)));
      }
    }
    return cid;
  }

}
