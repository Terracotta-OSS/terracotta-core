/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.Sink;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapGetValueRequest;
import com.tc.object.ServerMapRequestType;

import java.util.Collection;

public class ServerMapRequestValueContext extends ServerMapRequestContext {

  private final Collection<ServerMapGetValueRequest> getValueRequests;

  public ServerMapRequestValueContext(final ClientID clientID, final ObjectID mapID,
                                      final Collection<ServerMapGetValueRequest> getValueRequests,
                                      final Sink destinationSink) {
    super(clientID, mapID, destinationSink);
    this.getValueRequests = getValueRequests;
  }

  public Collection<ServerMapGetValueRequest> getValueRequests() {
    return this.getValueRequests;
  }

  @Override
  public String toString() {
    return super.toString() + " [ value requests : " + this.getValueRequests + "]";
  }

  @Override
  public ServerMapRequestType getRequestType() {
    return ServerMapRequestType.GET_VALUE_FOR_KEY;
  }

}
