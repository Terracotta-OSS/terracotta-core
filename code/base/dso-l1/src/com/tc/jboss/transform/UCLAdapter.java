/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.jboss.transform;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.loaders.NamedClassLoader;

public class UCLAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {

  private static final String LOADER_DESC_FIELD = ByteCodeUtil.TC_FIELD_PREFIX + "loaderDesc";
  private String              owner;

  public UCLAdapter() {
    super(null);
  }

  private UCLAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new UCLAdapter(visitor, loader);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    interfaces = ByteCodeUtil.addInterfaces(interfaces, new String[] { NamedClassLoader.CLASS });
    super.visit(version, access, name, signature, superName, interfaces);
    owner = name;
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if ("<init>".equals(name)) {
      mv = new CstrAdapter(mv);
    }

    return mv;
  }

  public void visitEnd() {
    addLoaderDescField();
    addNamedClassLoaderMethods();
    super.visitEnd();
  }

  private void addNamedClassLoaderMethods() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC, "__tc_setClassLoaderName",
                                         "(Ljava/lang/String;)V", null, null);
    mv.visitTypeInsn(NEW, "java/lang/UnsupportedOperationException");
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/UnsupportedOperationException", "<init>", "()V");
    mv.visitInsn(ATHROW);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    mv = super.visitMethod(ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC, "__tc_getClassLoaderName", "()Ljava/lang/String;",
                           null, null);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, owner, LOADER_DESC_FIELD, "Ljava/lang/String;");
    Label l1 = new Label();
    mv.visitJumpInsn(IFNONNULL, l1);
    mv.visitTypeInsn(NEW, "java/lang/UnsupportedOperationException");
    mv.visitInsn(DUP);
    mv.visitTypeInsn(NEW, "java/lang/StringBuffer");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("Classes from this loader cannot be shared ");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "(Ljava/lang/String;)V");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                       "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/UnsupportedOperationException", "<init>", "(Ljava/lang/String;)V");
    mv.visitInsn(ATHROW);
    mv.visitLabel(l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, owner, LOADER_DESC_FIELD, "Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addLoaderDescField() {
    super.visitField(ACC_SYNTHETIC | ACC_TRANSIENT | ACC_PRIVATE | ACC_FINAL, LOADER_DESC_FIELD, "Ljava/lang/String;",
                     null, null);
  }

  private class CstrAdapter extends MethodAdapter implements Opcodes {

    public CstrAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitInsn(int opcode) {
      if (RETURN == opcode) {
        assignLoaderDesc();

        super.visitVarInsn(ALOAD, 0);
        super.visitFieldInsn(GETFIELD, owner, LOADER_DESC_FIELD, "Ljava/lang/String;");
        Label isNull = new Label();
        super.visitJumpInsn(IFNULL, isNull);
        super.visitVarInsn(ALOAD, 0);
        super.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper",
                              "registerGlobalLoader", "(Lcom/tc/object/loaders/NamedClassLoader;)V");
        super.visitLabel(isNull);
      }

      super.visitInsn(opcode);
    }

    private void assignLoaderDesc() {
      super.visitVarInsn(ALOAD, 0);
      super.visitVarInsn(ALOAD, 0);
      super.visitMethodInsn(INVOKESTATIC, "com/tc/jboss/JBossLoaderNaming", "getLoaderName",
                            "(Ljava/lang/ClassLoader;)Ljava/lang/String;");
      super.visitFieldInsn(PUTFIELD, owner, LOADER_DESC_FIELD, "Ljava/lang/String;");
    }

  }

}
