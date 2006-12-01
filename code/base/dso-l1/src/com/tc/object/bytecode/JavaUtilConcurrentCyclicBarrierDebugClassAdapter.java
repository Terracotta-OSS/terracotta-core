/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.MethodAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class JavaUtilConcurrentCyclicBarrierDebugClassAdapter extends ClassAdapter implements Opcodes {

  public JavaUtilConcurrentCyclicBarrierDebugClassAdapter(ClassVisitor cv) {
    super(cv);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("dowait".equals(name) && "(ZJ)I".equals(desc)) {
      mv = new DoWaitMethodVisitor(mv);
    }

    return mv;
  }

  public void visitEnd() {
    addDumpStateMethod();
    super.visitEnd();
  }
  
  private void addDumpStateMethod() {
    MethodVisitor mv = super.visitMethod(ACC_PRIVATE + ACC_SYNTHETIC, "dumpState", "()V", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    mv.visitLdcInsn("Current Status CyclicBarrier:");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    mv.visitTypeInsn(NEW, "java/lang/StringBuffer");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("-- parties: ");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "(Ljava/lang/String;)V");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/CyclicBarrier", "parties", "I");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(I)Ljava/lang/StringBuffer;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    mv.visitTypeInsn(NEW, "java/lang/StringBuffer");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("-- barrierCommand: ");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "(Ljava/lang/String;)V");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/CyclicBarrier", "barrierCommand", "Ljava/lang/Runnable;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuffer;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    mv.visitTypeInsn(NEW, "java/lang/StringBuffer");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("-- generation: ");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "(Ljava/lang/String;)V");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/CyclicBarrier", "generation", "Ljava/util/concurrent/CyclicBarrier$Generation;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuffer;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    mv.visitTypeInsn(NEW, "java/lang/StringBuffer");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("-- trip: ");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "(Ljava/lang/String;)V");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/CyclicBarrier", "trip", "Ljava/util/concurrent/locks/Condition;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuffer;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitInsn(RETURN);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }
  
  private static class DoWaitMethodVisitor extends MethodAdapter implements Opcodes {
    private Label target;
    
    public DoWaitMethodVisitor(MethodVisitor mv) {
      super(mv);
    }
    
    public void visitJumpInsn(int opcode, Label label) {
      super.visitJumpInsn(opcode, label);
      if (IF_ACMPEQ == opcode) {
        target = label;
      }
    }
    
    public void visitLabel(Label label) {
      super.visitLabel(label);
      if (label.equals(target)) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
        mv.visitLdcInsn("Wake up, but local generation the same as new generation.");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
        Label l42 = new Label();
        mv.visitLabel(l42);
        mv.visitLineNumber(124, l42);
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
        mv.visitLdcInsn("Entering debug block.");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
        Label l43 = new Label();
        mv.visitLabel(l43);
        mv.visitLineNumber(125, l43);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/CyclicBarrier", "dumpState", "()V");
        Label l44 = new Label();
        mv.visitLabel(l44);
        mv.visitLineNumber(126, l44);
        mv.visitLdcInsn(new Long(5000L));
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V");
        Label l45 = new Label();
        mv.visitLabel(l45);
        mv.visitLineNumber(127, l45);
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(NEW, "java/lang/StringBuffer");
        mv.visitInsn(DUP);
        mv.visitLdcInsn("Is local generation equal to new generation: ");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "(Ljava/lang/String;)V");
        mv.visitVarInsn(ALOAD, 5);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "java/util/concurrent/CyclicBarrier", "generation", "Ljava/util/concurrent/CyclicBarrier$Generation;");
        Label l46 = new Label();
        mv.visitJumpInsn(IF_ACMPNE, l46);
        mv.visitInsn(ICONST_1);
        Label l47 = new Label();
        mv.visitJumpInsn(GOTO, l47);
        mv.visitLabel(l46);
        mv.visitInsn(ICONST_0);
        mv.visitLabel(l47);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Z)Ljava/lang/StringBuffer;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
        Label l48 = new Label();
        mv.visitLabel(l48);
        mv.visitLineNumber(128, l48);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/CyclicBarrier", "dumpState", "()V");
        Label l49 = new Label();
        mv.visitLabel(l49);
      }
    }
  }
}