/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapGetValueRequest;
import com.tc.object.ServerMapRequestID;
import com.tc.object.ServerMapRequestType;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;

import java.util.Collection;
import java.util.Map;

public final class ServerMapRequestContext implements ObjectManagerResultsContext {

  private final static TCLogger              logger    = TCLogging.getLogger(ServerMapRequestContext.class);

  private final ServerMapRequestType         requestType;
  private final ObjectIDSet                  lookupIDs = new ObjectIDSet();
  private final ObjectID                     mapID;
  private final ClientID                     clientID;
  private final Sink                         destinationSink;

  final ServerMapRequestID                   getSizeRequestID;
  final Collection<ServerMapGetValueRequest> getValueRequests;

  public ServerMapRequestContext(final ClientID clientID, final ObjectID mapID,
                                 final Collection<ServerMapGetValueRequest> getValueRequests, final Sink destinationSink) {
    this(ServerMapRequestType.GET_VALUE_FOR_KEY, null, clientID, mapID, destinationSink, getValueRequests);
  }

  public ServerMapRequestContext(final ServerMapRequestType type, final ServerMapRequestID requestID, final ClientID clientID, final ObjectID mapID,
                                 final Sink destinationSink) {
    this(type, requestID, clientID, mapID, destinationSink, null);
  }

  private ServerMapRequestContext(final ServerMapRequestType requestType, final ServerMapRequestID requestID,
                                  final ClientID clientID, final ObjectID mapID, final Sink destinationSink,
                                  final Collection<ServerMapGetValueRequest> getValueRequests) {
    this.requestType = requestType;
    this.getSizeRequestID = requestID;
    this.clientID = clientID;
    this.mapID = mapID;
    this.destinationSink = destinationSink;
    this.getValueRequests = getValueRequests;
    this.lookupIDs.add(mapID);
  }

  public ServerMapRequestType getRequestType() {
    return this.requestType;
  }

  public ServerMapRequestID getRequestID() {
    if (this.requestType == ServerMapRequestType.GET_VALUE_FOR_KEY) { throw new AssertionError(
                                                                                      " Request type is not GET VALUE FOR KEY shouldn't exist : "
                                                                                          + this); }
    return this.getSizeRequestID;
  }

  public ClientID getClientID() {
    return this.clientID;
  }

  public ObjectID getServerTCMapID() {
    return this.mapID;
  }

  public Collection<ServerMapGetValueRequest> getValueRequests() {
    return this.getValueRequests;
  }

  @Override
  public String toString() {
    return "RequestEntryForKeyContext [  mapID = " + this.mapID + " clientID : " + this.clientID + " requestType : "
           + this.requestType + "  size requestID : " + this.getSizeRequestID + " value requests : "
           + this.getValueRequests + "]";
  }

  public ObjectIDSet getLookupIDs() {
    return this.lookupIDs;
  }

  public ObjectIDSet getNewObjectIDs() {
    return TCCollections.EMPTY_OBJECT_ID_SET;
  }

  public void setResults(final ObjectManagerLookupResults results) {
    final ObjectIDSet missingObjects = results.getMissingObjectIDs();
    
    if (!missingObjects.isEmpty()) {
      logger.error("Missing ObjectIDs : " + missingObjects + " Request Context : " + this);
      final ServerMapMissingObjectResponseContext responseContext = new ServerMapMissingObjectResponseContext(this.mapID);
      this.destinationSink.add(responseContext);
      return;
    }
    
    final Map<ObjectID, ManagedObject> objects = results.getObjects();
    
    if (objects.size() != 1) { throw new AssertionError("Asked for 1, got more or less"); }

    final ManagedObject mo = objects.get(this.mapID);

    if (mo == null) { throw new AssertionError("ServerMap (mapID " + this.mapID + ") is null "); }

    final EntryForKeyResponseContext responseContext = new EntryForKeyResponseContext(mo, this.mapID);
    this.destinationSink.add(responseContext);
  }

  public boolean updateStats() {
    return true;
  }
}