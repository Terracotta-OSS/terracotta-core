/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapRequestType;

public interface ServerMapResponseMessage extends TCMessage {

  public ObjectID getMapID();

  public ServerMapRequestType getRequestType();

  public void send();

}
