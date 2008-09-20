/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.net.groups.GroupID;

public interface ClientGroupMessageChannel extends ClientMessageChannel {

  public ClientMessageChannel getChannel(GroupID groupID);

  public GroupID[] getGroupIDs();

  public TCMessage createMessage(GroupID groupID, TCMessageType type);

  public void broadcast(final TCMessageImpl message);

}
