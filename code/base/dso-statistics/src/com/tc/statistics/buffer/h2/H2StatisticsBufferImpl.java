/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.buffer.h2;

import org.h2.constant.ErrorCode;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticsSystemType;
import com.tc.statistics.buffer.AbstractStatisticsBuffer;
import com.tc.statistics.buffer.StatisticsConsumer;
import com.tc.statistics.buffer.exceptions.StatisticsBufferCaptureSessionCreationErrorException;
import com.tc.statistics.buffer.exceptions.StatisticsBufferDatabaseCloseErrorException;
import com.tc.statistics.buffer.exceptions.StatisticsBufferDatabaseOpenErrorException;
import com.tc.statistics.buffer.exceptions.StatisticsBufferException;
import com.tc.statistics.buffer.exceptions.StatisticsBufferInstallationErrorException;
import com.tc.statistics.buffer.exceptions.StatisticsBufferSetupErrorException;
import com.tc.statistics.buffer.exceptions.StatisticsBufferStartCapturingErrorException;
import com.tc.statistics.buffer.exceptions.StatisticsBufferStartCapturingSessionNotFoundException;
import com.tc.statistics.buffer.exceptions.StatisticsBufferStatisticConsumptionErrorException;
import com.tc.statistics.buffer.exceptions.StatisticsBufferStatisticStorageErrorException;
import com.tc.statistics.buffer.exceptions.StatisticsBufferStopCapturingErrorException;
import com.tc.statistics.buffer.exceptions.StatisticsBufferStopCapturingSessionNotFoundException;
import com.tc.statistics.buffer.exceptions.StatisticsBufferUnknownCaptureSessionException;
import com.tc.statistics.config.DSOStatisticsConfig;
import com.tc.statistics.database.StatisticsDatabase;
import com.tc.statistics.database.exceptions.StatisticsDatabaseException;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.Random;

public class H2StatisticsBufferImpl extends AbstractStatisticsBuffer {
  public final static int DATABASE_STRUCTURE_VERSION = 4;
  
  private final static long DATABASE_STRUCTURE_CHECKSUM = 293260301L;

  public final static String H2_URL_SUFFIX = "statistics-buffer";

  private final static TCLogger LOGGER = TCLogging.getLogger(H2StatisticsBufferImpl.class);

  private final static String SQL_NEXT_LOCALSESSIONID = "SELECT nextval('seq_localsession')";
  private final static String SQL_NEXT_STATISTICLOGID = "SELECT nextval('seq_statisticlog')";
  private final static String SQL_NEXT_CONSUMPTIONID = "SELECT nextval('seq_consumption')";
  private final static String SQL_MAKE_ALL_CONSUMABLE = "UPDATE statisticlog SET consumptionid = NULL";
  private final static String SQL_RETRIEVE_LOCAL_SESSIONID = "SELECT localsessionid FROM capturesession WHERE clustersessionid = ?";
  private final static String SQL_RETRIEVE_CAPTURESESSION = "SELECT * FROM capturesession WHERE clustersessionid = ?";
  private final static String SQL_STOP_CAPTURESESSION = "UPDATE capturesession SET stop = ? WHERE clustersessionid = ? AND start IS NOT NULL AND stop IS NULL";
  private final static String SQL_CREATE_CAPTURESESSION = "INSERT INTO capturesession (localsessionid, clustersessionid) VALUES (?, ?)";
  private final static String SQL_START_CAPTURESESSION = "UPDATE capturesession SET start = ? WHERE clustersessionid = ? AND stop IS NULL";
  private final static String SQL_INSERT_STATISTICSDATA = "INSERT INTO statisticlog (id, localsessionid, agentip, agentdifferentiator, moment, statname, statelement, datanumber, datatext, datatimestamp, datadecimal) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
  private final static String SQL_MARK_FOR_CONSUMPTION_LIMIT = "MERGE INTO statisticlog(id, consumptionid) KEY(id) SELECT id, ? FROM statisticlog WHERE consumptionid IS NULL AND localsessionid = ? ORDER BY moment LIMIT ?";
  private final static String SQL_MARK_FOR_CONSUMPTION = "UPDATE statisticlog SET consumptionid = ? WHERE consumptionid IS NULL AND localsessionid = ?";
  private final static String SQL_CONSUME_STATISTICDATA = "SELECT * FROM statisticlog WHERE consumptionid = ? ORDER BY moment ASC, id ASC";
  private final static String SQL_RESET_CONSUMPTIONID = "UPDATE statisticlog SET consumptionid = NULL WHERE consumptionid = ?";

