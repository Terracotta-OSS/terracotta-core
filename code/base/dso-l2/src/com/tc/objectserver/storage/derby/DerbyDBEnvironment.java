/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import org.apache.commons.io.FileUtils;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.object.ServerDBBackupMBean;
import com.tc.objectserver.persistence.api.ManagedObjectStoreStats;
import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.persistence.db.DatabaseNotOpenException;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.api.OffheapStats;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.api.TCBytesToBytesDatabase;
import com.tc.objectserver.storage.api.TCIntToBytesDatabase;
import com.tc.objectserver.storage.api.TCLongDatabase;
import com.tc.objectserver.storage.api.TCLongToStringDatabase;
import com.tc.objectserver.storage.api.TCMapsDatabase;
import com.tc.objectserver.storage.api.TCObjectDatabase;
import com.tc.objectserver.storage.api.TCRootDatabase;
import com.tc.objectserver.storage.api.TCStringToStringDatabase;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.util.sequence.MutableSequence;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DerbyDBEnvironment implements DBEnvironment {
  public static final String    DRIVER        = "org.apache.derby.jdbc.EmbeddedDriver";
  public static final String    PROTOCOL      = "jdbc:derby:";
  public static final String    DB_NAME       = "objectDB";
  private static final Object   CONTROL_LOCK  = new Object();

  private final Map             tables        = new HashMap();
  private final Properties      derbyProps;
  private final QueryProvider   queryProvider = new DerbyQueryProvider();
  private final boolean         isParanoid;
  private final File            envHome;
  private DBEnvironmentStatus   status;
  private DerbyControlDB        controlDB;
  private final SampledCounter  l2FaultFromDisk;

  private static final TCLogger logger        = TCLogging.getLogger(DerbyDBEnvironment.class);

  public DerbyDBEnvironment(boolean paranoid, File home, SampledCounter l2FaultFromDisk) throws IOException {
    this(paranoid, home, new Properties(), l2FaultFromDisk);
  }

  public DerbyDBEnvironment(boolean paranoid, File home, Properties props, SampledCounter l2FaultFromDisk)
      throws IOException {
    this.isParanoid = paranoid;
    this.envHome = home;
    this.derbyProps = props;
    this.l2FaultFromDisk = l2FaultFromDisk;
    FileUtils.forceMkdir(this.envHome);
    logger.info("Using DERBY DBEnvironment ...");
    Properties p = System.getProperties();
    p.setProperty("derby.system.home", this.envHome.getAbsolutePath());
    File derbyPropsFile = new File(this.envHome.getAbsoluteFile() + File.separator + "derby.properties");
    if (!derbyProps.isEmpty() && !derbyPropsFile.exists()) {
      FileOutputStream fos = new FileOutputStream(derbyPropsFile);
      try {
        derbyProps.store(fos, "Derby Properties File");
        logger.info("Derby Properties file created with: " + derbyProps);
      } finally {
        fos.close();
      }
    }
  }

  public static boolean tableExists(Connection connection, String table) throws SQLException {
    DatabaseMetaData dbmd = connection.getMetaData();

    String[] types = { "TABLE" };
    ResultSet resultSet = dbmd.getTables(null, null, "%", types);
    while (resultSet.next()) {
      String tableName = resultSet.getString(3);
      if (tableName.equalsIgnoreCase(table)) {
        resultSet.close();
        connection.commit();
        return true;
      }
    }
    return false;
  }

  public synchronized boolean open() throws TCDatabaseException {
    boolean openResult;
    try {
      openDatabase();

      status = DBEnvironmentStatus.STATUS_OPENING;

      // now open control db
      synchronized (CONTROL_LOCK) {
        controlDB = new DerbyControlDB(CONTROL_DB, createConnection(), queryProvider);
        openResult = controlDB.isClean();
        if (!openResult) {
          this.status = DBEnvironmentStatus.STATUS_INIT;
          forceClose();
          return openResult;
        }
      }

      if (!this.isParanoid) {
        this.controlDB.setDirty();
      }

      createTablesIfRequired();

    } catch (TCDatabaseException e) {
      this.status = DBEnvironmentStatus.STATUS_ERROR;
      forceClose();
      throw e;
    } catch (Error e) {
      this.status = DBEnvironmentStatus.STATUS_ERROR;
      forceClose();
      throw e;
    } catch (RuntimeException e) {
      this.status = DBEnvironmentStatus.STATUS_ERROR;
      forceClose();
      throw e;
    }

    status = DBEnvironmentStatus.STATUS_OPEN;
    return openResult;
  }

  public void createDatabase() throws TCDatabaseException {
    // loading the Driver
    try {
      Class.forName(DRIVER).newInstance();
    } catch (ClassNotFoundException cnfe) {
      String message = "Unable to load the JDBC driver " + DRIVER;
      throw new TCDatabaseException(message);
    } catch (InstantiationException ie) {
      String message = "Unable to instantiate the JDBC driver " + DRIVER;
      throw new TCDatabaseException(message);
    } catch (IllegalAccessException iae) {
      String message = "Not allowed to access the JDBC driver " + DRIVER;
      throw new TCDatabaseException(message);
    }

    Properties attributesProps = new Properties();
    attributesProps.put("create", "true");
    Connection conn;
    try {
      conn = DriverManager.getConnection(PROTOCOL + envHome.getAbsolutePath() + File.separator + DB_NAME
                                         + ";logDevice=" + envHome.getAbsolutePath(), attributesProps);
      conn.setAutoCommit(false);
      conn.close();
    } catch (SQLException e) {
      throw new TCDatabaseException(e);
    }
  }

  public void openDatabase() throws TCDatabaseException {
    createDatabase();
  }

  protected Connection createConnection() throws TCDatabaseException {
    try {
      Connection connection = DriverManager.getConnection(PROTOCOL + envHome.getAbsolutePath() + File.separator
                                                          + DB_NAME + ";");
      connection.setAutoCommit(false);
      return connection;
    } catch (SQLException e) {
      throw new TCDatabaseException(e);
    }
  }

  private void createTablesIfRequired() throws TCDatabaseException {
    Connection connection = createConnection();

    newObjectDB(connection);
    newRootDB(connection);
    newBytesToBlobDB(OBJECT_OID_STORE_DB_NAME, connection);
    newBytesToBlobDB(MAPS_OID_STORE_DB_NAME, connection);
    newBytesToBlobDB(OID_STORE_LOG_DB_NAME, connection);
    newBytesToBlobDB(EVICTABLE_OID_STORE_DB_NAME, connection);
    newLongDB(CLIENT_STATE_DB_NAME, connection);
    newBytesToBlobDB(TRANSACTION_DB_NAME, connection);
    newIntToBytesDB(CLASS_DB_NAME, connection);
    newLongToStringDB(STRING_INDEX_DB_NAME, connection);
    newStringToStringDB(CLUSTER_STATE_STORE, connection);
    newMapsDatabase(connection);

    try {
      DerbyDBSequence.createSequenceTable(connection);
    } catch (SQLException e) {
      try {
        connection.rollback();
        connection.close();
      } catch (SQLException e1) {
        throw new TCDatabaseException(e1);
      }
      throw new TCDatabaseException(e);
    }

    try {
      connection.close();
    } catch (SQLException e) {
      throw new TCDatabaseException(e);
    }
  }

  private void newObjectDB(Connection connection) throws TCDatabaseException {
    TCObjectDatabase db = new DerbyTCObjectDatabase(OBJECT_DB_NAME, connection, queryProvider, l2FaultFromDisk);
    tables.put(OBJECT_DB_NAME, db);
  }

  private void newRootDB(Connection connection) throws TCDatabaseException {
    TCRootDatabase db = new DerbyTCRootDatabase(ROOT_DB_NAME, connection, queryProvider);
    tables.put(ROOT_DB_NAME, db);
  }

  private void newBytesToBlobDB(String tableName, Connection connection) throws TCDatabaseException {
    TCBytesToBytesDatabase db = new DerbyTCBytesToBlobDB(tableName, connection, queryProvider);
    tables.put(tableName, db);
  }

  private void newLongDB(String tableName, Connection connection) throws TCDatabaseException {
    TCLongDatabase db = new DerbyTCLongDatabase(tableName, connection, queryProvider);
    tables.put(tableName, db);
  }

  private void newIntToBytesDB(String tableName, Connection connection) throws TCDatabaseException {
    TCIntToBytesDatabase db = new DerbyTCIntToBytesDatabase(tableName, connection, queryProvider);
    tables.put(tableName, db);
  }

  private void newLongToStringDB(String tableName, Connection connection) throws TCDatabaseException {
    TCLongToStringDatabase db = new DerbyTCLongToStringDatabase(tableName, connection, queryProvider);
    tables.put(tableName, db);
  }

  private void newStringToStringDB(String tableName, Connection connection) throws TCDatabaseException {
    TCStringToStringDatabase db = new DerbyTCStringToStringDatabase(tableName, connection, queryProvider);
    tables.put(tableName, db);
  }

  private void newMapsDatabase(Connection connection) throws TCDatabaseException {
    TCMapsDatabase db = new DerbyTCMapsDatabase(MAP_DB_NAME, connection, queryProvider);
    tables.put(MAP_DB_NAME, db);
  }

  public synchronized void close() {
    status = DBEnvironmentStatus.STATUS_CLOSING;

    forceClose();

    status = DBEnvironmentStatus.STATUS_CLOSED;
  }

  private void forceClose() {
    try {
      Connection connection = DriverManager.getConnection(PROTOCOL + envHome.getAbsolutePath() + File.separator
                                                          + DB_NAME + ";logDevice=" + envHome.getAbsolutePath()
                                                          + ";shutdown=true");
      connection.close();
    } catch (Exception e) {
      logger.info("Shutdown" + e.getMessage());
    }
  }

  public synchronized boolean isOpen() {
    return status == DBEnvironmentStatus.STATUS_OPEN;
  }

  public File getEnvironmentHome() {
    return envHome;
  }

  public static final String getClusterStateStoreName() {
    return CLUSTER_STATE_STORE;
  }

  public synchronized TCObjectDatabase getObjectDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCObjectDatabase) tables.get(OBJECT_DB_NAME);
  }

  public synchronized TCBytesToBytesDatabase getObjectOidStoreDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCBytesToBlobDB) tables.get(OBJECT_OID_STORE_DB_NAME);
  }

  public synchronized TCBytesToBytesDatabase getMapsOidStoreDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCBytesToBlobDB) tables.get(MAPS_OID_STORE_DB_NAME);
  }

  public synchronized TCBytesToBytesDatabase getOidStoreLogDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCBytesToBlobDB) tables.get(OID_STORE_LOG_DB_NAME);
  }

  public synchronized TCBytesToBytesDatabase getEvictableOidStoreDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCBytesToBlobDB) tables.get(EVICTABLE_OID_STORE_DB_NAME);
  }

  public synchronized TCRootDatabase getRootDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCRootDatabase) tables.get(ROOT_DB_NAME);
  }

  public synchronized TCLongDatabase getClientStateDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCLongDatabase) tables.get(CLIENT_STATE_DB_NAME);
  }

  public synchronized TCBytesToBytesDatabase getTransactionDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCBytesToBlobDB) tables.get(TRANSACTION_DB_NAME);
  }

  public synchronized TCIntToBytesDatabase getClassDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCIntToBytesDatabase) tables.get(CLASS_DB_NAME);
  }

  public synchronized TCMapsDatabase getMapsDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCMapsDatabase) tables.get(MAP_DB_NAME);
  }

  public synchronized TCLongToStringDatabase getStringIndexDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCLongToStringDatabase) tables.get(STRING_INDEX_DB_NAME);
  }

  public synchronized TCStringToStringDatabase getClusterStateStoreDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCStringToStringDatabase) tables.get(CLUSTER_STATE_STORE);
  }

  public MutableSequence getSequence(PersistenceTransactionProvider ptxp, TCLogger log, String sequenceID,
                                     int startValue) {
    try {
      return new DerbyDBSequence((DerbyPersistenceTransactionProvider) getPersistenceTransactionProvider(), sequenceID,
                                 startValue);
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public PersistenceTransactionProvider getPersistenceTransactionProvider() {
    return new DerbyPersistenceTransactionProvider(this);
  }

  public boolean isParanoidMode() {
    return isParanoid;
  }

  private void assertOpen() throws DatabaseNotOpenException {
    if (DBEnvironmentStatus.STATUS_OPEN != status) throw new DatabaseNotOpenException(
                                                                                      "Database environment should be open but isn't.");
  }

  public OffheapStats getOffheapStats() {
    return OffheapStats.NULL_OFFHEAP_STATS;
  }

  public StatisticRetrievalAction[] getSRAs() {
    return new StatisticRetrievalAction[] {};
  }

  public void initBackupMbean(ServerDBBackupMBean mBean) {
    // TODO: no db backup
  }

  public void initObjectStoreStats(ManagedObjectStoreStats objectStoreStats) {
    //
  }
}
