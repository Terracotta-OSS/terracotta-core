/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import org.apache.commons.io.FileUtils;

import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentStats;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.StatsConfig;
import com.sleepycat.je.Transaction;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class DBEnvironment {

  private static final TCLogger            clogger                     = CustomerLogging.getDSOGenericLogger();
  private static final TCLogger            logger                      = TCLogging.getLogger(DBEnvironment.class);

  private static final String              GLOBAL_SEQUENCE_DATABASE    = "global_sequence_db";
  private static final String              ROOT_DB_NAME                = "roots";
  private static final String              OBJECT_DB_NAME              = "objects";
  private static final String              OBJECT_OID_STORE_DB_NAME    = "objects_oid_store";
  private static final String              MAPS_OID_STORE_DB_NAME      = "mapsdatabase_oid_store";
  private static final String              EVICTABLE_OID_STORE_DB_NAME = "evictable_oid_store";
  private static final String              OID_STORE_LOG_DB_NAME       = "oid_store_log";

  private static final String              CLIENT_STATE_DB_NAME        = "clientstates";
  private static final String              TRANSACTION_DB_NAME         = "transactions";
  private static final String              STRING_INDEX_DB_NAME        = "stringindex";
  private static final String              CLASS_DB_NAME               = "classdefinitions";
  private static final String              MAP_DB_NAME                 = "mapsdatabase";
  private static final String              CLUSTER_STATE_STORE         = "clusterstatestore";

  private static final Object              CONTROL_LOCK                = new Object();

  private static final DBEnvironmentStatus STATUS_INIT                 = new DBEnvironmentStatus("INIT");
  private static final DBEnvironmentStatus STATUS_ERROR                = new DBEnvironmentStatus("ERROR");
  private static final DBEnvironmentStatus STATUS_OPENING              = new DBEnvironmentStatus("OPENING");
  private static final DBEnvironmentStatus STATUS_OPEN                 = new DBEnvironmentStatus("OPEN");
  private static final DBEnvironmentStatus STATUS_CLOSING              = new DBEnvironmentStatus("CLOSING");
  private static final DBEnvironmentStatus STATUS_CLOSED               = new DBEnvironmentStatus("CLOSED");

  private static final DatabaseEntry       CLEAN_FLAG_KEY              = new DatabaseEntry(new byte[] { 1 });
  private static final byte                IS_CLEAN                    = 1;
  private static final byte                IS_DIRTY                    = 2;
  private static final long                SLEEP_TIME_ON_STARTUP_ERROR = 500;
  private static final int                 STARTUP_RETRY_COUNT         = 5;

  private final List                       createdDatabases;
  private final Map                        databasesByName;
  private final File                       envHome;
  private EnvironmentConfig                ecfg;
  private DatabaseConfig                   dbcfg;
  private ClassCatalogWrapper              catalog;

  private Environment                      env;
  private Database                         controlDB;
  private DBEnvironmentStatus              status                      = STATUS_INIT;
  private DatabaseOpenResult               openResult                  = null;

  private final boolean                    paranoid;

  public DBEnvironment(final boolean paranoid, final File envHome) throws IOException {
    this(paranoid, envHome, new Properties());
  }

  public DBEnvironment(final boolean paranoid, final File envHome, final Properties jeProperties) throws IOException {
    this(new HashMap(), new LinkedList(), paranoid, envHome);
    this.ecfg = new EnvironmentConfig(jeProperties);
    this.ecfg.setTransactional(true);
    this.ecfg.setAllowCreate(true);
    this.ecfg.setReadOnly(false);
    // this.ecfg.setTxnWriteNoSync(!paranoid);
    this.ecfg.setTxnNoSync(!paranoid);
    this.dbcfg = new DatabaseConfig();
    this.dbcfg.setAllowCreate(true);
    this.dbcfg.setTransactional(true);

    logger.info("Env config = " + this.ecfg + " DB Config = " + this.dbcfg + " JE Properties = " + jeProperties);
  }

  // For tests
  DBEnvironment(final boolean paranoid, final File envHome, final EnvironmentConfig ecfg, final DatabaseConfig dbcfg)
      throws IOException {
    this(new HashMap(), new LinkedList(), paranoid, envHome, ecfg, dbcfg);
  }

  // For tests
  DBEnvironment(final Map databasesByName, final List createdDatabases, final boolean paranoid, final File envHome,
                final EnvironmentConfig ecfg, final DatabaseConfig dbcfg) throws IOException {
    this(databasesByName, createdDatabases, paranoid, envHome);
    this.ecfg = ecfg;
    this.dbcfg = dbcfg;
  }

  /**
   * Note: it is not currently safe to create more than one of these instances in the same process. Sleepycat is
   * supposed to keep more than one process from opening a writable handle to the same database, but it allows you to
   * create more than one writable handle within the same process. So, don't do that.
   */
  private DBEnvironment(final Map databasesByName, final List createdDatabases, final boolean paranoid,
                        final File envHome) throws IOException {
    this.databasesByName = databasesByName;
    this.createdDatabases = createdDatabases;
    this.paranoid = paranoid;
    this.envHome = envHome;
    FileUtils.forceMkdir(this.envHome);
  }

  public boolean isParanoidMode() {
    return this.paranoid;
  }

  public synchronized DatabaseOpenResult open() throws TCDatabaseException {
    if ((this.status != STATUS_INIT) && (this.status != STATUS_CLOSED)) { throw new DatabaseOpenException(
                                                                                                          "Database environment isn't in INIT/CLOSED state."); }

    this.status = STATUS_OPENING;
    try {
      this.env = openEnvironment();
      synchronized (CONTROL_LOCK) {
        // XXX: Note: this doesn't guard against multiple instances in different
        // classloaders...
        this.controlDB = this.env.openDatabase(null, "control", this.dbcfg);
        this.openResult = new DatabaseOpenResult(isClean());
        if (!this.openResult.isClean()) {
          this.status = STATUS_INIT;
          forceClose();
          return this.openResult;
        }
      }
      if (!this.paranoid) {
        setDirty();
      }
      this.catalog = new ClassCatalogWrapper(this.env, this.dbcfg);
      newDatabase(this.env, GLOBAL_SEQUENCE_DATABASE);
      newDatabase(this.env, OBJECT_DB_NAME);
      newDatabase(this.env, OBJECT_OID_STORE_DB_NAME);
      newDatabase(this.env, MAPS_OID_STORE_DB_NAME);
      newDatabase(this.env, EVICTABLE_OID_STORE_DB_NAME);
      newDatabase(this.env, OID_STORE_LOG_DB_NAME);
      newDatabase(this.env, ROOT_DB_NAME);

      newDatabase(this.env, CLIENT_STATE_DB_NAME);
      newDatabase(this.env, TRANSACTION_DB_NAME);
      newDatabase(this.env, STRING_INDEX_DB_NAME);
      newDatabase(this.env, CLASS_DB_NAME);
      newDatabase(this.env, MAP_DB_NAME);
      newDatabase(this.env, CLUSTER_STATE_STORE);
    } catch (final DatabaseException e) {
      this.status = STATUS_ERROR;
      forceClose();
      throw new TCDatabaseException(e);
    } catch (final Error e) {
      this.status = STATUS_ERROR;
      forceClose();
      throw e;
    } catch (final RuntimeException e) {
      this.status = STATUS_ERROR;
      forceClose();
      throw e;
    }

    this.status = STATUS_OPEN;
    return this.openResult;
  }

  private void cinfo(final Object message) {
    clogger.info("DB Environment: " + message);
  }

  public synchronized void close() throws TCDatabaseException {
    assertOpen();
    this.status = STATUS_CLOSING;
    cinfo("Closing...");

    try {
      for (final Iterator i = this.createdDatabases.iterator(); i.hasNext();) {
        final Database db = (Database) i.next();
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
    } catch (final Exception de) {
      throw new TCDatabaseException(de.getMessage());
    }
    this.controlDB = null;
    this.env = null;

    this.status = STATUS_CLOSED;
    cinfo("Closed.");
  }

  public synchronized boolean isOpen() {
    return STATUS_OPEN.equals(this.status);
  }

  // This is for testing and cleanup on error.
  synchronized void forceClose() {
    final List toClose = new ArrayList(this.createdDatabases);
    toClose.add(this.controlDB);
    for (final Iterator i = toClose.iterator(); i.hasNext();) {
      try {
        final Database db = (Database) i.next();
        if (db != null) {
          db.close();
        }
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }

    try {
      if (this.catalog != null) {
        this.catalog.close();
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }

    try {
      if (this.env != null) {
        this.env.close();
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  public File getEnvironmentHome() {
    return this.envHome;
  }

  public synchronized Environment getEnvironment() throws TCDatabaseException {
    assertOpen();
    return this.env;
  }

  public EnvironmentStats getStats(final StatsConfig config) throws TCDatabaseException {
    try {
      return this.env.getStats(config);
    } catch (final Exception e) {
      throw new TCDatabaseException(e.getMessage());
    }
  }

  public synchronized Database getObjectDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) this.databasesByName.get(OBJECT_DB_NAME);
  }

  public synchronized Database getObjectOidStoreDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) this.databasesByName.get(OBJECT_OID_STORE_DB_NAME);
  }

  public synchronized Database getMapsOidStoreDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) this.databasesByName.get(MAPS_OID_STORE_DB_NAME);
  }

  public synchronized Database getEvictableOidStoreDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) this.databasesByName.get(EVICTABLE_OID_STORE_DB_NAME);
  }

  public synchronized Database getOidStoreLogDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) this.databasesByName.get(OID_STORE_LOG_DB_NAME);
  }

  public synchronized ClassCatalogWrapper getClassCatalogWrapper() throws TCDatabaseException {
    assertOpen();
    return this.catalog;
  }

  public synchronized Database getRootDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) this.databasesByName.get(ROOT_DB_NAME);
  }

  public Database getClientStateDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) this.databasesByName.get(CLIENT_STATE_DB_NAME);
  }

  public Database getTransactionDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) this.databasesByName.get(TRANSACTION_DB_NAME);
  }

  public Database getGlobalSequenceDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) this.databasesByName.get(GLOBAL_SEQUENCE_DATABASE);
  }

  public Database getClassDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) this.databasesByName.get(CLASS_DB_NAME);
  }

  public Database getMapsDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) this.databasesByName.get(MAP_DB_NAME);
  }

  public Database getStringIndexDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) this.databasesByName.get(STRING_INDEX_DB_NAME);
  }

  public Database getClusterStateStoreDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) this.databasesByName.get(CLUSTER_STATE_STORE);
  }

  private void assertNotError() throws TCDatabaseException {
    if (STATUS_ERROR == this.status) { throw new TCDatabaseException(
                                                                     "Attempt to operate on an environment in an error state."); }
  }

  private void assertOpening() {
    if (STATUS_OPENING != this.status) { throw new AssertionError("Database environment should be opening but isn't"); }
  }

  private void assertOpen() throws TCDatabaseException {
    assertNotError();
    if (STATUS_OPEN != this.status) { throw new DatabaseNotOpenException(
                                                                         "Database environment should be open but isn't."); }
  }

  private void assertClosing() {
    if (STATUS_CLOSING != this.status) { throw new AssertionError("Database environment should be closing but isn't"); }
  }

  private boolean isClean() throws TCDatabaseException {
    assertOpening();
    final DatabaseEntry value = new DatabaseEntry(new byte[] { 0 });
    final Transaction tx = newTransaction();
    OperationStatus stat;
    try {
      stat = this.controlDB.get(tx, CLEAN_FLAG_KEY, value, LockMode.DEFAULT);
      tx.commit();
    } catch (final Exception e) {
      throw new TCDatabaseException(e.getMessage());
    }
    return OperationStatus.NOTFOUND.equals(stat)
           || (OperationStatus.SUCCESS.equals(stat) && value.getData()[0] == IS_CLEAN);
  }

  private void setDirty() throws TCDatabaseException {
    assertOpening();
    final DatabaseEntry value = new DatabaseEntry(new byte[] { IS_DIRTY });
    final Transaction tx = newTransaction();
    OperationStatus stat;
    try {
      stat = this.controlDB.put(tx, CLEAN_FLAG_KEY, value);
    } catch (final Exception e) {
      throw new TCDatabaseException(e.getMessage());
    }
    if (!OperationStatus.SUCCESS.equals(stat)) { throw new TCDatabaseException("Unexpected operation status "
                                                                               + "trying to unset clean flag: " + stat); }
    try {
      tx.commitSync();
    } catch (final Exception e) {
      throw new TCDatabaseException(e.getMessage());
    }
  }

  private Transaction newTransaction() throws TCDatabaseException {
    try {
      final Transaction tx = this.env.beginTransaction(null, null);
      return tx;
    } catch (final Exception de) {
      throw new TCDatabaseException(de.getMessage());
    }
  }

  private void setClean() throws TCDatabaseException {
    assertClosing();
    final DatabaseEntry value = new DatabaseEntry(new byte[] { IS_CLEAN });
    final Transaction tx = newTransaction();
    OperationStatus stat;
    try {
      stat = this.controlDB.put(tx, CLEAN_FLAG_KEY, value);
    } catch (final Exception e) {
      throw new TCDatabaseException(e.getMessage());
    }
    if (!OperationStatus.SUCCESS.equals(stat)) { throw new TCDatabaseException("Unexpected operation status "
                                                                               + "trying to set clean flag: " + stat); }
    try {
      tx.commitSync();
    } catch (final Exception e) {
      throw new TCDatabaseException(e.getMessage());
    }
  }

  private void newDatabase(final Environment e, final String name) throws TCDatabaseException {
    try {
      final Database db = e.openDatabase(null, name, this.dbcfg);
      this.createdDatabases.add(db);
      this.databasesByName.put(name, db);
    } catch (final Exception de) {
      throw new TCDatabaseException(de.getMessage());
    }
  }

  private Environment openEnvironment() throws TCDatabaseException {
    int count = 0;
    while (true) {
      try {
        return new Environment(this.envHome, this.ecfg);
      } catch (final Exception dbe) {
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

  private static final class DBEnvironmentStatus {
    private final String description;

    DBEnvironmentStatus(final String desc) {
      this.description = desc;
    }

    @Override
    public String toString() {
      return this.description;
    }
  }

  public static final class ClassCatalogWrapper {

    private final StoredClassCatalog catalog;
    private boolean                  closed = false;

    private ClassCatalogWrapper(final Environment env, final DatabaseConfig cfg) throws DatabaseException {
      this.catalog = new StoredClassCatalog(env.openDatabase(null, "java_class_catalog", cfg));
    }

    public final ClassCatalog getClassCatalog() {
      return this.catalog;
    }

    synchronized void close() throws DatabaseException {
      if (this.closed) { throw new IllegalStateException("Already closed."); }
      this.catalog.close();
      this.closed = true;
    }
  }

}