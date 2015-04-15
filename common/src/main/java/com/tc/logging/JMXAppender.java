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

  @Override
  protected void append(final LoggingEvent event) {
    broadcastingBean.broadcastLogEvent(getLayout().format(event), event.getThrowableStrRep());
  }

  @Override
  public boolean requiresLayout() {
    return false;
  }

  @Override
  public void close() {
    // Do nothing
  }

}
