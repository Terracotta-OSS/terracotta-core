/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.ServerID;
import com.tc.net.protocol.tcm.MessageChannel;

public interface TCGroupMember {

  public ServerID getLocalNodeID();

  public ServerID getPeerNodeID();

  public MessageChannel getChannel();

  public void send(GroupMessage msg) throws GroupException;
  
  public void sendIgnoreNotReady(GroupMessage msg);

  public void setTCGroupManager(TCGroupManagerImpl manager);

  public TCGroupManagerImpl getTCGroupManager();

  public boolean isReady();

  public void setReady(boolean isReady);

  public boolean isJoinedEventFired();

  public void setJoinedEventFired(boolean isReady);

  public void close();

  public boolean isHighPriorityNode();

  public void memberAddingInProcess();

  public void abortMemberAdding();

  public void notifyMemberAdded();
}