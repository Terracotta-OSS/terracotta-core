/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.logging.TCLogger;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.objectserver.persistence.db.DBPersistorImpl.DBPersistorBase;
import com.tc.objectserver.persistence.inmemory.ClientNotFoundException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.api.TCLongDatabase;
import com.tc.objectserver.storage.api.TCDatabaseReturnConstants.Status;
import com.tc.util.sequence.MutableSequence;

import java.util.HashSet;
import java.util.Set;

class ClientStatePersistorImpl extends DBPersistorBase implements ClientStatePersistor {

  private final TCLongDatabase                 db;
  private final PersistenceTransactionProvider ptp;
  private final TCLogger                       logger;
  private final MutableSequence                connectionIDSequence;

  ClientStatePersistorImpl(final TCLogger logger, final PersistenceTransactionProvider ptp,
                           final MutableSequence connectionIDSequence, final TCLongDatabase db) {
    this.logger = logger;
    this.ptp = ptp;
    this.db = db;
    this.connectionIDSequence = connectionIDSequence;
  }

  public MutableSequence getConnectionIDSequence() {
    return this.connectionIDSequence;
  }

  public synchronized boolean containsClient(ChannelID id) {
    try {
      PersistenceTransaction tx = ptp.newTransaction();
      boolean status = db.contains(id.toLong(), tx);
      tx.commit();
      return status;
    } catch (Exception e) {
      throw new DBException(e);
    }
  }

  public synchronized Set loadClientIDs() {
    Set set = new HashSet();
    try {
      PersistenceTransaction tx = ptp.newTransaction();
      Set<Long> tempSet = db.getAllKeys(tx);
      for (Long l : tempSet) {
        set.add(new ChannelID(l));
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new DBException(e);
    }
    return set;
  }

  public synchronized void saveClientState(ChannelID clientID) {
    // someday, maybe we'll need to save more state, but for now, this stops
    // from overwriting the sequence.
    if (containsClient(clientID)) return;
    basicSave(clientID.toLong());
    logger.debug("Saved client state for " + clientID);
  }

  private void basicSave(long clientID) {
    try {
      PersistenceTransaction tx = ptp.newTransaction();
      Status status = db.put(clientID, tx);
      if (status != Status.SUCCESS) {
        tx.abort();
        throw new DBException("Unable to save client state: ChannelID " + clientID + "; status: " + status);
      }
      tx.commit();
    } catch (Exception e) {
      throw new DBException(e);
    }
  }

  public synchronized void deleteClientState(ChannelID id) {
    try {
      PersistenceTransaction tx = ptp.newTransaction();

      Status status = db.delete(id.toLong(), tx);
      if (Status.NOT_FOUND.equals(status)) {
        tx.abort();
        throw new ClientNotFoundException("Client not found: " + id);
      }
      if (!Status.SUCCESS.equals(status)) {
        tx.abort();
        throw new DBException("Unable to delete client state: " + id + "; status: " + status);
      }

      tx.commit();
      logger.info("Deleted client state for " + id);
    } catch (Exception e) {
      throw new DBException(e);
    }
  }
}