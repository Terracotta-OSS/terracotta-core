/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import org.apache.log4j.Layout;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;

public class TCRollingFileAppender extends RollingFileAppender {
  private static final PatternLayout DUMP_PATTERN_LAYOUT = new PatternLayout(TCLogging.DUMP_PATTERN);

  public TCRollingFileAppender(Layout layout, String logPath, boolean append) throws IOException {
    super(layout, logPath, append);
  }

  @Override
  public void subAppend(LoggingEvent event) {
    Layout prevLayout = this.getLayout();
    try {
      if (event.getLoggerName().equals(TCLogging.DUMP_LOGGER_NAME)) {
        this.setLayout(DUMP_PATTERN_LAYOUT);
      }
      super.subAppend(event);
    } finally {
      this.setLayout(prevLayout);
    }
  }
}
