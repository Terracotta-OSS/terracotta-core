/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.database.impl;

import org.apache.commons.io.FileUtils;

import com.tc.statistics.database.exceptions.TCStatisticsDatabaseException;
import com.tc.statistics.database.exceptions.TCStatisticsDatabaseOpenErrorException;
import com.tc.util.Assert;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class H2StatisticsDatabase extends AbstractStatisticsDatabase {
  private final static String H2_JDBC_DRIVER = "org.h2.Driver";
  private final static String H2_URL_PREFIX = "jdbc:h2:";
  private final static String H2_USER = "sa";
  private final static String H2_PASSWORD = "";

  private final File dbDir;
  private final String urlSuffix;

  public H2StatisticsDatabase(final File dbDir, final String urlSuffix) {
    if (null == dbDir) Assert.fail("dbDir can't be null");
    if (!dbDir.exists()) Assert.fail("dbDir '" + dbDir.getAbsolutePath() + "' doesn't exist");
    if (!dbDir.isDirectory()) Assert.fail("dbDir '" + dbDir.getAbsolutePath() + "' is not a directory");
    if (!dbDir.canWrite()) Assert.fail("dbDir '" + dbDir.getAbsolutePath() + "' is not writable");
    if (null == urlSuffix) Assert.fail("urlSuffix can't be null");
    this.dbDir = dbDir;
    this.urlSuffix = urlSuffix+";LOG=0";
  }

  public synchronized void reinitialize() throws TCStatisticsDatabaseException {
    close();
    try {
      FileUtils.cleanDirectory(dbDir);
    } catch (IOException e) {
      throw new TCStatisticsDatabaseException("Unexpected error while reinitializing the H2 database at '"+dbDir.getAbsolutePath()+"'.", e);
    }
    open();
  }

  public synchronized void open() throws TCStatisticsDatabaseException {
    super.open(H2_JDBC_DRIVER);
  }

  protected void openConnection() throws TCStatisticsDatabaseException {
    String url = H2_URL_PREFIX + new File(dbDir, urlSuffix).getAbsolutePath();
    try {
      connection = DriverManager.getConnection(url, H2_USER, H2_PASSWORD);
      connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
      connection.setAutoCommit(true);
    } catch (SQLException e) {
      throw new TCStatisticsDatabaseOpenErrorException(url, H2_USER, H2_PASSWORD, e);
    }
  }
}