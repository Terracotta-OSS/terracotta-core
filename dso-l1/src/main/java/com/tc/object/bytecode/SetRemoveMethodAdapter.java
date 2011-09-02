/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.SerializationUtil;

public class SetRemoveMethodAdapter extends AbstractMethodAdapter implements Opcodes {

  private final String type;
  private final String mapType;
  private final String mapField;
  private final String mapFieldType;

  public SetRemoveMethodAdapter(String type, String mapType, String mapField, String mapFieldType) {
    this.type = type;
    this.mapType = mapType;
    this.mapField = mapField;
    this.mapFieldType = mapFieldType;
  }

  public MethodVisitor adapt(ClassVisitor cv) {
    String renamed = ByteCodeUtil.TC_METHOD_PREFIX + this.methodName;

    addNewRemoveMethod(cv, renamed);

    return cv.visitMethod(ACC_PRIVATE | ACC_SYNTHETIC, renamed, this.description, this.signature, this.exceptions);
  }

  private void addNewRemoveMethod(ClassVisitor cv, String renamed) {
    MethodVisitor mv = cv.visitMethod(this.access, this.methodName, this.description, this.signature, this.exceptions);

    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, type, "__tc_isManaged", "()Z");
    Label notManaged = new Label();
    mv.visitJumpInsn(IFEQ, notManaged);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "checkWriteAccess", "(Ljava/lang/Object;)V");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, type, mapField, "L" + mapFieldType + ";");
    mv.visitTypeInsn(CHECKCAST, mapType);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, mapType, "removeEntryForKey", "(Ljava/lang/Object;)L" + mapType + "$Entry;");
    mv.visitVarInsn(ASTORE, 2);
    mv.visitVarInsn(ALOAD, 2);
    Label entryNotNull = new Label();
    mv.visitJumpInsn(IFNONNULL, entryNotNull);
    mv.visitInsn(ICONST_0);
    mv.visitInsn(IRETURN);
    mv.visitLabel(entryNotNull);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitLdcInsn(SerializationUtil.REMOVE_SIGNATURE);
    mv.visitInsn(ICONST_1);
    mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getKey", "()Ljava/lang/Object;");
    mv.visitInsn(AASTORE);
    mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "logicalInvoke",
                       "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
    mv.visitInsn(ICONST_1);
    mv.visitInsn(IRETURN);
    mv.visitLabel(notManaged);
    ByteCodeUtil.pushThis(mv);
    ByteCodeUtil.pushMethodArguments(this.access, this.description, mv);
    mv.visitMethodInsn(INVOKESPECIAL, this.ownerDots.replace('.', '/'), renamed, this.description);
    mv.visitInsn(IRETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  public boolean doesOriginalNeedAdapting() {
    return false;
  }

}
