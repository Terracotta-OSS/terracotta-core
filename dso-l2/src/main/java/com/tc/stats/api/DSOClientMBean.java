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
package com.tc.stats.api;

import com.tc.management.TerracottaMBean;
import com.tc.management.beans.TerracottaOperatorEventsMBean;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;

import javax.management.ObjectName;

public interface DSOClientMBean extends TerracottaMBean {
  public static final String TUNNELED_BEANS_REGISTERED = "tunneled.beans.registered";

  long getClientID();

  String getNodeID();

  boolean isTunneledBeansRegistered();

  ObjectName getL1InfoBeanName();

  L1InfoMBean getL1InfoBean();

  ObjectName getL1DumperBeanName();

  ObjectName getL1OperatorEventsBeanName();

  ObjectName getEnterpriseTCClientBeanName();

  TerracottaOperatorEventsMBean getL1OperatorEventsBean();

  ChannelID getChannelID();

  String getRemoteAddress();

  long getTransactionRate();

  long getReadRate();

  long getWriteRate();

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
