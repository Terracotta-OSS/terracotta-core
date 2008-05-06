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

  public static final ObjectName SESSION_PRODUCT_PUBLIC;
  public static final ObjectName CLUSTER_BEAN_PUBLIC;
  public static final ObjectName L1INFO_PUBLIC;
  public static final ObjectName RUNTIME_LOGGING_PUBLIC;
  public static final ObjectName INSTRUMENTATION_LOGGING_PUBLIC;
  public static final ObjectName RUNTIME_OUTPUT_OPTIONS_PUBLIC;

  static {
    try {
      SESSION_PRODUCT_PUBLIC = TerracottaManagement.createObjectName(Type.Sessions, Subsystem.None, null,
                                                                     "Terracotta for Sessions", true);
      CLUSTER_BEAN_PUBLIC = TerracottaManagement.createObjectName(Type.Cluster, Subsystem.None, null,
                                                                  "Terracotta Cluster Bean", true);
      L1INFO_PUBLIC = TerracottaManagement.createObjectName(Type.DsoClient, Subsystem.None, null, "L1 Info Bean", true);
      RUNTIME_LOGGING_PUBLIC = TerracottaManagement.createObjectName(Type.DsoClient, Subsystem.Logging, null,
                                                                     "Terracotta Runtime Logging Bean", true);
      INSTRUMENTATION_LOGGING_PUBLIC = TerracottaManagement.createObjectName(Type.DsoClient, Subsystem.Logging, null,
                                                                             "Terracotta Instrumentation Logging Bean",
                                                                             true);
      RUNTIME_OUTPUT_OPTIONS_PUBLIC = TerracottaManagement.createObjectName(Type.DsoClient, Subsystem.Logging, null,
                                                                            "Terracotta Runtime Output Options Bean",
                                                                            true);
    } catch (MalformedObjectNameException mone) {
      throw new RuntimeException(mone);
    } catch (NullPointerException npe) {
      throw new RuntimeException(npe);
    }
  }

}
