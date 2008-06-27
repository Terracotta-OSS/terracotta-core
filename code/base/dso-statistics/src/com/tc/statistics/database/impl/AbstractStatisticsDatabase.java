/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.database.impl;

import com.tc.statistics.StatisticData;
import com.tc.statistics.database.StatisticsDatabase;
import com.tc.statistics.database.exceptions.StatisticsDatabaseCloseErrorException;
import com.tc.statistics.database.exceptions.StatisticsDatabaseException;
import com.tc.statistics.database.exceptions.StatisticsDatabaseNotFoundException;
import com.tc.statistics.database.exceptions.StatisticsDatabaseNotReadyException;
import com.tc.statistics.database.exceptions.StatisticsDatabaseStatementPreparationErrorException;
import com.tc.statistics.database.exceptions.StatisticsDatabaseStoreVersionErrorException;
import com.tc.statistics.database.exceptions.StatisticsDatabaseStructureFuturedatedError;
import com.tc.statistics.database.exceptions.StatisticsDatabaseStructureOutdatedError;
import com.tc.statistics.database.exceptions.StatisticsDatabaseVersionCheckErrorException;
import com.tc.statistics.jdbc.ChecksumCalculator;
import com.tc.statistics.jdbc.JdbcHelper;
import com.tc.statistics.jdbc.PreparedStatementHandler;
import com.tc.statistics.jdbc.ResultSetHandler;
import com.tc.util.Assert;
import com.tc.util.concurrent.CopyOnWriteArrayMap;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class AbstractStatisticsDatabase implements StatisticsDatabase {
  protected final Map           preparedStatements = new CopyOnWriteArrayMap();

  protected volatile Connection connection;

  protected synchronized void open(final String driver) throws StatisticsDatabaseException {
    if (connection != null) return;

    try {
      Class.forName(driver);
    } catch (ClassNotFoundException e) {
      throw new StatisticsDatabaseNotFoundException(driver, e);
    }

    openConnection();
  }

  protected abstract void openConnection() throws StatisticsDatabaseException;

  public void ensureExistingConnection() throws StatisticsDatabaseException {
    if (null == connection) { throw new StatisticsDatabaseNotReadyException(); }
  }

  public Connection getConnection() {
    return connection;
  }

  public PreparedStatement createPreparedStatement(final String sql) throws StatisticsDatabaseException {
    ensureExistingConnection();

    try {
      PreparedStatement stmt = connection.prepareStatement(sql);
      PreparedStatement previous = (PreparedStatement) preparedStatements.put(sql, stmt);
      if (previous != null) {
        previous.close();
      }
      return stmt;
    } catch (SQLException e) {
      throw new StatisticsDatabaseStatementPreparationErrorException(sql, e);
    }
  }

  public PreparedStatement getPreparedStatement(final String sql) {
    return (PreparedStatement) preparedStatements.get(sql);
  }

  public void createVersionTable() throws SQLException {
    JdbcHelper.executeUpdate(getConnection(), "CREATE TABLE IF NOT EXISTS dbstructureversion ("
                                              + "version INT NOT NULL PRIMARY KEY, " + "created TIMESTAMP NOT NULL)");
  }

  // TODO: Currently version checks just fail hard when they don't match, in the future this should
  // be made more intelligent to automatically migrate from older versions to newer ones.
  public void checkVersion(final int currentVersion, long currentChecksum) throws StatisticsDatabaseException {
    ChecksumCalculator csc = JdbcHelper.getActiveChecksumCalculator();
    Assert.assertNotNull("Expected the checksum of SQL statements to be calculated at this time.", csc);

    long checksum = csc.checksum();
    Assert
        .assertTrue(
                    "The checksum of the SQL that creates the database structure doesn't correspond to the checksum that corresponds to the version number of the database structure. Any significant change to the database structure should increase the version number and adapt the SQL checksum. The current checksum is "
                        + checksum + "L.", currentChecksum == checksum);

    final Integer[] version = new Integer[1];
    final Date[] created = new Date[1];

    try {
      getConnection().setAutoCommit(false);
      try {
        try {
          JdbcHelper.executeQuery(getConnection(), "SELECT version, created FROM dbstructureversion",
                                  new ResultSetHandler() {
                                    public void useResultSet(ResultSet resultSet) throws SQLException {
                                      if (resultSet.next()) {
                                        version[0] = new Integer(resultSet.getInt("version"));
                                        created[0] = resultSet.getTimestamp("created");
                                      }
                                    }
                                  });
        } catch (SQLException e) {
          throw new StatisticsDatabaseVersionCheckErrorException("Unexpected error while checking the version.", e);
        }

        if (null == version[0]) {
          storeCurrentVersion(currentVersion);
        } else {
          if (version[0].intValue() < currentVersion) {
            throw new StatisticsDatabaseStructureOutdatedError(version[0].intValue(), currentVersion, created[0]);
          } else if (version[0].intValue() > currentVersion) { throw new StatisticsDatabaseStructureFuturedatedError(
                                                                                                                     version[0]
                                                                                                                         .intValue(),
                                                                                                                     currentVersion,
                                                                                                                     created[0]); }
        }

        getConnection().commit();
      } catch (StatisticsDatabaseException e) {
        getConnection().rollback();
        throw e;
      } finally {
        getConnection().setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new StatisticsDatabaseVersionCheckErrorException("Unexpected error while checking the version.", e);
    }
  }

  private void storeCurrentVersion(final int currentVersion) throws StatisticsDatabaseException {
    try {
      JdbcHelper.executeUpdate(getConnection(),
                               "INSERT INTO dbstructureversion (version, created) VALUES (?, CURRENT_TIMESTAMP)",
                               new PreparedStatementHandler() {
                                 public void setParameters(PreparedStatement statement) throws SQLException {
                                   statement.setInt(1, currentVersion);
                                 }
                               });
    } catch (SQLException e) {
      throw new StatisticsDatabaseStoreVersionErrorException(currentVersion, e);
    }
  }

  public StatisticData getStatisticsData(String sessionId, final ResultSet resultSet) throws SQLException {
    StatisticData data = new StatisticData().sessionId(sessionId).agentIp(resultSet.getString("agentip"))
        .agentDifferentiator(resultSet.getString("agentdifferentiator")).moment(resultSet.getTimestamp("moment"))
        .name(resultSet.getString("statName")).element(resultSet.getString("statElement"));

    long datanumber = resultSet.getLong("dataNumber");
    if (!resultSet.wasNull()) {
      data.data(new Long(datanumber));
    } else {
      String datatext = resultSet.getString("dataText");
      if (!resultSet.wasNull()) {
        data.data(datatext);
      } else {
        Timestamp datatimestamp = resultSet.getTimestamp("dataTimestamp");
        if (!resultSet.wasNull()) {
          data.data(datatimestamp);
        } else {
          BigDecimal datadecimal = resultSet.getBigDecimal("dataDecimal");
          if (!resultSet.wasNull()) {
            data.data(datadecimal);
          }
        }
      }
    }
    return data;
  }

  public synchronized void close() throws StatisticsDatabaseException {
    if (null == connection) return;

    SQLException exception = null;

    try {
      try {
        Set entries = preparedStatements.entrySet();
        for (Iterator entries_it = entries.iterator(); entries_it.hasNext();) {
          Map.Entry entry = (Map.Entry) entries_it.next();
          PreparedStatement stmt = (PreparedStatement) entry.getValue();
          try {
            stmt.close();
          } catch (SQLException e) {
            if (exception != null) {
              e.setNextException(exception);
            }
            exception = e;
          }
        }

        preparedStatements.clear();
      } finally {
        try {
          connection.close();
        } catch (SQLException e) {
          if (exception != null) {
            e.setNextException(exception);
          }
          exception = e;
        }
      }
    } finally {
      connection = null;
    }

    if (exception != null) { throw new StatisticsDatabaseCloseErrorException(exception); }
  }
}