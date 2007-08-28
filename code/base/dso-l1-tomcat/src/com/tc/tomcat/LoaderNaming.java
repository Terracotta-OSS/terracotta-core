/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.tomcat;

import org.apache.catalina.Container;

public class LoaderNaming {

  public static String getFullyQualifiedName(Container c) {
    StringBuffer rv = new StringBuffer();
    while (c != null) {
      rv.insert(0, ":" + c.getName());
      c = c.getParent();
    }

    if (rv.length() > 0) {
      rv.delete(0, 1);
    }

    return rv.toString();
  }

}
