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
