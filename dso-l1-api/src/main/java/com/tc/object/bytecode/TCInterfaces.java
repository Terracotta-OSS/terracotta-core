/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

import java.util.ArrayList;
import java.util.List;

public class TCInterfaces {
  public static Class[] purgeTCInterfaces(final Class[] interfaces) {
    if ((interfaces == null) || (interfaces.length == 0)) { return interfaces; }

    List rv = new ArrayList();
    for (Class iface : interfaces) {
      if ((iface != Manageable.class) && (iface != TransparentAccess.class)) {
        rv.add(iface);
      }
    }

    return (Class[]) rv.toArray(new Class[rv.size()]);
  }
}
