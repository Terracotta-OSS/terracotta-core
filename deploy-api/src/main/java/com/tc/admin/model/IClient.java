/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.management.beans.logging.InstrumentationLoggingMBean;
import com.tc.management.beans.logging.RuntimeLoggingMBean;
import com.tc.management.beans.logging.RuntimeOutputOptionsMBean;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;

import java.util.Map;

import javax.management.ObjectName;

public interface IClient extends IClusterNode {
  static final IClient[] NULL_SET                               = {};

  static final String    POLLED_ATTR_PENDING_TRANSACTIONS_COUNT = "PendingTransactionsCount";

  ObjectName getBeanName();

  ObjectName getL1InfoBeanName();

  long getChannelID();

  ClientID getClientID();

  String getRemoteAddress();

  Map getL1Statistics();

  Number[] getDSOStatistics(String[] names);

  InstrumentationLoggingMBean getInstrumentationLoggingBean();

  RuntimeLoggingMBean getRuntimeLoggingBean();

  RuntimeOutputOptionsMBean getRuntimeOutputOptionsBean();

  boolean isResident(ObjectID oid);

  ObjectName getTunneledBeanName(ObjectName on);

  void killClient();

  String dump();
}
