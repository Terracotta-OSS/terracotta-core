/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.text;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.io.IOException;
import java.io.StringWriter;

public class LogWriter extends StringWriter {
  private TCLogger logger = TCLogging.getLogger(LogWriter.class);

  public LogWriter(TCLogger logger) {
    this.logger = logger;
  }
  
  @Override
  public void flush() {
    StringBuffer buffer = getBuffer();
    if (buffer.length() <= 0) { return; }
    logger.info(buffer.toString());
    buffer.delete(0, buffer.length());
  }

  @Override
  public void close() throws IOException {
    super.close();
    flush();
  }
}
