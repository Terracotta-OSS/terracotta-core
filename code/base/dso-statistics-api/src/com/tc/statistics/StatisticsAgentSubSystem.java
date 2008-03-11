/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics;

import com.tc.config.schema.NewStatisticsConfig;
import com.tc.statistics.buffer.StatisticsBuffer;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.statistics.beans.StatisticsEmitterMBean;
import com.tc.statistics.beans.StatisticsManagerMBean;

import javax.management.MBeanServer;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;

public interface StatisticsAgentSubSystem {
  boolean isActive();

  void setDefaultAgentIp(String defaultAgentIp);

  void setDefaultAgentDifferentiator(String defaultAgentDifferentiator);

  boolean setup(NewStatisticsConfig config);

  void registerMBeans(MBeanServer server) throws MBeanRegistrationException, NotCompliantMBeanException, InstanceAlreadyExistsException;

  void unregisterMBeans(MBeanServer server) throws InstanceNotFoundException, MBeanRegistrationException;

  StatisticsBuffer getStatisticsBuffer();

  StatisticsEmitterMBean getStatisticsEmitterMBean();

  StatisticsManagerMBean getStatisticsManagerMBean();

  StatisticsRetrievalRegistry getStatisticsRetrievalRegistry();

  void disableJMX() throws Exception;

  void cleanup() throws Exception;
}
