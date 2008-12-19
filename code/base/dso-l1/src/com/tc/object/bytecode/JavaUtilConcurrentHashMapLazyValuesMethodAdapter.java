/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.commons.LocalVariablesSorter;

class JavaUtilConcurrentHashMapLazyValuesMethodAdapter extends LocalVariablesSorter implements Opcodes {
  private boolean storeOnlyNonNull = false;

  public JavaUtilConcurrentHashMapLazyValuesMethodAdapter(final int access, final String desc, final MethodVisitor mv,
                                                          final boolean storeOnlyNonNull) {
    super(access, desc, mv);

    this.storeOnlyNonNull = storeOnlyNonNull;
  }

  public void visitFieldInsn(int opcode, String owner, String name, String desc) {
    if (GETFIELD == opcode && "java/util/concurrent/ConcurrentHashMap$HashEntry".equals(owner) && "value".equals(name)
        && "Ljava/lang/Object;".equals(desc)) {

      if (storeOnlyNonNull) {
        visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/ConcurrentHashMap$HashEntry",
                        JavaUtilConcurrentHashMapHashEntryAdapter.GET_VALUE_STORE_ONLY_NONNULL, "()Ljava/lang/Object;");
      } else {
        visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/ConcurrentHashMap$HashEntry",
                        JavaUtilConcurrentHashMapHashEntryAdapter.GET_VALUE, "()Ljava/lang/Object;");
      }
    } else if (GETFIELD == opcode && "java/util/concurrent/ConcurrentHashMap$HashEntry".equals(owner) && "raw_value".equals(name)
        && "Ljava/lang/Object;".equals(desc)) {
      /* This is the matching partner to the logic in JavaUtilConcurrentHashMapSegmentAdapter.  If someone tries to do a
       * GETFIELD on the non-existent "raw_value" field, we take it to mean "Give me a raw field read of the 'value'
       * field".  So that is what we do.  I consider this slightly nicer than generating a new method just for the sake
       * of doing an uninstrumented field read in one place in the code.
       */
      super.visitFieldInsn(opcode, owner, "value", desc);
    } else if (PUTFIELD == opcode && "java/util/concurrent/ConcurrentHashMap$HashEntry".equals(owner)
               && "value".equals(name) && "Ljava/lang/Object;".equals(desc)) {
      visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/ConcurrentHashMap$HashEntry",
                      TCMapEntry.TC_RAWSETVALUE_METHOD_NAME, TCMapEntry.TC_RAWSETVALUE_METHOD_DESC);
    } else {
      super.visitFieldInsn(opcode, owner, name, desc);
    }
  }
}