/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;
import com.tc.logging.TCLogger;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIdFactory;
import com.tc.objectserver.l1.api.ClientState;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.PersistentSequence;
import com.tc.objectserver.persistence.impl.ClientNotFoundException;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor.SleepycatPersistorBase;
import com.tc.util.Conversion;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

class ClientStatePersistorImpl extends SleepycatPersistorBase implements ClientStatePersistor {

  final ConnectionIdFactory                    connectionIDFactory;
  private final Database                       db;
  private final CursorConfig                   cursorConfig;
  private final DatabaseEntry                  key;
  private final DatabaseEntry                  value;
  private final PersistenceTransactionProvider ptp;
  private final TCLogger                       logger;
  private final PersistentSequence             connectionIDSequence;

  ClientStatePersistorImpl(final TCLogger logger, final PersistenceTransactionProvider ptp,
                           final PersistentSequence connectionIDSequence, final Database db) {
    this.logger = logger;
    this.ptp = ptp;
    this.cursorConfig = new CursorConfig();
    this.cursorConfig.setReadCommitted(true);
    this.db = db;
    this.key = new DatabaseEntry();
    this.value = new DatabaseEntry();
    this.connectionIDSequence = connectionIDSequence;
    this.connectionIDFactory = new ConnectionIdFactory() {

      public ConnectionID nextConnectionId() {
        long clientID = connectionIDSequence.next();
        // Make sure we save the fact that we are giving out this id to someone in the database before giving it out.
        basicSave(clientID);
        ConnectionID rv = new ConnectionID(clientID, connectionIDSequence.getUID());
        logger.debug("New connection id created: " + rv);
        return rv;
      }

    };
  }

  public ConnectionIdFactory getConnectionIDFactory() {
    return this.connectionIDFactory;
  }

  public synchronized boolean containsClient(ChannelID id) {
    setKey(id);
    try {
      PersistenceTransaction tx = ptp.newTransaction();
      OperationStatus status = db.get(pt2nt(tx), key, value, LockMode.DEFAULT);
      tx.commit();
      return OperationStatus.SUCCESS.equals(status);
    } catch (DatabaseException e) {
      throw new DBException(e);
    }
  }

  public synchronized Iterator loadClientIDs() {
    Set set = new HashSet();
    Cursor cursor;
    try {
      PersistenceTransaction tx = ptp.newTransaction();
      cursor = db.openCursor(pt2nt(tx), cursorConfig);
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        set.add(new ChannelID(Conversion.bytes2Long(key.getData())));
      }
      cursor.close();
      tx.commit();
    } catch (DatabaseException e) {
      e.printStackTrace();
      throw new DBException(e);
    }
    return set.iterator();
  }

  public synchronized Set loadConnectionIDs() {
    String uid = connectionIDSequence.getUID();
    Set connections = new HashSet();
    for (Iterator i = loadClientIDs(); i.hasNext();) {
      connections.add(new ConnectionID(((ChannelID) i.next()).toLong(), uid));
    }
    return connections;
  }

  public synchronized void saveClientState(ClientState clientState) {
    // someday, maybe we'll need to save more state, but for now, this stops
    // from overwriting the sequence.
    if (containsClient(clientState.getClientID())) return;
    basicSave(clientState.getClientID().toLong());
    logger.debug("Saved client state for " + clientState.getClientID());
  }

  private void basicSave(long clientID) {

    setKey(clientID);
    value.setData(Conversion.long2Bytes(0));
    try {
      PersistenceTransaction tx = ptp.newTransaction();
      Transaction realTx = pt2nt(tx);
      OperationStatus status = db.put(realTx, key, value);
      if (!OperationStatus.SUCCESS.equals(status)) {
        realTx.abort();
        throw new DBException("Unable to save client state: ChannelID " + clientID + "; status: " + status);
      }
      tx.commit();
    } catch (DatabaseException e) {
      throw new DBException(e);
    }
  }

  public synchronized void deleteClientState(ChannelID id) throws ClientNotFoundException {
    setKey(id);
    try {
      PersistenceTransaction tx = ptp.newTransaction();
      Transaction realTx = pt2nt(tx);

      OperationStatus status = db.delete(realTx, key);
      if (OperationStatus.NOTFOUND.equals(status)) {
        realTx.abort();
        throw new ClientNotFoundException("Client not found: " + id);
      }
      if (!OperationStatus.SUCCESS.equals(status)) {
        realTx.abort();
        throw new DBException("Unable to delete client state: " + id + "; status: " + status);
      }

      tx.commit();
      logger.debug("Deleted client state for " + id);
    } catch (DatabaseException e) {
      throw new DBException(e);
    }
  }

  private void setKey(ChannelID id) {
    setKey(id.toLong());
  }

  private void setKey(long id) {
    setData(id, key);
  }

  private void setData(long id, DatabaseEntry entry) {
    entry.setData(Conversion.long2Bytes(id));
  }

}