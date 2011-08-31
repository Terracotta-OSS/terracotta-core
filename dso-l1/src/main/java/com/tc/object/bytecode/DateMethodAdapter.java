/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.object.SerializationUtil;
import com.tc.object.config.MethodSpec;

public class DateMethodAdapter extends LogicalMethodAdapter {

  public DateMethodAdapter() {
    super();
  }

  public DateMethodAdapter(String methodName, int instrumentationType) {
    super(methodName, instrumentationType);
  }

  @Override
  public MethodVisitor adapt(ClassVisitor classVisitor) {
    MethodVisitor mv = super.adapt(classVisitor);

    if (getOwnerSlashes().equals("java/sql/Timestamp")) { return new TimestampMethodAdapter(mv); }
    return mv;
  }

  @Override
  protected void createWrapperMethod(ClassVisitor classVisitor) {
    switch (getInstrumentationType()) {
      case MethodSpec.DATE_ADD_SET_TIME_WRAPPER_LOG:
        addSetTimeMethodWrapper(classVisitor);
        break;
      case MethodSpec.TIMESTAMP_SET_TIME_METHOD_WRAPPER_LOG:
        addTimestampSetTimeMethodWrapper(classVisitor);
        break;
      default:
        super.createWrapperMethod(classVisitor);
    }
  }

  private static class TimestampMethodAdapter extends MethodAdapter implements Opcodes {
    public TimestampMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if ((opcode == INVOKESPECIAL) && ("setTime".equals(name))) {
        super.visitMethodInsn(opcode, owner, ByteCodeUtil.TC_METHOD_PREFIX + name, desc);
      } else {
        super.visitMethodInsn(opcode, owner, name, desc);
      }
    }
  }

  private void addTimestampSetTimeMethodWrapper(ClassVisitor classVisitor) {
    MethodVisitor mv = classVisitor.visitMethod(getWrapperAccess(), getMethodName(), getDescription(), getSignature(),
                                                getExceptions());
    addCheckWriteAccessInstrumentedCode(mv, true);
    Label l0 = new Label();
    mv.visitLabel(l0);
    ByteCodeUtil.pushThis(mv);
    Type[] params = Type.getArgumentTypes(getDescription());
    Type returnType = Type.getReturnType(getDescription());
    for (int i = 0; i < params.length; i++) {
      mv.visitVarInsn(params[i].getOpcode(ILOAD), i + 1);
    }

    mv.visitMethodInsn(INVOKESPECIAL, getOwnerSlashes(), getNewName(), getDescription());
    if (!returnType.equals(Type.VOID_TYPE)) {
      mv.visitVarInsn(returnType.getOpcode(ISTORE), params.length + 1);
    }
    ByteCodeUtil.pushThis(mv);
    mv.visitLdcInsn(getMethodName() + getDescription());

    ByteCodeUtil.createParametersToArrayByteCode(mv, params);
    mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "logicalInvoke",
                       "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");

    ByteCodeUtil.pushThis(mv);
    mv.visitLdcInsn("setNanos(I)V");
    mv.visitInsn(ICONST_1);
    mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_0);
    mv.visitTypeInsn(NEW, "java/lang/Integer");
    mv.visitInsn(DUP);
    ByteCodeUtil.pushThis(mv);
    mv.visitMethodInsn(INVOKESPECIAL, getOwnerSlashes(), "getNanos", "()I");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
    mv.visitInsn(AASTORE);
    mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "logicalInvoke",
                       "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");

    if (!returnType.equals(Type.VOID_TYPE)) {
      mv.visitVarInsn(returnType.getOpcode(ILOAD), params.length + 1);
    }
    mv.visitInsn(returnType.getOpcode(IRETURN));
    mv.visitMaxs(0, 0);
    mv.visitEnd();

  }

  private void addSetTimeMethodWrapper(ClassVisitor classVisitor) {
    MethodVisitor mv = classVisitor.visitMethod(getWrapperAccess(), getMethodName(), getDescription(), getSignature(),
                                                getExceptions());
    addCheckWriteAccessInstrumentedCode(mv, true);
    Label l0 = new Label();
    mv.visitLabel(l0);
    ByteCodeUtil.pushThis(mv);
    Type[] params = Type.getArgumentTypes(getDescription());
    Type returnType = Type.getReturnType(getDescription());
    for (int i = 0; i < params.length; i++) {
      mv.visitVarInsn(params[i].getOpcode(ILOAD), i + 1);
    }

    mv.visitMethodInsn(INVOKESPECIAL, getOwnerSlashes(), getNewName(), getDescription());

    addSetTimeInstrumentedCode(mv, params.length + 1);

    mv.visitInsn(returnType.getOpcode(IRETURN));
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addSetTimeInstrumentedCode(MethodVisitor mv, int variableOffset) {
    String getTimeDescription = "()J";
    Type getTimeReturnType = Type.getReturnType(getTimeDescription);
    ByteCodeUtil.pushThis(mv);
    mv.visitMethodInsn(INVOKESPECIAL, getOwnerSlashes(), "getTime", getTimeDescription);
    mv.visitVarInsn(getTimeReturnType.getOpcode(ISTORE), variableOffset);

    String setTimeDescription = "(J)V";
    Type[] setTimeParams = Type.getArgumentTypes(setTimeDescription);

    ByteCodeUtil.pushThis(mv);
    mv.visitLdcInsn(SerializationUtil.SET_TIME_SIGNATURE);

    ByteCodeUtil.createParametersToArrayByteCode(mv, setTimeParams, variableOffset);

    mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "logicalInvoke",
                       "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
  }

}
