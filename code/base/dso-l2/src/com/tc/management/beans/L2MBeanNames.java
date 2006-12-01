/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.TerracottaManagement;
import com.tc.management.TerracottaManagement.Subsystem;
import com.tc.management.TerracottaManagement.Type;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class L2MBeanNames {

  public static final ObjectName TC_SERVER_INFO;
  public static final ObjectName LOGGER;
  public static final ObjectName DSO;
  public static final ObjectName DSO_APP_EVENTS;
  public static final ObjectName OBJECT_MANAGEMENT;

  static {
    try {
      TC_SERVER_INFO = TerracottaManagement.createObjectName(Type.Server, Subsystem.None, null, "Terracotta Server",
                                                             false);
      LOGGER = TerracottaManagement.createObjectName(Type.Server, Subsystem.None, null, "Logger", false);
      DSO = TerracottaManagement.createObjectName(Type.Server, Subsystem.None, null, "DSO", false);
      DSO_APP_EVENTS = TerracottaManagement.createObjectName(Type.Server, Subsystem.None, null, "Application Events",
                                                             false);
      OBJECT_MANAGEMENT = TerracottaManagement.createObjectName(Type.Server, Subsystem.ObjectManagement, null,
                                                                "ObjectManagement", true);
    } catch (MalformedObjectNameException mone) {
      throw new RuntimeException(mone);
    } catch (NullPointerException npe) {
      throw new RuntimeException(npe);
    }
  }

}
