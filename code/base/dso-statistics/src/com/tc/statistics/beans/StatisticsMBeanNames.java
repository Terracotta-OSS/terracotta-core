/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.beans;

import com.tc.exception.TCRuntimeException;
import com.tc.management.TerracottaManagement;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class StatisticsMBeanNames {
  public final static ObjectName STATISTICS_EMITTER;
  public final static ObjectName STATISTICS_MANAGER;
  public final static ObjectName STATISTICS_GATEWAY;
  public final static ObjectName STATISTICS_GATHERER;

  static {
    try {
      STATISTICS_EMITTER = TerracottaManagement.createObjectName(TerracottaManagement.Type.Agent,
                                                                 TerracottaManagement.Subsystem.Statistics, null,
                                                                 "Terracotta Statistics Emitter",
                                                                 TerracottaManagement.MBeanDomain.INTERNAL);
      STATISTICS_MANAGER = TerracottaManagement.createObjectName(TerracottaManagement.Type.Agent,
                                                                 TerracottaManagement.Subsystem.Statistics, null,
                                                                 "Terracotta Statistics Manager",
                                                                 TerracottaManagement.MBeanDomain.INTERNAL);
      STATISTICS_GATEWAY = TerracottaManagement.createObjectName(TerracottaManagement.Type.Server,
                                                                 TerracottaManagement.Subsystem.Statistics, null,
                                                                 "Terracotta Statistics Gateway",
                                                                 TerracottaManagement.MBeanDomain.INTERNAL);
      STATISTICS_GATHERER = TerracottaManagement.createObjectName(TerracottaManagement.Type.Server,
                                                                  TerracottaManagement.Subsystem.Statistics, null,
                                                                  "Terracotta Statistics Gatherer",
                                                                  TerracottaManagement.MBeanDomain.PUBLIC);
    } catch (MalformedObjectNameException mone) {
      throw new TCRuntimeException(mone);
    } catch (NullPointerException npe) {
      throw new TCRuntimeException(npe);
    }
  }
}