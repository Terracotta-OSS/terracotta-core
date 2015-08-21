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
