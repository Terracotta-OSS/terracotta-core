/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.util.FieldUtils;

import java.util.HashSet;
import java.util.Set;

public class JavaLangReflectFieldAdapter extends ClassAdapter implements Opcodes {

  private static final Set setters = new HashSet();
  private static final Set getters = new HashSet();

  static {
    getters.add("get(Ljava/lang/Object;)Ljava/lang/Object;");

    setters.add("set(Ljava/lang/Object;Ljava/lang/Object;)V");
    setters.add("setByte(Ljava/lang/Object;B)V");
    setters.add("setBoolean(Ljava/lang/Object;Z)V");
    setters.add("setChar(Ljava/lang/Object;C)V");
    setters.add("setDouble(Ljava/lang/Object;D)V");
    setters.add("setFloat(Ljava/lang/Object;F)V");
    setters.add("setInt(Ljava/lang/Object;I)V");
    setters.add("setLong(Ljava/lang/Object;J)V");
    setters.add("setShort(Ljava/lang/Object;S)V");
  }

  public JavaLangReflectFieldAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    String method = name + desc;

    if (setters.contains(method)) {
      callFieldUtilSetter(mv, name, desc);
    } else if (getters.contains(method)) {
      rewriteGetter(mv, name, desc);
      return null;
    }

    return mv;
  }

  private String getFieldUtilsSetterDesc(String desc) {
    int index = desc.indexOf(")");
    StringBuffer sb = new StringBuffer(desc.substring(0, index));
    sb.append("Ljava/lang/reflect/Field;)Z");
    return sb.toString();
  }

  private void callFieldUtilSetter(MethodVisitor mv, String name, String desc) {
    Type type = Type.getArgumentTypes(desc)[1];
    Label notSet = new Label();
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(type.getOpcode(ILOAD), 2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESTATIC, FieldUtils.CLASS, name, getFieldUtilsSetterDesc(desc));
    mv.visitJumpInsn(IFEQ, notSet);
    mv.visitInsn(RETURN);
    mv.visitLabel(notSet);
  }

  private void rewriteGetter(MethodVisitor mv, String name, String desc) {
    Type returnType = Type.getReturnType(desc);

    mv.visitCode();
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/reflect/Field", "getFieldAccessor",
                       "(Ljava/lang/Object;)Lsun/reflect/FieldAccessor;");
    mv.visitMethodInsn(INVOKESTATIC, FieldUtils.CLASS, name, FieldUtils.GET_DESC + returnType.getDescriptor());
    mv.visitInsn(returnType.getOpcode(IRETURN));
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

}