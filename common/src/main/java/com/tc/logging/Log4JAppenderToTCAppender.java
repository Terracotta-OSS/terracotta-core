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

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

public class Log4JAppenderToTCAppender extends AppenderSkeleton {

  private final TCAppender appender;

  public Log4JAppenderToTCAppender(TCAppender appender) {
    this.appender = appender;
  }

  @Override
  protected void append(LoggingEvent event) {
    ThrowableInformation throwableInformation = event.getThrowableInformation();
    Throwable t = (throwableInformation == null) ? null : throwableInformation.getThrowable();
    appender.append(LogLevelImpl.fromLog4JLevel(event.getLevel()), event.getMessage(), t);
  }

  @Override
  public void close() {
    //
  }

  @Override
  public boolean requiresLayout() {
    return false;
  }

}
