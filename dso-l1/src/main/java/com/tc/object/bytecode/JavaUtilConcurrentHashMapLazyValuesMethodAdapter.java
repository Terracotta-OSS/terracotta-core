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
    } else if (PUTFIELD == opcode && "java/util/concurrent/ConcurrentHashMap$HashEntry".equals(owner)
               && "value".equals(name) && "Ljava/lang/Object;".equals(desc)) {
      visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/ConcurrentHashMap$HashEntry",
                      TCMapEntry.TC_RAWSETVALUE_METHOD_NAME, TCMapEntry.TC_RAWSETVALUE_METHOD_DESC);
    } else {
      super.visitFieldInsn(opcode, owner, name, desc);
    }
  }
}