/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.logging.TCLogger;
import com.tc.management.beans.object.ServerDBBackupMBean;
import com.tc.objectserver.persistence.api.ManagedObjectStoreStats;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.util.sequence.MutableSequence;

import java.io.File;

// This should be the class which should be used by Derby Db environment and Berkeley db env
public interface DBEnvironment {
  static final String GLOBAL_SEQUENCE_DATABASE    = "global_sequence_db";
  static final String ROOT_DB_NAME                = "roots";
  static final String OBJECT_DB_NAME              = "objects";
  static final String OBJECT_OID_STORE_DB_NAME    = "objects_oid_store";
  static final String MAPS_OID_STORE_DB_NAME      = "mapsdatabase_oid_store";
  static final String OID_STORE_LOG_DB_NAME       = "oid_store_log";
  static final String EVICTABLE_OID_STORE_DB_NAME = "evictable_oid_store";

  static final String CLIENT_STATE_DB_NAME        = "clientstates";
  static final String TRANSACTION_DB_NAME         = "transactions";
  static final String STRING_INDEX_DB_NAME        = "stringindex";
  static final String CLASS_DB_NAME               = "classdefinitions";
  static final String MAP_DB_NAME                 = "mapsdatabase";
  static final String CLUSTER_STATE_STORE         = "clusterstatestore";
  static final String CONTROL_DB                  = "controldb";

  enum DBEnvironmentStatus {
    STATUS_INIT, STATUS_ERROR, STATUS_OPENING, STATUS_OPEN, STATUS_CLOSING, STATUS_CLOSED
  }

  /**
   * Opens the database and creates all the databases.
   */
  public abstract boolean open() throws TCDatabaseException;

  /**
   * Closes the database. Will fail if there are some open transactions and cursors.
   */
  public abstract void close() throws TCDatabaseException;

  /**
   * Returns true if the database is open.
   */
  public abstract boolean isOpen();

  /**
   * Return the current environment home
   */
  public abstract File getEnvironmentHome();

  /**
   * Returns whether permanent store strategy has been enabled.
   */
  public abstract boolean isParanoidMode();

  /**
   * Initializes the back up mbean.
   */
  public abstract void initBackupMbean(ServerDBBackupMBean mBean) throws TCDatabaseException;

  /**
   * Initializes Object Store Stats.
   */
  public abstract void initObjectStoreStats(ManagedObjectStoreStats objectStoreStats);

  /**
   * Returns the persistence transaction provider which can be used to create new transactions.
   */
  public abstract PersistenceTransactionProvider getPersistenceTransactionProvider();

  /**
   * Returns the Object Database to be used by Persistor classes.
   */
  public abstract TCLongToBytesDatabase getObjectDatabase() throws TCDatabaseException;

  /**
   * Oid Stores which are used by FastObjectOidManagerImpl
   */
  public abstract TCBytesToBytesDatabase getObjectOidStoreDatabase() throws TCDatabaseException;

  public abstract TCBytesToBytesDatabase getMapsOidStoreDatabase() throws TCDatabaseException;

  public abstract TCBytesToBytesDatabase getOidStoreLogDatabase() throws TCDatabaseException;

  public abstract TCBytesToBytesDatabase getEvictableOidStoreDatabase() throws TCDatabaseException;

  /**
   * Returns the Root database
   */
  public abstract TCRootDatabase getRootDatabase() throws TCDatabaseException;

  /**
   * A long database for storing the client ids.
   */
  public abstract TCLongDatabase getClientStateDatabase() throws TCDatabaseException;

  /**
   * Returns the transaction database.
   */
  public abstract TCTransactionStoreDatabase getTransactionDatabase() throws TCDatabaseException;

  /**
   * Returns the class database.
   */
  public abstract TCIntToBytesDatabase getClassDatabase() throws TCDatabaseException;

  /**
   * Returns the maps database.
   */
  public abstract TCMapsDatabase getMapsDatabase() throws TCDatabaseException;

  public abstract TCLongToStringDatabase getStringIndexDatabase() throws TCDatabaseException;

  public abstract TCStringToStringDatabase getClusterStateStoreDatabase() throws TCDatabaseException;

  /**
   * Return the persistent sequence.
   */
  public abstract MutableSequence getSequence(PersistenceTransactionProvider ptxp, TCLogger logger, String sequenceID,
                                              int startValue);

  public StatisticRetrievalAction[] getSRAs();

  public abstract OffheapStats getOffheapStats();
}
