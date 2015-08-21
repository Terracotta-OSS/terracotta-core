/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.TerracottaManagement;
import com.tc.management.TerracottaManagement.Subsystem;
import com.tc.management.TerracottaManagement.Type;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class L1MBeanNames {

  public static final ObjectName CLUSTER_BEAN_PUBLIC;
  public static final ObjectName L1INFO_PUBLIC;
  public static final ObjectName ENTERPRISE_TC_CLIENT;

  static {
    try {
      CLUSTER_BEAN_PUBLIC = TerracottaManagement.createObjectName(Type.Cluster, Subsystem.None, null,
                                                                  "Terracotta Cluster Bean",
                                                                  TerracottaManagement.MBeanDomain.PUBLIC);
      L1INFO_PUBLIC = TerracottaManagement.createObjectName(Type.DsoClient, Subsystem.None, null, "L1 Info Bean",
                                                            TerracottaManagement.MBeanDomain.PUBLIC);
      ENTERPRISE_TC_CLIENT = TerracottaManagement.createObjectName(Type.DsoClient, Subsystem.Logging, null,
                                                                   "Terracotta Enterprise Bean",
                                                                   TerracottaManagement.MBeanDomain.PUBLIC);
    } catch (MalformedObjectNameException mone) {
      throw new RuntimeException(mone);
    } catch (NullPointerException npe) {
      throw new RuntimeException(npe);
    }
  }

}
