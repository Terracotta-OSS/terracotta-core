/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.geronimo.transform;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.FieldVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.loaders.NamedClassLoader;

/**
 * Adds NamedClassLoader interface to Geronimo loader, and register it with DSO
 */
public class MultiParentClassLoaderAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {

  private String idDesc;

  public MultiParentClassLoaderAdapter() {
    super(null);
  }
  
  private MultiParentClassLoaderAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }
  
  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new MultiParentClassLoaderAdapter(visitor, loader);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    interfaces = ByteCodeUtil.addInterfaces(interfaces, new String[] { NamedClassLoader.CLASS });
    super.visit(version, access, name, signature, superName, interfaces);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("<init>".equals(name)) { return new CstrVisitor(mv); }
    return mv;
  }

  public void visitEnd() {
    addNamedClassLoaderMethods();
    super.visitEnd();
  }

  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    if ("id".equals(name)) {
      // the type of the "id" field is different between 1.0 and 1.1
      idDesc = desc;
    }

    return super.visitField(access, name, desc, signature, value);
  }

  private void addNamedClassLoaderMethods() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC, "__tc_getClassLoaderName",
                                         "()Ljava/lang/String;", null, null);
    mv.visitCode();
    mv.visitFieldInsn(GETSTATIC, "com/tc/object/loaders/Namespace", "GERONIMO_NAMESPACE", "Ljava/lang/String;");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/geronimo/kernel/config/MultiParentClassLoader", "getId", "()"
                                                                                                           + idDesc);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/loaders/Namespace", "createLoaderName",
                       "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/geronimo/GeronimoLoaderNaming", "adjustName",
                       "(Ljava/lang/String;)Ljava/lang/String;");

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

  private static class CstrVisitor extends MethodAdapter implements Opcodes {

    public CstrVisitor(MethodVisitor mv) {
      super(mv);
    }

    public void visitInsn(int opcode) {
      if (opcode == RETURN) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper",
                           "registerGlobalLoader", "(" + NamedClassLoader.TYPE + ")V");
      }
      super.visitInsn(opcode);
    }

  }

}
