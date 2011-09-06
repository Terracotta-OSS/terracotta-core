/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.NodeID;

import java.util.List;

public interface GroupResponse {

  /**
   * @return a list of all responses received
   */
  public List<? extends GroupMessage> getResponses();

  /**
   * @return returns a response from nodeID. If the node corresponding to the nodeID did not send any response (due to
   *         disconnect or otherwise) then returns null.
   */
  public GroupMessage getResponse(NodeID nodeID);

}
