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
import com.tc.object.SerializationUtil;

public class JavaUtilConcurrentLinkedBlockingQueueClassAdapter extends ClassAdapter implements Opcodes {
  private static final String TC_TAKE_METHOD_NAME    = ByteCodeUtil.TC_METHOD_PREFIX + "take";
  private static final String TC_PUT_METHOD_NAME     = ByteCodeUtil.TC_METHOD_PREFIX + "put";
  private static final String TC_EXTRACT_METHOD_NAME = ByteCodeUtil.TC_METHOD_PREFIX + "extract";
  private static final String TC_EXTRACT_METHOD_DESC = "()Ljava/lang/Object;";

  private static final String GET_ITEM_METHOD_NAME   = "getItem";
  private static final String GET_ITEM_METHOD_DESC   = "()Ljava/lang/Object;";

  public JavaUtilConcurrentLinkedBlockingQueueClassAdapter(ClassVisitor cv) {
    super(cv);
  }

  public void visitEnd() {
    addTCExtractMethod();
    addTCTakeMethod();
    addTCPutMethod();
    addInitMethodCode();
    super.visitEnd();
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    // Recreating the instrumented method because it is simpler and it performs a little better than
    // injecting code.
    mv = new NodeMethodAdapter(mv);
    if ("remove".equals(name) && "(Ljava/lang/Object;)Z".equals(desc)) {
      recreateRemoveMethod(mv);
    } else if ("offer".equals(name) && "(Ljava/lang/Object;)Z".equals(desc)) {
      recreateOfferMethod(mv);
    } else if ("offer".equals(name) && "(Ljava/lang/Object;JLjava/util/concurrent/TimeUnit;)Z".equals(desc)) {
      recreateOfferTimeoutMethod(mv);
    } else if ("put".equals(name) && "(Ljava/lang/Object;)V".equals(desc)) {
      recreatePutMethod(mv);
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
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/LinkedBlockingQueue", TC_EXTRACT_METHOD_NAME,
                       TC_EXTRACT_METHOD_DESC);
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
  
  private void recreateOfferTimeoutMethod(MethodVisitor mv) {
    mv.visitCode();
    Label l0 = new Label();
    Label l1 = new Label();
    mv.visitTryCatchBlock(l0, l1, l1, "java/lang/InterruptedException");
    Label l2 = new Label();
    Label l3 = new Label();
    Label l4 = new Label();
    mv.visitTryCatchBlock(l2, l3, l4, null);
    mv.visitTryCatchBlock(l0, l4, l4, null);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitLineNumber(266, l5);
    mv.visitVarInsn(ALOAD, 1);
    Label l6 = new Label();
    mv.visitJumpInsn(IFNONNULL, l6);
    mv.visitTypeInsn(NEW, "java/lang/NullPointerException");
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NullPointerException", "<init>", "()V");
    mv.visitInsn(ATHROW);
    mv.visitLabel(l6);
    mv.visitLineNumber(267, l6);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitVarInsn(LLOAD, 2);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/TimeUnit", "toNanos", "(J)J");
    mv.visitVarInsn(LSTORE, 5);
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitLineNumber(268, l7);
    mv.visitInsn(ICONST_M1);
    mv.visitVarInsn(ISTORE, 7);
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitLineNumber(269, l8);
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "putLock",
                      "Ljava/util/concurrent/locks/ReentrantLock;");
    mv.visitVarInsn(ASTORE, 8);
    Label l9 = new Label();
    mv.visitLabel(l9);
    mv.visitLineNumber(270, l9);
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "count",
                      "Ljava/util/concurrent/atomic/AtomicInteger;");
    mv.visitVarInsn(ASTORE, 9);
    Label l10 = new Label();
    mv.visitLabel(l10);
    mv.visitLineNumber(271, l10);
    mv.visitVarInsn(ALOAD, 8);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "lockInterruptibly", "()V");
    mv.visitLabel(l2);
    mv.visitLineNumber(274, l2);
    mv.visitVarInsn(ALOAD, 9);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "get", "()I");
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "capacity", "I");
    Label l11 = new Label();
    mv.visitJumpInsn(IF_ICMPGE, l11);
    Label l12 = new Label();
    mv.visitLabel(l12);
    mv.visitLineNumber(275, l12);
    ByteCodeUtil.pushThis(mv);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/LinkedBlockingQueue", "insert", "(Ljava/lang/Object;)V");
    Label l13 = new Label();
    mv.visitLabel(l13);
    mv.visitLineNumber(276, l13);
    ByteCodeUtil.pushThis(mv);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isManaged", "(Ljava/lang/Object;)Z");
    Label l14 = new Label();
    mv.visitJumpInsn(IFEQ, l14);
    Label l15 = new Label();
    mv.visitLabel(l15);
    mv.visitLineNumber(277, l15);
    mv.visitVarInsn(ALOAD, 9);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "get", "()I");
    mv.visitVarInsn(ISTORE, 7);
    Label l16 = new Label();
    mv.visitLabel(l16);
    mv.visitLineNumber(278, l16);
    ByteCodeUtil.pushThis(mv);
    mv.visitVarInsn(ALOAD, 8);
    mv.visitLdcInsn("put(Ljava/lang/Object;)V");
    mv.visitInsn(ICONST_1);
    mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(AASTORE);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvokeWithTransaction",
                       "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
    Label l17 = new Label();
    mv.visitLabel(l17);
    mv.visitLineNumber(279, l17);
    mv.visitVarInsn(ALOAD, 9);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "getAndIncrement", "()I");
    mv.visitInsn(POP);
    Label l18 = new Label();
    mv.visitJumpInsn(GOTO, l18);
    mv.visitLabel(l14);
    mv.visitLineNumber(281, l14);
    mv.visitVarInsn(ALOAD, 9);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "getAndIncrement", "()I");
    mv.visitVarInsn(ISTORE, 7);
    mv.visitLabel(l18);
    mv.visitLineNumber(283, l18);
    mv.visitVarInsn(ILOAD, 7);
    mv.visitInsn(ICONST_1);
    mv.visitInsn(IADD);
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "capacity", "I");
    Label l19 = new Label();
    mv.visitJumpInsn(IF_ICMPGE, l19);
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "notFull",
                      "Ljava/util/concurrent/locks/Condition;");
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/concurrent/locks/Condition", "signal", "()V");
    Label l20 = new Label();
    mv.visitLabel(l20);
    mv.visitLineNumber(284, l20);
    mv.visitJumpInsn(GOTO, l19);
    mv.visitLabel(l11);
    mv.visitLineNumber(286, l11);
    mv.visitVarInsn(LLOAD, 5);
    mv.visitInsn(LCONST_0);
    mv.visitInsn(LCMP);
    mv.visitJumpInsn(IFGT, l0);
    mv.visitLabel(l3);
    mv.visitLineNumber(295, l3);
    mv.visitVarInsn(ALOAD, 8);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "unlock", "()V");
    Label l21 = new Label();
    mv.visitLabel(l21);
    mv.visitLineNumber(286, l21);
    mv.visitInsn(ICONST_0);
    mv.visitInsn(IRETURN);
    mv.visitLabel(l0);
    mv.visitLineNumber(288, l0);
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "notFull",
                      "Ljava/util/concurrent/locks/Condition;");
    mv.visitVarInsn(LLOAD, 5);
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/concurrent/locks/Condition", "awaitNanos", "(J)J");
    mv.visitVarInsn(LSTORE, 5);
    mv.visitJumpInsn(GOTO, l2);
    mv.visitLabel(l1);
    mv.visitLineNumber(289, l1);
    mv.visitVarInsn(ASTORE, 10);
    Label l22 = new Label();
    mv.visitLabel(l22);
    mv.visitLineNumber(290, l22);
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "notFull",
                      "Ljava/util/concurrent/locks/Condition;");
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/concurrent/locks/Condition", "signal", "()V");
    Label l23 = new Label();
    mv.visitLabel(l23);
    mv.visitLineNumber(291, l23);
    mv.visitVarInsn(ALOAD, 10);
    mv.visitInsn(ATHROW);
    Label l24 = new Label();
    mv.visitLabel(l24);
    mv.visitLineNumber(273, l24);
    mv.visitJumpInsn(GOTO, l2);
    mv.visitJumpInsn(GOTO, l19);
    mv.visitLabel(l4);
    mv.visitLineNumber(294, l4);
    mv.visitVarInsn(ASTORE, 11);
    Label l25 = new Label();
    mv.visitLabel(l25);
    mv.visitLineNumber(295, l25);
    mv.visitVarInsn(ALOAD, 8);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "unlock", "()V");
    Label l26 = new Label();
    mv.visitLabel(l26);
    mv.visitLineNumber(296, l26);
    mv.visitVarInsn(ALOAD, 11);
    mv.visitInsn(ATHROW);
    mv.visitLabel(l19);
    mv.visitLineNumber(295, l19);
    mv.visitVarInsn(ALOAD, 8);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "unlock", "()V");
    Label l27 = new Label();
    mv.visitLabel(l27);
    mv.visitLineNumber(297, l27);
    mv.visitVarInsn(ILOAD, 7);
    Label l28 = new Label();
    mv.visitJumpInsn(IFNE, l28);
    ByteCodeUtil.pushThis(mv);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/LinkedBlockingQueue", "signalNotEmpty", "()V");
    mv.visitLabel(l28);
    mv.visitLineNumber(298, l28);
    mv.visitInsn(ICONST_1);
    mv.visitInsn(IRETURN);
    Label l29 = new Label();
    mv.visitLabel(l29);
    mv.visitLocalVariable("this", "Ljava/util/concurrent/LinkedBlockingQueue;",
                          "Ljava/util/concurrent/LinkedBlockingQueue<TE;>;", l5, l29, 0);
    mv.visitLocalVariable("o", "Ljava/lang/Object;", "TE;", l5, l29, 1);
    mv.visitLocalVariable("timeout", "J", null, l5, l29, 2);
    mv.visitLocalVariable("unit", "Ljava/util/concurrent/TimeUnit;", null, l5, l29, 4);
    mv.visitLocalVariable("nanos", "J", null, l7, l29, 5);
    mv.visitLocalVariable("c", "I", null, l8, l29, 7);
    mv.visitLocalVariable("putLock", "Ljava/util/concurrent/locks/ReentrantLock;", null, l9, l29, 8);
    mv.visitLocalVariable("count", "Ljava/util/concurrent/atomic/AtomicInteger;", null, l10, l29, 9);
    mv.visitLocalVariable("ie", "Ljava/lang/InterruptedException;", null, l22, l24, 10);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void recreatePutMethod(MethodVisitor mv) {
    mv.visitCode();
    Label l0 = new Label();
    Label l1 = new Label();
    mv.visitTryCatchBlock(l0, l1, l1, "java/lang/InterruptedException");
    Label l2 = new Label();
    mv.visitTryCatchBlock(l0, l2, l2, null);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLineNumber(217, l3);
    mv.visitVarInsn(ALOAD, 1);
    Label l4 = new Label();
    mv.visitJumpInsn(IFNONNULL, l4);
    mv.visitTypeInsn(NEW, "java/lang/NullPointerException");
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NullPointerException", "<init>", "()V");
    mv.visitInsn(ATHROW);
    mv.visitLabel(l4);
    mv.visitLineNumber(220, l4);
    mv.visitInsn(ICONST_M1);
    mv.visitVarInsn(ISTORE, 2);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitLineNumber(221, l5);
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "putLock",
                      "Ljava/util/concurrent/locks/ReentrantLock;");
    mv.visitVarInsn(ASTORE, 3);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitLineNumber(222, l6);
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "count",
                      "Ljava/util/concurrent/atomic/AtomicInteger;");
    mv.visitVarInsn(ASTORE, 4);
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitLineNumber(223, l7);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "lockInterruptibly", "()V");
    mv.visitLabel(l0);
    mv.visitLineNumber(231, l0);
    Label l8 = new Label();
    mv.visitJumpInsn(GOTO, l8);
    Label l9 = new Label();
    mv.visitLabel(l9);
    mv.visitLineNumber(232, l9);
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "notFull",
                      "Ljava/util/concurrent/locks/Condition;");
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/concurrent/locks/Condition", "await", "()V");
    mv.visitLabel(l8);
    mv.visitLineNumber(231, l8);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "get", "()I");
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "capacity", "I");
    mv.visitJumpInsn(IF_ICMPEQ, l9);
    Label l10 = new Label();
    mv.visitJumpInsn(GOTO, l10);
    mv.visitLabel(l1);
    mv.visitLineNumber(233, l1);
    mv.visitVarInsn(ASTORE, 5);
    Label l11 = new Label();
    mv.visitLabel(l11);
    mv.visitLineNumber(234, l11);
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "notFull",
                      "Ljava/util/concurrent/locks/Condition;");
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/concurrent/locks/Condition", "signal", "()V");
    Label l12 = new Label();
    mv.visitLabel(l12);
    mv.visitLineNumber(235, l12);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitInsn(ATHROW);
    mv.visitLabel(l10);
    mv.visitLineNumber(237, l10);
    ByteCodeUtil.pushThis(mv);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/LinkedBlockingQueue", "insert", "(Ljava/lang/Object;)V");
    Label l13 = new Label();
    mv.visitLabel(l13);
    mv.visitLineNumber(238, l13);
    ByteCodeUtil.pushThis(mv);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isManaged", "(Ljava/lang/Object;)Z");
    Label l14 = new Label();
    mv.visitJumpInsn(IFEQ, l14);
    Label l15 = new Label();
    mv.visitLabel(l15);
    mv.visitLineNumber(239, l15);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "get", "()I");
    mv.visitVarInsn(ISTORE, 2);
    Label l16 = new Label();
    mv.visitLabel(l16);
    mv.visitLineNumber(240, l16);
    ByteCodeUtil.pushThis(mv);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitLdcInsn("put(Ljava/lang/Object;)V");
    mv.visitInsn(ICONST_1);
    mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(AASTORE);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvokeWithTransaction",
                       "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
    Label l17 = new Label();
    mv.visitLabel(l17);
    mv.visitLineNumber(241, l17);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "getAndIncrement", "()I");
    mv.visitInsn(POP);
    Label l18 = new Label();
    mv.visitJumpInsn(GOTO, l18);
    mv.visitLabel(l14);
    mv.visitLineNumber(243, l14);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "getAndIncrement", "()I");
    mv.visitVarInsn(ISTORE, 2);
    mv.visitLabel(l18);
    mv.visitLineNumber(245, l18);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitInsn(ICONST_1);
    mv.visitInsn(IADD);
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "capacity", "I");
    Label l19 = new Label();
    mv.visitJumpInsn(IF_ICMPGE, l19);
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "notFull",
                      "Ljava/util/concurrent/locks/Condition;");
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/concurrent/locks/Condition", "signal", "()V");
    mv.visitJumpInsn(GOTO, l19);
    mv.visitLabel(l2);
    mv.visitLineNumber(246, l2);
    mv.visitVarInsn(ASTORE, 6);
    Label l20 = new Label();
    mv.visitLabel(l20);
    mv.visitLineNumber(247, l20);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "unlock", "()V");
    Label l21 = new Label();
    mv.visitLabel(l21);
    mv.visitLineNumber(248, l21);
    mv.visitVarInsn(ALOAD, 6);
    mv.visitInsn(ATHROW);
    mv.visitLabel(l19);
    mv.visitLineNumber(247, l19);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "unlock", "()V");
    Label l22 = new Label();
    mv.visitLabel(l22);
    mv.visitLineNumber(249, l22);
    mv.visitVarInsn(ILOAD, 2);
    Label l23 = new Label();
    mv.visitJumpInsn(IFNE, l23);
    ByteCodeUtil.pushThis(mv);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/LinkedBlockingQueue", "signalNotEmpty", "()V");
    mv.visitLabel(l23);
    mv.visitLineNumber(250, l23);
    mv.visitInsn(RETURN);
    Label l24 = new Label();
    mv.visitLabel(l24);
    mv.visitLocalVariable("this", "Ljava/util/concurrent/LinkedBlockingQueue;",
                          "Ljava/util/concurrent/LinkedBlockingQueue<TE;>;", l3, l24, 0);
    mv.visitLocalVariable("o", "Ljava/lang/Object;", "TE;", l3, l24, 1);
    mv.visitLocalVariable("c", "I", null, l5, l24, 2);
    mv.visitLocalVariable("putLock", "Ljava/util/concurrent/locks/ReentrantLock;", null, l6, l24, 3);
    mv.visitLocalVariable("count", "Ljava/util/concurrent/atomic/AtomicInteger;", null, l7, l24, 4);
    mv.visitLocalVariable("ie", "Ljava/lang/InterruptedException;", null, l11, l10, 5);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void recreateOfferMethod(MethodVisitor mv) {
    mv.visitCode();
    Label l0 = new Label();
    Label l1 = new Label();
    mv.visitTryCatchBlock(l0, l1, l1, null);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(309, l2);
    mv.visitVarInsn(ALOAD, 1);
    Label l3 = new Label();
    mv.visitJumpInsn(IFNONNULL, l3);
    mv.visitTypeInsn(NEW, "java/lang/NullPointerException");
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NullPointerException", "<init>", "()V");
    mv.visitInsn(ATHROW);
    mv.visitLabel(l3);
    mv.visitLineNumber(310, l3);
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "count",
                      "Ljava/util/concurrent/atomic/AtomicInteger;");
    mv.visitVarInsn(ASTORE, 2);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitLineNumber(311, l4);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "get", "()I");
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "capacity", "I");
    Label l5 = new Label();
    mv.visitJumpInsn(IF_ICMPNE, l5);
    mv.visitInsn(ICONST_0);
    mv.visitInsn(IRETURN);
    mv.visitLabel(l5);
    mv.visitLineNumber(312, l5);
    mv.visitInsn(ICONST_M1);
    mv.visitVarInsn(ISTORE, 3);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitLineNumber(313, l6);
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "putLock",
                      "Ljava/util/concurrent/locks/ReentrantLock;");
    mv.visitVarInsn(ASTORE, 4);
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitLineNumber(314, l7);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "lock", "()V");
    mv.visitLabel(l0);
    mv.visitLineNumber(316, l0);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "get", "()I");
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "capacity", "I");
    Label l8 = new Label();
    mv.visitJumpInsn(IF_ICMPGE, l8);
    Label l9 = new Label();
    mv.visitLabel(l9);
    mv.visitLineNumber(317, l9);
    ByteCodeUtil.pushThis(mv);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/LinkedBlockingQueue", "insert", "(Ljava/lang/Object;)V");
    Label l10 = new Label();
    mv.visitLabel(l10);
    mv.visitLineNumber(318, l10);
    ByteCodeUtil.pushThis(mv);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isManaged", "(Ljava/lang/Object;)Z");
    Label l11 = new Label();
    mv.visitJumpInsn(IFEQ, l11);
    Label l12 = new Label();
    mv.visitLabel(l12);
    mv.visitLineNumber(319, l12);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "get", "()I");
    mv.visitVarInsn(ISTORE, 3);
    Label l13 = new Label();
    mv.visitLabel(l13);
    mv.visitLineNumber(320, l13);
    ByteCodeUtil.pushThis(mv);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitLdcInsn("put(Ljava/lang/Object;)V");
    mv.visitInsn(ICONST_1);
    mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(AASTORE);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvokeWithTransaction",
                       "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
    Label l14 = new Label();
    mv.visitLabel(l14);
    mv.visitLineNumber(321, l14);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "getAndIncrement", "()I");
    mv.visitInsn(POP);
    Label l15 = new Label();
    mv.visitJumpInsn(GOTO, l15);
    mv.visitLabel(l11);
    mv.visitLineNumber(323, l11);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "getAndIncrement", "()I");
    mv.visitVarInsn(ISTORE, 3);
    mv.visitLabel(l15);
    mv.visitLineNumber(325, l15);
    mv.visitVarInsn(ILOAD, 3);
    mv.visitInsn(ICONST_1);
    mv.visitInsn(IADD);
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "capacity", "I");
    mv.visitJumpInsn(IF_ICMPGE, l8);
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "notFull",
                      "Ljava/util/concurrent/locks/Condition;");
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/concurrent/locks/Condition", "signal", "()V");
    mv.visitJumpInsn(GOTO, l8);
    mv.visitLabel(l1);
    mv.visitLineNumber(327, l1);
    mv.visitVarInsn(ASTORE, 5);
    Label l16 = new Label();
    mv.visitLabel(l16);
    mv.visitLineNumber(328, l16);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "unlock", "()V");
    Label l17 = new Label();
    mv.visitLabel(l17);
    mv.visitLineNumber(329, l17);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitInsn(ATHROW);
    mv.visitLabel(l8);
    mv.visitLineNumber(328, l8);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/ReentrantLock", "unlock", "()V");
    Label l18 = new Label();
    mv.visitLabel(l18);
    mv.visitLineNumber(330, l18);
    mv.visitVarInsn(ILOAD, 3);
    Label l19 = new Label();
    mv.visitJumpInsn(IFNE, l19);
    ByteCodeUtil.pushThis(mv);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/LinkedBlockingQueue", "signalNotEmpty", "()V");
    mv.visitLabel(l19);
    mv.visitLineNumber(331, l19);
    mv.visitVarInsn(ILOAD, 3);
    Label l20 = new Label();
    mv.visitJumpInsn(IFLT, l20);
    mv.visitInsn(ICONST_1);
    mv.visitInsn(IRETURN);
    mv.visitLabel(l20);
    mv.visitInsn(ICONST_0);
    mv.visitInsn(IRETURN);
    Label l21 = new Label();
    mv.visitLabel(l21);
    mv.visitLocalVariable("this", "Ljava/util/concurrent/LinkedBlockingQueue;",
                          "Ljava/util/concurrent/LinkedBlockingQueue<TE;>;", l2, l21, 0);
    mv.visitLocalVariable("o", "Ljava/lang/Object;", "TE;", l2, l21, 1);
    mv.visitLocalVariable("count", "Ljava/util/concurrent/atomic/AtomicInteger;", null, l4, l21, 2);
    mv.visitLocalVariable("c", "I", null, l6, l21, 3);
    mv.visitLocalVariable("putLock", "Ljava/util/concurrent/locks/ReentrantLock;", null, l7, l21, 4);
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
  
  private void addTCExtractMethod() {
    MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, TC_EXTRACT_METHOD_NAME, TC_EXTRACT_METHOD_DESC, "()TE;", null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(144, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "head", "Ljava/util/concurrent/LinkedBlockingQueue$Node;");
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue$Node", "next", "Ljava/util/concurrent/LinkedBlockingQueue$Node;");
    mv.visitVarInsn(ASTORE, 1);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLineNumber(145, l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitFieldInsn(PUTFIELD, "java/util/concurrent/LinkedBlockingQueue", "head", "Ljava/util/concurrent/LinkedBlockingQueue$Node;");
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(146, l2);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue$Node", "item", "Ljava/lang/Object;");
    mv.visitVarInsn(ASTORE, 2);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLineNumber(147, l3);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(ACONST_NULL);
    mv.visitFieldInsn(PUTFIELD, "java/util/concurrent/LinkedBlockingQueue$Node", "item", "Ljava/lang/Object;");
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitLineNumber(148, l4);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInsn(ARETURN);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitLocalVariable("this", "Ljava/util/concurrent/LinkedBlockingQueue;", "Ljava/util/concurrent/LinkedBlockingQueue<TE;>;", l0, l5, 0);
    mv.visitLocalVariable("first", "Ljava/util/concurrent/LinkedBlockingQueue$Node;", "Ljava/util/concurrent/LinkedBlockingQueue$Node<TE;>;", l1, l5, 1);
    mv.visitLocalVariable("x", "Ljava/lang/Object;", "TE;", l3, l5, 2);
    mv.visitMaxs(2, 3);
    mv.visitEnd();
  }

  static class NodeMethodAdapter extends MethodAdapter implements Opcodes {
    public NodeMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      if (GETFIELD == opcode && "java/util/concurrent/LinkedBlockingQueue$Node".equals(owner) && "item".equals(name)
          && "Ljava/lang/Object;".equals(desc)) {
        mv.visitMethodInsn(INVOKEVIRTUAL, owner, GET_ITEM_METHOD_NAME, GET_ITEM_METHOD_DESC);
      } else {
        super.visitFieldInsn(opcode, owner, name, desc);
      }
    }
  }
}
