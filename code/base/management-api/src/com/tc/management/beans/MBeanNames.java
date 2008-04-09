/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.beans;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.tc.management.TerracottaManagement;
import com.tc.management.TerracottaManagement.Subsystem;
import com.tc.management.TerracottaManagement.Type;

public class MBeanNames {

  public static final ObjectName CLIENT_TX_INTERNAL;
  public static final ObjectName SESSION_INTERNAL;

  static {
    try {
      CLIENT_TX_INTERNAL = TerracottaManagement.createObjectName(TerracottaManagement.Type.DsoClient,
                                                                 TerracottaManagement.Subsystem.Tx, null,
                                                                 "Client transactions", false);
      SESSION_INTERNAL = TerracottaManagement.createObjectName(Type.Sessions, Subsystem.None, null, "Session stats",
                                                               false);
    } catch (MalformedObjectNameException mone) {
      throw new RuntimeException(mone);
    } catch (NullPointerException npe) {
      throw new RuntimeException(npe);
    }
  }

}
