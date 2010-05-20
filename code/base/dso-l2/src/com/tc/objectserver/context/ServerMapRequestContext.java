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

  public ServerMapRequestContext(final ServerMapRequestID requestID, final ClientID clientID, final ObjectID mapID,
                                 final Sink destinationSink) {
    this(ServerMapRequestType.GET_SIZE, requestID, clientID, mapID, destinationSink, null);
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

  public ServerMapRequestID getSizeRequestID() {
    if (this.requestType != ServerMapRequestType.GET_SIZE) { throw new AssertionError(
                                                                                      " Request type is not GET SIZE : "
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
    final Map<ObjectID, ManagedObject> objects = results.getObjects();
    final ObjectIDSet missingObjects = results.getMissingObjectIDs();

    if (!missingObjects.isEmpty()) {
      logger.error("Ignoring Missing ObjectIDs : " + missingObjects + " Request Context : " + this);
      // TODO:: Fix this
      return;
    }
    if (objects.size() != 1) { throw new AssertionError("Asked for 1, got more or less"); }

    final ManagedObject mo = objects.get(this.mapID);

    if (mo == null) { throw new AssertionError("ServerTCMap (mapID " + this.mapID + ") is null "); }

    final EntryForKeyResponseContext responseContext = new EntryForKeyResponseContext(mo, this.mapID);
    this.destinationSink.add(responseContext);
  }

  public boolean updateStats() {
    return true;
  }

  @Override
  public boolean equals(Object other) {

    boolean equals = false;
    if (other instanceof ServerMapRequestContext) {
      ServerMapRequestContext context = (ServerMapRequestContext) other;

      equals = requestType.equals(context.getRequestType()) && lookupIDs.equals(context.getLookupIDs())
               && mapID.equals(context.getServerTCMapID()) && clientID.equals(context.getClientID());

      if (getSizeRequestID != null) {
        equals = equals && getSizeRequestID.equals(context.getSizeRequestID);
      }

      if (getValueRequests != null) {
        equals = equals && getValueRequests.equals(context.getValueRequests);
      }
    }
    return equals;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;
    
    hashCode = (31 * requestType.hashCode()) + (31 * lookupIDs.hashCode()) +
    (31 * mapID.hashCode()) + (31 * clientID.hashCode());
    
    if (getSizeRequestID != null) {
      hashCode = hashCode + (31 * getSizeRequestID.hashCode());
    }

    if (getValueRequests != null) {
      hashCode = hashCode + (31 * getValueRequests.hashCode());
    }
    
    return hashCode;
  }
  
  

}