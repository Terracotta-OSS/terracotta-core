/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.object.ObjectID;
import com.tc.object.ServerMapRequestID;

public interface GetAllSizeServerMapRequestMessage extends ServerMapRequestMessage {

  public void initializeGetAllSizeRequest(ServerMapRequestID requestID, ObjectID[] maps);

  public ServerMapRequestID getRequestID();

  public ObjectID[] getMaps();

}
