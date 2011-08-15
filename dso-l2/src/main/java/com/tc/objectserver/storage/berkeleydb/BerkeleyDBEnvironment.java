/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.storage.berkeleydb;

import org.apache.commons.io.FileUtils;

import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Durability;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentStats;
import com.sleepycat.je.JEVersion;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.StatsConfig;
import com.sleepycat.je.Transaction;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.object.ServerDBBackupMBean;
import com.tc.objectserver.persistence.api.ManagedObjectStoreStats;
import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.persistence.db.DatabaseNotOpenException;
import com.tc.objectserver.persistence.db.DatabaseOpenException;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.persistence.inmemory.NullPersistenceTransactionProvider;
import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.api.OffheapStats;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.api.TCBytesToBytesDatabase;
import com.tc.objectserver.storage.api.TCIntToBytesDatabase;
import com.tc.objectserver.storage.api.TCLongDatabase;
import com.tc.objectserver.storage.api.TCLongToBytesDatabase;
import com.tc.objectserver.storage.api.TCLongToStringDatabase;
import com.tc.objectserver.storage.api.TCMapsDatabase;
import com.tc.objectserver.storage.api.TCRootDatabase;
import com.tc.objectserver.storage.api.TCStringToStringDatabase;
import com.tc.objectserver.storage.api.TCTransactionStoreDatabase;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.retrieval.actions.SRAForBerkeleyDB;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.sequence.MutableSequence;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class BerkeleyDBEnvironment implements DBEnvironment {

  private static final TCLogger      clogger                     = CustomerLogging.getDSOGenericLogger();
  private static final TCLogger      logger                      = TCLogging.getLogger(BerkeleyDBEnvironment.class);

  private static final Object        CONTROL_LOCK                = new Object();

  private static final DatabaseEntry CLEAN_FLAG_KEY              = new DatabaseEntry(new byte[] { 1 });
  private static final byte          IS_CLEAN                    = 1;
  private static final byte          IS_DIRTY                    = 2;
  private static final long          SLEEP_TIME_ON_STARTUP_ERROR = 500;
  private static final int           STARTUP_RETRY_COUNT         = 5;

  private final List                 createdDatabases;
  private final Map                  databasesByName;
  private final File                 envHome;
  private EnvironmentConfig          ecfg;
  private DatabaseConfig             dbcfg;
  private ClassCatalogWrapper        catalog;

  private Environment                env;
  private Database                   controlDB;
  private DBEnvironmentStatus        status                      = DBEnvironmentStatus.STATUS_INIT;
  private boolean                    openResult                  = false;

  private final boolean              paranoid;
  private final SampledCounter       l2FaultFromDisk;
  private final SRAForBerkeleyDB     sraBerkeleyDB;

  public BerkeleyDBEnvironment(boolean paranoid, File envHome) throws IOException {
    this(paranoid, envHome, new Properties(), SampledCounter.NULL_SAMPLED_COUNTER, false);
  }

  public BerkeleyDBEnvironment(boolean paranoid, File envHome, Properties jeProperties, SampledCounter l2FaultFrmDisk,
                               boolean offheapEnabled) throws IOException {
    this(new HashMap(), new LinkedList(), paranoid, envHome, l2FaultFrmDisk);

    if (!isParanoidMode() && offheapEnabled) {
      final Integer newBDBMemPercentage = Integer.parseInt(jeProperties.getProperty("je.maxMemoryPercent")) / 3;
      jeProperties.setProperty("je.maxMemoryPercent", newBDBMemPercentage.toString());
      logger.info("Since running OffHeap in temp-swap mode, setting je.maxMemoryPercent to "
                  + newBDBMemPercentage.toString());
    }

    this.ecfg = new EnvironmentConfig(jeProperties);
    this.ecfg.setTransactional(paranoid);
    this.ecfg.setAllowCreate(true);
    this.ecfg.setReadOnly(false);
    if (!paranoid) {
      this.ecfg.setDurability(Durability.COMMIT_WRITE_NO_SYNC);
    }
    this.dbcfg = new DatabaseConfig();
    this.dbcfg.setAllowCreate(true);
    this.dbcfg.setTransactional(paranoid);
    logger.info("Env config = " + this.ecfg + " DB Config = " + this.dbcfg + " JE Properties = " + jeProperties);
  }

  public void initBackupMbean(ServerDBBackupMBean mBean) throws TCDatabaseException {
    ((BerkeleyServerDBBackup) mBean).initDbEnvironment(this.getEnvironment(), this.getEnvironmentHome());
  }

  public void initObjectStoreStats(ManagedObjectStoreStats objectStoreStats) {
    //
  }

  // For tests
  BerkeleyDBEnvironment(boolean paranoid, File envHome, EnvironmentConfig ecfg, DatabaseConfig dbcfg)
      throws IOException {
    this(new HashMap(), new LinkedList(), paranoid, envHome, ecfg, dbcfg);
  }

  // For tests
  public BerkeleyDBEnvironment(Map databasesByName, List createdDatabases, boolean paranoid, File envHome,
                               EnvironmentConfig ecfg, DatabaseConfig dbcfg) throws IOException {
    this(databasesByName, createdDatabases, paranoid, envHome, SampledCounter.NULL_SAMPLED_COUNTER);
    this.ecfg = ecfg;
    this.dbcfg = dbcfg;
  }

  /**
   * Note: it is not currently safe to create more than one of these instances in the same process. Sleepycat is
   * supposed to keep more than one process from opening a writable handle to the same database, but it allows you to
   * create more than one writable handle within the same process. So, don't do that.
   */
  private BerkeleyDBEnvironment(Map databasesByName, List createdDatabases, boolean paranoid, File envHome,
                                final SampledCounter l2FaultFrmDisk) throws IOException {
    this.databasesByName = databasesByName;
    this.createdDatabases = createdDatabases;
    this.paranoid = paranoid;
    this.envHome = envHome;
    this.l2FaultFromDisk = l2FaultFrmDisk;
    this.sraBerkeleyDB = new SRAForBerkeleyDB(this);
    FileUtils.forceMkdir(this.envHome);
    logger.info("Sleepy cat version being used " + JEVersion.CURRENT_VERSION.getVersionString());
  }

  public boolean isParanoidMode() {
    return paranoid;
  }

  public synchronized boolean open() throws TCDatabaseException {
    if ((status != DBEnvironmentStatus.STATUS_INIT) && (status != DBEnvironmentStatus.STATUS_CLOSED)) { throw new DatabaseOpenException(
                                                                                                                                        "Database environment isn't in INIT/CLOSED state."); }

    status = DBEnvironmentStatus.STATUS_OPENING;
    try {
      env = openEnvironment();
      synchronized (CONTROL_LOCK) {
        // XXX: Note: this doesn't guard against multiple instances in different
        // classloaders...
        controlDB = env.openDatabase(null, "control", this.dbcfg);
        openResult = isClean();
        if (!openResult) {
          this.status = DBEnvironmentStatus.STATUS_INIT;
          forceClose();
          return openResult;
        }
      }
      if (!this.paranoid) setDirty();
      this.catalog = new ClassCatalogWrapper(env, dbcfg);
      newDatabase(env, GLOBAL_SEQUENCE_DATABASE);
      newObjectDB(env, OBJECT_DB_NAME);
      newBytesBytesDB(env, OBJECT_OID_STORE_DB_NAME);
      newBytesBytesDB(env, MAPS_OID_STORE_DB_NAME);
      newBytesBytesDB(env, OID_STORE_LOG_DB_NAME);
      newBytesBytesDB(env, EVICTABLE_OID_STORE_DB_NAME);
      newRootDB(env, ROOT_DB_NAME);

      newLongDB(env, CLIENT_STATE_DB_NAME);
      newTransactionStoreDB(env, TRANSACTION_DB_NAME);
      newLongToStringDatabase(env, STRING_INDEX_DB_NAME);
      newIntToBytesDatabase(env, CLASS_DB_NAME);
      newMapsDatabase(env, MAP_DB_NAME);
      newStringToStringDatabase(env, CLUSTER_STATE_STORE);
    } catch (DatabaseException e) {
      this.status = DBEnvironmentStatus.STATUS_ERROR;
      forceClose();
      throw new TCDatabaseException(e);
    } catch (Error e) {
      this.status = DBEnvironmentStatus.STATUS_ERROR;
      forceClose();
      throw e;
    } catch (RuntimeException e) {
      this.status = DBEnvironmentStatus.STATUS_ERROR;
      forceClose();
      throw e;
    }

    this.status = DBEnvironmentStatus.STATUS_OPEN;
    return openResult;
  }

  private void cinfo(Object message) {
    clogger.info("DB Environment: " + message);
  }

  public synchronized void close() throws TCDatabaseException {
    assertOpen();
    status = DBEnvironmentStatus.STATUS_CLOSING;
    cinfo("Closing...");

    try {
      for (Iterator i = createdDatabases.iterator(); i.hasNext();) {
        Object o = i.next();
        Database db = null;
        if (o instanceof AbstractBerkeleyDatabase) {
          db = ((AbstractBerkeleyDatabase) o).getDatabase();
        } else {
          db = (Database) o;
        }

        cinfo("Closing database: " + db.getDatabaseName() + "...");
        db.close();
      }
      cinfo("Closing class catalog...");
      this.catalog.close();
      setClean();
      if (this.controlDB != null) {
        cinfo("Closing control database...");
        this.controlDB.close();
      }
      if (this.env != null) {
        cinfo("Closing environment...");
        this.env.close();
      }
    } catch (Exception de) {
      de.printStackTrace();
      throw new TCDatabaseException(de.getMessage());
    }
    this.controlDB = null;
    this.env = null;

    status = DBEnvironmentStatus.STATUS_CLOSED;
    cinfo("Closed.");
  }

  public synchronized boolean isOpen() {
    return DBEnvironmentStatus.STATUS_OPEN.equals(status);
  }

  // This is for testing and cleanup on error.
  synchronized void forceClose() {
    List toClose = new ArrayList(createdDatabases);
    toClose.add(controlDB);
    for (Iterator i = toClose.iterator(); i.hasNext();) {
      try {
        Object o = i.next();
        Database db = null;
        if (o instanceof AbstractBerkeleyDatabase) {
          db = ((AbstractBerkeleyDatabase) o).getDatabase();
        } else {
          db = (Database) o;
        }
        if (db != null) db.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    try {
      if (this.catalog != null) this.catalog.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      if (env != null) env.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public File getEnvironmentHome() {
    return envHome;
  }

  public synchronized Environment getEnvironment() throws TCDatabaseException {
    assertOpen();
    return env;
  }

  public EnvironmentStats getStats() throws TCDatabaseException {
    final StatsConfig sc = new StatsConfig();
    sc.setClear(true);
    try {
      return env.getStats(sc);
    } catch (Exception e) {
      throw new TCDatabaseException(e.getMessage());
    }
  }

  public StatisticRetrievalAction[] getSRAs() {
    return new StatisticRetrievalAction[] { sraBerkeleyDB };
  }

  public synchronized TCLongToBytesDatabase getObjectDatabase() throws TCDatabaseException {
    assertOpen();
    return (TCLongToBytesDatabase) databasesByName.get(OBJECT_DB_NAME);
  }

  public synchronized TCBytesToBytesDatabase getObjectOidStoreDatabase() throws TCDatabaseException {
    assertOpen();
    return (BerkeleyDBTCBytesBytesDatabase) databasesByName.get(OBJECT_OID_STORE_DB_NAME);
  }

  public synchronized TCBytesToBytesDatabase getMapsOidStoreDatabase() throws TCDatabaseException {
    assertOpen();
    return (BerkeleyDBTCBytesBytesDatabase) databasesByName.get(MAPS_OID_STORE_DB_NAME);
  }

  public synchronized TCBytesToBytesDatabase getOidStoreLogDatabase() throws TCDatabaseException {
    assertOpen();
    return (BerkeleyDBTCBytesBytesDatabase) databasesByName.get(OID_STORE_LOG_DB_NAME);
  }

  public TCBytesToBytesDatabase getEvictableOidStoreDatabase() throws TCDatabaseException {
    assertOpen();
    return (BerkeleyDBTCBytesBytesDatabase) databasesByName.get(EVICTABLE_OID_STORE_DB_NAME);
  }

  public synchronized ClassCatalogWrapper getClassCatalogWrapper() throws TCDatabaseException {
    assertOpen();
    return catalog;
  }

  public synchronized TCRootDatabase getRootDatabase() throws TCDatabaseException {
    assertOpen();
    return (BerkeleyDBTCRootDatabase) databasesByName.get(ROOT_DB_NAME);
  }

  public synchronized TCLongDatabase getClientStateDatabase() throws TCDatabaseException {
    assertOpen();
    return (BerkeleyDBTCLongDatabase) databasesByName.get(CLIENT_STATE_DB_NAME);
  }

  public synchronized TCTransactionStoreDatabase getTransactionDatabase() throws TCDatabaseException {
    assertOpen();
    return (TCTransactionStoreDatabase) databasesByName.get(TRANSACTION_DB_NAME);
  }

  public synchronized TCIntToBytesDatabase getClassDatabase() throws TCDatabaseException {
    assertOpen();
    return (BerkeleyDBTCIntToBytesDatabase) databasesByName.get(CLASS_DB_NAME);
  }

  public synchronized TCMapsDatabase getMapsDatabase() throws TCDatabaseException {
    assertOpen();
    return (BerkeleyDBTCMapsDatabase) databasesByName.get(MAP_DB_NAME);
  }

  public synchronized TCLongToStringDatabase getStringIndexDatabase() throws TCDatabaseException {
    assertOpen();
    return (BerkeleyDBTCLongToStringDatabase) databasesByName.get(STRING_INDEX_DB_NAME);
  }

  public synchronized TCStringToStringDatabase getClusterStateStoreDatabase() throws TCDatabaseException {
    assertOpen();
    return (BerkeleyDBTCStringtoStringDatabase) databasesByName.get(CLUSTER_STATE_STORE);
  }

  private void assertNotError() throws TCDatabaseException {
    if (DBEnvironmentStatus.STATUS_ERROR == status) throw new TCDatabaseException(
                                                                                  "Attempt to operate on an environment in an error state.");
  }

  private void assertOpening() {
    if (DBEnvironmentStatus.STATUS_OPENING != status) throw new AssertionError(
                                                                               "Database environment should be opening but isn't");
  }

  private void assertOpen() throws TCDatabaseException {
    assertNotError();
    if (DBEnvironmentStatus.STATUS_OPEN != status) throw new DatabaseNotOpenException(
                                                                                      "Database environment should be open but isn't.");
  }

  private void assertClosing() {
    if (DBEnvironmentStatus.STATUS_CLOSING != status) throw new AssertionError(
                                                                               "Database environment should be closing but isn't");
  }

  private boolean isClean() throws TCDatabaseException {
    assertOpening();
    DatabaseEntry value = new DatabaseEntry(new byte[] { 0 });
    Transaction tx = newTransaction();
    OperationStatus stat;
    try {
      stat = controlDB.get(tx, CLEAN_FLAG_KEY, value, LockMode.DEFAULT);
      if (tx != null) {
        tx.commit();
      }
    } catch (Exception e) {
      throw new TCDatabaseException(e.getMessage());
    }
    return OperationStatus.NOTFOUND.equals(stat)
           || (OperationStatus.SUCCESS.equals(stat) && value.getData()[0] == IS_CLEAN);
  }

  private void setDirty() throws TCDatabaseException {
    assertOpening();
    DatabaseEntry value = new DatabaseEntry(new byte[] { IS_DIRTY });
    Transaction tx = newTransaction();
    OperationStatus stat;
    try {
      stat = controlDB.put(tx, CLEAN_FLAG_KEY, value);
    } catch (Exception e) {
      throw new TCDatabaseException(e.getMessage());
    }
    if (!OperationStatus.SUCCESS.equals(stat)) throw new TCDatabaseException("Unexpected operation status "
                                                                             + "trying to unset clean flag: " + stat);
    try {
      if (tx != null) {
        tx.commitSync();
      }
    } catch (Exception e) {
      throw new TCDatabaseException(e.getMessage());
    }
  }

  private Transaction newTransaction() throws TCDatabaseException {
    if (!paranoid) { return null; }
    try {
      Transaction tx = env.beginTransaction(null, null);
      return tx;
    } catch (Exception de) {
      throw new TCDatabaseException(de.getMessage());
    }
  }

  private void setClean() throws TCDatabaseException {
    assertClosing();
    DatabaseEntry value = new DatabaseEntry(new byte[] { IS_CLEAN });
    Transaction tx = newTransaction();
    OperationStatus stat;
    try {
      stat = controlDB.put(tx, CLEAN_FLAG_KEY, value);
    } catch (Exception e) {
      throw new TCDatabaseException(e.getMessage());
    }
    if (!OperationStatus.SUCCESS.equals(stat)) throw new TCDatabaseException("Unexpected operation status "
                                                                             + "trying to set clean flag: " + stat);
    try {
      if (tx != null) {
        tx.commitSync();
      }
    } catch (Exception e) {
      throw new TCDatabaseException(e.getMessage());
    }
  }

  private void newObjectDB(Environment e, String name) throws TCDatabaseException {
    try {
      Database db = e.openDatabase(null, name, dbcfg);
      BerkeleyDBTCLongToBytesDatabase objectDatabse = new BerkeleyDBTCLongToBytesDatabase(db, this.l2FaultFromDisk);

      createdDatabases.add(objectDatabse);
      databasesByName.put(name, objectDatabse);
    } catch (Exception de) {
      throw new TCDatabaseException(de.getMessage());
    }
  }

  private void newRootDB(Environment e, String name) throws TCDatabaseException {
    try {
      Database db = e.openDatabase(null, name, dbcfg);
      BerkeleyDBTCRootDatabase bdb = new BerkeleyDBTCRootDatabase(db);
      createdDatabases.add(bdb);
      databasesByName.put(name, bdb);
    } catch (Exception de) {
      throw new TCDatabaseException(de.getMessage());
    }
  }

  private void newBytesBytesDB(Environment e, String name) throws TCDatabaseException {
    try {
      Database db = e.openDatabase(null, name, dbcfg);
      BerkeleyDBTCBytesBytesDatabase bdb = new BerkeleyDBTCBytesBytesDatabase(db);
      createdDatabases.add(bdb);
      databasesByName.put(name, bdb);
    } catch (Exception de) {
      throw new TCDatabaseException(de.getMessage());
    }
  }

  private void newTransactionStoreDB(Environment e, String name) throws TCDatabaseException {
    try {
      Database db = e.openDatabase(null, name, dbcfg);
      TCTransactionStoreDatabase bdb = new BerkeleyDBTCLongToBytesDatabase(db);
      createdDatabases.add(bdb);
      databasesByName.put(name, bdb);
    } catch (Exception de) {
      throw new TCDatabaseException(de.getMessage());
    }
  }

  private void newLongDB(Environment e, String name) throws TCDatabaseException {
    try {
      Database db = e.openDatabase(null, name, dbcfg);
      BerkeleyDBTCLongDatabase bdb = new BerkeleyDBTCLongDatabase(db);
      createdDatabases.add(bdb);
      databasesByName.put(name, bdb);
    } catch (Exception de) {
      throw new TCDatabaseException(de.getMessage());
    }
  }

  private void newIntToBytesDatabase(Environment e, String name) throws TCDatabaseException {
    try {
      Database db = e.openDatabase(null, name, dbcfg);
      BerkeleyDBTCIntToBytesDatabase bdb = new BerkeleyDBTCIntToBytesDatabase(db);
      createdDatabases.add(bdb);
      databasesByName.put(name, bdb);
    } catch (Exception de) {
      throw new TCDatabaseException(de.getMessage());
    }
  }

  private void newLongToStringDatabase(Environment e, String name) throws TCDatabaseException {
    try {
      Database db = e.openDatabase(null, name, dbcfg);
      BerkeleyDBTCLongToStringDatabase bdb = new BerkeleyDBTCLongToStringDatabase(catalog.getClassCatalog(), db);
      createdDatabases.add(bdb);
      databasesByName.put(name, bdb);
    } catch (Exception de) {
      throw new TCDatabaseException(de.getMessage());
    }
  }

  private void newStringToStringDatabase(Environment e, String name) throws TCDatabaseException {
    try {
      Database db = e.openDatabase(null, name, dbcfg);
      BerkeleyDBTCStringtoStringDatabase bdb = new BerkeleyDBTCStringtoStringDatabase(db);
      createdDatabases.add(bdb);
      databasesByName.put(name, bdb);
    } catch (Exception de) {
      throw new TCDatabaseException(de.getMessage());
    }
  }

  private void newMapsDatabase(Environment e, String name) throws TCDatabaseException {
    try {
      Database db = e.openDatabase(null, name, dbcfg);
      BerkeleyDBTCMapsDatabase bdb = new BerkeleyDBTCMapsDatabase(db);
      createdDatabases.add(bdb);
      databasesByName.put(name, bdb);
    } catch (Exception de) {
      throw new TCDatabaseException(de.getMessage());
    }
  }

  private void newDatabase(Environment e, String name) throws TCDatabaseException {
    try {
      Database db = e.openDatabase(null, name, dbcfg);
      createdDatabases.add(db);
      databasesByName.put(name, db);
    } catch (Exception de) {
      throw new TCDatabaseException(de.getMessage());
    }
  }

  private Environment openEnvironment() throws TCDatabaseException {
    int count = 0;
    while (true) {
      try {
        return new Environment(envHome, ecfg);
      } catch (Exception dbe) {
        if (++count <= STARTUP_RETRY_COUNT) {
          logger.warn("Unable to open DB environment. " + dbe.getMessage() + " Retrying after "
                      + SLEEP_TIME_ON_STARTUP_ERROR + " ms");
          ThreadUtil.reallySleep(SLEEP_TIME_ON_STARTUP_ERROR);
        } else {
          throw new TCDatabaseException(dbe.getMessage());
        }
      }
    }
  }

  public static final String getClusterStateStoreName() {
    return CLUSTER_STATE_STORE;
  }

  public static final class ClassCatalogWrapper {

    private final StoredClassCatalog catalog;
    private boolean                  closed = false;

    private ClassCatalogWrapper(Environment env, DatabaseConfig cfg) throws DatabaseException {
      catalog = new StoredClassCatalog(env.openDatabase(null, "java_class_catalog", cfg));
    }

    public final ClassCatalog getClassCatalog() {
      return this.catalog;
    }

    synchronized void close() throws DatabaseException {
      if (closed) throw new IllegalStateException("Already closed.");
      this.catalog.close();
      closed = true;
    }
  }

  public MutableSequence getSequence(PersistenceTransactionProvider ptxp, TCLogger log, String sequenceID,
                                     int startValue) {
    return new BerkeleyDBSequence(ptxp, log, sequenceID, startValue, (Database) databasesByName
        .get(GLOBAL_SEQUENCE_DATABASE));
  }

  public PersistenceTransactionProvider getPersistenceTransactionProvider() {
    try {
      if (paranoid) {
        return new BerkeleyDBPersistenceTransactionProvider(getEnvironment());
      } else {
        return new NullPersistenceTransactionProvider();
      }
    } catch (TCDatabaseException e) {
      throw new DBException(e);
    }
  }

  public OffheapStats getOffheapStats() {
    return OffheapStats.NULL_OFFHEAP_STATS;
  }

  public void printDatabaseStats(Writer writer) throws Exception {
    new BerkeleyDBStatisticsHandler(this.env, writer).report();
  }

}