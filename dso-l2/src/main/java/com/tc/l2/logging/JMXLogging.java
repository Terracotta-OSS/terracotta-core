/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.l2.logging;

import com.tc.logging.TCLogging;
import com.tc.logging.TCLoggingService;
import org.apache.log4j.PatternLayout;

public class JMXLogging {

  private static JMXAppender        jmxAppender;

  static { 
//  hack to get the underlying service for logging implementation
    TCLoggingService service = TCLogging.getLoggingService();
    // all logging goes to JMX based appender
    if (service instanceof TCLoggingLog4J) {
      jmxAppender = new JMXAppender();
      jmxAppender.setLayout(new PatternLayout(TCLoggingLog4J.FILE_AND_JMX_PATTERN));
      jmxAppender.setName("JMX appender");
      ((TCLoggingLog4J)service).addToAllLoggers(jmxAppender);
    }
  }
  
  public static JMXAppender getJMXAppender() {
    return jmxAppender;
  }
}
