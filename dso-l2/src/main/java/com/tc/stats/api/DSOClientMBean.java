/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.stats.api;

import com.tc.management.TerracottaMBean;
import com.tc.management.beans.TerracottaOperatorEventsMBean;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.management.beans.logging.InstrumentationLoggingMBean;
import com.tc.management.beans.logging.RuntimeLoggingMBean;
import com.tc.management.beans.logging.RuntimeOutputOptionsMBean;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;

import javax.management.ObjectName;

public interface DSOClientMBean extends TerracottaMBean {
  public static final String TUNNELED_BEANS_REGISTERED = "tunneled.beans.registered";

  ClientID getClientID();

  String getNodeID();

  boolean isTunneledBeansRegistered();

  ObjectName getL1InfoBeanName();

  L1InfoMBean getL1InfoBean();

  ObjectName getL1DumperBeanName();

  ObjectName getInstrumentationLoggingBeanName();

  InstrumentationLoggingMBean getInstrumentationLoggingBean();

  ObjectName getRuntimeLoggingBeanName();

  RuntimeLoggingMBean getRuntimeLoggingBean();

  ObjectName getRuntimeOutputOptionsBeanName();

  RuntimeOutputOptionsMBean getRuntimeOutputOptionsBean();
  
  ObjectName getL1OperatorEventsBeanName();
  
  ObjectName getEnterpriseTCClientBeanName();

  TerracottaOperatorEventsMBean getL1OperatorEventsBean();

  ChannelID getChannelID();

  String getRemoteAddress();

  long getTransactionRate();

  long getObjectFaultRate();

  long getObjectFlushRate();

  long getPendingTransactionsCount();

  Number[] getStatistics(String[] names);

  int getLiveObjectCount();

  boolean isResident(ObjectID oid);

  void killClient();
  
  long getServerMapGetSizeRequestsCount();

  long getServerMapGetValueRequestsCount();

  long getServerMapGetSizeRequestsRate();

  long getServerMapGetValueRequestsRate();
}
