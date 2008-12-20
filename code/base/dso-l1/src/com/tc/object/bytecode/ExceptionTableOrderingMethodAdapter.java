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

/**
 * Provides correct sorting of exception handlers based on their catch ranges.
 * <p>
 * This fixes a hypothesized bug in ASM.  An exception handler nested within an existing handler via instrumentation can
 * result in the outer handler having its entry before the inner handler in the method's exception table.  In the case
 * of an intersection of the two catch types (both handlers are potential candidates for catching the exception) the
 * inner handler added by instrumentation will not be executed, the exception will be caught by the outer handler first
 * (See CDV-391).
 */
public class ExceptionTableOrderingMethodAdapter extends MethodAdapter {

  /**
   * Assembled list of method exception handlers.
   */
  private final List handlers = new ArrayList();

  public ExceptionTableOrderingMethodAdapter(MethodVisitor mv) {
    super(mv);
  }

  /**
   * Caches try/catch blocks in a local data structure.
   */
  public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
    handlers.add(new Handler(start, end, handler, type, handlers.size()));
  }

  /**
   * Sorts the local list of exception handlers and then visits them in the correct order.
   * <p>
   * Following this it obviously calls <code>super.visitMax(...)</code>
   */
  public void visitMaxs(int maxStack, int maxLocals) {
    Collections.sort(handlers);
    for (Iterator it = handlers.iterator(); it.hasNext(); ) {
      Handler h = (Handler) it.next();
      h.accept(mv);
    }
    super.visitMaxs(maxStack, maxLocals);
  }
  
  /**
   * Represents an exception handler.
   */
  private static class Handler implements Comparable {

    /**
     * Constructs a Handler instance with the given properties.
     * 
     * @param start Label representing the start of the try block (inclusive).
     * @param end Label representing the end of the try block (exclusive).
     * @param handler Label representing the start of the handler code.
     * @param type String representation of the internal Java type caught by this handler (<code>null</code> for finally).
     * @param index integer representing the position in the original visited order. 
     */
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
     * <code>null</code> to catch any exceptions.
     */
    private final String desc;

    /**
     * Handlers position in the original visited order.
     */
    private final int index;

    /**
     * Visit the supplied MethodVisitor with this try/catch block.
     */
    void accept(MethodVisitor mv) {
      mv.visitTryCatchBlock(start, end, handler, desc);
    }

    /**
     * Defines the natural ordering of try blocks.  We order the exception table by the following rules:
     * <ol>
     * <li>Non-intersecting try blocks are ordered in the table by their bytecode position.</li>
     * <li>Blocks entirely contained by other blocks precede their enclosers in the table</li>
     * <li>Blocks that partially intersect will throw <code>IllegalStateException</code>.  They cannot be generated from
     * Java source code.</li>
     * </ol>
     */
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
          //identically scoped - revert to visited order
          return h0.index - h1.index;
        } else {
          //tightest scope comes first
          return h0Size - h1Size;
        }
      }

      //screwed up
      throw new IllegalStateException("Handler Ranges Not Language Compliant: " + h0 + " and " + h1);
    }
  }
}

