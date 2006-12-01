/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.Sink;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectRequestID;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * @author steve This is the context needed to make a request to the server for a specific Managed root.
 */
public class ManagedObjectRequestContext implements ObjectManagerResultsContext, PrettyPrintable {
  private final long            timestamp;
  private final ChannelID       channelID;
  private final Collection      requestedObjectIDs;
  private Map                   objects;
  private final ObjectRequestID requestID;
  private boolean               moreObjects    = false;
  private int                   batchCount     = 0;
  private Set                   lookupPendingObjectIDs;
  private final int             maxRequestDepth;
  private boolean               pendingRequest = false;
  private final Sink            sink;

  public ManagedObjectRequestContext(ChannelID channelID, ObjectRequestID requestID, Collection ids,
                                     int maxRequestDepth, Sink sink) {
    this.maxRequestDepth = maxRequestDepth;
    this.sink = sink;
    this.timestamp = System.currentTimeMillis();
    this.channelID = channelID;
    this.requestID = requestID;
    this.requestedObjectIDs = ids;
  }

  public int getMaxRequestDepth() {
    return this.maxRequestDepth;
  }

  public boolean hasMoreObjects() {
    return moreObjects;
  }

  public ChannelID getChannelID() {
    return channelID;
  }

  public int getBatchCount() {
    return batchCount;
  }

  public ObjectRequestID getRequestID() {
    return this.requestID;
  }

  public Collection getRequestedObjectIDs() {
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
    out.indent().println("channelID: " + channelID);
    out.indent().println("requestID: " + requestID);
    out.indent().print("requestedObjectIDs: ").println(requestedObjectIDs);
    return rv;
  }

  public void setResults(ChannelID chID, Collection ids, ObjectManagerLookupResults results) {
    this.objects = results.getObjects();
    this.lookupPendingObjectIDs = results.getLookupPendingObjectIDs();
    this.sink.add(this);  // Add to next stage
  }

  public Set getCheckedOutObjectIDs() {
    return Collections.EMPTY_SET;
  }

  public boolean isPendingRequest() {
    return pendingRequest;
  }

  public void makePending(ChannelID chID, Collection ids) {
    pendingRequest = true;
  }

  public Sink getSink() {
    return this.sink;
  }

}
