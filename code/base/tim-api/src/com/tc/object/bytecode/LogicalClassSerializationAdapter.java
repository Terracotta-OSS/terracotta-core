/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class LogicalClassSerializationAdapter implements Opcodes {
  public static final String SERIALIZATION_OVERRIDE_METHOD_NAME = ByteCodeUtil.TC_METHOD_PREFIX
                                                                  + "isSerializationOverride";
  public static final String SERIALIZATION_OVERRIDE_METHOD_DESC = "()Z";
  public static final String WRITE_OBJECT_SIGNATURE             = "writeObject(Ljava/io/ObjectOutputStream;)V";
  public static final String READ_OBJECT_SIGNATURE              = "readObject(Ljava/io/ObjectInputStream;)V";

  public static void addCheckSerializationOverrideMethod(ClassVisitor cv, boolean returnValue) {
    MethodVisitor mv = cv.visitMethod(ACC_PROTECTED, SERIALIZATION_OVERRIDE_METHOD_NAME,
                                      SERIALIZATION_OVERRIDE_METHOD_DESC, null, null);
    mv.visitCode();
    if (returnValue) {
      mv.visitInsn(ICONST_1);
    } else {
      mv.visitInsn(ICONST_0);
    }
    mv.visitInsn(IRETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  public static void addDelegateFieldWriteObjectCode(MethodVisitor mv, String classNameSlashes,
                                                     String logicalExtendingClassName, String delegateFieldName) {
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, classNameSlashes, ByteCodeUtil.fieldGetterMethod(delegateFieldName),
                       "()L" + logicalExtendingClassName + ";");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/ObjectOutputStream", "writeObject", "(Ljava/lang/Object;)V");
  }

  public static void addDelegateFieldReadObjectCode(MethodVisitor mv, String classNameSlashes,
                                                    String logicalExtendingClassName, String delegateFieldName) {
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/ObjectInputStream", "readObject", "()Ljava/lang/Object;");
    mv.visitTypeInsn(CHECKCAST, logicalExtendingClassName);
    mv.visitMethodInsn(INVOKESPECIAL, classNameSlashes, ByteCodeUtil.fieldSetterMethod(delegateFieldName),
                       "(L" + logicalExtendingClassName + ";)V");
  }

  public static class LogicalClassSerializationMethodAdapter extends MethodAdapter implements Opcodes {
    private final String classNameSlashes;

    public LogicalClassSerializationMethodAdapter(MethodVisitor mv, String classNameSlashes) {
      super(mv);
      this.classNameSlashes = classNameSlashes;
    }

    @Override
    public void visitCode() {
      super.visitCode();

      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, classNameSlashes, SERIALIZATION_OVERRIDE_METHOD_NAME,
                         SERIALIZATION_OVERRIDE_METHOD_DESC);
      Label l1 = new Label();
      mv.visitJumpInsn(IFEQ, l1);
      mv.visitInsn(RETURN);
      visitLabel(l1);
    }
  }

  public static class LogicalSubclassSerializationMethodAdapter extends MethodAdapter implements Opcodes {
    private final String methodSignature;
    private final String classNameSlashes;
    private final String logicalExtendingClassName;
    private final String delegateFieldName;

    public LogicalSubclassSerializationMethodAdapter(MethodVisitor mv, String methodSignature, String classNameSlashes,
                                                     String logicalExtendingClassName, String delegateFieldName) {
      super(mv);
      this.methodSignature = methodSignature;
      this.classNameSlashes = classNameSlashes;
      this.logicalExtendingClassName = logicalExtendingClassName;
      this.delegateFieldName = delegateFieldName;
    }

    @Override
    public void visitCode() {
      super.visitCode();
      if (WRITE_OBJECT_SIGNATURE.equals(methodSignature)) {
        addDelegateFieldWriteObjectCode(mv, classNameSlashes, logicalExtendingClassName, delegateFieldName);
      } else if (READ_OBJECT_SIGNATURE.equals(methodSignature)) {
        addDelegateFieldReadObjectCode(mv, classNameSlashes, logicalExtendingClassName, delegateFieldName);
      }
    }
  }

  public static class LogicalClassSerializationClassAdapter extends ClassAdapter implements Opcodes {
    private final String classNameSlashes;

    public LogicalClassSerializationClassAdapter(ClassVisitor cv, String className) {
      super(cv);
      this.classNameSlashes = className.replace(ChangeClassNameHierarchyAdapter.DOT_DELIMITER,
                                                ChangeClassNameHierarchyAdapter.SLASH_DELIMITER);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      String methodDesc = name + desc;
      if (WRITE_OBJECT_SIGNATURE.equals(methodDesc) || READ_OBJECT_SIGNATURE.equals(methodDesc)) { //
        return new LogicalClassSerializationAdapter.LogicalClassSerializationMethodAdapter(super
            .visitMethod(access, name, desc, signature, exceptions), classNameSlashes);
      }

      return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void visitEnd() {
      addCheckSerializationOverrideMethod(cv, false);
      super.visitEnd();
    }
  }

}
