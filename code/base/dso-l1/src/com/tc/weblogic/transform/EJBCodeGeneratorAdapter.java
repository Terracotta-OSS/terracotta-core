/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.weblogic.transform;


import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ByteCodeUtil;

public class EJBCodeGeneratorAdapter extends ClassAdapter {

  public EJBCodeGeneratorAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("appendInterfaceMethodDeclaration".equals(name)) {
      mv = new AppendInterfaceMethodDeclarationAdapter(mv);
    }
    return mv;
  }

  private static class AppendInterfaceMethodDeclarationAdapter extends MethodAdapter implements Opcodes {

    public AppendInterfaceMethodDeclarationAdapter(MethodVisitor mv) {
      super(mv);
      Label l = new Label();
      mv.visitVarInsn(ALOAD, 2);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "getName", "()Ljava/lang/String;");
      mv.visitLdcInsn(ByteCodeUtil.TC_METHOD_PREFIX);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z");
      mv.visitJumpInsn(IFEQ, l);
      mv.visitInsn(RETURN);
      mv.visitLabel(l);
    }

  }

}
