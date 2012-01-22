/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.TerracottaManagement;
import com.tc.management.TerracottaManagement.Subsystem;
import com.tc.management.TerracottaManagement.Type;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class MBeanNames {

  public static final ObjectName L1DUMPER_INTERNAL;
  public static final ObjectName OPERATOR_EVENTS_PUBLIC;

  static {
    try {
      L1DUMPER_INTERNAL = TerracottaManagement.createObjectName(Type.DsoClient, Subsystem.None, null,
                                                                "DSO Client Dump Bean", TerracottaManagement.MBeanDomain.INTERNAL);
      
      OPERATOR_EVENTS_PUBLIC = TerracottaManagement.createObjectName(Type.TcOperatorEvents, Subsystem.None, null,
                                                                     "Terracotta Operator Events Bean",
                                                                     TerracottaManagement.MBeanDomain.PUBLIC);
    } catch (MalformedObjectNameException mone) {
      throw new RuntimeException(mone);
    } catch (NullPointerException npe) {
      throw new RuntimeException(npe);
    }
  }

}
