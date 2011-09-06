/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.l1.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.PostInit;
import com.tc.async.api.Sink;
import com.tc.invalidation.Invalidations;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.objectserver.context.InvalidateObjectsForClientContext;
import com.tc.objectserver.context.ValidateObjectsRequestContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.api.InvalidateObjectManager;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TxnsInSystemCompletionListener;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.TCConcurrentStore;
import com.tc.util.concurrent.TCConcurrentStore.TCConcurrentStoreCallback;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class naturally batches invalidation to the clients by internally using a MultiMap
 */
public class InvalidateObjectManagerImpl implements InvalidateObjectManager, PostInit {
  private static final TCLogger logger = TCLogging.getLogger(InvalidateObjectManagerImpl.class);

  private static enum State {
    INITIAL, STARTED
  }

  private volatile State                                   state                       = State.INITIAL;

  private final TCConcurrentStore<ClientID, Invalidations> invalidateMap               = new TCConcurrentStore<ClientID, Invalidations>(
                                                                                                                                        256,
                                                                                                                                        0.75f,
                                                                                                                                        128);
  private final ConcurrentHashMap<ClientID, Invalidations> validateMap                 = new ConcurrentHashMap<ClientID, Invalidations>(
                                                                                                                                        32,
                                                                                                                                        0.75f,
                                                                                                                                        16);
  private final AddCallbackForInvalidations                addCallbackForInvalidations = new AddCallbackForInvalidations();
  private Sink                                             invalidateSink;
  private Sink                                             validateSink;

  private final ServerTransactionManager                   transactionManager;

  public InvalidateObjectManagerImpl(ServerTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

  public void invalidateObjectFor(ClientID clientID, Invalidations invalidations) {
    Boolean bool = (Boolean) invalidateMap.executeUnderWriteLock(clientID, invalidations, addCallbackForInvalidations);
    if (bool.booleanValue()) {
      invalidateSink.add(new InvalidateObjectsForClientContext(clientID));
    }
  }

  public Invalidations getObjectsIDsToInvalidate(ClientID clientID) {
    return invalidateMap.remove(clientID);
  }

  public void initializeContext(ConfigurationContext context) {
    this.invalidateSink = context.getStage(ServerConfigurationContext.INVALIDATE_OBJECTS_STAGE).getSink();
    this.validateSink = context.getStage(ServerConfigurationContext.VALIDATE_OBJECTS_STAGE).getSink();
  }

  public void validateObjects(ObjectIDSet validEntries) {
    for (Iterator i = validateMap.entrySet().iterator(); i.hasNext();) {
      Entry<ClientID, Invalidations> e = (Entry<ClientID, Invalidations>) i.next();
      Invalidations invalidations = e.getValue();
      invalidations.removeAll(validEntries);

      if (!invalidations.isEmpty()) {
        logger.info("Invalidating " + invalidations.size() + " entries in " + e.getKey() + " after restart");
        invalidateObjectFor(e.getKey(), invalidations);
      }
      i.remove();
    }
  }

  public void addObjectsToValidateFor(ClientID clientID, Invalidations invalidations) {
    if (state != State.INITIAL) { throw new AssertionError(
                                                           "Objects can be added for validation only in INITIAL state : state = "
                                                               + state + " clientID = " + clientID
                                                               + " objectIDsToValidate = " + invalidations.size()); }
    if (!invalidations.isEmpty()) {
      Invalidations old = validateMap.put(clientID, invalidations);
      if (old != null) { throw new AssertionError("Same client send validate objects twice : " + clientID
                                                  + " objects to validate : " + invalidations.size()); }
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

  private static class AddCallbackForInvalidations implements TCConcurrentStoreCallback<ClientID, Invalidations> {
    public Object callback(ClientID key, Object param, Map<ClientID, Invalidations> segment) {
      boolean newEntry = false;
      Invalidations newInvalidations = (Invalidations) param;
      Invalidations invalidations = segment.get(key);
      if (invalidations == null) {
        segment.put(key, newInvalidations);
        newEntry = true;
      } else {
        invalidations.add(newInvalidations);
      }
      return newEntry;
    }
  }
}
