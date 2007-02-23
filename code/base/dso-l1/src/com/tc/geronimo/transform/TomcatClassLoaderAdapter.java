/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.geronimo.transform;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.loaders.NamedClassLoader;

public class TomcatClassLoaderAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {

  public TomcatClassLoaderAdapter() {
    super(null);
  }

  private TomcatClassLoaderAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new TomcatClassLoaderAdapter(visitor, loader);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    interfaces = ByteCodeUtil.addInterfaces(interfaces, new String[] { NamedClassLoader.CLASS });
    super.visit(version, access, name, signature, superName, interfaces);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("<init>".equals(name) && Type.getArgumentTypes(desc).length > 0) {
      mv = new CstrAdapter(mv);
    }

    return mv;
  }

  public void visitEnd() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC, "__tc_getClassLoaderName",
                                         "()Ljava/lang/String;", null, null);
    mv.visitCode();
    mv.visitFieldInsn(GETSTATIC, "com/tc/object/loaders/Namespace", "GERONIMO_NAMESPACE", "Ljava/lang/String;");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/net/URLClassLoader", "getParent", "()Ljava/lang/ClassLoader;");
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

    super.visitEnd();
  }

  private static class CstrAdapter extends MethodAdapter implements Opcodes {

    public CstrAdapter(MethodVisitor mv) {
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
