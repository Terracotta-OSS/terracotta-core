/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.weblogic.transform;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ByteCodeUtil;

import java.lang.reflect.Modifier;

public class ServerAdapter extends ClassAdapter implements Opcodes {

  public ServerAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    if ("main".equals(name) && "([Ljava/lang/String;)V".equals(desc) && Modifier.isStatic(access)
        && Modifier.isPublic(access)) {
      name = ByteCodeUtil.TC_METHOD_PREFIX + name;
    }

    return super.visitMethod(access, name, desc, signature, exceptions);
  }

  public void visitEnd() {
    addMainWrapper();
    super.visitEnd();
  }

  private void addMainWrapper() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
    mv.visitCode();

    mv.visitLdcInsn("weblogic.Server");
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;");
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/SessionsHelper", "injectClasses",
                       "(Ljava/lang/ClassLoader;)V");

    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESTATIC, "weblogic/Server", ByteCodeUtil.TC_METHOD_PREFIX + "main",
                       "([Ljava/lang/String;)V");
    Label l2 = new Label();
    mv.visitJumpInsn(GOTO, l2);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitVarInsn(ASTORE, 2);
    Label l4 = new Label();
    mv.visitJumpInsn(JSR, l4);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInsn(ATHROW);
    mv.visitLabel(l4);
    mv.visitVarInsn(ASTORE, 1);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper", "shutdown", "()V");
    Label l7 = new Label();
    mv.visitJumpInsn(GOTO, l7);
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitVarInsn(ASTORE, 3);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V");
    mv.visitLabel(l7);
    mv.visitVarInsn(RET, 1);
    mv.visitLabel(l2);
    mv.visitJumpInsn(JSR, l4);
    Label l10 = new Label();
    mv.visitLabel(l10);
    mv.visitInsn(RETURN);
    mv.visitTryCatchBlock(l1, l3, l3, null);
    mv.visitTryCatchBlock(l2, l10, l3, null);
    mv.visitTryCatchBlock(l6, l8, l8, "java/lang/Throwable");
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

}
