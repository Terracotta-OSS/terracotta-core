/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.l1.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.PostInit;
import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.objectserver.context.InvalidateObjectsForClientContext;
import com.tc.objectserver.context.ValidateObjectsRequestContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.api.InvalidateObjectManager;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TxnsInSystemCompletionListener;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.TCConcurrentMultiMap;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class naturally batches invalidation to the clients by internally using a MultiMap
 */
public class InvalidateObjectManagerImpl implements InvalidateObjectManager, PostInit {
  private static final TCLogger logger = TCLogging.getLogger(InvalidateObjectManagerImpl.class);

  private static enum State {
    INITIAL, STARTED
  }

  private volatile State                                   state         = State.INITIAL;

  private final TCConcurrentMultiMap<ClientID, ObjectID>   invalidateMap = new TCConcurrentMultiMap<ClientID, ObjectID>(
                                                                                                                        256,
                                                                                                                        0.75f,
                                                                                                                        128);
  private final ConcurrentHashMap<ClientID, Set<ObjectID>> validateMap   = new ConcurrentHashMap<ClientID, Set<ObjectID>>(
                                                                                                                          32,
                                                                                                                          0.75f,
                                                                                                                          16);
  private Sink                                             invalidateSink;
  private Sink                                             validateSink;

  private final ServerTransactionManager                   transactionManager;

  public InvalidateObjectManagerImpl(ServerTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

  public void invalidateObjectFor(ClientID clientID, Set<ObjectID> oids) {
    if (invalidateMap.addAll(clientID, oids)) {
      invalidateSink.add(new InvalidateObjectsForClientContext(clientID));
    }
  }

  public Set<ObjectID> getObjectsIDsToInvalidate(ClientID clientID) {
    return invalidateMap.removeAll(clientID);
  }

  public void initializeContext(ConfigurationContext context) {
    this.invalidateSink = context.getStage(ServerConfigurationContext.INVALIDATE_OBJECTS_STAGE).getSink();
    this.validateSink = context.getStage(ServerConfigurationContext.VALIDATE_OBJECTS_STAGE).getSink();
  }

  public void validateObjects(ObjectIDSet validEntries) {
    for (Iterator i = validateMap.entrySet().iterator(); i.hasNext();) {
      Entry<ClientID, Set<ObjectID>> e = (Entry<ClientID, Set<ObjectID>>) i.next();
      Set<ObjectID> invalids = getInvalidsFrom(validEntries, e.getValue());
      if (!invalids.isEmpty()) {
        logger.info("Invalidating " + invalids.size() + " entries in " + e.getKey() + " after restart");
        invalidateObjectFor(e.getKey(), invalids);
      }
      i.remove();
    }
  }

  private Set<ObjectID> getInvalidsFrom(ObjectIDSet validEntries, Set<ObjectID> toCheck) {
    Set<ObjectID> invalids = new ObjectIDSet();
    for (ObjectID oid : toCheck) {
      if (!validEntries.contains(oid)) {
        invalids.add(oid);
      }
    }
    return invalids;
  }

  public void addObjectsToValidateFor(ClientID clientID, Set objectIDsToValidate) {
    if (state != State.INITIAL) { throw new AssertionError(
                                                           "Objects can be added for validation only in INITIAL state : state = "
                                                               + state + " clientID = " + clientID
                                                               + " objectIDsToValidate = " + objectIDsToValidate.size()); }
    if (!objectIDsToValidate.isEmpty()) {
      Set<ObjectID> old = validateMap.put(clientID, objectIDsToValidate);
      if (old != null) { throw new AssertionError("Same client send validate objects twice : " + clientID
                                                  + " objects to validate : " + objectIDsToValidate.size()); }
    }
  }

  public void start() {
    state = State.STARTED;
    transactionManager.callBackOnResentTxnsInSystemCompletion(new TxnsInSystemCompletionListener() {

      public void onCompletion() {
        int size = validateMap.size();
        logger.info("Restart txn processing complete : Adding validation of Objects for " + size + " Clients");
        if (size > 0) {
          validateSink.add(new ValidateObjectsRequestContext());
        }
      }
    });
  }
}
