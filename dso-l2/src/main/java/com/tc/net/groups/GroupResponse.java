/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.NodeID;
import com.tc.net.groups.GroupMessage;

import java.util.List;

public interface GroupResponse<M extends GroupMessage> {

  /**
   * @return a list of all responses received
   */
  public List<M> getResponses();

  /**
   * @return returns a response from nodeID. If the node corresponding to the nodeID did not send any response (due to
   *         disconnect or otherwise) then returns null.
   */
  public M getResponse(NodeID nodeID);

}
