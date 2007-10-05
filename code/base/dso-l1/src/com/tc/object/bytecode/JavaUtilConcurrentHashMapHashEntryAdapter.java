/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class JavaUtilConcurrentHashMapHashEntryAdapter extends ClassAdapter implements Opcodes {
  public final static String TC_RAWSETVALUE_METHOD_NAME = ByteCodeUtil.TC_METHOD_PREFIX + "rawsetvalue";
  public final static String TC_RAWSETVALUE_METHOD_DESC = "(Ljava/lang/Object;)V";

  public JavaUtilConcurrentHashMapHashEntryAdapter(ClassVisitor cv) {
    super(cv);
  }
  
  public void visitEnd() {
    createTCSetValueMethod();

    super.visitEnd();
  }
  
  private void createTCSetValueMethod() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC | ACC_SYNCHRONIZED, TC_RAWSETVALUE_METHOD_NAME, TC_RAWSETVALUE_METHOD_DESC, null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitFieldInsn(PUTFIELD, "java/util/concurrent/ConcurrentHashMap$HashEntry", "value", "Ljava/lang/Object;");
    mv.visitInsn(RETURN);
    mv.visitMaxs(2, 2);
    mv.visitEnd();
  }
}
