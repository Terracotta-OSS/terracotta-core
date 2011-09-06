/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.store.h2;

import org.apache.commons.lang.StringEscapeUtils;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArraySet;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticDataCSVParser;
import com.tc.statistics.database.StatisticsDatabase;
import com.tc.statistics.database.exceptions.StatisticsDatabaseException;
import com.tc.statistics.database.impl.H2StatisticsDatabase;
import com.tc.statistics.jdbc.CaptureChecksum;
import com.tc.statistics.jdbc.JdbcHelper;
import com.tc.statistics.jdbc.PreparedStatementHandler;
import com.tc.statistics.jdbc.ResultSetHandler;
import com.tc.statistics.store.StatisticDataUser;
import com.tc.statistics.store.StatisticsRetrievalCriteria;
import com.tc.statistics.store.StatisticsStore;
import com.tc.statistics.store.StatisticsStoreImportListener;
import com.tc.statistics.store.StatisticsStoreListener;
import com.tc.statistics.store.TextualDataFormat;
import com.tc.statistics.store.exceptions.StatisticsStoreCacheCreationErrorException;
import com.tc.statistics.store.exceptions.StatisticsStoreClearAllStatisticsErrorException;
import com.tc.statistics.store.exceptions.StatisticsStoreClearStatisticsErrorException;
import com.tc.statistics.store.exceptions.StatisticsStoreCloseErrorException;
import com.tc.statistics.store.exceptions.StatisticsStoreException;
import com.tc.statistics.store.exceptions.StatisticsStoreInstallationErrorException;
import com.tc.statistics.store.exceptions.StatisticsStoreOpenErrorException;
import com.tc.statistics.store.exceptions.StatisticsStoreRetrievalErrorException;
import com.tc.statistics.store.exceptions.StatisticsStoreSessionIdsRetrievalErrorException;
import com.tc.statistics.store.exceptions.StatisticsStoreSetupErrorException;
import com.tc.statistics.store.exceptions.StatisticsStoreStatisticStorageErrorException;
import com.tc.util.Assert;
import com.tc.util.concurrent.FileLockGuard;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class H2StatisticsStoreImpl implements StatisticsStore {
  public final static int            DATABASE_STRUCTURE_VERSION              = 6;

  public final static String         H2_URL_SUFFIX                           = "statistics-store";

  private final static TCLogger      LOGGER                                  = TCLogging
                                                                                 .getLogger(H2StatisticsStoreImpl.class);

  private final static long          DATABASE_STRUCTURE_CHECKSUM             = 2820643252L;

  private final static String        SQL_NEXT_STATISTICLOGID                 = "SELECT nextval('seq_statisticlog')";
  private final static String        SQL_GET_AVAILABLE_SESSIONIDS            = "SELECT sessionid FROM statisticlog GROUP BY sessionid ORDER BY sessionid ASC";
  private final static String        SQL_GET_AVAILABLE_AGENT_DIFFERENTIATORS = "SELECT agentdifferentiator FROM statisticlog WHERE sessionid = ? GROUP BY agentdifferentiator ORDER BY agentdifferentiator ASC";
  private final static String        SQL_INSERT_STATISTICSDATA               = "INSERT INTO statisticlog (id, sessionid, agentip, agentdifferentiator, moment, statname, statelement, datanumber, datatext, datatimestamp, datadecimal) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
  private final static String        SQL_CLEAR_SESSION_STATISTICS            = "DELETE FROM statisticlog WHERE sessionid = ?";
  private final static String        SQL_CLEAR_ALL_STATISTICS                = "DELETE FROM statisticlog";

  protected final StatisticsDatabase database;

  private final File                 lockFile;
  private final Set                  listeners                               = new CopyOnWriteArraySet();

  private volatile boolean           open                                    = false;

  private static final Random        rand                                    = new Random();

  public H2StatisticsStoreImpl(final File dbDir) {
    final String suffix;
    if (TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.CVT_STORE_RANDOM_SUFFIX_ENABLED, false)) {
      synchronized (rand) {
        suffix = H2_URL_SUFFIX + "-" + rand.nextInt() + "." + System.currentTimeMillis();
      }
    } else {
      suffix = H2_URL_SUFFIX;
    }
    this.database = new H2StatisticsDatabase(dbDir, suffix);
    this.lockFile = new File(dbDir, suffix + "-tc.lck");
  }

  public void open() throws StatisticsStoreException {
    synchronized (this) {
      try {
        FileLockGuard.guard(lockFile, new FileLockGuard.Guarded() {
          @Override
          public void execute() throws FileLockGuard.InnerException {
            try {
              try {
                database.open();
              } catch (StatisticsDatabaseException e) {
                throw new StatisticsStoreOpenErrorException(e);
              }

              install();
              setupPreparedStatements();
            } catch (StatisticsStoreException e) {
              throw new FileLockGuard.InnerException(e);
            }
          }
        });
      } catch (FileLockGuard.InnerException e) {
        throw (StatisticsStoreException) e.getInnerException();
      } catch (IOException e) {
        throw new StatisticsStoreException("Unexpected error while obtaining or releasing lock file.", e);
      }

      open = true;
    }

    fireOpened();
  }

  public void close() throws StatisticsStoreException {
    synchronized (this) {
      try {
        database.close();
      } catch (StatisticsDatabaseException e) {
        throw new StatisticsStoreCloseErrorException(e);
      }

      open = false;
    }

    fireClosed();
  }

  public synchronized void reinitialize() throws StatisticsStoreException {
    boolean was_open = open;
    close();
    try {
      database.reinitialize();
    } catch (StatisticsDatabaseException e) {
      throw new StatisticsStoreException("Unexpected error while reinitializing the statistics store.", e);
    }
    if (was_open) {
      open();
    }
  }

  protected void install() throws StatisticsStoreException {
    try {
      database.ensureExistingConnection();

      JdbcHelper.calculateChecksum(new CaptureChecksum() {
        @Override
        public void execute() throws Exception {
          database.getConnection().setAutoCommit(false);

          try {
            /*
             * ==================================================================== == !!! IMPORTANT !!! == == Any
             * significant change to the structure of the database == should increase the version number of the
             * database, which is == stored in the DATABASE_STRUCTURE_VERSION field of this class. == You will need to
             * update the DATABASE_STRUCTURE_CHECKSUM field == also since it serves as a safeguard to ensure that the
             * version is == always adapted. The correct checksum value will be given to you == when a checksum mismatch
             * is detected. ====================================================================
             */

            database.createVersionTable();

            JdbcHelper.executeUpdate(database.getConnection(), "CREATE SEQUENCE IF NOT EXISTS seq_statisticlog");

            JdbcHelper.executeUpdate(database.getConnection(), "CREATE TABLE IF NOT EXISTS statisticlog ("
                                                               + "id BIGINT NOT NULL PRIMARY KEY, "
                                                               + "sessionid VARCHAR(255) NOT NULL, "
                                                               + "agentip VARCHAR(39) NOT NULL, "
                                                               + "agentdifferentiator VARCHAR(255) NULL, "
                                                               + "moment TIMESTAMP NOT NULL, "
                                                               + "statname VARCHAR(255) NOT NULL,"
                                                               + "statelement VARCHAR(255) NULL, "
                                                               + "datanumber BIGINT NULL, "
                                                               + "datatext LONGVARCHAR NULL, "
                                                               + "datatimestamp TIMESTAMP NULL, "
                                                               + "datadecimal DECIMAL(8, 4) NULL)");

            recreateCachedStatisticsStructureTable();

            createStatisticLogIndexes();

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
      throw new StatisticsStoreInstallationErrorException(e);
    }
  }

  public void recreateCaches() throws StatisticsStoreException {
    try {
      recreateCachedStatisticsStructureTable();
    } catch (SQLException e) {
      throw new StatisticsStoreCacheCreationErrorException(e);
    }
  }

  private void recreateCachedStatisticsStructureTable() throws SQLException {
    JdbcHelper.executeUpdate(database.getConnection(), "DROP TABLE IF EXISTS cachedstatlogstructure");

    JdbcHelper.executeUpdate(database.getConnection(),
                             "CREATE TABLE IF NOT EXISTS cachedstatlogstructure AS "
                                 + "SELECT sessionid, agentip, agentdifferentiator, statname, statelement "
                                 + "FROM statisticlog "
                                 + "GROUP BY sessionid, agentip, agentdifferentiator, statname, statelement");
  }

  private void createStatisticLogIndexes() throws SQLException {
    JdbcHelper.executeUpdate(database.getConnection(),
                             "CREATE INDEX IF NOT EXISTS idx_statisticlog_sessionid ON statisticlog(sessionid)");

    JdbcHelper.executeUpdate(database.getConnection(),
                             "CREATE INDEX IF NOT EXISTS idx_statisticlog_agentip ON statisticlog(agentip)");

    JdbcHelper
        .executeUpdate(database.getConnection(),
                       "CREATE INDEX IF NOT EXISTS idx_statisticlog_agentdifferentiator ON statisticlog(agentdifferentiator)");

    JdbcHelper.executeUpdate(database.getConnection(),
                             "CREATE INDEX IF NOT EXISTS idx_statisticlog_moment ON statisticlog(moment)");

    JdbcHelper.executeUpdate(database.getConnection(),
                             "CREATE INDEX IF NOT EXISTS idx_statisticlog_statname ON statisticlog(statname)");

    JdbcHelper.executeUpdate(database.getConnection(),
                             "CREATE INDEX IF NOT EXISTS idx_statisticlog_statelement ON statisticlog(statelement)");
  }

  private void dropStatisticLogIndexes() throws SQLException {
    JdbcHelper.executeUpdate(database.getConnection(), "DROP INDEX IF EXISTS idx_statisticlog_sessionid");

    JdbcHelper.executeUpdate(database.getConnection(), "DROP INDEX IF EXISTS idx_statisticlog_agentip");

    JdbcHelper.executeUpdate(database.getConnection(), "DROP INDEX IF EXISTS idx_statisticlog_agentdifferentiator");

    JdbcHelper.executeUpdate(database.getConnection(), "DROP INDEX IF EXISTS idx_statisticlog_moment");

    JdbcHelper.executeUpdate(database.getConnection(), "DROP INDEX IF EXISTS idx_statisticlog_statname");

    JdbcHelper.executeUpdate(database.getConnection(), "DROP INDEX IF EXISTS idx_statisticlog_statelement");
  }

  private void setupPreparedStatements() throws StatisticsStoreException {
    try {
      database.createPreparedStatement(SQL_NEXT_STATISTICLOGID);
      database.createPreparedStatement(SQL_GET_AVAILABLE_SESSIONIDS);
    } catch (StatisticsDatabaseException e) {
      throw new StatisticsStoreSetupErrorException(e);
    }
  }

  public void storeStatistic(final StatisticData data) throws StatisticsStoreException {
    Assert.assertNotNull("data", data);
    Assert.assertNotNull("sessionId property of data", data.getSessionId());
    Assert.assertNotNull("agentIp property of data", data.getAgentIp());
    Assert.assertNotNull("agentDifferentiator property of data", data.getAgentDifferentiator());
    Assert.assertNotNull("moment property of data", data.getMoment());
    Assert.assertNotNull("name property of data", data.getName());

    final long id;
    final int row_count;

    try {
      database.ensureExistingConnection();

      // obtain a new ID for the statistic data
      id = JdbcHelper.fetchNextSequenceValue(database.getPreparedStatement(SQL_NEXT_STATISTICLOGID));

      // insert the statistic data with the provided values
      row_count = JdbcHelper.executeUpdate(database.getConnection(), SQL_INSERT_STATISTICSDATA,
                                           new PreparedStatementHandler() {
                                             public void setParameters(PreparedStatement statement) throws SQLException {
                                               setStatisticDataParameters(statement, id, data);
                                             }
                                           });
    } catch (Exception e) {
      throw new StatisticsStoreStatisticStorageErrorException(data, e);
    }

    // ensure that a row was inserted
    if (row_count != 1) { throw new StatisticsStoreStatisticStorageErrorException(id, data, null); }
  }

  private void setStatisticDataParameters(PreparedStatement statement, long id, StatisticData data) throws SQLException {
    statement.setLong(1, id);
    statement.setString(2, data.getSessionId());
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
      statement.setBigDecimal(11, (BigDecimal) data.getData());
    } else if (data.getData() instanceof Number) {
      statement.setLong(8, ((Number) data.getData()).longValue());
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
      statement.setTimestamp(10, new Timestamp(((Date) data.getData()).getTime()));
      statement.setNull(11, Types.NUMERIC);
    }
  }

  public void importCsvStatistics(final Reader reader, final StatisticsStoreImportListener listener)
      throws StatisticsStoreException {
    Assert.assertNotNull("reader", reader);

    try {
      database.ensureExistingConnection();
    } catch (StatisticsDatabaseException e) {
      throw new StatisticsStoreException("Database not connected.", e);
    }

    if (listener != null) {
      listener.started();
    }

    long count = 0L;
    final BufferedReader buffered_reader = new BufferedReader(reader);
    boolean first_line = true;
    String line;

    PreparedStatement ps_insert = null;
    try {
      ps_insert = database.getConnection().prepareStatement(SQL_INSERT_STATISTICSDATA);

      try {
        dropStatisticLogIndexes();

        try {
          database.getConnection().setAutoCommit(false);
          try {
            while ((line = buffered_reader.readLine()) != null) {
              if (first_line) {
                first_line = false;
                continue;
              }

              final StatisticData data = StatisticDataCSVParser
                  .newInstanceFromCsvLine(StatisticDataCSVParser.CURRENT_CSV_VERSION, line);
              if (data != null) {
                ps_insert.clearParameters();

                final long id = JdbcHelper.fetchNextSequenceValue(database
                    .getPreparedStatement(SQL_NEXT_STATISTICLOGID));
                setStatisticDataParameters(ps_insert, id, data);
                ps_insert.addBatch();
                count++;

                // notify about every 1000 inserts
                if (listener != null && 0 == count % 1000) {
                  listener.imported(count);
                }

                // execute every 5000 inserts in batch
                if (0 == count % 5000) {
                  ps_insert.executeBatch();
                }

                // commit every 50000 entries
                if (0 == count % 50000) {
                  database.getConnection().commit();
                }
              } else {
                LOGGER.warn("CSV line couldn't be parsed: " + line);
              }
            }

            // excute the remaining batch inserts
            ps_insert.executeBatch();
            if (listener != null && count % 1000 != 0) {
              listener.imported(count);
            }
          } catch (IOException e) {
            throw new StatisticsStoreException("Error while reading text.", e);
          } catch (ParseException e) {
            throw new StatisticsStoreException("Error while converting from CSV.", e);
          } finally {
            database.getConnection().setAutoCommit(true);
          }
        } catch (SQLException e) {
          throw new StatisticsStoreException("Error while starting the transaction.", e);
        }
      } catch (SQLException e) {
        throw new StatisticsStoreException("Error dropping the statistic log indexes.", e);
      } finally {
        if (listener != null) {
          listener.optimizing();
        }

        try {
          createStatisticLogIndexes();
        } catch (SQLException e) {
          LOGGER.warn("Couldn't re-create the statistic log indexes.", e);
        }
      }

    } catch (SQLException e) {
      throw new StatisticsStoreException("Error while preparing SQL statement '" + SQL_INSERT_STATISTICSDATA + "'.", e);
    } finally {
      try {
        if (ps_insert != null) {
          ps_insert.close();
        }
      } catch (SQLException e) {
        LOGGER.warn("Couldn't close the prepared statement for SQL '" + SQL_INSERT_STATISTICSDATA + "'.", e);
      }

      recreateCaches();
    }

    if (listener != null) {
      listener.finished(count);
    }
  }

  public void retrieveStatistics(final StatisticsRetrievalCriteria criteria, final StatisticDataUser user)
      throws StatisticsStoreException {
    Assert.assertNotNull("criteria", criteria);

    try {
      database.ensureExistingConnection();

      List sql_where = new ArrayList();
      if (criteria.getAgentIp() != null) {
        sql_where.add("agentip = ?");
      }
      if (criteria.getAgentDifferentiator() != null) {
        sql_where.add("agentdifferentiator = ?");
      }
      if (criteria.getSessionId() != null) {
        sql_where.add("sessionid = ?");
      }
      if (criteria.getStart() != null) {
        sql_where.add("moment >= ?");
      }
      if (criteria.getStop() != null) {
        sql_where.add("moment <= ?");
      }
      if (criteria.getNames().size() > 0) {
        StringBuffer where_names = new StringBuffer();
        for (int i = 0; i < criteria.getNames().size(); i++) {
          if (where_names.length() > 0) {
            where_names.append(", ");
          }
          where_names.append("?");
        }
        sql_where.add("statname IN (" + where_names + ")");
      }
      if (criteria.getElements().size() > 0) {
        StringBuffer where_elements = new StringBuffer();
        for (int i = 0; i < criteria.getElements().size(); i++) {
          if (where_elements.length() > 0) {
            where_elements.append(", ");
          }
          where_elements.append("?");
        }
        sql_where.add("statelement IN (" + where_elements + ")");
      }

      StringBuffer sql = new StringBuffer("SELECT * FROM statisticlog");
      if (sql_where.size() > 0) {
        sql.append(" WHERE ");
        boolean first = true;
        for (Iterator it = sql_where.iterator(); it.hasNext();) {
          if (first) {
            first = false;
          } else {
            sql.append(" AND ");
          }
          sql.append(it.next());
        }
      }
      sql.append(" ORDER BY sessionid ASC, moment ASC, statname ASC, statelement ASC, id ASC");

      JdbcHelper.executeQuery(database.getConnection(), sql.toString(), new PreparedStatementHandler() {
        public void setParameters(PreparedStatement statement) throws SQLException {
          int param = 1;
          if (criteria.getAgentIp() != null) {
            statement.setString(param++, criteria.getAgentIp());
          }
          if (criteria.getAgentDifferentiator() != null) {
            statement.setString(param++, criteria.getAgentDifferentiator());
          }
          if (criteria.getSessionId() != null) {
            statement.setString(param++, criteria.getSessionId());
          }
          if (criteria.getStart() != null) {
            statement.setTimestamp(param++, new Timestamp(criteria.getStart().getTime()));
          }
          if (criteria.getStop() != null) {
            statement.setTimestamp(param++, new Timestamp(criteria.getStop().getTime()));
          }
          if (criteria.getNames().size() > 0) {
            for (Iterator it = criteria.getNames().iterator(); it.hasNext();) {
              statement.setString(param++, (String) it.next());
            }
          }
          if (criteria.getElements().size() > 0) {
            for (Iterator it = criteria.getElements().iterator(); it.hasNext();) {
              statement.setString(param++, (String) it.next());
            }
          }
        }
      }, new ResultSetHandler() {
        public void useResultSet(ResultSet resultSet) throws SQLException {
          while (resultSet.next()) {
            StatisticData data = database.getStatisticsData(resultSet.getString("sessionid"), resultSet);

            // consume the data
            if (!user.useStatisticData(data)) { return; }
          }
        }
      });
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new StatisticsStoreRetrievalErrorException(criteria, e);
    }
  }

  public void aggregateStatisticsData(final Writer writer, final TextualDataFormat format, final String sessionId,
                                      final String agentDifferentiator, final String[] names, final String[] elements,
                                      final Long interval) throws StatisticsStoreException {
    Assert.assertNotNull("format", format);
    Assert.assertNotNull("sessionId", sessionId);
    Assert.assertNotNull("agentDifferentiator", agentDifferentiator);
    Assert.assertNotNull("names", names);
    Assert.assertTrue("names array can't be empty", names.length > 0);

    final boolean xml_format = TextualDataFormat.XML.equals(format);
    try {
      if (xml_format) {
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<data>\n");
      }

      final boolean[] has_data = new boolean[] { false };

      final StatisticsRetrievalCriteria criteria = new StatisticsRetrievalCriteria().sessionId(sessionId)
          .agentDifferentiator(agentDifferentiator).setNames(names).setElements(elements);
      if (interval != null) {
        long now = System.currentTimeMillis();
        criteria.start(new Date(now - interval.longValue()));
      }
      retrieveStatistics(criteria, new StatisticDataUser() {
        private Date lastMoment   = null;
        private int  valueCounter = 0;

        public boolean useStatisticData(final StatisticData data) {
          try {
            if (null == lastMoment || !lastMoment.equals(data.getMoment())) {
              valueCounter = 0;

              if (null == lastMoment) {
                has_data[0] = true;
                if (xml_format) {
                  writer.write("<d>");
                }
              } else {
                if (xml_format) {
                  writer.write("</d>\n<d>");
                } else {
                  writer.write("\n");
                }
              }
              if (xml_format) {
                writer.write("<m>");
                writer.write(String.valueOf(data.getMoment().getTime()));
                writer.write("</m>");
              } else {
                writer.write(String.valueOf(data.getMoment().getTime()));
              }
              lastMoment = data.getMoment();
            }

            final Object data_value = data.getData();
            if (data_value != null) {
              valueCounter++;

              if (xml_format) {
                writer.write("<v" + valueCounter + ">");
                if (data_value instanceof String) {
                  writer.write(StringEscapeUtils.escapeXml(data_value.toString()));
                } else if (data_value instanceof Date) {
                  writer.write(String.valueOf(((Date) data_value).getTime()));
                } else {
                  writer.write(String.valueOf(data_value));
                }
                writer.write("</v" + valueCounter + ">");
              } else {
                writer.write(",");
                if (data_value instanceof String) {
                  writer.write(StatisticData.escapeForCsv(data_value.toString()));
                } else if (data_value instanceof Date) {
                  writer.write(String.valueOf(((Date) data_value).getTime()));
                } else {
                  writer.write(String.valueOf(data_value));
                }
              }
            }
          } catch (IOException e) {
            LOGGER.warn("Unexpected error while writing aggregated statistic data.", e);
            return false;
          }

          return true;
        }
      });
      if (xml_format) {
        if (has_data[0]) {
          writer.write("</d>");
        }
        writer.write("\n</data>");
      }
    } catch (IOException e) {
      LOGGER.warn("Unexpected error while writing aggregated statistic data.", e);
    }
  }

  public void retrieveStatisticsAsCsvStream(final OutputStream os, final String filenameBase,
                                            final StatisticsRetrievalCriteria criteria, final boolean zipContents)
      throws StatisticsStoreException {
    final OutputStream out;

    try {
      try {
        final ZipOutputStream zipstream;
        if (!zipContents) {
          zipstream = null;
          out = os;
        } else {
          zipstream = new ZipOutputStream(os);
          zipstream.setLevel(9);
          zipstream.setMethod(ZipOutputStream.DEFLATED);
          out = zipstream;
        }

        try {
          if (zipstream != null) {
            final ZipEntry zipentry = new ZipEntry(filenameBase + ".csv");
            zipentry.setComment(StatisticDataCSVParser.CURRENT_CSV_VERSION);
            zipstream.putNextEntry(zipentry);
          }

          try {
            out.write(StatisticDataCSVParser.CURRENT_CSV_HEADER.getBytes("UTF-8"));
            retrieveStatistics(criteria, new StatisticDataUser() {
              public boolean useStatisticData(final StatisticData data) {
                try {
                  out.write(data.toCsv().getBytes("UTF-8"));
                } catch (IOException e) {
                  // should never happen
                  throw new TCRuntimeException(e);
                }
                return true;
              }
            });
          } finally {
            if (zipstream != null) {
              zipstream.closeEntry();
            }
          }
        } finally {
          if (zipstream != null) {
            zipstream.close();
          }
        }
      } finally {
        os.close();
      }
    } catch (IOException e) {
      throw new StatisticsStoreRetrievalErrorException(criteria, e);
    }
  }

  public String[] getAvailableSessionIds() throws StatisticsStoreException {
    final List results = new ArrayList();
    try {
      database.ensureExistingConnection();

      JdbcHelper.executeQuery(database.getPreparedStatement(SQL_GET_AVAILABLE_SESSIONIDS), new ResultSetHandler() {
        public void useResultSet(ResultSet resultSet) throws SQLException {
          while (resultSet.next()) {
            results.add(resultSet.getString("sessionid"));
          }
        }
      });
    } catch (Exception e) {
      throw new StatisticsStoreSessionIdsRetrievalErrorException(e);
    }

    return (String[]) results.toArray(new String[results.size()]);
  }

  public String[] getAvailableAgentDifferentiators(final String sessionId) throws StatisticsStoreException {
    final List results = new ArrayList();
    try {
      database.ensureExistingConnection();

      JdbcHelper.executeQuery(database.getConnection(), SQL_GET_AVAILABLE_AGENT_DIFFERENTIATORS,
                              new PreparedStatementHandler() {
                                public void setParameters(PreparedStatement statement) throws SQLException {
                                  statement.setString(1, sessionId);
                                }
                              }, new ResultSetHandler() {
                                public void useResultSet(ResultSet resultSet) throws SQLException {
                                  while (resultSet.next()) {
                                    results.add(resultSet.getString("agentdifferentiator"));
                                  }
                                }
                              });
    } catch (Exception e) {
      throw new StatisticsStoreException("getAvailableNodes", e);
    }

    return (String[]) results.toArray(new String[results.size()]);
  }

  public void clearStatistics(final String sessionId) throws StatisticsStoreException {
    try {
      database.ensureExistingConnection();

      // remove statistics, based on the provided session Id
      JdbcHelper.executeUpdate(database.getConnection(), SQL_CLEAR_SESSION_STATISTICS, new PreparedStatementHandler() {
        public void setParameters(PreparedStatement statement) throws SQLException {
          statement.setString(1, sessionId);
        }
      });
    } catch (Exception e) {
      throw new StatisticsStoreClearStatisticsErrorException(sessionId, e);
    }

    fireSessionCleared(sessionId);
  }

  public void clearAllStatistics() throws StatisticsStoreException {
    try {
      database.ensureExistingConnection();

      JdbcHelper.executeUpdate(database.getConnection(), SQL_CLEAR_ALL_STATISTICS);
    } catch (Exception e) {
      throw new StatisticsStoreClearAllStatisticsErrorException(e);
    }

    fireAllSessionsCleared();
  }

  public void addListener(final StatisticsStoreListener listener) {
    if (null == listener) { return; }

    listeners.add(listener);
  }

  public void removeListener(final StatisticsStoreListener listener) {
    if (null == listener) { return; }

    listeners.remove(listener);
  }

  private void fireOpened() {
    if (listeners.size() > 0) {
      for (Iterator it = listeners.iterator(); it.hasNext();) {
        ((StatisticsStoreListener) it.next()).opened();
      }
    }
  }

  private void fireClosed() {
    if (listeners.size() > 0) {
      for (Iterator it = listeners.iterator(); it.hasNext();) {
        ((StatisticsStoreListener) it.next()).closed();
      }
    }
  }

  private void fireSessionCleared(final String sessionId) {
    if (listeners.size() > 0) {
      for (Iterator it = listeners.iterator(); it.hasNext();) {
        ((StatisticsStoreListener) it.next()).sessionCleared(sessionId);
      }
    }
  }

  private void fireAllSessionsCleared() {
    if (listeners.size() > 0) {
      for (Iterator it = listeners.iterator(); it.hasNext();) {
        ((StatisticsStoreListener) it.next()).allSessionsCleared();
      }
    }
  }
}
