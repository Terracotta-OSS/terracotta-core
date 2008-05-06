/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package org.terracotta.modules.surefire_2_3;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class JUnitTestSuiteAdapter extends ClassAdapter implements
    ClassAdapterFactory, Opcodes {

  public static final String CLUSTERED_JUNIT_BARRIER_CLASS = "org.terracotta.modules.surefire_2_3.JUnitBarrier";
  
  public JUnitTestSuiteAdapter() {
    super(null);
  }

  public JUnitTestSuiteAdapter(ClassVisitor cv) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new JUnitTestSuiteAdapter(visitor);
  }

  public MethodVisitor visitMethod(int access, String name, String desc,
      String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature,
        exceptions);
    if (!name.equals("runTest")) {
      return mv;
    }

    return new MethodAdapter(mv) {
      public void visitCode() {
        super.visitCode();
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESTATIC,
            CLUSTERED_JUNIT_BARRIER_CLASS.replace('.', '/'),
            "createBarrierAndWait", "(Ljunit/framework/Test;)V");
      }
    };
  }

  // private void addRunTest() {
  // MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "runTest",
  // "(Ljunit/framework/Test;Ljunit/framework/TestResult;)V",
  // null, null);
  // mv.visitCode();
  // Label l0 = new Label();
  // mv.visitLabel(l0);
  // mv.visitVarInsn(ALOAD, 1);
  // mv.visitMethodInsn(INVOKESTATIC,
  // "org/terracotta/modules/surefire_2_3/JUnitBarrier", "createBarrierAndWait",
  // "(Ljunit/framework/Test;)V");
  //    
  // Label l1 = new Label();
  // mv.visitLabel(l1);
  // mv.visitVarInsn(ALOAD, 1);
  // mv.visitVarInsn(ALOAD, 2);
  // mv.visitMethodInsn(INVOKEINTERFACE, "junit/framework/Test", "run",
  // "(Ljunit/framework/TestResult;)V");
  // Label l2 = new Label();
  // mv.visitLabel(l2);
  // mv.visitInsn(RETURN);
  // Label l3 = new Label();
  // mv.visitLabel(l3);
  // mv.visitLocalVariable("this", "Ljunit/framework/TestSuite;", null, l0, l3,
  // 0);
  // mv.visitLocalVariable("test", "Ljunit/framework/Test;", null, l0, l3, 1);
  // mv.visitLocalVariable("result", "Ljunit/framework/TestResult;", null, l0,
  // l3, 2);
  // mv.visitMaxs(0, 0);
  // mv.visitEnd();
  // }

  /**
   * <pre>
   * private static void createBarrierAndWait(Test t) {
   *   int numberOfNodes;
   *   try {
   *     numberOfNodes = Integer.parseInt(System
   *         .getProperty(&quot;tc.numberOfNodes&quot;, &quot;0&quot;));
   *   } catch (Exception ex) {
   *     numberOfNodes = 0;
   *   }
   *   if (numberOfNodes == 0) {
   *     return;
   *   }
   * 
   *   String testName = t.getClass().getName();
   *   if (t instanceof TestCase) {
   *     testName = testName + ((TestCase) t).getName();
   *   }
   * 
   *   String globalLock = &quot;@junit_test_suite_lock&quot;;
   * 
   *   CyclicBarrier barrier;
   * 
   *   ManagerUtil.beginLock(globalLock, Manager.LOCK_TYPE_WRITE);
   *   try {
   *     barrier = (CyclicBarrier) ManagerUtil.lookupOrCreateRoot(&quot;barrier:&quot;
   *         + testName, new CyclicBarrier(numberOfNodes));
   *   } finally {
   *     ManagerUtil.commitLock(globalLock);
   *   }
   * 
   *   try {
   *     barrier.barrier();
   *   } catch (Exception e) {
   *     e.printStackTrace();
   *   }
   * }
   * </pre>
   */
  // private void addCreateBarrierAndWait() {
  // MethodVisitor mv = cv.visitMethod(ACC_PRIVATE + ACC_STATIC,
  // "createBarrierAndWait", "(Ljunit/framework/Test;)V", null, null);
  // mv.visitCode();
  // Label l0 = new Label();
  // Label l1 = new Label();
  // Label l2 = new Label();
  // mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
  // Label l3 = new Label();
  // Label l4 = new Label();
  // mv.visitTryCatchBlock(l3, l4, l4, null);
  // Label l5 = new Label();
  // Label l6 = new Label();
  // mv.visitTryCatchBlock(l5, l6, l4, null);
  // Label l7 = new Label();
  // Label l8 = new Label();
  // mv.visitTryCatchBlock(l6, l7, l8, "java/lang/Exception");
  // mv.visitLabel(l0);
  // mv.visitLdcInsn("tc.numberOfNodes");
  // mv.visitLdcInsn("0");
  // mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "getProperty",
  // "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
  // mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "parseInt",
  // "(Ljava/lang/String;)I");
  // mv.visitVarInsn(ISTORE, 1);
  // mv.visitLabel(l1);
  // Label l9 = new Label();
  // mv.visitJumpInsn(GOTO, l9);
  // mv.visitLabel(l2);
  // mv.visitVarInsn(ASTORE, 2);
  // Label l10 = new Label();
  // mv.visitLabel(l10);
  // mv.visitInsn(ICONST_0);
  // mv.visitVarInsn(ISTORE, 1);
  // mv.visitLabel(l9);
  // mv.visitVarInsn(ILOAD, 1);
  // Label l11 = new Label();
  // mv.visitJumpInsn(IFNE, l11);
  // Label l12 = new Label();
  // mv.visitLabel(l12);
  // mv.visitInsn(RETURN);
  // mv.visitLabel(l11);
  // mv.visitVarInsn(ALOAD, 0);
  // mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass",
  // "()Ljava/lang/Class;");
  // mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName",
  // "()Ljava/lang/String;");
  // mv.visitVarInsn(ASTORE, 2);
  // Label l13 = new Label();
  // mv.visitLabel(l13);
  // mv.visitVarInsn(ALOAD, 0);
  // mv.visitTypeInsn(INSTANCEOF, "junit/framework/TestCase");
  // Label l14 = new Label();
  // mv.visitJumpInsn(IFEQ, l14);
  // Label l15 = new Label();
  // mv.visitLabel(l15);
  // mv.visitTypeInsn(NEW, "java/lang/StringBuffer");
  // mv.visitInsn(DUP);
  // mv.visitVarInsn(ALOAD, 2);
  // mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf",
  // "(Ljava/lang/Object;)Ljava/lang/String;");
  // mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuffer", "<init>",
  // "(Ljava/lang/String;)V");
  // mv.visitVarInsn(ALOAD, 0);
  // mv.visitTypeInsn(CHECKCAST, "junit/framework/TestCase");
  // mv.visitMethodInsn(INVOKEVIRTUAL, "junit/framework/TestCase", "getName",
  // "()Ljava/lang/String;");
  // mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
  // "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
  // mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString",
  // "()Ljava/lang/String;");
  // mv.visitVarInsn(ASTORE, 2);
  // mv.visitLabel(l14);
  // mv.visitLdcInsn("@junit_test_suite_lock");
  // mv.visitVarInsn(ASTORE, 3);
  // Label l16 = new Label();
  // mv.visitLabel(l16);
  // mv.visitVarInsn(ALOAD, 3);
  // mv.visitInsn(ICONST_2);
  // mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil",
  // "beginLock", "(Ljava/lang/String;I)V");
  // mv.visitLabel(l3);
  // mv.visitTypeInsn(NEW, "java/lang/StringBuffer");
  // mv.visitInsn(DUP);
  // mv.visitLdcInsn("barrier:");
  // mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuffer", "<init>",
  // "(Ljava/lang/String;)V");
  // mv.visitVarInsn(ALOAD, 2);
  // mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
  // "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
  // mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString",
  // "()Ljava/lang/String;");
  // mv.visitTypeInsn(NEW, "EDU/oswego/cs/dl/util/concurrent/CyclicBarrier");
  // mv.visitInsn(DUP);
  // mv.visitVarInsn(ILOAD, 1);
  // mv.visitMethodInsn(INVOKESPECIAL,
  // "EDU/oswego/cs/dl/util/concurrent/CyclicBarrier", "<init>", "(I)V");
  // mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil",
  // "lookupOrCreateRoot",
  // "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;");
  // mv.visitTypeInsn(CHECKCAST,
  // "EDU/oswego/cs/dl/util/concurrent/CyclicBarrier");
  // mv.visitVarInsn(ASTORE, 4);
  // Label l17 = new Label();
  // mv.visitLabel(l17);
  // mv.visitJumpInsn(GOTO, l5);
  // mv.visitLabel(l4);
  // mv.visitVarInsn(ASTORE, 6);
  // Label l18 = new Label();
  // mv.visitJumpInsn(JSR, l18);
  // Label l19 = new Label();
  // mv.visitLabel(l19);
  // mv.visitVarInsn(ALOAD, 6);
  // mv.visitInsn(ATHROW);
  // mv.visitLabel(l18);
  // mv.visitVarInsn(ASTORE, 5);
  // Label l20 = new Label();
  // mv.visitLabel(l20);
  // mv.visitVarInsn(ALOAD, 3);
  // mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil",
  // "commitLock", "(Ljava/lang/String;)V");
  // Label l21 = new Label();
  // mv.visitLabel(l21);
  // mv.visitVarInsn(RET, 5);
  // mv.visitLabel(l5);
  // mv.visitJumpInsn(JSR, l18);
  // mv.visitLabel(l6);
  // mv.visitVarInsn(ALOAD, 4);
  // mv.visitMethodInsn(INVOKEVIRTUAL,
  // "EDU/oswego/cs/dl/util/concurrent/CyclicBarrier", "barrier", "()I");
  // mv.visitInsn(POP);
  // mv.visitLabel(l7);
  // Label l22 = new Label();
  // mv.visitJumpInsn(GOTO, l22);
  // mv.visitLabel(l8);
  // mv.visitVarInsn(ASTORE, 5);
  // Label l23 = new Label();
  // mv.visitLabel(l23);
  // mv.visitVarInsn(ALOAD, 5);
  // mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "printStackTrace",
  // "()V");
  // mv.visitLabel(l22);
  // mv.visitInsn(RETURN);
  // Label l24 = new Label();
  // mv.visitLabel(l24);
  // mv.visitLocalVariable("t", "Ljunit/framework/Test;", null, l0, l24, 0);
  // mv.visitLocalVariable("numberOfNodes", "I", null, l1, l2, 1);
  // mv.visitLocalVariable("numberOfNodes", "I", null, l9, l24, 1);
  // mv.visitLocalVariable("ex", "Ljava/lang/Exception;", null, l10, l9, 2);
  // mv.visitLocalVariable("testName", "Ljava/lang/String;", null, l13, l24, 2);
  // mv.visitLocalVariable("globalLock", "Ljava/lang/String;", null, l16, l24,
  // 3);
  // mv.visitLocalVariable("barrier",
  // "LEDU/oswego/cs/dl/util/concurrent/CyclicBarrier;", null, l17, l4, 4);
  // mv.visitLocalVariable("barrier",
  // "LEDU/oswego/cs/dl/util/concurrent/CyclicBarrier;", null, l6, l24, 4);
  // mv.visitLocalVariable("e", "Ljava/lang/Exception;", null, l23, l22, 5);
  // mv.visitMaxs(4, 7);
  // mv.visitEnd();
  // }
}