  private final static Random rand = new Random();

  private final StatisticsSystemType type;
  private final DSOStatisticsConfig config;
  private final File lockFile;
  private final StatisticsDatabase database;

  private volatile boolean open = false;

  public H2StatisticsBufferImpl(final StatisticsSystemType type, final DSOStatisticsConfig config, final File dbDir) {
    super();
    
    Assert.assertNotNull("type", type);
    Assert.assertNotNull("config", config);
    
    final String suffix;
    if (TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.CVT_BUFFER_RANDOM_SUFFIX_ENABLED, false)) {
      synchronized (rand) {
        suffix = H2_URL_SUFFIX + "-" + rand.nextInt() + "." + System.currentTimeMillis();
      }
    } else {
      suffix = H2_URL_SUFFIX;
    }
    this.database = new H2StatisticsDatabase(dbDir, suffix);
    this.type = type;
    this.config = config;
    this.lockFile = new File(dbDir, suffix+"-tc.lck");
  }

  public void open() throws StatisticsBufferException {
    if (StatisticsSystemType.CLIENT == type &&
      TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.CVT_CLIENT_FAIL_BUFFER_OPEN, false)) {
      throw new StatisticsBufferException("Forcibly failing opening the statistics buffer through the " + TCPropertiesConsts.CVT_CLIENT_FAIL_BUFFER_OPEN + " property", null);
    }

    synchronized (this) {
      try {
        FileLockGuard.guard(lockFile, new FileLockGuard.Guarded() {
          public void execute() throws FileLockGuard.InnerException {
            try {
              try {
                database.open();
              } catch (StatisticsDatabaseException e) {
                throw new StatisticsBufferDatabaseOpenErrorException(e);
              }

              install();
              setupPreparedStatements();
              makeAllDataConsumable();
            } catch (StatisticsBufferException e) {
              throw new FileLockGuard.InnerException(e);
            }
          }
        });
      } catch (FileLockGuard.InnerException e) {
        throw (StatisticsBufferException)e.getInnerException();
      } catch (IOException e) {
        throw new StatisticsBufferException("Unexpected error while obtaining or releasing lock file.", e);
      }

      open = true;
    }
    fireOpened();
  }

  public void close() throws StatisticsBufferException {
    synchronized (this) {
      fireClosing();

      try {
        database.close();
      } catch (StatisticsDatabaseException e) {
        throw new StatisticsBufferDatabaseCloseErrorException(e);
      }

      open = false;
    }
    fireClosed();
  }

  public synchronized void reinitialize() throws StatisticsBufferException {
    boolean was_open = open;
    close();
    try {
      database.reinitialize();
    } catch (StatisticsDatabaseException e) {
      throw new StatisticsBufferException("Unexpected error while reinitializing the statistics buffer.", e);
    }
    if (was_open) {
      open();
    }
  }

  protected void install() throws StatisticsBufferException {
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
                "datatext LONGVARCHAR NULL, " +
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
        throw new StatisticsBufferInstallationErrorException(e);
    }
  }

  private void setupPreparedStatements() throws StatisticsBufferException {
    try {
      database.createPreparedStatement(SQL_NEXT_LOCALSESSIONID);
      database.createPreparedStatement(SQL_NEXT_STATISTICLOGID);
      database.createPreparedStatement(SQL_NEXT_CONSUMPTIONID);
      database.createPreparedStatement(SQL_MAKE_ALL_CONSUMABLE);
    } catch (StatisticsDatabaseException e) {
      throw new StatisticsBufferSetupErrorException("Unexpected error while preparing the statements for the H2 statistics buffer.", e);
    }
  }

  private void makeAllDataConsumable() throws StatisticsBufferException {
    try {
      database.getPreparedStatement(SQL_MAKE_ALL_CONSUMABLE).executeUpdate();
    } catch (SQLException e) {
      throw new StatisticsBufferSetupErrorException("Unexpected error while making all the existing data consumable in the H2 statistics buffer.", e);
    }
  }

  public StatisticsRetriever createCaptureSession(final String sessionId) throws StatisticsBufferException {
    checkDefaultAgentInfo();
    Assert.assertNotNull("sessionId", sessionId);

    final long local_sessionid;
    int row_count;
    try {
      database.ensureExistingConnection();

      local_sessionid = JdbcHelper.fetchNextSequenceValue(database.getPreparedStatement(SQL_NEXT_LOCALSESSIONID));
    } catch (Exception e) {
      throw new StatisticsBufferCaptureSessionCreationErrorException(sessionId, e);
    }

    try {
      row_count = JdbcHelper.executeUpdate(database.getConnection(), SQL_CREATE_CAPTURESESSION, new PreparedStatementHandler() {
        public void setParameters(PreparedStatement statement) throws SQLException {
          statement.setLong(1, local_sessionid);
          statement.setString(2, sessionId);
        }
      });

      if (row_count != 1) {
        throw new StatisticsBufferCaptureSessionCreationErrorException(sessionId, local_sessionid);
      }
    } catch (SQLException e) {
      if (ErrorCode.DUPLICATE_KEY_1 == e.getErrorCode()) {
        LOGGER.warn("The capture session with ID '" + sessionId + "' already exists, not creating it again.");
      } else {
        throw new StatisticsBufferCaptureSessionCreationErrorException(sessionId, local_sessionid);
      }
    }

    return new StatisticsRetrieverImpl(config.createChild(), this, sessionId);
  }

  public void startCapturing(final String sessionId) throws StatisticsBufferException {
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
      throw new StatisticsBufferStartCapturingErrorException(sessionId, e);
    }

    if (row_count != 1) {
      throw new StatisticsBufferStartCapturingSessionNotFoundException(sessionId);
    }

    fireCapturingStarted(sessionId);
  }

  public void stopCapturing(final String sessionId) throws StatisticsBufferException {
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
      throw new StatisticsBufferStopCapturingErrorException(sessionId, e);
    }

    if (!found[0]) {
      throw new StatisticsBufferStopCapturingSessionNotFoundException(sessionId);
    }

    if (row_count > 0) {
      fireCapturingStopped(sessionId);
    }
  }

  private long retrieveLocalSessionId(final String sessionId) throws SQLException, StatisticsBufferUnknownCaptureSessionException {
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
      throw new StatisticsBufferUnknownCaptureSessionException(sessionId, null);
    }

    return local_sessionid[0];
  }

  public void storeStatistic(final StatisticData data) throws StatisticsBufferException {
    Assert.assertNotNull("data", data);
    Assert.assertNotNull("sessionId property of data", data.getSessionId());
    Assert.assertNotNull("moment property of data", data.getMoment());
    Assert.assertNotNull("name property of data", data.getName());

    fillInDefaultValues(data);
    
    Assert.assertNotNull("agentIp property of data", data.getAgentIp());
    Assert.assertNotNull("agentDifferentiator property of data", data.getAgentDifferentiator());

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
          statement.setString(4, data.getAgentDifferentiator());
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
      throw new StatisticsBufferStatisticStorageErrorException(data, e);
    }

    // ensure that a row was inserted
    if (row_count != 1) {
      throw new StatisticsBufferStatisticStorageErrorException(id, data);
    }
  }

  public void consumeStatistics(final String sessionId, final StatisticsConsumer consumer) throws StatisticsBufferException {
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
      throw new StatisticsBufferStatisticConsumptionErrorException(sessionId, e);
    }
  }
}