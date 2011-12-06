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
import com.tc.objectserver.storage.api.TCLongToBytesDatabase;
import com.tc.objectserver.storage.api.TCLongToStringDatabase;
import com.tc.objectserver.storage.api.TCMapsDatabase;
import com.tc.objectserver.storage.api.TCRootDatabase;
import com.tc.objectserver.storage.api.TCStringToStringDatabase;
import com.tc.objectserver.storage.api.TCTransactionStoreDatabase;
import com.tc.properties.TCPropertiesConsts;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.util.sequence.MutableSequence;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class DerbyDBEnvironment implements DBEnvironment {
  public static final String               DRIVER                         = "org.apache.derby.jdbc.EmbeddedDriver";
  public static final String               PROTOCOL                       = "jdbc:derby:";
  public static final String               DB_NAME                        = "datadb";
  private static final int                 DEFAULT_PAGE_SIZE              = 32768;
  private static final Map<Integer, Float> PAGE_CACHE_OVERHEAD_FACTOR_MAP = new HashMap<Integer, Float>() {
                                                                            {
                                                                              put(4096, 0.67F);
                                                                              put(8192, 0.753F);
                                                                              put(16384, 0.80F);
                                                                              put(32768, 0.826F);
                                                                            }
                                                                          };
  private static final float               LOG_BUFFER_HEAP_PERCENTAGE     = 0.1f;
  private static final int                 NUMBER_OF_LOG_BUFFERS          = 3;
  private static final int                 MAX_LOG_BUFFER_SIZE            = 50 * 1024 * 1024;

  private final Map                        tables                         = new HashMap();
  private final Properties                 derbyProps;
  private final QueryProvider              queryProvider                  = new DerbyQueryProvider();
  private final boolean                    isParanoid;
  private final String                     logDevice;
  private final File                       envHome;
  private DBEnvironmentStatus              status;
  private DerbyControlDB                   controlDB;
  private final SampledCounter             l2FaultFromDisk;

  private static final TCLogger            logger                         = TCLogging
                                                                              .getLogger(DerbyDBEnvironment.class);

  public DerbyDBEnvironment(boolean paranoid, File home) throws IOException {
    this(paranoid, home, new Properties(), SampledCounter.NULL_SAMPLED_COUNTER, false);
  }

  public DerbyDBEnvironment(boolean paranoid, File home, Properties props, SampledCounter l2FaultFromDisk,
                            boolean offheapEnabled) throws IOException {
    this.isParanoid = paranoid;
    this.envHome = home;
    this.derbyProps = props;
    this.l2FaultFromDisk = l2FaultFromDisk;

    if (!isParanoidMode() && offheapEnabled) {
      final Integer newDerbyMemPercentage = Integer.parseInt(derbyProps
          .getProperty(TCPropertiesConsts.DERBY_MAXMEMORYPERCENT)) / 3;
      derbyProps.setProperty(TCPropertiesConsts.DERBY_MAXMEMORYPERCENT, newDerbyMemPercentage.toString());
      logger.info("Since running OffHeap in temp-swap mode, setting " + TCPropertiesConsts.DERBY_MAXMEMORYPERCENT
                  + " to " + newDerbyMemPercentage.toString());
    }

    cleanLogDeviceIfRequired(paranoid);

    FileUtils.forceMkdir(this.envHome);
    logger.info("Using DERBY DBEnvironment ...");

    logDevice = derbyProps.getProperty(TCPropertiesConsts.DERBY_LOG_DEVICE, envHome.getAbsolutePath());
    logger.info("Writing derby transaction logs to " + logDevice);

    if (!derbyProps.containsKey(TCPropertiesConsts.DERBY_STORAGE_PAGESIZE)
        || !PAGE_CACHE_OVERHEAD_FACTOR_MAP.containsKey(Integer.parseInt(derbyProps
            .getProperty(TCPropertiesConsts.DERBY_STORAGE_PAGESIZE)))) {
      logger.info(TCPropertiesConsts.DERBY_STORAGE_PAGESIZE + " unset. Setting to a default of " + DEFAULT_PAGE_SIZE
                  + ".");
      derbyProps.setProperty(TCPropertiesConsts.DERBY_STORAGE_PAGESIZE, "" + DEFAULT_PAGE_SIZE);
    }

    // Perform heap size calculation to determine how many cache pages to give derby
    if (derbyProps.containsKey(TCPropertiesConsts.DERBY_MAXMEMORYPERCENT)) {
      // Heap memory to give to Derby
      float heapUsage = Runtime.getRuntime().maxMemory()
                        * (Float.parseFloat(derbyProps.getProperty(TCPropertiesConsts.DERBY_MAXMEMORYPERCENT)) / 100.0f);

      // Calculate log buffer size
      int logBufferMemory = Math.min((int) (heapUsage * LOG_BUFFER_HEAP_PERCENTAGE), MAX_LOG_BUFFER_SIZE);
      logger.info("Setting derby transaction log buffer size to " + logBufferMemory / (1024 * 1024) + "M.");
      derbyProps.setProperty(TCPropertiesConsts.DERBY_LOG_BUFFER_SIZE, "" + (logBufferMemory / NUMBER_OF_LOG_BUFFERS));

      // Calculate page cache size
      int pageSize = Integer.parseInt(derbyProps.getProperty(TCPropertiesConsts.DERBY_STORAGE_PAGESIZE));
      float pageMemory = heapUsage - logBufferMemory;
      float cacheOverheadFactor = PAGE_CACHE_OVERHEAD_FACTOR_MAP.get(pageSize);
      // Total number of pages is reduced by a factor (depending on the page size) to account for derby's cache
      // overhead.
      int pageCacheSize = (int) ((pageMemory * cacheOverheadFactor) / pageSize);
      if (derbyProps.containsKey(TCPropertiesConsts.DERBY_STORAGE_PAGECACHESIZE)) {
        logger.warn(TCPropertiesConsts.L2_DERBYDB_DERBY_STORAGE_PAGECACHESIZE + " overridden by setting "
                    + TCPropertiesConsts.L2_DERBYDB_MAXMEMORYPERCENT + ".");
      }
      logger.info("Setting derby page cache heap usage to " + pageMemory / (1024 * 1024) + "M (" + pageCacheSize
                  + " pages).");
      derbyProps.setProperty(TCPropertiesConsts.DERBY_STORAGE_PAGECACHESIZE, "" + pageCacheSize);
    }

    Properties p = System.getProperties();
    p.setProperty("derby.system.home", this.envHome.getAbsolutePath());

    if (!isParanoid) {
      derbyProps.setProperty(TCPropertiesConsts.DERBY_SYSTEM_DURABILITY, "test");
    } else {
      derbyProps.remove(TCPropertiesConsts.DERBY_SYSTEM_DURABILITY);
    }

    derbyProps.setProperty(TCPropertiesConsts.DERBY_STREAM_ERROR_METHOD,
                           "com.tc.objectserver.storage.derby.DerbyDBEnvironment.getWriter");
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

  private void cleanLogDeviceIfRequired(boolean paranoid) {
    if (derbyProps.containsKey(TCPropertiesConsts.DERBY_LOG_DEVICE) && !paranoid) {
      // clean logDevice if not already empty
      String logDeviceDir = derbyProps.getProperty(TCPropertiesConsts.DERBY_LOG_DEVICE);
      try {
        File f = new File(logDeviceDir);
        if (f.isDirectory() && f.list().length > 0) {
          FileUtils.cleanDirectory(f);
        }
      } catch (Throwable t) {
        logger.warn("Error while deleting logDevice directory " + logDeviceDir);
      }
    }
  }

  public static boolean tableExists(Connection connection, String table) throws SQLException {
    DatabaseMetaData dbmd = connection.getMetaData();
    ResultSet resultSet = dbmd.getTables(null, null, table.toUpperCase(), null);
    boolean tableExists = resultSet.next();
    resultSet.close();
    connection.commit();
    return tableExists;
  }

  public synchronized boolean open() throws TCDatabaseException {
    boolean openResult;
    try {
      if (status == DBEnvironmentStatus.STATUS_OPEN) return true;

      status = DBEnvironmentStatus.STATUS_OPENING;
      openDatabase();

      // now open control db
      controlDB = new DerbyControlDB(CONTROL_DB, createConnection(), queryProvider);
      openResult = controlDB.isClean();
      if (!openResult) {
        this.status = DBEnvironmentStatus.STATUS_INIT;
        forceClose();
        return openResult;
      }

      if (!this.isParanoid) {
        this.controlDB.setDirty();
      }

      createTablesIfRequired();

      status = DBEnvironmentStatus.STATUS_OPEN;
      return openResult;

    } finally {
      if (status == DBEnvironmentStatus.STATUS_OPENING) {
        status = DBEnvironmentStatus.STATUS_ERROR;
        forceClose();
      }
    }

  }

  public void openDatabase() throws TCDatabaseException {
    // loading the Driver
    try {
      Class.forName(DRIVER).newInstance();
    } catch (ClassNotFoundException cnfe) {
      String message = "Unable to load the JDBC driver " + DRIVER;
      throw new TCDatabaseException(message, cnfe);
    } catch (InstantiationException ie) {
      String message = "Unable to instantiate the JDBC driver " + DRIVER;
      throw new TCDatabaseException(message, ie);
    } catch (IllegalAccessException iae) {
      String message = "Not allowed to access the JDBC driver " + DRIVER;
      throw new TCDatabaseException(message, iae);
    }

    Properties attributesProps = new Properties();
    attributesProps.setProperty("create", "true");
    attributesProps.setProperty("logDevice", logDevice);
    Connection conn = null;
    ThreadDumpOnTimeout td = new ThreadDumpOnTimeout(Thread.currentThread(), 20 * 1000);
    try {
      td.start();
      conn = DriverManager.getConnection(PROTOCOL + envHome.getAbsolutePath() + File.separator + DB_NAME + ";",
                                         attributesProps);
      conn.close();
    } catch (SQLException e) {
      String errorMessage;
      if (e.getErrorCode() == 40000 && e.getSQLState().equals("XJ040")
          && e.getNextException().getSQLState().equals("XSLAB")) {
        errorMessage = e.getNextException().getMessage() + "\n";
      } else {
        errorMessage = "Error opening derby database. Try restarting server after cleaning up derby data directory and logDevice directory . ErrorCode: "
                       + e.getErrorCode() + "; SQLState: " + e.getSQLState() + "\n";
      }
      logger.fatal(errorMessage, e);
      throw new TCDatabaseException(errorMessage);
    } finally {
      td.cancel();
    }
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
    newBytesToBytesDB(OBJECT_OID_STORE_DB_NAME, connection);
    newBytesToBytesDB(MAPS_OID_STORE_DB_NAME, connection);
    newBytesToBlobDB(OID_STORE_LOG_DB_NAME, connection);
    newBytesToBytesDB(EVICTABLE_OID_STORE_DB_NAME, connection);
    newLongDB(CLIENT_STATE_DB_NAME, connection);
    newTransactionStoreDatabase(connection);
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
    TCLongToBytesDatabase db = new DerbyTCLongToBytesDatabase(OBJECT_DB_NAME, connection, queryProvider,
                                                              l2FaultFromDisk);
    tables.put(OBJECT_DB_NAME, db);
  }

  private void newRootDB(Connection connection) throws TCDatabaseException {
    TCRootDatabase db = new DerbyTCRootDatabase(ROOT_DB_NAME, connection, queryProvider);
    tables.put(ROOT_DB_NAME, db);
  }

  private void newBytesToBlobDB(String tableName, Connection connection) throws TCDatabaseException {
    TCBytesToBytesDatabase db = new DerbyTCBytesToBlobDatabase(tableName, connection, queryProvider);
    tables.put(tableName, db);
  }

  private void newBytesToBytesDB(String tableName, Connection connection) throws TCDatabaseException {
    TCBytesToBytesDatabase db = new DerbyTCBytesToBytesDatabase(tableName, connection, queryProvider);
    tables.put(tableName, db);
  }

  private void newTransactionStoreDatabase(Connection connection) throws TCDatabaseException {
    TCTransactionStoreDatabase db = new DerbyTCLongToBytesDatabase(TRANSACTION_DB_NAME, connection, queryProvider);
    tables.put(TRANSACTION_DB_NAME, db);
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

  public synchronized void close() throws TCDatabaseException {
    status = DBEnvironmentStatus.STATUS_CLOSING;
    forceClose();
    status = DBEnvironmentStatus.STATUS_CLOSED;
  }

  private void forceClose() throws TCDatabaseException {
    try {
      Connection connection = DriverManager.getConnection(PROTOCOL + envHome.getAbsolutePath() + File.separator
                                                          + DB_NAME + ";shutdown=true");
      connection.close();
    } catch (SQLException e) {
      if ("08006".equals(e.getSQLState())) {
        logger.info("Derby shut down normally.");
      } else {
        throw new TCDatabaseException(e);
      }
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

  public synchronized TCLongToBytesDatabase getObjectDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCLongToBytesDatabase) tables.get(OBJECT_DB_NAME);
  }

  public synchronized TCBytesToBytesDatabase getObjectOidStoreDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCBytesToBlobDatabase) tables.get(OBJECT_OID_STORE_DB_NAME);
  }

  public synchronized TCBytesToBytesDatabase getMapsOidStoreDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCBytesToBlobDatabase) tables.get(MAPS_OID_STORE_DB_NAME);
  }

  public synchronized TCBytesToBytesDatabase getOidStoreLogDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCBytesToBlobDatabase) tables.get(OID_STORE_LOG_DB_NAME);
  }

  public synchronized TCBytesToBytesDatabase getEvictableOidStoreDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCBytesToBlobDatabase) tables.get(EVICTABLE_OID_STORE_DB_NAME);
  }

  public synchronized TCRootDatabase getRootDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCRootDatabase) tables.get(ROOT_DB_NAME);
  }

  public synchronized TCLongDatabase getClientStateDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCLongDatabase) tables.get(CLIENT_STATE_DB_NAME);
  }

  public synchronized TCTransactionStoreDatabase getTransactionDatabase() throws TCDatabaseException {
    assertOpen();
    return (TCTransactionStoreDatabase) tables.get(TRANSACTION_DB_NAME);
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
    ((DerbyServerDBBackup) mBean).initDbEnvironment(this.envHome);
  }

  public void initObjectStoreStats(ManagedObjectStoreStats objectStoreStats) {
    //
  }

  public static Writer getWriter() {
    return WRITER;
  }

  private static class ThreadDumpOnTimeout {
    private final Timer  timer = new Timer("ThreadDumpOnTimeout");
    private final Thread thread;
    private final long   timeout;

    private ThreadDumpOnTimeout(Thread thread, long timeout) {
      this.thread = thread;
      this.timeout = timeout;
    }

    private void start() {
      timer.schedule(new TimerTask() {
        @Override
        public void run() {
          logger.info("Exceeded timeout of " + timeout + "ms. Stack trace for thread " + thread.getName());
          for (StackTraceElement ste : thread.getStackTrace()) {
            logger.info(ste.toString());
          }
        }
      }, timeout);
    }

    private void cancel() {
      timer.cancel();
    }
  }

  private static final Writer WRITER = new Writer() {
                                       private final TCLogger derbyLogger = TCLogging.getDerbyLogger();

                                       @Override
                                       public void close() {
                                         // do nothing
                                       }

                                       @Override
                                       public void flush() {
                                         // do nothing
                                       }

                                       @Override
                                       public void write(char[] cbuf, int off, int len) {
                                         derbyLogger.info(new String(cbuf, off, len));
                                       }
                                     };

  public void printDatabaseStats(Writer writer) throws Exception {
    new DerbyStatisticsHandler(this, writer).report();
  }

  Map getTables() {
    return this.tables;
  }
}
