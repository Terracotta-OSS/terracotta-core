/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.Sink;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapRequestID;
import com.tc.object.ServerMapRequestType;

public class ServerMapRequestAllKeysContext extends ServerMapRequestContext {

  private final ServerMapRequestID requestID;

  public ServerMapRequestAllKeysContext(final ServerMapRequestID requestID, final ClientID clientID,
                                        final ObjectID mapID, final Sink destinationSink) {
    super(clientID, mapID, destinationSink);
    this.requestID = requestID;
  }

  @Override
  public ServerMapRequestID getRequestID() {
    return this.requestID;
  }

  @Override
  public ServerMapRequestType getRequestType() {
    return ServerMapRequestType.GET_ALL_KEYS;
  }

}
