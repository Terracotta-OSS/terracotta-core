/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans.object;

import org.apache.commons.io.FileUtils;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.util.DbBackup;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.AbstractNotifyingMBean;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;

import javax.management.NotCompliantMBeanException;

public class ServerDBBackup extends AbstractNotifyingMBean implements ServerDBBackupMBean {

  private boolean                   enabled;
  private final String              defaultPathForBackup;
  private final long                throttleTime;
  private final SynchronizedBoolean isBackupRunning;
  private String                    envHome;
  private Environment               env;
  private DbBackup                  backupHelper;
  private FileLoggerForBackup       backupFileLogger;
  private static final TCLogger     logger = TCLogging.getLogger(ServerDBBackup.class);

  // Only used for Unit Test cases
  public ServerDBBackup(String destDir) throws NotCompliantMBeanException {
    super(ServerDBBackupMBean.class);
    throttleTime = 0;
    defaultPathForBackup = destDir;
    isBackupRunning = new SynchronizedBoolean(false);
  }

  public ServerDBBackup(L2ConfigurationSetupManager configSetupManager) throws NotCompliantMBeanException {
    super(ServerDBBackupMBean.class);

    isBackupRunning = new SynchronizedBoolean(false);

    String destDir = safeFilePath(configSetupManager.commonl2Config().serverDbBackupPath());
    throttleTime = TCPropertiesImpl.getProperties().getLong(TCPropertiesConsts.L2_DATA_BACKUP_THROTTLE_TIME, 0);

    if (destDir == null) {
      destDir = safeFilePath(configSetupManager.commonl2Config().dataPath());
      destDir = destDir + File.separator + "backup";
    }

    defaultPathForBackup = destDir;
  }

  private static String safeFilePath(File file) {
    try {
      return file.getCanonicalPath();
    } catch (IOException ioe) {
      return file.getAbsolutePath();
    }
  }

  private void checkEnabled() {
    if (!isBackupEnabled()) {
      RuntimeException e = new RuntimeException(
                                                "The Terracotta Server instance might not have been started in persistent mode. So the requested operation cannot be performed.");

      backupFailed(e);
      throw e;
    }
  }

  public boolean isBackUpRunning() {
    checkEnabled();
    return isBackupRunning.get();
  }

  public String getDefaultPathForBackup() {
    // checkEnabled();
    return defaultPathForBackup;
  }

  public void runBackUp() throws IOException {
    checkEnabled();
    runBackUp(defaultPathForBackup);
  }

  public void runBackUp(String destinationDir) throws IOException {
    if (destinationDir == null) destinationDir = defaultPathForBackup;

    backupFileLogger = new FileLoggerForBackup(destinationDir);

    destinationDir = destinationDir + File.separator + L2DSOConfig.OBJECTDB_DIRNAME;
    backupFileLogger.logStartMessage();
    logger.info("Starting backup");

    checkEnabled();

    if (!isBackupRunning.commit(false, true)) {
      RuntimeException e = new RuntimeException("Another Backup already in progress. Please try after some time.");
      backupFailed(e);
      throw e;
    }

    logger.info("The destination directory is:" + destinationDir);

    try {
      sendNotification(BACKUP_STARTED, this);
      performBackup(destinationDir);
    } catch (DatabaseException e) {
      logger.warn(e);
      backupFailed(e);
      throw new IOException(e.getMessage());
    } catch (IOException e) {
      backupFailed(e);
      throw e;
    } catch (Exception e) {
      backupFailed(e);
      throw new RuntimeException(e);
    } finally {
      isBackupRunning.set(false);
    }
    logger.info("Backup Successfully Completed");
    backupFileLogger.logCompletedMessage();
    sendNotification(BACKUP_COMPLETED, this);
  }

  private void performBackup(String destinationDir) throws DatabaseException, IOException {
    long lastFileCopiedInPrevBackup = readLastFileCopied(destinationDir);

    if (backupHelper == null) backupHelper = new DbBackup(env);

    // Start backup, find out what needs to be copied.
    backupHelper.startBackup();

    String[] filesForBackup = null;

    if (lastFileCopiedInPrevBackup != -1) {
      filesForBackup = backupHelper.getLogFilesInBackupSet(lastFileCopiedInPrevBackup);
      backupFileLogger.logMessage("Taking Incremental Backup");
    } else {
      filesForBackup = backupHelper.getLogFilesInBackupSet();
      backupFileLogger.logMessage("Taking Full Backup");
    }

    logger.info("Total Number of files to be copied:" + filesForBackup.length);

    try {
      // Copy the files to archival storage.
      copyFiles(filesForBackup, envHome, destinationDir);
    } finally {
      backupHelper.endBackup();
    }
  }

