/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.loaders;


import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ByteCodeUtil;

/**
 * Adds NamedClassLoader interface to a class
 */
public class StandardClassLoaderAdapter extends ClassAdapter implements Opcodes {

  private String loaderName;

  public StandardClassLoaderAdapter(ClassVisitor cv, String loaderName) {
    super(cv);
    this.loaderName = loaderName;
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    interfaces = ByteCodeUtil.addInterfaces(interfaces, new String[] { NamedClassLoader.CLASS });
    super.visit(version, access, name, signature, superName, interfaces);
  }

  public void visitEnd() {
    addNamedClassLoaderMethods();
    super.visitEnd();
  }

  private void addNamedClassLoaderMethods() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC, "__tc_getClassLoaderName",
                                         "()Ljava/lang/String;", null, null);
    mv.visitCode();
    mv.visitLdcInsn(loaderName);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    mv = super.visitMethod(ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC, "__tc_setClassLoaderName", "(Ljava/lang/String;)V",
                           null, null);
    mv.visitCode();
    mv.visitTypeInsn(NEW, "java/lang/AssertionError");
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/AssertionError", "<init>", "()V");
    mv.visitInsn(ATHROW);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

}
