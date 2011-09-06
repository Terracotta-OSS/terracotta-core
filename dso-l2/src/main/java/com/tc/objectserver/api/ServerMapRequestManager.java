/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapGetValueRequest;
import com.tc.object.ServerMapRequestID;
import com.tc.objectserver.context.ServerMapGetAllSizeHelper;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.text.PrettyPrintable;

import java.util.Collection;

public interface ServerMapRequestManager extends PrettyPrintable {

  public void requestSize(ServerMapRequestID requestID, ClientID clientID, ObjectID mapID,
                          ServerMapGetAllSizeHelper helper);

  public void requestAllKeys(ServerMapRequestID requestID, ClientID clientID, ObjectID mapID);

  public void sendResponseFor(ObjectID mapID, ManagedObject managedObject);

  public void sendMissingObjectResponseFor(ObjectID mapID);

  public void requestValues(ClientID clientID, ObjectID mapID, Collection<ServerMapGetValueRequest> requests);

}
