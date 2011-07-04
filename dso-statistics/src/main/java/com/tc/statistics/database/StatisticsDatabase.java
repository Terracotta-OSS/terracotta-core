/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.database;

import com.tc.statistics.StatisticData;
import com.tc.statistics.database.exceptions.StatisticsDatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface StatisticsDatabase {
  public Connection getConnection();

  public PreparedStatement createPreparedStatement(String sql) throws StatisticsDatabaseException;

  public PreparedStatement getPreparedStatement(String sql);

  public void reinitialize() throws StatisticsDatabaseException;

  public void open() throws StatisticsDatabaseException;

  public void ensureExistingConnection() throws StatisticsDatabaseException;

  public void close() throws StatisticsDatabaseException;

  public StatisticData getStatisticsData(String sessionId, ResultSet resultSet) throws SQLException;

  public void createVersionTable() throws SQLException;

  public void checkVersion(int currentVersion, long currentChecksum) throws StatisticsDatabaseException;
}
