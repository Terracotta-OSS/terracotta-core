/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.Sink;
import com.tc.net.groups.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestID;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This is the context needed to make a request to the server for a specific Managed root.
 */
public class ManagedObjectRequestContext implements ObjectManagerResultsContext, PrettyPrintable {
  private final long            timestamp;
  private final ClientID        clientID;
  private final Set             requestedObjectIDs;
  private Map                   objects;
  private final ObjectRequestID requestID;
  private boolean               moreObjects    = false;
  private int                   batchCount     = 0;
  private Set                   lookupPendingObjectIDs;
  private final int             maxRequestDepth;
  private final Sink            sink;
  private final Set             missingObjects = new HashSet();
  private final String          requestingThreadName;

  public ManagedObjectRequestContext(ClientID clientID, ObjectRequestID requestID, Set ids, int maxRequestDepth,
                                     Sink sink, String requestingThreadName) {
    this.maxRequestDepth = maxRequestDepth;
    this.sink = sink;
    this.requestingThreadName = requestingThreadName;
    this.timestamp = System.currentTimeMillis();
    this.clientID = clientID;
    this.requestID = requestID;
    this.requestedObjectIDs = ids;
  }

  public int getMaxRequestDepth() {
    return this.maxRequestDepth;
  }

  public boolean hasMoreObjects() {
    return moreObjects;
  }

  public ClientID getRequestedNodeID() {
    return clientID;
  }

  public int getBatchCount() {
    return batchCount;
  }

  public ObjectRequestID getRequestID() {
    return this.requestID;
  }

  public Set getLookupIDs() {
    return requestedObjectIDs;
  }

  public Collection getObjects() {
    return objects.values();
  }

  public Set getLookupPendingObjectIDs() {
    return this.lookupPendingObjectIDs;
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.println("ManagedObjectRequestContext");
    PrettyPrinter rv = out;
    out = out.duplicateAndIndent();
    out.indent().println(new Date(timestamp));
    out.indent().println("channelID: " + clientID);
    out.indent().println("requestID: " + requestID);
    out.indent().print("requestedObjectIDs: ").println(requestedObjectIDs);
    return rv;
  }

  public String toString() {
    return "ManagedObjectRequestContext@" + System.identityHashCode(this) + " [ " + clientID + " , " + requestID
           + " , " + requestedObjectIDs + ", requestingThread = " + requestingThreadName + " ]";
  }

  public void setResults(ObjectManagerLookupResults results) {
    this.objects = results.getObjects();
    this.lookupPendingObjectIDs = results.getLookupPendingObjectIDs();
    this.sink.add(this); // Add to next stage
  }

  public Sink getSink() {
    return this.sink;
  }

  public Set getNewObjectIDs() {
    return Collections.EMPTY_SET;
  }

  public void missingObject(ObjectID oid) {
    missingObjects.add(oid);
  }

  public Set getMissingObjectIDs() {
    return missingObjects;
  }

}
