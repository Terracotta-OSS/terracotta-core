/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.weblogic.transform;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;

public class EJBCodeGeneratorAdapter extends ClassAdapter implements ClassAdapterFactory {

  public EJBCodeGeneratorAdapter() {
    super(null);
  }

  private EJBCodeGeneratorAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new EJBCodeGeneratorAdapter(visitor, loader);
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
    }

    public void visitCode() {
      super.visitCode();
      Label notTC = new Label();
      mv.visitVarInsn(ALOAD, 2);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "getName", "()Ljava/lang/String;");
      mv.visitLdcInsn(ByteCodeUtil.TC_METHOD_PREFIX);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z");
      mv.visitJumpInsn(IFEQ, notTC);
      mv.visitInsn(RETURN);
      mv.visitLabel(notTC);
    }

  }

}
