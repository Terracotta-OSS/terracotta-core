/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class WindowUtil implements PrettyPrintable {
  private final int size;
  private final List list;
  public WindowUtil(int size) {
    this.size = size;
    this.list = new LinkedList();
  }
  
  public void add(Object o) {
    synchronized (list) {
      if (list.size() == size) {
        list.remove(0);
      }
      list.add(o);
    }
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
      out.println("WindowUtil");
      PrettyPrinter rv = out;
      out = out.duplicateAndIndent();
      synchronized (list) {
        for (Iterator i=list.iterator(); i.hasNext(); ) {
          out.indent().visit(i.next()).println();
        }
      }
      return rv;
  }
}
