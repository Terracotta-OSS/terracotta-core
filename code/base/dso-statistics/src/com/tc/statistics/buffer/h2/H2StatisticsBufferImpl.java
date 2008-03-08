/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.buffer.h2;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArraySet;

import com.tc.properties.TCPropertiesImpl;
import com.tc.statistics.StatisticData;
import com.tc.statistics.buffer.StatisticsBuffer;
import com.tc.statistics.buffer.StatisticsBufferListener;
import com.tc.statistics.buffer.StatisticsConsumer;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferCaptureSessionCreationErrorException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferDatabaseCloseErrorException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferDatabaseOpenErrorException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferInstallationErrorException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferSetupErrorException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferStartCapturingErrorException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferStartCapturingSessionNotFoundException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferStatisticConsumptionErrorException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferStatisticStorageErrorException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferStopCapturingErrorException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferStopCapturingSessionNotFoundException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferUnknownCaptureSessionException;
import com.tc.statistics.config.StatisticsConfig;
import com.tc.statistics.database.StatisticsDatabase;
import com.tc.statistics.database.exceptions.TCStatisticsDatabaseException;
import com.tc.statistics.database.impl.H2StatisticsDatabase;
import com.tc.statistics.jdbc.CaptureChecksum;
import com.tc.statistics.jdbc.JdbcHelper;
import com.tc.statistics.jdbc.PreparedStatementHandler;
import com.tc.statistics.jdbc.ResultSetHandler;
import com.tc.statistics.retrieval.StatisticsRetriever;
import com.tc.statistics.retrieval.impl.StatisticsRetrieverImpl;
import com.tc.util.Assert;
import com.tc.util.concurrent.FileLockGuard;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class H2StatisticsBufferImpl implements StatisticsBuffer {
  public final static int DATABASE_STRUCTURE_VERSION = 3;
  
  private final static long DATABASE_STRUCTURE_CHECKSUM = 1001158033L;

  public final static String H2_URL_SUFFIX = "statistics-buffer";

  private final static String SQL_NEXT_LOCALSESSIONID = "SELECT nextval('seq_localsession')";
  private final static String SQL_NEXT_STATISTICLOGID = "SELECT nextval('seq_statisticlog')";
  private final static String SQL_NEXT_CONSUMPTIONID = "SELECT nextval('seq_consumption')";
  private final static String SQL_MAKE_ALL_CONSUMABLE = "UPDATE statisticlog SET consumptionid = NULL";
  private static final String SQL_RETRIEVE_LOCAL_SESSIONID = "SELECT localsessionid FROM capturesession WHERE clustersessionid = ?";
  private static final String SQL_RETRIEVE_CAPTURESESSION = "SELECT * FROM capturesession WHERE clustersessionid = ?";
  private static final String SQL_STOP_CAPTURESESSION = "UPDATE capturesession SET stop = ? WHERE clustersessionid = ? AND start IS NOT NULL AND stop IS NULL";
  private static final String SQL_CREATE_CAPTURESESSION = "INSERT INTO capturesession (localsessionid, clustersessionid) VALUES (?, ?)";
  private static final String SQL_START_CAPTURESESSION = "UPDATE capturesession SET start = ? WHERE clustersessionid = ? AND start IS NULL";
  private final static String SQL_INSERT_STATISTICSDATA = "INSERT INTO statisticlog (id, localsessionid, agentip, agentdifferentiator, moment, statname, statelement, datanumber, datatext, datatimestamp, datadecimal) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
  private static final String SQL_MARK_FOR_CONSUMPTION_LIMIT = "MERGE INTO statisticlog(id, consumptionid) KEY(id) SELECT id, ? FROM statisticlog WHERE consumptionid IS NULL AND localsessionid = ? ORDER BY moment LIMIT ?";
  private static final String SQL_MARK_FOR_CONSUMPTION = "UPDATE statisticlog SET consumptionid = ? WHERE consumptionid IS NULL AND localsessionid = ?";
  private static final String SQL_CONSUME_STATISTICDATA = "SELECT * FROM statisticlog WHERE consumptionid = ? ORDER BY moment ASC, id ASC";
  private static final String SQL_RESET_CONSUMPTIONID = "UPDATE statisticlog SET consumptionid = NULL WHERE consumptionid = ?";

  private final StatisticsConfig config;
  private final File lockFile;
  private final StatisticsDatabase database;

  private volatile boolean open = false;
  private volatile String defaultAgentIp;
  private volatile String defaultAgentDifferentiator = null;

  private final Set listeners = new CopyOnWriteArraySet();

  private static final Random rand = new Random();

  public H2StatisticsBufferImpl(final StatisticsConfig config, final File dbDir) throws TCStatisticsBufferException {
    Assert.assertNotNull("config", config);
    final String suffix;
    if (TCPropertiesImpl.getProperties().getBoolean("cvt.buffer.randomsuffix.enabled", false)) {
      synchronized (rand) {
        suffix = H2_URL_SUFFIX + "-" + rand.nextInt() + "." + System.currentTimeMillis();
      }
    } else {
      suffix = H2_URL_SUFFIX;
    }
    this.database = new H2StatisticsDatabase(dbDir, suffix);
    this.config = config;
    this.lockFile = new File(dbDir, suffix+"-tc.lck");
    try {
      this.defaultAgentIp = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
     throw new TCStatisticsBufferException("Unexpected error while getting localhost address.", e);
    }
  }

  public void setDefaultAgentIp(String defaultAgentIp) {
    this.defaultAgentIp = defaultAgentIp;
  }

  public void setDefaultAgentDifferentiator(String defaultAgentDifferentiator) {
    this.defaultAgentDifferentiator = defaultAgentDifferentiator;
  }

  public String getDefaultAgentIp() {
    return defaultAgentIp;
  }

  public String getDefaultAgentDifferentiator() {
    return defaultAgentDifferentiator;
  }

  public synchronized void open() throws TCStatisticsBufferException {
    try {
      FileLockGuard.guard(lockFile, new FileLockGuard.Guarded() {
        public void execute() throws FileLockGuard.InnerException {
          try {
            try {
              database.open();
            } catch (TCStatisticsDatabaseException e) {
              throw new TCStatisticsBufferDatabaseOpenErrorException(e);
            }

            install();
            setupPreparedStatements();
            makeAllDataConsumable();
          } catch (TCStatisticsBufferException e) {
            throw new FileLockGuard.InnerException(e);
          }
        }
      });
    } catch (FileLockGuard.InnerException e) {
      throw (TCStatisticsBufferException)e.getInnerException();
    } catch (IOException e) {
      throw new TCStatisticsBufferException("Unexpected error while obtaining or releasing lock file.", e);
    }

    open = true;
  }

  public synchronized void close() throws TCStatisticsBufferException {
    try {
      database.close();
    } catch (TCStatisticsDatabaseException e) {
      throw new TCStatisticsBufferDatabaseCloseErrorException(e);
    }
    
    open = false;
  }

  public synchronized void reinitialize() throws TCStatisticsBufferException {
    boolean was_open = open;
    close();
    try {
      database.reinitialize();
    } catch (TCStatisticsDatabaseException e) {
      throw new TCStatisticsBufferException("Unexpected error while reinitializing the statistics buffer.", e);
    }
    if (was_open) {
      open();
    }
  }

  protected void install() throws TCStatisticsBufferException {
    try {
      database.ensureExistingConnection();

      JdbcHelper.calculateChecksum(new CaptureChecksum() {
        public void execute() throws Exception {
          database.getConnection().setAutoCommit(false);

          try {
            /*====================================================================
              == !!! IMPORTANT !!!
              ==
              == Any significant change to the structure of the database
              == should increase the version number of the database, which is
              == stored in the DATABASE_STRUCTURE_VERSION field of this class.
              == You will need to update the DATABASE_STRUCTURE_CHECKSUM field
              == also since it serves as a safeguard to ensure that the version is
              == always adapted. The correct checksum value will be given to you
              == when a checksum mismatch is detected.
              ====================================================================*/

            database.createVersionTable();

            JdbcHelper.executeUpdate(database.getConnection(),
              "CREATE SEQUENCE IF NOT EXISTS seq_localsession");

            JdbcHelper.executeUpdate(database.getConnection(),
              "CREATE TABLE IF NOT EXISTS capturesession (" +
                "localsessionid BIGINT NOT NULL PRIMARY KEY, " +
                "clustersessionid VARCHAR(255) NOT NULL UNIQUE, " +
                "start TIMESTAMP NULL, " +
                "stop TIMESTAMP NULL)");

            JdbcHelper.executeUpdate(database.getConnection(),
              "CREATE SEQUENCE IF NOT EXISTS seq_statisticlog");

            JdbcHelper.executeUpdate(database.getConnection(),
              "CREATE SEQUENCE IF NOT EXISTS seq_consumption");

            JdbcHelper.executeUpdate(database.getConnection(),
              "CREATE TABLE IF NOT EXISTS statisticlog (" +
                "id BIGINT NOT NULL PRIMARY KEY, " +
                "localsessionid BIGINT NOT NULL, " +
                "agentip VARCHAR(39) NOT NULL, " +
                "agentdifferentiator VARCHAR(255) NULL, " +
                "moment TIMESTAMP NOT NULL, " +
                "statname VARCHAR(255) NOT NULL," +
                "statelement VARCHAR(255) NULL, " +
                "datanumber BIGINT NULL, " +
                "datatext TEXT NULL, " +
                "datatimestamp TIMESTAMP NULL, " +
                "datadecimal DECIMAL(8, 4) NULL, " +
                "consumptionid BIGINT NULL)");

            JdbcHelper.executeUpdate(database.getConnection(),
              "CREATE INDEX IF NOT EXISTS idx_capturesession_clustersessionid ON capturesession(clustersessionid)");
            JdbcHelper.executeUpdate(database.getConnection(),
              "CREATE INDEX IF NOT EXISTS idx_statisticlog_localsessionid ON statisticlog(localsessionid)");
            JdbcHelper.executeUpdate(database.getConnection(),
              "CREATE INDEX IF NOT EXISTS idx_statisticlog_consumptionid ON statisticlog(consumptionid)");

            database.getConnection().commit();
          } catch (Exception e) {
            database.getConnection().rollback();
            throw e;
          } finally {
            database.getConnection().setAutoCommit(true);
          }

          database.checkVersion(DATABASE_STRUCTURE_VERSION, DATABASE_STRUCTURE_CHECKSUM);
        }
      });
    } catch (Exception e) {
        throw new TCStatisticsBufferInstallationErrorException(e);
    }
  }

  private void setupPreparedStatements() throws TCStatisticsBufferException {
    try {
      database.createPreparedStatement(SQL_NEXT_LOCALSESSIONID);
      database.createPreparedStatement(SQL_NEXT_STATISTICLOGID);
      database.createPreparedStatement(SQL_NEXT_CONSUMPTIONID);
      database.createPreparedStatement(SQL_MAKE_ALL_CONSUMABLE);
    } catch (TCStatisticsDatabaseException e) {
      throw new TCStatisticsBufferSetupErrorException("Unexpected error while preparing the statements for the H2 statistics buffer.", e);
    }
  }

  private void makeAllDataConsumable() throws TCStatisticsBufferException {
    try {
      database.getPreparedStatement(SQL_MAKE_ALL_CONSUMABLE).executeUpdate();
    } catch (SQLException e) {
      throw new TCStatisticsBufferSetupErrorException("Unexpected error while making all the existing data consumable in the H2 statistics buffer.", e);
    }
  }

  public StatisticsRetriever createCaptureSession(final String sessionId) throws TCStatisticsBufferException {
    Assert.assertNotNull("sessionId", sessionId);

    final long local_sessionid;
    final int row_count;
    try {
      database.ensureExistingConnection();

      local_sessionid = JdbcHelper.fetchNextSequenceValue(database.getPreparedStatement(SQL_NEXT_LOCALSESSIONID));

      row_count = JdbcHelper.executeUpdate(database.getConnection(), SQL_CREATE_CAPTURESESSION, new PreparedStatementHandler() {
        public void setParameters(PreparedStatement statement) throws SQLException {
          statement.setLong(1, local_sessionid);
          statement.setString(2, sessionId);
        }
      });
    } catch (Exception e) {
      throw new TCStatisticsBufferCaptureSessionCreationErrorException(sessionId, e);
    }

    if (row_count != 1) {
      throw new TCStatisticsBufferCaptureSessionCreationErrorException(sessionId, local_sessionid);
    }

    return new StatisticsRetrieverImpl(config.createChild(), this, sessionId);
  }

  public void startCapturing(final String sessionId) throws TCStatisticsBufferException {
    final int row_count;
    try {
      database.ensureExistingConnection();

      row_count = JdbcHelper.executeUpdate(database.getConnection(), SQL_START_CAPTURESESSION, new PreparedStatementHandler() {
        public void setParameters(PreparedStatement statement) throws SQLException {
          statement.setTimestamp(1, new Timestamp(new Date().getTime()));
          statement.setString(2, sessionId);
        }
      });
    } catch (Exception e) {
      throw new TCStatisticsBufferStartCapturingErrorException(sessionId, e);
    }

    if (row_count != 1) {
      throw new TCStatisticsBufferStartCapturingSessionNotFoundException(sessionId);
    }

    fireCapturingStarted(sessionId);
  }

  public void stopCapturing(final String sessionId) throws TCStatisticsBufferException {
    final boolean[] found = new boolean[] {false};
    final int row_count;
    try {
      database.ensureExistingConnection();

      database.getConnection().setAutoCommit(false);
      try {

        JdbcHelper.executeQuery(database.getConnection(), SQL_RETRIEVE_CAPTURESESSION, new PreparedStatementHandler() {
          public void setParameters(PreparedStatement statement) throws SQLException {
            statement.setString(1, sessionId);
          }
        }, new ResultSetHandler() {
          public void useResultSet(ResultSet resultSet) throws SQLException {
            if (resultSet.next()) {
              found[0] = true;
            }
          }
        });

        if (found[0]) {
          row_count = JdbcHelper.executeUpdate(database.getConnection(), SQL_STOP_CAPTURESESSION, new PreparedStatementHandler() {
            public void setParameters(PreparedStatement statement) throws SQLException {
              statement.setTimestamp(1, new Timestamp(new Date().getTime()));
              statement.setString(2, sessionId);
            }
          });
        } else {
          row_count = 0;
        }
      } finally {
        database.getConnection().commit();
        database.getConnection().setAutoCommit(true);
      }
    } catch (Exception e) {
      throw new TCStatisticsBufferStopCapturingErrorException(sessionId, e);
    }

    if (!found[0]) {
      throw new TCStatisticsBufferStopCapturingSessionNotFoundException(sessionId);
    }

    if (row_count > 0) {
      fireCapturingStopped(sessionId);
    }
  }

  private long retrieveLocalSessionId(final String sessionId) throws SQLException, TCStatisticsBufferUnknownCaptureSessionException {
    final long local_sessionid[] = new long[] {-1};
    JdbcHelper.executeQuery(database.getConnection(), SQL_RETRIEVE_LOCAL_SESSIONID, new PreparedStatementHandler() {
      public void setParameters(PreparedStatement statement) throws SQLException {
        statement.setString(1, sessionId);
      }
    }, new ResultSetHandler() {
      public void useResultSet(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
          local_sessionid[0] = resultSet.getLong("localsessionid");
        }
      }
    });

    // ensure that the local session ID was found
    if (-1 == local_sessionid[0]) {
      throw new TCStatisticsBufferUnknownCaptureSessionException(sessionId, null);
    }

    return local_sessionid[0];
  }

  public void storeStatistic(final StatisticData data) throws TCStatisticsBufferException {
    Assert.assertNotNull("data", data);
    Assert.assertNotNull("sessionId property of data", data.getSessionId());
    Assert.assertNotNull("moment property of data", data.getMoment());
    Assert.assertNotNull("name property of data", data.getName());

    if (null == data.getAgentIp()) {
      data.setAgentIp(defaultAgentIp);
    }
    if (null == data.getAgentDifferentiator()) {
      data.setAgentDifferentiator(defaultAgentDifferentiator);
    }
    
    Assert.assertNotNull("agentIp property of data", data.getAgentIp());

    final long id;
    final int row_count;

    try {
      database.ensureExistingConnection();

      // obtain the local session ID
      final long local_sessionid = retrieveLocalSessionId(data.getSessionId());

      // obtain a new ID for the statistic data
      id = JdbcHelper.fetchNextSequenceValue(database.getPreparedStatement(SQL_NEXT_STATISTICLOGID));

      // insert the statistic data with the provided values
      row_count = JdbcHelper.executeUpdate(database.getConnection(), SQL_INSERT_STATISTICSDATA, new PreparedStatementHandler() {
        public void setParameters(PreparedStatement statement) throws SQLException {
          statement.setLong(1, id);
          statement.setLong(2, local_sessionid);
          statement.setString(3, data.getAgentIp());
          if (null == data.getAgentDifferentiator()) {
            statement.setNull(4, Types.VARCHAR);
          } else {
            statement.setString(4, data.getAgentDifferentiator());
          }
          statement.setTimestamp(5, new Timestamp(data.getMoment().getTime()));
          statement.setString(6, data.getName());
          if (null == data.getElement()) {
            statement.setNull(7, Types.VARCHAR);
          } else {
            statement.setString(7, data.getElement());
          }
          if (null == data.getData()) {
            statement.setNull(8, Types.BIGINT);
            statement.setNull(9, Types.VARCHAR);
            statement.setNull(10, Types.TIMESTAMP);
            statement.setNull(11, Types.NUMERIC);
          } else if (data.getData() instanceof BigDecimal) {
            statement.setNull(8, Types.BIGINT);
            statement.setNull(9, Types.VARCHAR);
            statement.setNull(10, Types.TIMESTAMP);
            statement.setBigDecimal(11, (BigDecimal)data.getData());
          } else if (data.getData() instanceof Number) {
            statement.setLong(8, ((Number)data.getData()).longValue());
            statement.setNull(9, Types.VARCHAR);
            statement.setNull(10, Types.TIMESTAMP);
            statement.setNull(11, Types.NUMERIC);
          } else if (data.getData() instanceof CharSequence) {
            statement.setNull(8, Types.BIGINT);
            statement.setString(9, data.getData().toString());
            statement.setNull(10, Types.TIMESTAMP);
            statement.setNull(11, Types.NUMERIC);
          } else if (data.getData() instanceof Date) {
            statement.setNull(8, Types.BIGINT);
            statement.setNull(9, Types.VARCHAR);
            statement.setTimestamp(10, new java.sql.Timestamp(((Date)data.getData()).getTime()));
            statement.setNull(11, Types.NUMERIC);
          }
        }
      });
    } catch (Exception e) {
      throw new TCStatisticsBufferStatisticStorageErrorException(data, e);
    }

    // ensure that a row was inserted
    if (row_count != 1) {
      throw new TCStatisticsBufferStatisticStorageErrorException(id, data);
    }
  }

  public void consumeStatistics(final String sessionId, final StatisticsConsumer consumer) throws TCStatisticsBufferException {
    Assert.assertNotNull("sessionId", sessionId);
    Assert.assertNotNull("consumer", consumer);

    try {
      database.ensureExistingConnection();

      // obtain the local session ID
      final long local_sessionid = retrieveLocalSessionId(sessionId);

      // create a unique ID for this consumption phase
      final long consumption_id = JdbcHelper.fetchNextSequenceValue(database.getPreparedStatement(SQL_NEXT_CONSUMPTIONID));

      // reserve all existing statistic data with the provided session ID
      // for the consumption ID
      final boolean limit_consumption = consumer.getMaximumConsumedDataCount() > 0;
      final int row_count = JdbcHelper.executeUpdate(database.getConnection(), limit_consumption ? SQL_MARK_FOR_CONSUMPTION_LIMIT : SQL_MARK_FOR_CONSUMPTION, new PreparedStatementHandler() {
        public void setParameters(PreparedStatement statement) throws SQLException {
          statement.setLong(1, consumption_id);
          statement.setLong(2, local_sessionid);
          if (limit_consumption) {
            statement.setLong(3, consumer.getMaximumConsumedDataCount());
          }
        }
      });

      try {
        // consume all the statistic data in this capture session
        if (row_count > 0) {
          JdbcHelper.executeQuery(database.getConnection(), SQL_CONSUME_STATISTICDATA, new PreparedStatementHandler() {
            public void setParameters(PreparedStatement statement) throws SQLException {
              statement.setLong(1, consumption_id);
            }
          }, new ResultSetHandler() {
            public void useResultSet(ResultSet resultSet) throws SQLException {
              while (resultSet.next()) {
                // obtain the statistics data
                StatisticData data = database.getStatisticsData(sessionId, resultSet);

                // consume the data
                if (!consumer.consumeStatisticData(data)) {
                  return;
                }
                // delete the consumed statistic data from the log
                resultSet.deleteRow();
              }
            }
          });
        }
      } finally {
        // make the statistic data that wasn't consumed during this consumption phase
        // available again so that it can be picked up by another consumption operation
        JdbcHelper.executeUpdate(database.getConnection(), SQL_RESET_CONSUMPTIONID, new PreparedStatementHandler() {
          public void setParameters(PreparedStatement statement) throws SQLException {
            statement.setLong(1, consumption_id);
          }
        });
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new TCStatisticsBufferStatisticConsumptionErrorException(sessionId, e);
    }
  }

  public void addListener(final StatisticsBufferListener listener) {
    if (null == listener) {
      return;
    }

    listeners.add(listener);
  }

  public void removeListener(final StatisticsBufferListener listener) {
    if (null == listener) {
      return;
    }

    listeners.remove(listener);
  }

  private void fireCapturingStarted(final String sessionId) {
    if (listeners.size() > 0) {
      for (Iterator it = listeners.iterator(); it.hasNext(); ) {
        ((StatisticsBufferListener)it.next()).capturingStarted(sessionId);
      }
    }
  }

  private void fireCapturingStopped(final String sessionId) {
    if (listeners.size() > 0) {
      for (Iterator it = listeners.iterator(); it.hasNext(); ) {
        ((StatisticsBufferListener)it.next()).capturingStopped(sessionId);
      }
    }
  }
}