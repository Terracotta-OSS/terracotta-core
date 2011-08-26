/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

/**
 * Class adaptor for Throwable that prints out the exception class at the
 * beginning of the constructors.
 * Ie.:
 * sun.misc.MessageUtils.toStderr(this.getClass().getName());
 */
public class JavaLangThrowableDebugClassAdapter extends ClassAdapter implements Opcodes {

  public JavaLangThrowableDebugClassAdapter(ClassVisitor cv) {
    super(cv);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("<init>".equals(name)) {
      mv = new DebugConstructorMethodVisitor(mv);
    }

    return mv;
  }
  
  private static class DebugConstructorMethodVisitor extends MethodAdapter implements Opcodes {
    public DebugConstructorMethodVisitor(MethodVisitor mv) {
      super(mv);
    }

    public void visitInsn(int opcode) {
      if (RETURN == opcode) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;");
        mv.visitMethodInsn(INVOKESTATIC, "sun/misc/MessageUtils", "toStderr", "(Ljava/lang/String;)V");

        Label label_nomsg = new Label();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "getMessage", "()Ljava/lang/String;");
        mv.visitJumpInsn(IFNULL, label_nomsg);
        mv.visitLdcInsn(": ");
        mv.visitMethodInsn(INVOKESTATIC, "sun/misc/MessageUtils", "toStderr", "(Ljava/lang/String;)V");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "getMessage", "()Ljava/lang/String;");
        mv.visitMethodInsn(INVOKESTATIC, "sun/misc/MessageUtils", "toStderr", "(Ljava/lang/String;)V");

        mv.visitLabel(label_nomsg);
        mv.visitLdcInsn("\n");
        mv.visitMethodInsn(INVOKESTATIC, "sun/misc/MessageUtils", "toStderr", "(Ljava/lang/String;)V");
      }
      
      super.visitInsn(opcode);
    }
  }
}