/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.net.GroupID;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class RootsHolder {
  private final Map<GroupID, Map<String, ObjectID>> groupToRoots                = new HashMap<GroupID, Map<String, ObjectID>>();
  private final Map<GroupID, Set<String>>           groupToRootLookupInProgress = new HashMap<GroupID, Set<String>>();

  private final GroupID[]                           groupIds;

  public RootsHolder(GroupID[] groupIds) {
    this.groupIds = groupIds;
  }

  public synchronized void addRoot(String name, ObjectID oid) {
    GroupID gid = new GroupID(oid.getGroupID());
    Map<String, ObjectID> roots = groupToRoots.get(gid);
    if (roots == null) {
      roots = new HashMap<String, ObjectID>();
      groupToRoots.put(gid, roots);
    }

    roots.put(name, oid);
  }

  public synchronized boolean markRootLookupInProgress(String name, GroupID gid) {
    Set<String> rootLookupInProgress = groupToRootLookupInProgress.get(gid);
    if (rootLookupInProgress == null) {
      rootLookupInProgress = new HashSet<String>();
      groupToRootLookupInProgress.put(gid, rootLookupInProgress);
    }

    return rootLookupInProgress.add(name);
  }

  public synchronized boolean unmarkRootLookupInProgress(String name, GroupID gid) {
    Set<String> rootLookupInProgress = groupToRootLookupInProgress.get(gid);
    if (rootLookupInProgress == null) { return false; }

    boolean isRemoved = rootLookupInProgress.remove(name);
    if (rootLookupInProgress.size() == 0) {
      groupToRootLookupInProgress.remove(gid);
    }
    return isRemoved;
  }

  public synchronized boolean isLookupInProgress(String name, GroupID gid) {
    Set<String> rootLookupInProgress = groupToRootLookupInProgress.get(gid);
    if (rootLookupInProgress == null) { return false; }

    return rootLookupInProgress.contains(name);
  }

  public synchronized ObjectID getRootIDForName(String name, GroupID gid) {
    Map<String, ObjectID> roots = groupToRoots.get(gid);
    if (roots == null) { return null; }

    return roots.get(name);
  }

  public synchronized GroupID getGroupIDForRoot(String name) {
    int hashCode = name.hashCode();
    int index = Math.abs(hashCode % groupIds.length);
    return groupIds[index];
  }

  public synchronized int size() {
    int size = 0;
    for (Map.Entry<GroupID, Map<String, ObjectID>> entry : groupToRoots.entrySet()) {
      size += entry.getValue().size();
    }

    return size;
  }

}