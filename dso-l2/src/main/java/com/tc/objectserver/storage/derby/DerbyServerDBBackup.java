/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.management.beans.object.AbstractServerDBBackup;
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.io.File;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.management.NotCompliantMBeanException;

public class DerbyServerDBBackup extends AbstractServerDBBackup {

  public DerbyServerDBBackup(L2ConfigurationSetupManager configSetupManager) throws NotCompliantMBeanException {
    super(DerbyServerDBBackup.class.getCanonicalName(), getBackUpDestinationDir(configSetupManager), configSetupManager
        .dsoL2Config().getPersistence().getMode());
    long backupThrottle = TCPropertiesImpl.getProperties().getLong(TCPropertiesConsts.L2_DATA_BACKUP_THROTTLE_TIME, 0);
    if (backupThrottle > 0) {
      logger.warn("Backup throttle property " + TCPropertiesConsts.L2_DATA_BACKUP_THROTTLE_TIME + " = "
                  + backupThrottle + " doesn't apply to DerbyDB.");
    }
  }

  public void initDbEnvironment(File envHome) {
    enableDbBackup(envHome);
  }

  private Connection createConnection() throws TCDatabaseException {
    try {
      Connection connection = DriverManager.getConnection(DerbyDBEnvironment.PROTOCOL + getDbHome() + File.separator
                                                          + DerbyDBEnvironment.DB_NAME + ";");
      return connection;
    } catch (SQLException e) {
      throw new TCDatabaseException(e);
    }
  }

  @Override
  public void runBackUp() throws IOException {
    runBackUp(getDefaultPathForBackup());
  }

  @Override
  public void runBackUp(String destinationDir) throws IOException {

    if (destinationDir == null) {
      destinationDir = getDefaultPathForBackup();
    }

    destinationDir = destinationDir + File.separator + L2DSOConfig.OBJECTDB_DIRNAME;
    FileLoggerForBackup backupFileLogger = new FileLoggerForBackup(destinationDir);
    backupFileLogger.logStartMessage();
    validateBackupEnvironment(backupFileLogger);

    logger.info("Starting Derby backup to " + destinationDir);
    try {
      sendNotification(BACKUP_STARTED, this);
      performBackup(destinationDir);
    } catch (SQLException e) {
      backupFailed(backupFileLogger, e);
      throw new IOException(e.getMessage());
    } catch (Exception e) {
      backupFailed(backupFileLogger, e);
      throw new RuntimeException(e);
    } finally {
      markBackupCompleted();
    }

    logger.info("Derby Backup Successfully Completed");
    backupFileLogger.logCompletedMessage();
    sendNotification(BACKUP_COMPLETED, this);

  }

  private void performBackup(String destinationDir) throws TCDatabaseException, SQLException {
    CallableStatement cs = createConnection().prepareCall("CALL SYSCS_UTIL.SYSCS_BACKUP_DATABASE(?)");
    try {
      cs.setString(1, destinationDir);
      cs.execute();
    } catch (SQLException e) {
      logger.info("Unable to perform Derby Backup " + e);
      throw e;
    } finally {
      cs.close();
    }
  }
}
