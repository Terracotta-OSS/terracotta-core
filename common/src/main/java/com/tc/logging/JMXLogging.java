/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.logging;

import org.apache.log4j.PatternLayout;

public class JMXLogging {

  private static JMXAppender        jmxAppender;

  static { 
    // all logging goes to JMX based appender
    jmxAppender = new JMXAppender();
    jmxAppender.setLayout(new PatternLayout(TCLogging.FILE_AND_JMX_PATTERN));
    jmxAppender.setName("JMX appender");
    TCLogging.addToAllLoggers(jmxAppender);
  }
  
  public static JMXAppender getJMXAppender() {
    return jmxAppender;
  }
}
