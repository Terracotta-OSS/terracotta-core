/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import org.apache.log4j.Layout;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.helpers.CountingQuietWriter;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TCRollingFileAppender extends RollingFileAppender {
  private static final PatternLayout DUMP_PATTERN_LAYOUT  = new PatternLayout(TCLogging.DUMP_PATTERN);
  private static final PatternLayout DERBY_PATTERN_LAYOUT = new PatternLayout(TCLogging.DERBY_PATTERN);

  private final Pattern              pattern              = Pattern.compile("([^\\.]+)(\\.)(.*)");
  private Matcher                    matcher              = null;
  private String                     fileNamePrefix       = null;
  private String                     fileNameSuffix       = null;
  private boolean                    isNameWithExtension  = false;

  public TCRollingFileAppender(Layout layout, String logPath, boolean append) throws IOException {
    super(layout, logPath, append);
    matcher = pattern.matcher(fileName);
    isNameWithExtension = matcher.matches();
    if (isNameWithExtension) {
      fileNamePrefix = matcher.group(1);
      fileNameSuffix = matcher.group(3);
    }
  }

  @Override
  public void subAppend(LoggingEvent event) {
    Layout prevLayout = this.getLayout();
    try {
      if (event.getLoggerName().equals(TCLogging.DUMP_LOGGER_NAME)) {
        this.setLayout(DUMP_PATTERN_LAYOUT);
      } else if (event.getLoggerName().equals(TCLogging.DERBY_LOGGER_NAME)) {
        this.setLayout(DERBY_PATTERN_LAYOUT);
      }
      super.subAppend(event);
    } finally {
      this.setLayout(prevLayout);
    }
  }

  @Override
  public void rollOver() {
    if (isNameWithExtension) {
      rollOverBeautiful();
    } else {
      super.rollOver();
    }
  }

  /**
   * Renames files in a cleaner fashion. {@see https://jira.terracotta.org/jira/browse/DEV-8372}
   */
  private void rollOverBeautiful() {
    File file;
    File target;
    long nextRollover = 0;


    if (qw != null) {
      long size = ((CountingQuietWriter) qw).getCount();
      LogLog.debug("rolling over count=" + size);
      // if operation fails, do not roll again until
      // maxFileSize more bytes are written
      nextRollover = size + maxFileSize;
    }
    LogLog.debug("maxBackupIndex=" + maxBackupIndex);

    boolean renameSucceeded = true;
    // If maxBackups <= 0, then there is no file renaming to be done.
    if (maxBackupIndex > 0) {
      // Delete the oldest file, to keep Windows happy.
      file = new File(getFileName(maxBackupIndex));
      if (file.exists()) renameSucceeded = file.delete();

      // Map {(maxBackupIndex - 1), ..., 2, 1} to {maxBackupIndex, ..., 3, 2}
      for (int i = maxBackupIndex - 1; i >= 1 && renameSucceeded; i--) {
        file = new File(getFileName(i));
        if (file.exists()) {
          target = new File(getFileName(i + 1));
          LogLog.debug("Renaming file " + file + " to " + target);
          renameSucceeded = file.renameTo(target);
        }
      }

      if (renameSucceeded) {
        // Rename fileName to fileName.1
        target = new File(getFileName(1));

        this.closeFile(); // keep windows happy.

        file = new File(fileName);
        LogLog.debug("Renaming file " + file + " to " + target);
        renameSucceeded = file.renameTo(target);
        //
        // if file rename failed, reopen file with append = true
        //
        if (!renameSucceeded) {
          try {
            this.setFile(fileName, true, bufferedIO, bufferSize);
          } catch (IOException e) {
            if (e instanceof InterruptedIOException) {
              Thread.currentThread().interrupt();
            }
            LogLog.error("setFile(" + fileName + ", true) call failed.", e);
          }
        }
      }
    }

    //
    // if all renames were successful, then
    //
    if (renameSucceeded) {
      try {
        // This will also close the file. This is OK since multiple
        // close operations are safe.
        this.setFile(fileName, false, bufferedIO, bufferSize);
        nextRollover = 0;
      } catch (IOException e) {
        if (e instanceof InterruptedIOException) {
          Thread.currentThread().interrupt();
        }
        LogLog.error("setFile(" + fileName + ", false) call failed.", e);
      }
    }

  }

  private String getFileName(int i) {
    return fileNamePrefix + "-" + i + "." + fileNameSuffix;
  }
}