  public void reset() {
    //
  }

  /**
   * Finds the file number of the last file in the previous backup
   * 
   * @param path - Path of the destination directory
   * @return long - the last file number
   * @throws IOException
   */
  public long readLastFileCopied(String path) throws IOException {
    File dir = new File(path);
    if (!dir.exists() || (dir.exists() && !dir.isDirectory())) {
      if (!dir.mkdirs()) { throw new IOException("Failed to create a directory at the following path:" + path); }
      return -1;
    }

    FilenameFilter filter = new FilenameFilter() {
      public boolean accept(File directory, String name) {
        if (name.toLowerCase().endsWith(".jdb")) return true;
        return false;
      }
    };
    String[] list = null;

    try {
      list = dir.list(filter);
    } catch (SecurityException e) {
      return -1;
    }
    if (list == null || list.length == 0) return -1;

    String tempStr = null;
    long tempLong = -1;
    long result = -1;
    for (int i = 0; i < list.length; i++) {
      tempStr = list[i].substring(0, list[i].length() - 4);
      try {
        tempLong = Long.parseLong(tempStr, 16);
      } catch (NumberFormatException e) {
        logger.warn("Ignoring the file name while scanning for the *.jdb files:" + list[i]);
      }
      if (result < tempLong) result = tempLong;
    }

    return result;
  }

  private void copyFiles(String[] files, String srcPath, String destPath) throws IOException {
    File destDir = new File(destPath);

    int percentageCopied = 0;
    for (int i = 0; i < files.length; i++) {
      File in = new File(srcPath + File.separator + files[i]);
      FileUtils.copyFileToDirectory(in, destDir);
      if (((i * 100) / files.length - percentageCopied) >= 5) {
        percentageCopied = (i * 100) / files.length;
        sendNotification(PERCENTAGE_COPIED, this, percentageCopied + " Percentage completed");
      }
      ThreadUtil.reallySleep(throttleTime);
    }
  }

  public void setDbEnvironment(Environment bdbEnv, File bdbEnvHome) {
    this.env = bdbEnv;
    if (bdbEnvHome != null) this.envHome = bdbEnvHome.getAbsolutePath();
    setBackupEnabled(env != null && envHome != null);
  }

  private void setBackupEnabled(boolean enabled) {
    if (this.enabled != enabled) {
      this.enabled = enabled;
      sendNotification(BACKUP_ENABLED, this, Boolean.toString(enabled));
    }
  }

  public boolean isBackupEnabled() {
    return enabled;
  }

  public String getDbHome() {
    checkEnabled();

    return envHome;
  }

  private void backupFailed(Exception e) {
    backupFileLogger.logExceptions(e);
    backupFileLogger.logStopMessage();
    logger.warn(e.getMessage());
    sendNotification(BACKUP_FAILED, this, e.getMessage());
  }

  class FileLoggerForBackup {
    private final String       logFilePath;
    public final static String BACKUP_STARTED_MSG   = "Backup Started at ";
    public final static String BACKUP_STOPPED_MSG   = "Backup Stopped at ";
    public final static String BACKUP_COMPLETED_MSG = "Backup Completed at ";

    public FileLoggerForBackup(String backupPath) {
      logFilePath = backupPath + File.separator + "backup.log";
      try {
        File dir = new File(backupPath);
        if (!dir.exists()) {
          dir.mkdirs();
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
      PrintWriter writer = null;
      try {
        writer = new PrintWriter(new FileOutputStream(logFilePath, true));
        writer.println(message);
        writer.flush();
      } catch (Exception e) {
        // do nothing
      } finally {
        closeWriter(writer);
      }
    }

    private void closeWriter(PrintWriter writer) {
      try {
        writer.close();
      } catch (Exception e) {
        // ignore
      }
    }
  }
}
