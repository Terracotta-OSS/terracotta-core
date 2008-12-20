/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ExceptionTableOrderingMethodAdapter extends MethodAdapter {

  private final List handlers = new ArrayList();

  public ExceptionTableOrderingMethodAdapter(MethodVisitor mv) {
    super(mv);
  }

  public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
    handlers.add(new Handler(start, end, handler, type, handlers.size()));
  }

  public void visitMaxs(int maxStack, int maxLocals) {
    Collections.sort(handlers);
    for (Iterator it = handlers.iterator(); it.hasNext(); ) {
      Handler h = (Handler) it.next();
      h.accept(mv);
    }
    super.visitMaxs(maxStack, maxLocals);
  }
  
  private static class Handler implements Comparable {

    public Handler(Label start, Label end, Label handler, String type, int index) {
      this.start = start;
      this.end = end;
      this.handler = handler;
      this.desc = type;
      this.index = index;
    }
    /**
     * Beginning of the exception handler's scope (inclusive).
     */
    private final Label start;

    /**
     * End of the exception handler's scope (exclusive).
     */
    private final Label end;

    /**
     * Beginning of the exception handler's code.
     */
    private final Label handler;

    /**
     * Internal name of the type of exceptions handled by this handler, or
     * <tt>null</tt> to catch any exceptions.
     */
    private final String desc;

    /**
     * Visited Order.
     */
    private final int index;

    void accept(MethodVisitor mv) {
      mv.visitTryCatchBlock(start, end, handler, desc);
    }

    public int compareTo(Object o) {
      Handler h0 = this;
      Handler h1 = (Handler) o;

      int crossDeltaOne = h0.start.getOffset() - h1.end.getOffset();
      int crossDeltaTwo = h0.end.getOffset() - h1.start.getOffset();

      //h1 is entirely before h0
      if (crossDeltaOne >= 0) {
        return 1;
      }

      //h0 is entirely before h1
      if (crossDeltaTwo <= 0) {
        return -1;
      }

      //nesting
      if ((crossDeltaOne < 0) && (crossDeltaTwo > 0)) {
        int h0Size = h0.end.getOffset() - h0.start.getOffset();
        int h1Size = h1.end.getOffset() - h1.start.getOffset();

        if (h0Size == h1Size) {
          return h0.index - h1.index;
        } else {
          return h0Size - h1Size;
        }
      }

      //screwed up
      throw new IllegalStateException("Non-Compliant Handler Ranges : " + h0 + " and " + h1);
    }
  }
}

