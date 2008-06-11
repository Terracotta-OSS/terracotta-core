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
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.logging.LogLevel;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
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
import java.util.HashMap;

import javax.management.ListenerNotFoundException;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;

public class ServerDBBackup extends AbstractNotifyingMBean implements ServerDBBackupMBean {

  private final String              defaultPathForBackup;
  private String                    currentPathForBackup;
  private final long                throttleTime;
  private final SynchronizedBoolean isBackupRunning;
  private String                    envHome;
  private Environment               env;
  private DbBackup                  backupHelper;
  private FileLoggerForBackup       backupFileLogger;
  private HashMap                   mapOfListeners = new HashMap();
  private static final TCLogger     logger         = TCLogging.getLogger(ServerDBBackup.class);

  // Only used for Unit Test cases
  public ServerDBBackup(String destDir) throws NotCompliantMBeanException {
    super(ServerDBBackupMBean.class);
    throttleTime = 0;
    defaultPathForBackup = destDir;
    isBackupRunning = new SynchronizedBoolean(false);
  }

  public ServerDBBackup(L2TVSConfigurationSetupManager configSetupManager) throws NotCompliantMBeanException {
    super(ServerDBBackupMBean.class);

    isBackupRunning = new SynchronizedBoolean(false);

    String destDir = configSetupManager.commonl2Config().serverDbBackupPath().getFile().getAbsolutePath();
    throttleTime = TCPropertiesImpl.getProperties().getLong(TCPropertiesConsts.L2_DATA_BACKUP_THROTTLE_TIME, 0);

    if (destDir == null) {
      destDir = configSetupManager.commonl2Config().dataPath().getFile().getAbsolutePath();
      destDir = destDir + File.separator + "backup";
    }

    defaultPathForBackup = destDir;
  }

  private void checkEnabled() {
    if (!isBackupEnabled()) {
      RuntimeException e = new RuntimeException(
                                                "The bean is still not enabled. So the requested operation cannot be performed.");

      backupFailed(e);
      throw e;
    }
  }

  public boolean isBackUpRunning() {
    checkEnabled();

    return isBackupRunning.get();
  }

  public String getAbsolutePathForBackup() {
    checkEnabled();
    return currentPathForBackup;
  }

  public void runBackUp() throws IOException {
    checkEnabled();

    runBackUp(defaultPathForBackup);
  }

  public void runBackUp(String destinationDir) throws IOException {
    backupFileLogger = new FileLoggerForBackup(destinationDir);

    backupFileLogger.logStartMessage();
    logger.log(LogLevel.INFO, "Starting backup");

    checkEnabled();

    if (!isBackupRunning.commit(false, true)) {
      RuntimeException e = new RuntimeException("Another Backup already in progress. Please try after some time.");
      backupFailed(e);
      throw e;
    }

    if (destinationDir == null) destinationDir = defaultPathForBackup;
    currentPathForBackup = destinationDir;

    logger.log(LogLevel.INFO, "The destination directory is:" + destinationDir);

    try {
      sendNotification(BACKUP_STARTED, this);
      performBackup(destinationDir);
    } catch (DatabaseException e) {
      logger.log(LogLevel.WARN, e);
      backupFailed(e);
      throw new IOException(e.getMessage());
    } catch (IOException e) {
      backupFailed(e);
      throw e;
    } finally {
      isBackupRunning.set(false);
    }
    logger.log(LogLevel.INFO, "Backup Successfully Completed");
    backupFileLogger.logStopMessage();
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

    logger.log(LogLevel.INFO, "Total Number of files to be copied:" + filesForBackup.length);

    // Copy the files to archival storage.
    copyFiles(filesForBackup, envHome, destinationDir);

    backupHelper.endBackup();
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
    if (!dir.exists()  || (dir.exists() && !dir.isDirectory())) {
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

  public void setDbEnvironment(Environment environment, File environmentHome) {
    this.env = environment;
    if (environmentHome != null) this.envHome = environmentHome.getAbsolutePath();
  }

  public boolean isBackupEnabled() {
    return (env != null && envHome != null);
  }

  public String getDbHome() {
    checkEnabled();

    return envHome;
  }

  private void backupFailed(Exception e) {
    backupFileLogger.logExceptions(e);
    backupFileLogger.logStopMessage();
    sendNotification(BACKUP_FAILED, this, e.getMessage());
  }

  class FileLoggerForBackup {
    private String logFilePath;

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
      DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
      String date = dateFormat.format(new Date());
      logMessage("Backup Started at " + date);
    }

    public void logStopMessage() {
      DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
      String date = dateFormat.format(new Date());
      logMessage("Backup Stopped at " + date);
      logMessage("");
    }

    public void logExceptions(Exception e) {
      logMessage("Exception occured while taking the backup: " + e.getMessage());
    }

    public void logMessage(String message) {
      try {
        PrintWriter writer = null;
        writer = new PrintWriter(new FileOutputStream(logFilePath, true));

        writer.println(message);
        writer.flush();
        writer.close();
      } catch (Exception e) {
        // do nothing
      }
    }
  }

  public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object obj,
                                      String listenerName) {
    mapOfListeners.put(listenerName, listener);
    super.addNotificationListener(listener, filter, obj);
  }

  public void removeNotificationListener(String listenerName) throws ListenerNotFoundException {
    NotificationListener listener = (NotificationListener)mapOfListeners.get(listenerName);
    super.removeNotificationListener(listener);
  }
}
