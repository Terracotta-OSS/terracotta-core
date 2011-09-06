/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.jdbc;

import com.tc.util.Assert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class JdbcHelper {
  private final static ThreadLocal checksumCalculator = new ThreadLocal();

  public static void calculateChecksum(CaptureChecksum capture) throws Exception {
    Assert.assertNotNull("capture", capture);
    checksumCalculator.set(new ChecksumCalculator());
    try {
      capture.execute();
    } finally {
      checksumCalculator.remove();
    }
  }

  public static ChecksumCalculator getActiveChecksumCalculator() {
    return (ChecksumCalculator)checksumCalculator.get();
  }

  private static void appendChecksumPart(String sql) {
    ChecksumCalculator csc = getActiveChecksumCalculator();
    if (csc != null) {
      csc.append(sql);
    }
  }

  public static long fetchNextSequenceValue(final PreparedStatement psNextId) throws SQLException {
    ResultSet rs_id = psNextId.executeQuery();
    try {
      rs_id.next();
      return rs_id.getLong(1);
    } finally {
      rs_id.close();
    }
  }

  public static void executeUpdate(final Connection connection, final String sql) throws SQLException {
    appendChecksumPart(sql);

    Statement stmt = connection.createStatement();
    try {
      stmt.executeUpdate(sql);
    } finally {
      stmt.close();
    }
  }

  public static int executeUpdate(final Connection connection, final String sql, final PreparedStatementHandler handler) throws SQLException {
    Assert.assertNotNull("connection", connection);
    Assert.assertNotNull("handler", handler);

    appendChecksumPart(sql);
    
    PreparedStatement ps_update = connection.prepareStatement(sql);
    try {
      handler.setParameters(ps_update);
      return ps_update.executeUpdate();
    } finally {
      ps_update.close();
    }
  }

  public static void executeQuery(final Connection connection, final String sql, final PreparedStatementHandler psHandler, final ResultSetHandler rsHandler) throws SQLException {
    Assert.assertNotNull("connection", connection);
    Assert.assertNotNull("psHandler", psHandler);
    Assert.assertNotNull("rsHandler", rsHandler);

    appendChecksumPart(sql);

    PreparedStatement ps_query = connection.prepareStatement(sql);
    try {
      psHandler.setParameters(ps_query);
      ps_query.execute();
      ResultSet rs = ps_query.getResultSet();
      try {
        rsHandler.useResultSet(rs);
      } finally {
        rs.close();
      }
    } finally {
      ps_query.close();
    }
  }

  public static void executeQuery(final Connection connection, final String sql) throws SQLException {
    executeQuery(connection, sql, null);
  }

  public static void executeQuery(final Connection connection, final String sql, final ResultSetHandler rsHandler) throws SQLException {
    Assert.assertNotNull("connection", connection);

    appendChecksumPart(sql);

    Statement stmt = connection.createStatement();
    try {
      stmt.execute(sql);
      if (rsHandler != null) {
        ResultSet rs = stmt.getResultSet();
        try {
          rsHandler.useResultSet(rs);
        } finally {
          rs.close();
        }
      }
    } finally {
      stmt.close();
    }
  }

  public static void executeQuery(final PreparedStatement preparedStatement, final ResultSetHandler rsHandler) throws SQLException {
    Assert.assertNotNull("preparedStatement", preparedStatement);
    Assert.assertNotNull("rsHandler", rsHandler);
    Assert.assertTrue("You can't call this method when a checksum is being calculated for the SQL statements that have been run.", null == getActiveChecksumCalculator());

    preparedStatement.execute();
    ResultSet rs = preparedStatement.getResultSet();
    try {
      rsHandler.useResultSet(rs);
    } finally {
      rs.close();
    }
  }
}