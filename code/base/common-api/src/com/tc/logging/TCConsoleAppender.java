/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

public class TCConsoleAppender extends ConsoleAppender {
  private static final PatternLayout DUMP_PATTERN_LAYOUT  = new PatternLayout(TCLogging.DUMP_PATTERN);
  private static final PatternLayout DERBY_PATTERN_LAYOUT = new PatternLayout(TCLogging.DERBY_PATTERN);

  public TCConsoleAppender(PatternLayout layout, String systemErr) {
    super(layout, systemErr);
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
}
