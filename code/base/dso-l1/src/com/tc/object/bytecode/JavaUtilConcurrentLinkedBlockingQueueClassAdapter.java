/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.SerializationUtil;

public class JavaUtilConcurrentLinkedBlockingQueueClassAdapter extends ClassAdapter implements Opcodes {
  private static final String TC_TAKE_METHOD_NAME = ByteCodeUtil.TC_METHOD_PREFIX + "take";
  private static final String TC_PUT_METHOD_NAME  = ByteCodeUtil.TC_METHOD_PREFIX + "put";

  public JavaUtilConcurrentLinkedBlockingQueueClassAdapter(ClassVisitor cv) {
    super(cv);
  }

  public void visitEnd() {
    addTCTakeMethod();
    addTCPutMethod();
    addInitMethodCode();
    super.visitEnd();
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("remove".equals(name) && "(Ljava/lang/Object;)Z".equals(desc)) {
      recreateRemoveMethod(mv);
    }
    return mv;
  }
  
  /*
   * The __tc_put() is a non-blocking version of put() and is called in the applicator only.
   * We allow it to go over the capacity of the LinkedBlockingQueue but the capacity should
   * eventually goes down by take() method in the application thread.
   */
  private void addTCPutMethod() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, TC_PUT_METHOD_NAME, "(Ljava/lang/Object;)V", null, new String[] { "java/lang/InterruptedException" });
    mv.visitCode();
    Label l0 = new Label();
    Label l1 = new Label();
    mv.visitTryCatchBlock(l0, l1, l1, null);
    Label l2 = new Label();
    Label l3 = new Label();
    mv.visitTryCatchBlock(l2, l3, l1, null);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitVarInsn(ALOAD, 1);
    Label l5 = new Label();
    mv.visitJumpInsn(IFNONNULL, l5);
    mv.visitTypeInsn(NEW, "java/lang/NullPointerException");
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NullPointerException", "<init>", "()V");
    mv.visitInsn(ATHROW);
    mv.visitLabel(l5);
    mv.visitInsn(ICONST_M1);
    mv.visitVarInsn(ISTORE, 2);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "putLock", "Ljava/util/concurrent/locks/ReentrantLock;");
    mv.visitVarInsn(ASTORE, 3);
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "count", "Ljava/util/concurrent/atomic/AtomicInteger;");
    mv.visitVarInsn(ASTORE, 4);
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "lockInterruptibly", "()V");
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/LinkedBlockingQueue", "insert", "(Ljava/lang/Object;)V");
    Label l9 = new Label();
    mv.visitLabel(l9);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "getAndIncrement", "()I");
    mv.visitVarInsn(ISTORE, 2);
    Label l10 = new Label();
    mv.visitLabel(l10);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitInsn(ICONST_1);
    mv.visitInsn(IADD);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "capacity", "I");
    mv.visitJumpInsn(IF_ICMPGE, l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "notFull", "Ljava/util/concurrent/locks/Condition;");
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/concurrent/locks/Condition", "signal", "()V");
    mv.visitJumpInsn(GOTO, l2);
    mv.visitLabel(l1);
    mv.visitVarInsn(ASTORE, 6);
    Label l11 = new Label();
    mv.visitJumpInsn(JSR, l11);
    Label l12 = new Label();
    mv.visitLabel(l12);
    mv.visitVarInsn(ALOAD, 6);
    mv.visitInsn(ATHROW);
    mv.visitLabel(l11);
    mv.visitVarInsn(ASTORE, 5);
    Label l13 = new Label();
    mv.visitLabel(l13);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "unlock", "()V");
    Label l14 = new Label();
    mv.visitLabel(l14);
    mv.visitVarInsn(RET, 5);
    mv.visitLabel(l2);
    mv.visitJumpInsn(JSR, l11);
    mv.visitLabel(l3);
    mv.visitVarInsn(ILOAD, 2);
    Label l15 = new Label();
    mv.visitJumpInsn(IFNE, l15);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/LinkedBlockingQueue", "signalNotEmpty", "()V");
    mv.visitLabel(l15);
    mv.visitInsn(RETURN);
    Label l16 = new Label();
    mv.visitLabel(l16);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  /**
   * The __tc_take() method is the non-blocking version of take and is used by the applicator
   * only. We allow the capacity to fall below 0, but the capacity should eventually goes up
   * by the put() method in the application thread.
   */
  private void addTCTakeMethod() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, TC_TAKE_METHOD_NAME, "()Ljava/lang/Object;", null,
                                         new String[] { "java/lang/InterruptedException" });
    mv.visitCode();
    Label l0 = new Label();
    Label l1 = new Label();
    mv.visitTryCatchBlock(l0, l1, l1, null);
    Label l2 = new Label();
    Label l3 = new Label();
    mv.visitTryCatchBlock(l2, l3, l1, null);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitInsn(ICONST_M1);
    mv.visitVarInsn(ISTORE, 2);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "count",
                      "Ljava/util/concurrent/atomic/AtomicInteger;");
    mv.visitVarInsn(ASTORE, 3);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "takeLock",
                      "Ljava/util/concurrent/locks/ReentrantLock;");
    mv.visitVarInsn(ASTORE, 4);
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "lockInterruptibly", "()V");
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "head",
                      "Ljava/util/concurrent/LinkedBlockingQueue$Node;");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "last",
                      "Ljava/util/concurrent/LinkedBlockingQueue$Node;");
    Label l8 = new Label();
    mv.visitJumpInsn(IF_ACMPNE, l8);
    Label l9 = new Label();
    mv.visitLabel(l9);
    mv.visitTypeInsn(NEW, "com/tc/exception/TCRuntimeException");
    mv.visitInsn(DUP);
    mv.visitLdcInsn(TC_TAKE_METHOD_NAME + ": Trying to do a take from an empty queue.");
    mv.visitMethodInsn(INVOKESPECIAL, "com/tc/exception/TCRuntimeException", "<init>", "(Ljava/lang/String;)V");
    mv.visitInsn(ATHROW);
    mv.visitLabel(l8);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/LinkedBlockingQueue", "extract", "()Ljava/lang/Object;");
    mv.visitVarInsn(ASTORE, 1);
    Label l10 = new Label();
    mv.visitLabel(l10);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "getAndDecrement", "()I");
    mv.visitVarInsn(ISTORE, 2);
    mv.visitJumpInsn(GOTO, l2);
    mv.visitLabel(l1);
    mv.visitVarInsn(ASTORE, 6);
    Label l11 = new Label();
    mv.visitJumpInsn(JSR, l11);
    Label l12 = new Label();
    mv.visitLabel(l12);
    mv.visitVarInsn(ALOAD, 6);
    mv.visitInsn(ATHROW);
    mv.visitLabel(l11);
    mv.visitVarInsn(ASTORE, 5);
    Label l13 = new Label();
    mv.visitLabel(l13);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "unlock", "()V");
    Label l14 = new Label();
    mv.visitLabel(l14);
    mv.visitVarInsn(RET, 5);
    mv.visitLabel(l2);
    mv.visitJumpInsn(JSR, l11);
    mv.visitLabel(l3);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "capacity", "I");
    Label l15 = new Label();
    mv.visitJumpInsn(IF_ICMPNE, l15);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/LinkedBlockingQueue", "signalNotFull", "()V");
    mv.visitLabel(l15);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(ARETURN);
    Label l16 = new Label();
    mv.visitLabel(l16);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  // Rewriting the remove(Object) method of LinkedBlockingQueue with instrumented code.
  private void recreateRemoveMethod(MethodVisitor mv) {
    mv.visitCode();
    Label l0 = new Label();
    Label l1 = new Label();
    mv.visitTryCatchBlock(l0, l1, l1, null);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ISTORE, 2);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitVarInsn(ALOAD, 1);
    Label l4 = new Label();
    mv.visitJumpInsn(IFNONNULL, l4);
    mv.visitInsn(ICONST_0);
    mv.visitInsn(IRETURN);
    mv.visitLabel(l4);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ISTORE, 3);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/LinkedBlockingQueue", "fullyLock", "()V");
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "head",
                      "Ljava/util/concurrent/LinkedBlockingQueue$Node;");
    mv.visitVarInsn(ASTORE, 4);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "head",
                      "Ljava/util/concurrent/LinkedBlockingQueue$Node;");
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue$Node", "next",
                      "Ljava/util/concurrent/LinkedBlockingQueue$Node;");
    mv.visitVarInsn(ASTORE, 5);
    Label l7 = new Label();
    mv.visitLabel(l7);
    Label l8 = new Label();
    mv.visitJumpInsn(GOTO, l8);
    Label l9 = new Label();
    mv.visitLabel(l9);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue$Node", "item", "Ljava/lang/Object;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z");
    Label l10 = new Label();
    mv.visitJumpInsn(IFEQ, l10);
    Label l11 = new Label();
    mv.visitLabel(l11);
    mv.visitInsn(ICONST_1);
    mv.visitVarInsn(ISTORE, 3);
    Label l12 = new Label();
    mv.visitLabel(l12);
    Label l13 = new Label();
    mv.visitJumpInsn(GOTO, l13);
    mv.visitLabel(l10);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitVarInsn(ASTORE, 4);
    Label l14 = new Label();
    mv.visitLabel(l14);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue$Node", "next",
                      "Ljava/util/concurrent/LinkedBlockingQueue$Node;");
    mv.visitVarInsn(ASTORE, 5);
    Label l15 = new Label();
    mv.visitLabel(l15);
    mv.visitIincInsn(2, 1);
    mv.visitLabel(l8);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitJumpInsn(IFNONNULL, l9);
    mv.visitLabel(l13);
    mv.visitVarInsn(ILOAD, 3);
    Label l16 = new Label();
    mv.visitJumpInsn(IFEQ, l16);
    Label l17 = new Label();
    mv.visitLabel(l17);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitInsn(ACONST_NULL);
    mv.visitFieldInsn(PUTFIELD, "java/util/concurrent/LinkedBlockingQueue$Node", "item", "Ljava/lang/Object;");
    Label l18 = new Label();
    mv.visitLabel(l18);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue$Node", "next",
                      "Ljava/util/concurrent/LinkedBlockingQueue$Node;");
    mv.visitFieldInsn(PUTFIELD, "java/util/concurrent/LinkedBlockingQueue$Node", "next",
                      "Ljava/util/concurrent/LinkedBlockingQueue$Node;");
    Label l19 = new Label();
    mv.visitLabel(l19);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isManaged", "(Ljava/lang/Object;)Z");
    Label l20 = new Label();
    mv.visitJumpInsn(IFEQ, l20);
    Label l21 = new Label();
    mv.visitLabel(l21);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitLdcInsn(SerializationUtil.REMOVE_AT_SIGNATURE);
    mv.visitInsn(ICONST_1);
    mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_0);
    mv.visitTypeInsn(NEW, "java/lang/Integer");
    mv.visitInsn(DUP);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
    mv.visitInsn(AASTORE);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                       "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
    mv.visitLabel(l20);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "last",
                      "Ljava/util/concurrent/LinkedBlockingQueue$Node;");
    mv.visitVarInsn(ALOAD, 5);
    Label l22 = new Label();
    mv.visitJumpInsn(IF_ACMPNE, l22);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitFieldInsn(PUTFIELD, "java/util/concurrent/LinkedBlockingQueue", "last",
                      "Ljava/util/concurrent/LinkedBlockingQueue$Node;");
    mv.visitLabel(l22);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "count",
                      "Ljava/util/concurrent/atomic/AtomicInteger;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "getAndDecrement", "()I");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "capacity", "I");
    mv.visitJumpInsn(IF_ICMPNE, l16);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "notFull",
                      "Ljava/util/concurrent/locks/Condition;");
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/concurrent/locks/Condition", "signalAll", "()V");
    mv.visitJumpInsn(GOTO, l16);
    mv.visitLabel(l1);
    mv.visitVarInsn(ASTORE, 6);
    Label l23 = new Label();
    mv.visitLabel(l23);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/LinkedBlockingQueue", "fullyUnlock", "()V");
    Label l24 = new Label();
    mv.visitLabel(l24);
    mv.visitVarInsn(ALOAD, 6);
    mv.visitInsn(ATHROW);
    mv.visitLabel(l16);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/LinkedBlockingQueue", "fullyUnlock", "()V");
    Label l25 = new Label();
    mv.visitLabel(l25);
    mv.visitVarInsn(ILOAD, 3);
    mv.visitInsn(IRETURN);
    Label l26 = new Label();
    mv.visitLabel(l26);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addInitMethodCode() {
    MethodVisitor mv = super.visitMethod(ACC_PRIVATE + ACC_SYNTHETIC, "init", "()V", null, null);
    mv.visitCode();
    ByteCodeUtil.pushThis(mv);
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "takeLock",
                      "Ljava/util/concurrent/locks/ReentrantLock;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "newCondition",
                       "()Ljava/util/concurrent/locks/Condition;");
    mv.visitFieldInsn(PUTFIELD, "java/util/concurrent/LinkedBlockingQueue", "notEmpty",
                      "Ljava/util/concurrent/locks/Condition;");
    ByteCodeUtil.pushThis(mv);
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "putLock",
                      "Ljava/util/concurrent/locks/ReentrantLock;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "newCondition",
                       "()Ljava/util/concurrent/locks/Condition;");
    mv.visitFieldInsn(PUTFIELD, "java/util/concurrent/LinkedBlockingQueue", "notFull",
                      "Ljava/util/concurrent/locks/Condition;");
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }
}