/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.object.bytecode.rwsync;

import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;

/**
 * A strategy for modifying a method. The difference between this and a MethodAdapter is that a MethodStrategy has
 * access to the ClassVisitor and therefore can create additional methods if needed, for example to rename a method and
 * create a wrapper.
 * <p>
 * MethodStrategy instances are kept in statically initialized tables and reused. Implementations should not contain any
 * mutable state.
 */
public interface MethodStrategy {

  /**
   * Instrument a method, possibly creating other methods in the process. A do-nothing implementation might look like:
   * 
   * <pre>
   *   visitMethod(...) {
   *     return cv.visitMethod(access, name, desc, signature, exceptions);
   *   }
   * </pre>
   * 
   * This would delegate the visitMethod() call to the next class adapter in the chain, returning a method visitor which
   * could then be visited with the method's contents.
   * 
   * @param cv a way of calling the next class adapter in the chain. Do <em>not</em> pass in the samed ClassVisitor that
   *        is invoking the method strategy, or you'll recurse endlessly! Note that cv.visitMethod may be called more
   *        than once during a single call to this method, and must return a distinct MethodVisitor instance for each
   *        call.
   * @param ownerType the internal type name of the class that contains the method, e.g., "org/xyz/Foo$Bar"
   * @param outerType null if ownerType is a top-level type, or else the internal type name of its outer class e.g.
   *        "org/xyz/Foo"
   * @param outerDesc null if ownerType is a top-level type, or else the type descriptor of its outer class, e.g.
   *        "Lorg/xyz/Foo;"
   * @return a method visitor which can be called with the contents of the original method. Implementations must not
   *         return null, but may return a visitor that ignores all calls.
   */
  MethodVisitor visitMethod(ClassVisitor cv, String ownerType, String outerType, String outerDesc, int access,
                            String name, String desc, String signature, String[] exceptions);
}
