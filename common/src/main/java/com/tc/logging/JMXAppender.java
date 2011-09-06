/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.logging;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import com.tc.exception.TCRuntimeException;
import com.tc.management.beans.logging.TCLoggingBroadcaster;
import com.tc.management.beans.logging.TCLoggingBroadcasterMBean;

import javax.management.NotCompliantMBeanException;

/**
 * Special Appender that notifies JMX listeners on LoggingEvents.
 * 
 * @see org.apache.log4j.RollingFileAppender
 * @see TCLoggingBroadcasterMBean
 * @author gkeim
 */
public class JMXAppender extends AppenderSkeleton {

  private final TCLoggingBroadcaster broadcastingBean;

  public JMXAppender() {
    try {
      broadcastingBean = new TCLoggingBroadcaster();
    } catch (NotCompliantMBeanException ncmbe) {
      throw new TCRuntimeException("Unable to construct the broadcasting MBean: this is a programming error in "
                                   + TCLoggingBroadcaster.class.getName(), ncmbe);
    }
  }

  public final TCLoggingBroadcasterMBean getMBean() {
    return broadcastingBean;
  }

  protected void append(final LoggingEvent event) {
    broadcastingBean.broadcastLogEvent(getLayout().format(event));
  }

  public boolean requiresLayout() {
    return false;
  }

  public void close() {
    // Do nothing
  }

}
