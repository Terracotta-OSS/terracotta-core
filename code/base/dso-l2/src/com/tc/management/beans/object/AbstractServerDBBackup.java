/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans.object;

import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.stats.AbstractNotifyingMBean;
import com.tc.util.Assert;
import com.terracottatech.config.PersistenceMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.NotCompliantMBeanException;

public abstract class AbstractServerDBBackup extends AbstractNotifyingMBean implements ServerDBBackupMBean {

  protected final TCLogger           logger;
  private final AtomicBoolean        enabled;
  private final String               defaultPathForBackup;
  private final AtomicBoolean        isBackupRunning;
  private final PersistenceMode.Enum persistenceMode;
  private static final String        DEFAULT_BACKUP_DIR = "backup";
  private volatile File              envHome;

  public AbstractServerDBBackup(String dbName, final String defaultPathForBackup, PersistenceMode.Enum persistence)
      throws NotCompliantMBeanException {
    super(ServerDBBackupMBean.class);
    this.enabled = new AtomicBoolean(false);
    this.isBackupRunning = new AtomicBoolean(false);
    this.persistenceMode = persistence;
    this.logger = TCLogging.getLogger(dbName);
    this.defaultPathForBackup = defaultPathForBackup;
  }

  public abstract void runBackUp() throws IOException;

  public abstract void runBackUp(String path) throws IOException;

  protected void enableDbBackup(File dbEnvHome) {
    Assert.assertNotNull(dbEnvHome);
    this.envHome = dbEnvHome;
    if (this.enabled.compareAndSet(false, true)) {
      sendNotification(BACKUP_ENABLED, this, Boolean.toString(enabled.get()));
    }
  }

  protected static String getBackUpDestinationDir(L2ConfigurationSetupManager configSetupManager) {
    String destDir = safeFilePath(configSetupManager.commonl2Config().serverDbBackupPath());
    if (destDir == null) {
      destDir = safeFilePath(configSetupManager.commonl2Config().dataPath());
      destDir = destDir + File.separator + DEFAULT_BACKUP_DIR;
    }
    return destDir;
  }

  protected static String safeFilePath(File file) {
    try {
      return file.getCanonicalPath();
    } catch (IOException ioe) {
      return file.getAbsolutePath();
    }
  }

  public void reset() {
    // XXX:
  }

  public boolean isBackUpRunning() {
    return this.isBackupRunning.get();
  }

  public String getDefaultPathForBackup() {
    return this.defaultPathForBackup;
  }

  public PersistenceMode.Enum getPersistenceMode() {
    return persistenceMode;
  }

  public boolean isBackupEnabled() {
    return this.enabled.get();
  }

  public String getDbHome() {
    return this.envHome.getAbsolutePath();
  }

  protected boolean markBackupRunning() {
    return isBackupRunning.compareAndSet(false, true);
  }

  protected void markBackupCompleted() {
    this.isBackupRunning.set(false);
  }

  protected void validateBackupEnvironment(final FileLoggerForBackup backupFileLogger) {
    if (!getPersistenceMode().equals(PersistenceMode.PERMANENT_STORE)) {
      RuntimeException e = new RuntimeException(
                                                "The Terracotta Server is not running in persistent mode. So DB backup cannot be performed.");
      backupFailed(backupFileLogger, e);
      throw e;
    }

    if (!isBackupEnabled()) {
      RuntimeException e = new RuntimeException(
                                                "The Terracotta Server backup is not yet ready for backup. Try reconnecting to server and perform backup again");

      backupFailed(backupFileLogger, e);
      throw e;
    }

    if (!markBackupRunning()) {
      RuntimeException e = new RuntimeException("Another Backup already in progress. Please try after some time.");
      backupFailed(backupFileLogger, e);
      throw e;
    }

  }

  protected void backupFailed(FileLoggerForBackup backupFileLogger, Exception e) {
    backupFileLogger.logExceptions(e);
    backupFileLogger.logStopMessage();
    logger.warn(e.getMessage());
    sendNotification(BACKUP_FAILED, this, e.getMessage());
  }

  protected static class FileLoggerForBackup {
    private final String          logFilePath;
    private final static TCLogger logger               = TCLogging.getLogger(FileLoggerForBackup.class);
    public final static String    BACKUP_STARTED_MSG   = "Backup Started at ";
    public final static String    BACKUP_STOPPED_MSG   = "Backup Stopped at ";
    public final static String    BACKUP_COMPLETED_MSG = "Backup Completed at ";
    private PrintWriter           writer;

    public FileLoggerForBackup(String backupPath) {
      this.logFilePath = backupPath + File.separator + "backup.log";
      try {
        File dir = new File(backupPath);
        if (!dir.exists()) {
          if (!dir.mkdirs()) {
            logger.warn("Could not create dir " + dir.getAbsolutePath() + " for log file.");
          }
        }
        File logFile = new File(logFilePath);
        if (!logFile.exists()) {
          if (!logFile.createNewFile()) {
            logger.warn("Could not create a log file under the path: " + backupPath);
          }
        }
      } catch (Exception e) {
        logger.warn("Could not create a log file under the path: " + backupPath);
      }

      try {
        this.writer = new PrintWriter(new FileOutputStream(logFilePath, true));
      } catch (FileNotFoundException e1) {
        logger.warn("Could not create file writer as file " + logFilePath + " not found.");
      }

    }

    public void logStartMessage() {
      String date = getCurrentDateTime();
      logMessage(BACKUP_STARTED_MSG + date);
    }

    public void logCompletedMessage() {
      String date = getCurrentDateTime();
      logMessage(BACKUP_COMPLETED_MSG + date);
      logMessage("");
    }

    public void logStopMessage() {
      String date = getCurrentDateTime();
      logMessage(BACKUP_STOPPED_MSG + date);
      logMessage("");
    }

    private String getCurrentDateTime() {
      DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
      String date = dateFormat.format(new Date());
      return date;
    }

    public void logExceptions(Exception e) {
      logMessage("Exception occured while taking the backup: " + e.getMessage());
    }

    public void logMessage(String message) {
      if (writer != null) {
        writer.println(message);
        writer.flush();
      }
    }

  }
}
