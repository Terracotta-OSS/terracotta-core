/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

public class TCRollingFileAppender extends RollingFileAppender {
  private static final PatternLayout DUMP_PATTERN_LAYOUT  = new PatternLayout(TCLogging.DUMP_PATTERN);
  private static final PatternLayout DERBY_PATTERN_LAYOUT = new PatternLayout(TCLogging.DERBY_PATTERN);
  private long                       nextRollover         = 0;
  private String                     fileNamePrefix       = "";
  private String                     fileNameSuffix       = "";
  public TCRollingFileAppender(Layout layout, String logPath, boolean append) throws IOException {
    super(layout, logPath, append);
    int index = logPath.lastIndexOf('.');
    if (index != -1) {
      fileNamePrefix = logPath.substring(0, index);
      fileNameSuffix = logPath.substring(index);
    } else fileNamePrefix = logPath;
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

    File target;
    File file;

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
      file = new File(fileNamePrefix + '.' + maxBackupIndex + fileNameSuffix);
      if (file.exists()) renameSucceeded = file.delete();

      // Map {(maxBackupIndex - 1), ..., 2, 1} to {maxBackupIndex, ..., 3, 2}
      for (int i = maxBackupIndex - 1; i >= 1 && renameSucceeded; i--) {
        file = new File(fileNamePrefix + "." + i + fileNameSuffix);
        if (file.exists()) {
          target = new File(fileNamePrefix + '.' + (i + 1) + fileNameSuffix);
          LogLog.debug("Renaming file " + file + " to " + target);
          renameSucceeded = file.renameTo(target);
        }
      }

      if (renameSucceeded) {
        // Rename fileName to fileName.1
        target = new File(fileNamePrefix + "." + 1 + fileNameSuffix);

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

}
