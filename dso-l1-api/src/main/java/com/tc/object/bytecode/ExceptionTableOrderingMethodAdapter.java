/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Provides correct sorting of exception handlers based on their catch ranges.  This adapter breaks the visiting order
 * contract of {@link com.tc.asm.MethodVisitor}.
 * <p>
 * This adapter allows for the simple nesting of exception handlers in ASM.  Without using this adapter an exception
 * handler nested within an existing handler via instrumentation can result in the outer handler having its entry before
 * the inner handler in the method's exception table.  In the case of an intersection of the two catch types (both
 * handlers are potential candidates for catching the exception) the inner handler added by instrumentation will not be
 * executed, the exception will be caught by the outer handler first (See CDV-391).
 * <p>
 * Before using this adapter consider {@link com.tc.object.bytecode.TryCatchBlockSortingAdapter}.  This adapter breaks
 * the contract of MethodVisitor by visiting labels before their associated try/catch blocks.
 * <code>TryCatchBlockSortingAdapter</code> avoids this by using the ASM tree API.
 * 
 * @author Chris Dennis
 */
public class ExceptionTableOrderingMethodAdapter extends MethodAdapter {
  private static final TCLogger LOGGER = TCLogging.getLogger(ExceptionTableOrderingMethodAdapter.class);

  /**
   * Assembled list of method exception handlers.
   */
  private final List handlers = new ArrayList();

  private boolean visitMaxs = false;
  private int stack;
  private int locals;

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
   * Caches visited stack and local variable limits for later visitation.
   */
  public void visitMaxs(int maxStack, int maxLocals) {
    visitMaxs = true;
    stack = maxStack;
    locals = maxLocals;
  }

  /**
   * Sorts the local list of exception handlers and then visits them in the correct order.
   * <p>
   * Following this it calls <code>super.visitMaxs(...)</code> and <code>super.visitEnd()</code>
   */
  public void visitEnd() {
    Collections.sort(handlers);
    for (Iterator it = handlers.iterator(); it.hasNext(); ) {
      Handler h = (Handler) it.next();
      h.accept(mv);
    }

    if (visitMaxs) super.visitMaxs(stack, locals);

    super.visitEnd();
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

    public int hashCode() {
      try {
        return ((7 * start.getOffset()) ^ (11 * end.getOffset())
            ^ (13 * handler.getOffset()) ^ (desc == null ? 0 : (17 * desc.hashCode())));
      } catch (IllegalStateException e) {
        return ((7 * start.hashCode()) ^ (11 * end.hashCode())
            ^ (13 * handler.hashCode()) ^ (desc == null ? 0 : (17 * desc.hashCode())));
      }
    }

    public boolean equals(Object o) {
      if (!(o instanceof Handler)) return false;

      Handler h = (Handler) o;

      try {
        return ((start.getOffset() == h.start.getOffset())
            && (end.getOffset() == h.end.getOffset())
            && (handler.getOffset() == h.handler.getOffset())
            && (desc == null ? (desc == h.desc) : desc.equals(h.desc)));
      } catch (IllegalStateException e) {
        return ((start == h.start)
            && (end == h.end)
            && (handler == h.handler)
            && (desc == null ? (desc == h.desc) : desc.equals(h.desc)));
      }
    }

    /**
     * Defines the natural ordering of try blocks.  We order the exception table by the following rules:
     * <ol>
     * <li>Non-intersecting try blocks are ordered in the table by their bytecode position.</li>
     * <li>Blocks entirely contained by other blocks precede their enclosers in the table</li>
     * <li>Blocks that partially intersect are ordered per the original visited order.  (They cannot be generated from
     * Java source code)</li>
     * </ol>
     */
    public int compareTo(Object o) {
      Handler h0 = this;
      Handler h1 = (Handler) o;

      try {
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
      } catch (IllegalArgumentException e) {
        LOGGER.error("Not all handler labels were visited, reverting to the visited order (comparing " + h0 + " and " + h1 + ")");        
        return h0.index - h1.index;
      }

      LOGGER.warn("Exception Handler Ranges Not Language Compliant: " + h0 + " and " + h1);
      return h0.index - h1.index;
    }

    public String toString() {
      try {
        return "Exception Handler : Catches " + (desc == null ? "<finally>" : desc) + " @ [" + start.getOffset() + ".." + end.getOffset() + "] => " + handler.getOffset();
      } catch (IllegalStateException e) {
        return "Exception Handler : Catches " + (desc == null ? "<finally>" : desc) + " @ [" + start + ".." + end + "] => " + handler;        
      }
    }
  }
}

