/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.database.impl;

import org.h2.tools.DeleteDbFiles;

import com.tc.statistics.database.exceptions.StatisticsDatabaseException;
import com.tc.statistics.database.exceptions.StatisticsDatabaseOpenErrorException;
import com.tc.util.Assert;

import java.io.File;
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

  /**
   * @param dbDir must already exist and be writable.
   */
  public H2StatisticsDatabase(final File dbDir, final String urlSuffix) {
    if (null == dbDir) Assert.fail("dbDir can't be null");
    if (!dbDir.exists()) Assert.fail("dbDir '" + dbDir.getAbsolutePath() + "' doesn't exist");
    if (!dbDir.isDirectory()) Assert.fail("dbDir '" + dbDir.getAbsolutePath() + "' is not a directory");
    if (!dbDir.canWrite()) Assert.fail("dbDir '" + dbDir.getAbsolutePath() + "' is not writable");
    if (null == urlSuffix) Assert.fail("urlSuffix can't be null");
    this.dbDir = dbDir;
    this.urlSuffix = urlSuffix;
  }

  public synchronized void reinitialize() throws StatisticsDatabaseException {
    close();
    try {
      DeleteDbFiles.execute(dbDir.getAbsolutePath(), urlSuffix, false);
    } catch (SQLException e) {
      throw new StatisticsDatabaseException("Unexpected error while reinitializing the H2 database at '"+dbDir.getAbsolutePath()+"' and '" + urlSuffix + "'.", e);
    }
    open();
  }

  public synchronized void open() throws StatisticsDatabaseException {
    // Prevent H2 to call an explicit System.gc() when trying to access
    // database files and directory repeatedly
    // (see org.h2.store.fs.FileSystemDisk). The System.gc() call happens
    // after 8 retries, hence setting this system property to 8 will prevent
    // the call from happening (the default is 16).
    System.setProperty("h2.maxFileRetry", "8");

    super.open(H2_JDBC_DRIVER);
  }

  @Override
  protected void openConnection() throws StatisticsDatabaseException {
    String url = H2_URL_PREFIX + new File(dbDir, urlSuffix).getAbsolutePath();
    try {
      connection = DriverManager.getConnection(url+";LOG=0;DB_CLOSE_ON_EXIT=FALSE", H2_USER, H2_PASSWORD);
      connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
      connection.setAutoCommit(true);
    } catch (SQLException e) {
      throw new StatisticsDatabaseOpenErrorException(url, H2_USER, H2_PASSWORD, e);
    }
  }
}