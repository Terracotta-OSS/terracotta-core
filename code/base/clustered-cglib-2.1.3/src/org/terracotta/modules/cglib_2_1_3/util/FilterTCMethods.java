/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.cglib_2_1_3.util;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

public class FilterTCMethods {
  public static void filterTCMethods (Collection collection) {
    for (Iterator i=collection.iterator(); i.hasNext(); ) {
      Method m = (Method)i.next();
      if (m.getName().startsWith("__tc_")) {
        i.remove();
      }
    }
  }
}