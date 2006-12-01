/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.debug;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SocketChannel;

public class CCE {

  private static final TCLogger logger = TCLogging.getLogger(CCE.class);

  public static ClassCastException debug(Object obj, Class castTo, ClassCastException cce, Object caller) {
    // This madness is try to track down a mysterious ClassCastException we see on dual-proc macs
    // Since I'm checking in this code, I've tried to make sure it doesn't cause any additional ill
    // side effects (thus the catch Throwable)

    String message = null;
    try {
      StringBuffer buf = new StringBuffer();
      ClassLoader loader = castTo.getClassLoader();
      Class cl = castTo;
      buf.append("cast-to class: ").append(cl).append(" ").append(System.identityHashCode(cl)).append("\n");
      buf.append("cast-to loader: ").append(System.identityHashCode(loader)).append(" ").append(loader).append("\n");

      cl = obj == null ? null : obj.getClass();
      loader = obj == null ? null : cl.getClassLoader();
      buf.append("obj class: ").append(cl).append(" ").append(System.identityHashCode(cl)).append("\n");
      buf.append("obj: ").append(obj).append(", LOADER: ").append(System.identityHashCode(loader)).append(" ")
          .append(loader).append("\n");
      buf.append("Classes equal: ").append(castTo.equals(cl)).append("\n");
      buf.append("Loaders equal: ").append(equals(castTo.getClassLoader(), loader)).append("\n");
      buf.append("Assignable: ").append(castTo.isAssignableFrom(cl)).append("\n");

      while (cl != null) {
        String name = cl.getName();
        buf.append(
                   name + ": hc=" + System.identityHashCode(cl) + ", loader=" + cl.getClassLoader() + ", equals="
                       + (equals(cl, castTo) ? "TRUE" : "false")).append("\n");
        Class[] ifaces = cl == null ? new Class[] {} : cl.getInterfaces();
        for (int i = 0; i < ifaces.length; i++) {
          Class iface = ifaces[i];
          name = iface.getName();
          buf.append(
                     "\tiface(" + i + ") " + name + ": hc=" + System.identityHashCode(iface) + ", loader="
                         + iface.getClassLoader() + ", equals=" + (equals(iface, castTo) ? "TRUE" : "false"))
              .append("\n");
        }

        cl = cl.getSuperclass();
      }

      loader = caller == null ? null : caller.getClass().getClassLoader();
      buf.append("caller: ").append(System.identityHashCode(loader)).append(" ").append(loader).append("\n");

      message = buf.toString();
      logger.error(message);
    } catch (Throwable t) {
      // oops, I screwed up...just throw the original exception
      logger.error(t);
      return cce;
    }

    return new ClassCastException(cce.getMessage() + ": " + message);
  }

  private static boolean equals(Object o1, Object o2) {
    if ((o1 == null) && (o2 == null)) return true;
    if ((o1 == null) && (o2 != null)) return false;
    if ((o2 == null) && (o1 != null)) return false;
    return o1.equals(o2);
  }

  public static void main(String args[]) throws Exception {
    debug(SocketChannel.open(), GatheringByteChannel.class, new ClassCastException("yer mom"), new Object());
  }

}
