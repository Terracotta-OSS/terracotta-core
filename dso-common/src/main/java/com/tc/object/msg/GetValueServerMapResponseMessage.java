/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.object.ObjectID;
import com.tc.object.ServerMapGetValueResponse;

import java.util.Collection;

public interface GetValueServerMapResponseMessage extends ServerMapResponseMessage {

  public void initializeGetValueResponse(ObjectID mapID, Collection<ServerMapGetValueResponse> responses);

  public Collection<ServerMapGetValueResponse> getGetValueResponses();

}
