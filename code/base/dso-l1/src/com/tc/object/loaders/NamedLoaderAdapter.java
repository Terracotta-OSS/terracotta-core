/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.loaders;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;

/**
 * Adds the NamedClassLoader interface (and required impl) to a loader implementation
 */
public class NamedLoaderAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {

  private static final String LOADER_NAME_FIELD = ByteCodeUtil.TC_FIELD_PREFIX + "loaderName";
  private String              owner;

  public NamedLoaderAdapter() {
    super(null);
  }

  private NamedLoaderAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new NamedLoaderAdapter(visitor, loader);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    interfaces = ByteCodeUtil.addInterfaces(interfaces, new String[] { ByteCodeUtil.NAMEDCLASSLOADER_CLASS });
    super.visit(version, access, name, signature, superName, interfaces);
    this.owner = name;
  }

  public void visitEnd() {
    super.visitField(ACC_SYNTHETIC | ACC_VOLATILE | ACC_TRANSIENT | ACC_PRIVATE, LOADER_NAME_FIELD,
                     "Ljava/lang/String;", null, null);

    MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, "__tc_setClassLoaderName",
                                         "(Ljava/lang/String;)V", null, null);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitFieldInsn(PUTFIELD, owner, LOADER_NAME_FIELD, "Ljava/lang/String;");
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    mv = super.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, "__tc_getClassLoaderName", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, owner, LOADER_NAME_FIELD, "Ljava/lang/String;");
    Label l1 = new Label();
    mv.visitJumpInsn(IFNONNULL, l1);
    mv.visitTypeInsn(NEW, "java/lang/IllegalStateException");
    mv.visitInsn(DUP);
    mv.visitTypeInsn(NEW, "java/lang/StringBuffer");
    mv.visitInsn(DUP);
    mv
        .visitLdcInsn("Classloader name not set, instances defined from this loader not supported in Terracotta (loader: ");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "(Ljava/lang/String;)V");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                       "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    mv.visitLdcInsn(")");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                       "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "(Ljava/lang/String;)V");
    mv.visitInsn(ATHROW);
    mv.visitLabel(l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, owner, LOADER_NAME_FIELD, "Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    super.visitEnd();
  }

}
