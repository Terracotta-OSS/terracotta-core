/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.storage.berkeleydb;

import org.apache.commons.io.FileUtils;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.util.DbBackup;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.management.beans.object.AbstractServerDBBackup;
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.management.NotCompliantMBeanException;

public class BerkeleyServerDBBackup extends AbstractServerDBBackup {

  private final long           throttleTime;
  private DbBackup             backupHelper;
  private volatile Environment env;

  public BerkeleyServerDBBackup(L2ConfigurationSetupManager configSetupManager) throws NotCompliantMBeanException {
    super(BerkeleyServerDBBackup.class.getName(), getBackUpDestinationDir(configSetupManager), configSetupManager
        .dsoL2Config().getPersistence().getMode());
    throttleTime = TCPropertiesImpl.getProperties().getLong(TCPropertiesConsts.L2_DATA_BACKUP_THROTTLE_TIME, 0);
  }

  public void initDbEnvironment(Environment bdbEnv, File bdbEnvHome) {
    this.env = bdbEnv;
    Assert.eval(bdbEnvHome != null);
    enableDbBackup(bdbEnvHome);
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

    logger.info("Starting BerkelyDB backup to " + destinationDir);
    try {
      sendNotification(BACKUP_STARTED, this);
      performBackup(destinationDir, backupFileLogger);
    } catch (DatabaseException e) {
      logger.warn(e);
      backupFailed(backupFileLogger, e);
      throw new IOException(e.getMessage());
    } catch (IOException e) {
      backupFailed(backupFileLogger, e);
      throw e;
    } catch (Exception e) {
      backupFailed(backupFileLogger, e);
      throw new RuntimeException(e);
    } finally {
      markBackupCompleted();
    }

    logger.info("BerkeleyDB Backup Successfully Completed");
    backupFileLogger.logCompletedMessage();
    sendNotification(BACKUP_COMPLETED, this);
  }

  private void performBackup(String destinationDir, FileLoggerForBackup backupFileLogger) throws DatabaseException,
      IOException {
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
      copyFiles(filesForBackup, getDbHome(), destinationDir);
    } finally {
      backupHelper.endBackup();
    }
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
    for (String element : list) {
      tempStr = element.substring(0, element.length() - 4);
      try {
        tempLong = Long.parseLong(tempStr, 16);
      } catch (NumberFormatException e) {
        logger.warn("Ignoring the file name while scanning for the *.jdb files:" + element);
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

}
