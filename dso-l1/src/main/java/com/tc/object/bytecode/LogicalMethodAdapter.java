/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.object.config.MethodSpec;
import com.tc.object.logging.InstrumentationLogger;

import java.lang.reflect.Modifier;

/**
 * Used to create wrappers for logical methods
 */
public class LogicalMethodAdapter implements MethodAdapter, Opcodes {
  private String   ownerSlashes;
  private String   methodName;
  private String   originalMethodName;
  private String   description;
  private int      access;
  private String[] exceptions;
  private String   signature;
  private int      instrumentationType;
  private int      wrapperAccess;

  public LogicalMethodAdapter() {
    // When using this as a Method creator it doesn't need any of that stuff. yuck :-)
  }

  public LogicalMethodAdapter(String methodName, int instrumentationType) {
    this.methodName = methodName;
    this.instrumentationType = instrumentationType;
  }

  public void initialize(int anAccess, String aClassName, String aMethodName, String aOriginalMethodName,
                         String aDescription, String sig, String[] anExceptions, InstrumentationLogger logger,
                         MemberInfo info) {
    this.ownerSlashes = aClassName.replace('.', '/');
    this.methodName = aMethodName;
    this.originalMethodName = aOriginalMethodName;
    this.description = aDescription;
    this.wrapperAccess = anAccess & (~Modifier.SYNCHRONIZED); // wrapper method should have synch removed
    this.access = anAccess;
    this.exceptions = anExceptions;
    this.signature = sig;
  }

  public MethodVisitor adapt(ClassVisitor classVisitor) {
    createWrapperMethod(classVisitor);
    return classVisitor.visitMethod(access, getNewName(), description, signature, exceptions);
  }

  public boolean doesOriginalNeedAdapting() {
    return true;
  }

  protected String getNewName() {
    return ByteCodeUtil.TC_METHOD_PREFIX + methodName;
  }

  protected void createWrapperMethod(ClassVisitor classVisitor) {
    switch (instrumentationType) {
      case MethodSpec.ALWAYS_LOG:
        createAlwaysLogWrapperMethod(classVisitor, true);
        break;
      default:
        throw new AssertionError("illegal instrumentationType:" + instrumentationType);
    }

  }

  private void createAlwaysLogWrapperMethod(ClassVisitor classVisitor, boolean checkWriteAccessRequired) {
    MethodVisitor mv = classVisitor.visitMethod(wrapperAccess, methodName, description, signature, exceptions);
    if (checkWriteAccessRequired) {
      addCheckWriteAccessInstrumentedCode(mv, true);
    }
    Label l0 = new Label();
    mv.visitLabel(l0);
    ByteCodeUtil.pushThis(mv);
    Type[] params = Type.getArgumentTypes(description);
    Type returnType = Type.getReturnType(description);
    for (int i = 0; i < params.length; i++) {
      mv.visitVarInsn(params[i].getOpcode(ILOAD), i + 1);
    }

    mv.visitMethodInsn(INVOKESPECIAL, ownerSlashes, getNewName(), description);
    if (!returnType.equals(Type.VOID_TYPE)) {
      mv.visitVarInsn(returnType.getOpcode(ISTORE), params.length + 1);
    }
    ByteCodeUtil.pushThis(mv);
    mv.visitLdcInsn(originalMethodName + description);

    ByteCodeUtil.createParametersToArrayByteCode(mv, params);
    mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "logicalInvoke",
                       "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");

    if (!returnType.equals(Type.VOID_TYPE)) {
      mv.visitVarInsn(returnType.getOpcode(ILOAD), params.length + 1);
    }
    mv.visitInsn(returnType.getOpcode(IRETURN));
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  protected void addCheckWriteAccessInstrumentedCode(MethodVisitor mv, boolean checkManaged) {
    Label notManaged = new Label();

    if (checkManaged) {
      ByteCodeUtil.pushThis(mv);
      mv.visitMethodInsn(INVOKEVIRTUAL, getOwnerSlashes(), ClassAdapterBase.MANAGED_METHOD,
                         "()Lcom/tc/object/TCObject;");
      mv.visitJumpInsn(IFNULL, notManaged);
    }
    ByteCodeUtil.pushThis(mv);
    mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "checkWriteAccess", "(Ljava/lang/Object;)V");
    mv.visitLabel(notManaged);
  }

  protected int getInstrumentationType() {
    return instrumentationType;
  }

  protected int getAccess() {
    return access;
  }

  protected String getDescription() {
    return description;
  }

  protected String[] getExceptions() {
    return exceptions;
  }

  protected String getMethodName() {
    return methodName;
  }

  protected String getOwnerSlashes() {
    return ownerSlashes;
  }

  protected String getSignature() {
    return signature;
  }

  protected int getWrapperAccess() {
    return wrapperAccess;
  }

  protected String getOriginalMethodName() {
    return originalMethodName;
  }
}
