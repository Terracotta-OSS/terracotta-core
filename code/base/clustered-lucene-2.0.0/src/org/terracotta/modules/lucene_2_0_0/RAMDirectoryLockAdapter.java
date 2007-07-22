/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.lucene_2_0_0;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;

public class RAMDirectoryLockAdapter extends ClassAdapter implements ClassAdapterFactory, Opcodes {

  private static final String IS_SHARED_METHOD     = ByteCodeUtil.TC_METHOD_PREFIX + "isShared";
  private static final String DSO_LOCK_NAME_METHOD = ByteCodeUtil.TC_METHOD_PREFIX + "dsoLockName";

  private String              className;

  public RAMDirectoryLockAdapter(ClassVisitor cv) {
    super(cv);
  }

  public RAMDirectoryLockAdapter() {
    super(null);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new RAMDirectoryLockAdapter(visitor);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    this.className = name;
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    if ("obtain".equals(name) || "release".equals(name) || "isLocked".equals(name)) {
      name = ByteCodeUtil.TC_METHOD_PREFIX + name;
    }

    return super.visitMethod(access, name, desc, signature, exceptions);
  }

  public void visitEnd() {
    addIsShared();
    addDsoLockName();
    addObtainWrapper();
    addReleaseWrapper();
    addIsLockedWrapper();

    super.visitEnd();
  }

  private void addIsLockedWrapper() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "isLocked", "()Z", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, className, IS_SHARED_METHOD, "()Z");
    Label notShared = new Label();
    mv.visitJumpInsn(IFEQ, notShared);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, className, DSO_LOCK_NAME_METHOD, "()Ljava/lang/String;");
    mv.visitInsn(ICONST_2);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isLocked", "(Ljava/lang/Object;I)Z");
    mv.visitInsn(IRETURN);
    mv.visitLabel(notShared);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, className, "__tc_isLocked", "()Z");
    mv.visitInsn(IRETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addReleaseWrapper() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "release", "()V", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, className, IS_SHARED_METHOD, "()Z");
    Label notShared = new Label();
    mv.visitJumpInsn(IFEQ, notShared);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, className, DSO_LOCK_NAME_METHOD, "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "commitLock", "(Ljava/lang/String;)V");
    mv.visitInsn(RETURN);
    mv.visitLabel(notShared);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, className, "__tc_release", "()V");
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addDsoLockName() {
    MethodVisitor mv = super.visitMethod(ACC_PRIVATE, DSO_LOCK_NAME_METHOD, "()Ljava/lang/String;", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, className, "this$0", "Lorg/apache/lucene/store/RAMDirectory;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/lucene/store/RAMDirectory", ByteCodeUtil.fieldGetterMethod("files"),
        "()Ljava/util/Hashtable;");
    mv.visitTypeInsn(CHECKCAST, "com/tc/object/bytecode/Manageable");
    mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/bytecode/Manageable", "__tc_managed",
        "()Lcom/tc/object/TCObject;");
    mv.visitVarInsn(ASTORE, 1);
    mv.visitTypeInsn(NEW, "java/lang/StringBuffer");
    mv.visitInsn(DUP);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "(Ljava/lang/String;)V");
    mv.visitLdcInsn("_");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
        "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/TCObject", "getObjectID", "()Lcom/tc/object/ObjectID;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/tc/object/ObjectID", "toLong", "()J");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(J)Ljava/lang/StringBuffer;");
    mv.visitLdcInsn("_");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
        "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, className, "val$name", "Ljava/lang/String;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
        "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addIsShared() {
    MethodVisitor mv = super.visitMethod(ACC_PRIVATE, IS_SHARED_METHOD, "()Z", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, className, "this$0", "Lorg/apache/lucene/store/RAMDirectory;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/lucene/store/RAMDirectory", ByteCodeUtil.fieldGetterMethod("files"),
        "()Ljava/util/Hashtable;");
    mv.visitTypeInsn(CHECKCAST, "com/tc/object/bytecode/Manageable");
    mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/bytecode/Manageable", "__tc_isManaged", "()Z");
    mv.visitInsn(IRETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addObtainWrapper() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "obtain", "()Z", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, className, IS_SHARED_METHOD, "()Z");
    Label notShared = new Label();
    mv.visitJumpInsn(IFEQ, notShared);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, className, DSO_LOCK_NAME_METHOD, "()Ljava/lang/String;");
    mv.visitInsn(ICONST_2);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "beginLock", "(Ljava/lang/String;I)V");
    mv.visitInsn(ICONST_1);
    mv.visitInsn(IRETURN);
    mv.visitLabel(notShared);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, className, "__tc_obtain", "()Z");
    mv.visitInsn(IRETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

}
