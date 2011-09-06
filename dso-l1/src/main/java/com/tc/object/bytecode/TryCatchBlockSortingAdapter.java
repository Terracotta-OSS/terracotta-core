/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.tree.MethodNode;
import com.tc.asm.tree.TryCatchBlockNode;

import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides correct sorting of exception handlers based on their catch ranges.
 * <p>
 * This adapter allows for the simple nesting of exception handlers in ASM.  Without using this adapter an exception
 * handler nested within an existing handler via instrumentation can result in the outer handler having its entry before
 * the inner handler in the method's exception table.  In the case of an intersection of the two catch types (both
 * handlers are potential candidates for catching the exception) the inner handler added by instrumentation will not be
 * executed, the exception will be caught by the outer handler first (See CDV-391).
 * <p>
 * Unlike {@link com.tc.object.bytecode.ExceptionTableOrderingMethodAdapter} this implementation uses the ASM tree API
 * and so can visit subsequent <code>MethodVisitor</code>s correctly (i.e. using a valid ordering for the method calls).
 * Currently <code>ExceptionTableOrderingMethodAdapter</code> is used in the main instrumentation adapter chain as it is
 * faster and sufficient in that particular circumstance.
 *
 * @author Chris Dennis
 */
public class TryCatchBlockSortingAdapter extends MethodAdapter {

  private static final Logger LOGGER = Logger.getLogger(TryCatchBlockSortingAdapter.class.getName());

  /**
   * Method node to hold the structure of method visited upon it.
   */
  private final MethodNode method;
  
  /**
   * MethodVisitor that follows this adapter in the chain.
   */
  private final MethodVisitor next;

  /**
   * Constructs a sorting adapter that visits the supplied visitor with the processed method.
   */
  public TryCatchBlockSortingAdapter(MethodVisitor visitor) {
    super(new MethodNode(0, null, null, null, null));

    next = visitor;
    method = (MethodNode) mv;
  }

  @Override
  public void visitEnd() {
    super.visitEnd();

    sortTryCatchBlocks();
    method.accept(next);
  }

  /**
   * Sorts the visited try/catch blocks into a language compliant order.
   */
  private void sortTryCatchBlocks() {
    Collections.sort(method.tryCatchBlocks, new TryCatchBlockComparator(method));
  }

  /**
   * Comparator for <code>TryCatchBlockNode</code> objects that orders them correctly assuming they were generated from
   * a valid Java source file.
   * <p>
   * The rules enforced by the comparator are:
   * <ul>
   * <li>If <code>j</code> entirely precedes <code>i</code>
   * then <code>i</code> is greater than <code>j</code> (+ve return)</li>
   * <li>If <code>i</code> entirely precedes <code>j</code>
   * then <code>i</code> is less than <code>j</code> (-ve return)</li>
   * <li>If <code>i</code> encloses <code>j</code>
   * then <code>i</code> is greater than <code>j</code> (+ve return)</li>
   * <li>If <code>j</code> encloses <code>i</code>
   * then <code>i</code> is less than <code>j</code> (-ve return)</li>
   * <li>Else <code>i</code> and <code>j</code> are not language compliant and we return zero</li>
   * </ul>
   * <p>
   * Note: this comparator imposes orderings that are inconsistent with equals.
   */
  static class TryCatchBlockComparator implements Comparator<TryCatchBlockNode> {
    private final MethodNode method;
    
    public TryCatchBlockComparator(MethodNode method) {
      this.method = method;
    }
    
    public int compare(TryCatchBlockNode i, TryCatchBlockNode j) {

      int crossDeltaOne = method.instructions.indexOf(i.start) - method.instructions.indexOf(j.end);
      if (crossDeltaOne >= 0) {
        //j is entirely before i
        return 1;
      }

      int crossDeltaTwo = method.instructions.indexOf(i.end) - method.instructions.indexOf(j.start);
      if (crossDeltaTwo <= 0) {
        //i is entirely before j
        return -1;
      }

      if ((crossDeltaOne < 0) && (crossDeltaTwo > 0)) {
        int iSize = method.instructions.indexOf(i.end) - method.instructions.indexOf(i.start);
        int jSize = method.instructions.indexOf(j.end) - method.instructions.indexOf(j.start);

        //tightest scope comes first
        return iSize - jSize;
      }

      LOGGER.log(Level.WARNING, "try/catch block ranges {0} and {1} are not language compliant (partial overlap)", new Object[] {i, j});
      return 0;
    }
  }
}
