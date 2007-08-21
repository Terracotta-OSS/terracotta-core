/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
import com.tc.object.loaders.Namespace;

public class GenericClassLoaderAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {

  private static final String LOADER_DESC_FIELD = ByteCodeUtil.TC_FIELD_PREFIX + "loaderDesc";

  public GenericClassLoaderAdapter() {
    super(null);
  }

  private GenericClassLoaderAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new GenericClassLoaderAdapter(visitor, loader);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    interfaces = ByteCodeUtil.addInterfaces(interfaces, new String[] { ByteCodeUtil.NAMEDCLASSLOADER_CLASS });
    super.visit(version, access, name, signature, superName, interfaces);

    // add field for loader description
    super.visitField(ACC_PRIVATE | ACC_SYNTHETIC | ACC_TRANSIENT, LOADER_DESC_FIELD, "Ljava/lang/String;", null, null);
  }

  public void visitEnd() {
    addNamedClassLoaderMethods();
    super.visitEnd();
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("setAnnotation".equals(name)) {
      mv = new SetAnnotationAdatper(mv);
    }

    return mv;
  }

  private void addNamedClassLoaderMethods() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC, "__tc_getClassLoaderName",
                                         "()Ljava/lang/String;", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/utils/classloaders/GenericClassLoader", LOADER_DESC_FIELD,
                      "Ljava/lang/String;");
    Label loaderDescDefined = new Label();
    mv.visitJumpInsn(IFNONNULL, loaderDescDefined);
    mv.visitTypeInsn(NEW, "java/lang/IllegalStateException");
    mv.visitInsn(DUP);
    mv.visitTypeInsn(NEW, "java/lang/StringBuffer");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("No terracotta loader name defined for ");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "(Ljava/lang/String;)V");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                       "(Ljava/lang/Object;)Ljava/lang/StringBuffer;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "(Ljava/lang/String;)V");
    mv.visitInsn(ATHROW);
    mv.visitLabel(loaderDescDefined);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/utils/classloaders/GenericClassLoader", LOADER_DESC_FIELD,
                      "Ljava/lang/String;");
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

  private static class SetAnnotationAdatper extends MethodAdapter implements Opcodes {

    public SetAnnotationAdatper(MethodVisitor mv) {
      super(mv);
    }

    public void visitInsn(int opcode) {
      if (RETURN == opcode) {
        // update the terracotta loader desc for this loader
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(Namespace.WEBLOGIC_NAMESPACE);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/utils/classloaders/GenericClassLoader", "getAnnotation",
                           "()Lweblogic/utils/classloaders/Annotation;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "weblogic/utils/classloaders/Annotation", "getAnnotationString",
                           "()Ljava/lang/String;");
        mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/loaders/Namespace", "createLoaderName",
                           "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
        mv.visitFieldInsn(PUTFIELD, "weblogic/utils/classloaders/GenericClassLoader", LOADER_DESC_FIELD,
                          "Ljava/lang/String;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper",
                           "registerGlobalLoader", "(Lcom/tc/object/loaders/NamedClassLoader;)V");
      }
      super.visitInsn(opcode);
    }

  }

// for asm code above
//  void foo() {
//    ClassProcessorHelper.registerGlobalLoader((NamedClassLoader) this);
//  }

}
