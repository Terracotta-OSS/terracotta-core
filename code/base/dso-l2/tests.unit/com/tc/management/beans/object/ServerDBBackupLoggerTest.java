/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans.object;

import com.tc.management.beans.object.ServerDBBackup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.management.NotCompliantMBeanException;

import junit.framework.TestCase;

public class ServerDBBackupLoggerTest extends TestCase {

  private String[]            logMessages   = { "hi", "these", "are", "log", "messages" };
  private static final String EXCEPTION_MSG = "Exception in test";

  public void testLoggerFileExists() {
    String tempDir = "serverdbBackup" + File.separator + "testDirExists";

    ServerDBBackup.FileLoggerForBackup logger = createLogger(tempDir);
    assertNotNull(logger);

    File file = new File(tempDir + File.separator + "backup.log");
    assertTrue(file.exists());

    String errorDir = "serverdbBackup" + File.separator + "tempFile";
    createNewFile(errorDir);
    logger = createLogger(errorDir);

    file = new File(errorDir + File.separator + "backup.log");
    assertFalse(file.exists());
  }

  public void testLoggerMessages() throws IOException {
    String tempDir = "serverdbBackup" + File.separator + "loggerMessages";
    String filePath = tempDir + File.separator + "backup.log";
    ifFileExistsThenDelete(filePath);

    ServerDBBackup.FileLoggerForBackup logger = createLogger(tempDir);

    assertTrue(new File(filePath).exists());

    logMessages(logger);
    readAndVerifyMessages(filePath);
  }

  private void ifFileExistsThenDelete(String filePath) {
    File file = new File(filePath);
    file.delete();
  }

  private void readAndVerifyMessages(String filePath) throws FileNotFoundException, IOException {
    BufferedReader reader = new BufferedReader(new FileReader(filePath));

    String line = null;
    for (int i = 0; i < logMessages.length; i++)
      assertEquals(logMessages[i], reader.readLine());

    line = reader.readLine();
    assertTrue(line.startsWith(ServerDBBackup.FileLoggerForBackup.BACKUP_STARTED_MSG));

    line = reader.readLine();
    assertTrue(line.endsWith(EXCEPTION_MSG));

    line = reader.readLine();
    assertTrue(line.startsWith(ServerDBBackup.FileLoggerForBackup.BACKUP_STOPPED_MSG));

    reader.close();
  }

  private void logMessages(ServerDBBackup.FileLoggerForBackup logger) {
    for (int i = 0; i < logMessages.length; i++)
      logger.logMessage(logMessages[i]);

    logger.logStartMessage();
    logger.logExceptions(new RuntimeException(EXCEPTION_MSG));
    logger.logStopMessage();
  }

  private void createNewFile(String errorDir) {
    try {
      new File(errorDir).createNewFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public ServerDBBackup.FileLoggerForBackup createLogger(String tempDir) {
    ServerDBBackup serverDBBackup;
    try {
      serverDBBackup = new ServerDBBackup(tempDir);
    } catch (NotCompliantMBeanException e) {
      throw new RuntimeException(e);
    }
    ServerDBBackup.FileLoggerForBackup logger = null;
    try {
      logger = serverDBBackup.new FileLoggerForBackup(tempDir);
    } catch (Exception e) {
      fail("Should never throw an exception");
    }
    return logger;
  }
}
