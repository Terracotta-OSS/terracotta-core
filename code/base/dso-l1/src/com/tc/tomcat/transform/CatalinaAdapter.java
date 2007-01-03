/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.tomcat.transform;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ByteCodeUtil;

public class CatalinaAdapter extends ClassAdapter implements Opcodes {

  private static final String INJECT_CLASSES = ByteCodeUtil.TC_METHOD_PREFIX + "injectClasses";

  public CatalinaAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if ("start".equals(name) && "()V".equals(desc)) {
      return new StartAdapter(mv);
    } else if ("<clinit>".equals(name)) { return new CLInitMethodAdapter(mv); }

    return mv;
  }

  public void visitEnd() {
    addInjectClassesMethod();
  }

  private void addInjectClassesMethod() {
    MethodVisitor mv = visitMethod(ACC_PRIVATE + ACC_STATIC + ACC_SYNTHETIC, INJECT_CLASSES, "()V", null, null);
    mv.visitCode();
    Label l0 = new Label();
    Label l1 = new Label();
    Label l2 = new Label();
    mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
    mv.visitLabel(l0);
    mv.visitLdcInsn("org.apache.catalina.startup.Catalina");
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;");
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/SessionsHelper", "injectClasses",
                       "(Ljava/lang/ClassLoader;)V");
    mv.visitLabel(l1);
    Label l3 = new Label();
    mv.visitJumpInsn(GOTO, l3);
    mv.visitLabel(l2);
    mv.visitVarInsn(ASTORE, 0);
    mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
    mv.visitInsn(DUP);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V");
    mv.visitInsn(ATHROW);
    mv.visitLabel(l3);
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private static class CLInitMethodAdapter extends MethodAdapter implements Opcodes {

    public CLInitMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitInsn(int opcode) {
      if (opcode == Opcodes.RETURN) {
        super.visitMethodInsn(INVOKESTATIC, "org/apache/catalina/startup/Catalina", INJECT_CLASSES, "()V");
      }
      super.visitInsn(opcode);
    }
  }

  private static class StartAdapter extends MethodAdapter implements Opcodes {

    public StartAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitInsn(int opcode) {
      if (opcode == RETURN) {
        super.visitVarInsn(ALOAD, 0);
        super.visitFieldInsn(GETFIELD, "org/apache/catalina/startup/Catalina", "await", "Z");
        Label notAwait = new Label();
        super.visitJumpInsn(IFEQ, notAwait);
        super.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper", "shutdown", "()V");
        super.visitLabel(notAwait);
      }
      super.visitInsn(opcode);
    }
  }

}
