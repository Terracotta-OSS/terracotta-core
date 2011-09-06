/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.net.GroupID;
import com.tc.object.ObjectID;

/**
 * Helper to aggregate size of all stripes.
 * <p>
 * Not thread safe. Must hold an external lock to manipulate this object.
 */
public class ServerMapGetAllSizeHelper {
  private final GroupID groupID;
  private boolean       done      = false;
  private long          totalSize = 0;
  private int           waitFor;

  public ServerMapGetAllSizeHelper(final ObjectID[] mapIDs) {
    this.groupID = new GroupID(mapIDs[0].getGroupID());
    this.waitFor = mapIDs.length;
  }

  public void addSize(final ObjectID mapID, final int size) {
    this.totalSize += size;
    if (--waitFor == 0) {
      this.done = true;
    }
  }

  public boolean isDone() {
    return this.done;
  }

  public long getTotalSize() {
    return this.totalSize;
  }

  public GroupID getGroupID() {
    return this.groupID;
  }
}
