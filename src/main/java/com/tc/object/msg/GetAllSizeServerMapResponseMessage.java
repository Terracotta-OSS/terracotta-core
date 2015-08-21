/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.GroupID;
import com.tc.object.ServerMapRequestID;

public interface GetAllSizeServerMapResponseMessage extends ServerMapResponseMessage {

  public ServerMapRequestID getRequestID();

  public Long getSize();

  public GroupID getGroupID();

  public void initializeGetAllSizeResponse(GroupID groupID, ServerMapRequestID requestID, Long size);

}
