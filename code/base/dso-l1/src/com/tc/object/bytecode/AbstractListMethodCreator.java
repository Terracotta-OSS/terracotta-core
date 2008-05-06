/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class AbstractListMethodCreator implements MethodCreator, Opcodes {

  public void createMethods(ClassVisitor cv) {
    MethodVisitor mv = cv.visitMethod(ACC_PROTECTED, ByteCodeUtil.fieldGetterMethod("modCount"), "()I", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/AbstractList", "modCount", "I");
    mv.visitInsn(IRETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    mv = cv.visitMethod(ACC_PROTECTED, ByteCodeUtil.fieldSetterMethod("modCount"), "(I)V", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ILOAD, 1);
    mv.visitFieldInsn(PUTFIELD, "java/util/AbstractList", "modCount", "I");
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

  }

}
