/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.object.logging.InstrumentationLogger;

public class DistributedMethodCallAdapter implements MethodAdapter, Opcodes {
  private ManagerHelper         managerHelper;
  private int                   access;
  private String                className;
  private String                methodName;
  private String                description;
  private String[]              exceptions;
  private String                signature;
  private InstrumentationLogger instrumentationLogger;

  public DistributedMethodCallAdapter() {
    super();
  }

  public MethodVisitor adapt(ClassVisitor classVisitor) {
    final String newMethodName = "__tc_dmi_" + methodName;
    MethodVisitor codeVisitor = classVisitor.visitMethod(access, newMethodName, description, signature, exceptions);
    addDmiMethodWrapper(classVisitor, newMethodName);
    // addDistributedCall(codeVisitor, newMethodName, description);
    return codeVisitor;
  }

  private void addDmiMethodWrapper(ClassVisitor classVisitor, String newMethodName) {
    MethodVisitor mv = classVisitor.visitMethod(access, methodName, description, signature, exceptions);
    final int boolPos = ByteCodeUtil.getFirstLocalVariableOffset(access, description);
    final int exceptionPos = boolPos + 1;
    final int pcPos = exceptionPos + 1;
    final int rvPos = pcPos + 1;

    mv.visitCode();
    Label l0 = new Label();
    Label l1 = new Label();
    Label l2 = new Label();
    mv.visitTryCatchBlock(l0, l1, l2, null);

    addDistributedCall(mv, methodName, description);
    mv.visitVarInsn(ISTORE, boolPos);
    mv.visitLabel(l0);

    // call the renamed method and store it's return value
    mv.visitVarInsn(ALOAD, 0);
    ByteCodeUtil.pushMethodArguments(access, description, mv);
    mv.visitMethodInsn(INVOKEVIRTUAL, className.replace('.', '/'), newMethodName, description);
    final Type rvType = Type.getReturnType(description);
    final boolean returnVoid = rvType == Type.VOID_TYPE;
    if (!returnVoid) mv.visitVarInsn(rvType.getOpcode(ISTORE), rvPos);

    Label l4 = new Label();
    mv.visitJumpInsn(JSR, l4);
    mv.visitLabel(l1);
    if (!returnVoid) mv.visitVarInsn(rvType.getOpcode(ILOAD), rvPos);
    mv.visitInsn(rvType.getOpcode(IRETURN));

    mv.visitLabel(l2);
    mv.visitVarInsn(ASTORE, exceptionPos);
    mv.visitJumpInsn(JSR, l4);

    mv.visitVarInsn(ALOAD, exceptionPos);
    mv.visitInsn(ATHROW);
    mv.visitLabel(l4);
    mv.visitVarInsn(ASTORE, pcPos);

    mv.visitVarInsn(ILOAD, boolPos);
    Label l7 = new Label();
    mv.visitJumpInsn(IFEQ, l7);
    managerHelper.callManagerMethod("distributedMethodCallCommit", mv);
    mv.visitLabel(l7);
    mv.visitVarInsn(RET, pcPos);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addDistributedCall(MethodVisitor mv, String name, String desc) {
    if (instrumentationLogger.distMethodCallInsertion()) {
      instrumentationLogger.distMethodCallInserted(className, name, desc);
    }

    ByteCodeUtil.pushThis(mv);
    mv.visitLdcInsn(name + desc);
    ByteCodeUtil.createParametersToArrayByteCode(mv, Type.getArgumentTypes(desc));
    managerHelper.callManagerMethod("distributedMethodCall", mv);
  }

  public boolean doesOriginalNeedAdapting() {
    return true;
  }

  public void initialize(ManagerHelper aManagerHelper, int anAccess, String aClassName, String aMethodName,
                         String aOriginalMethodName, String aDescription, String sig, String[] anExceptions,
                         InstrumentationLogger logger) {
    this.managerHelper = aManagerHelper;
    this.access = anAccess;
    this.className = aClassName;
    this.methodName = aMethodName;
    this.description = aDescription;
    this.exceptions = anExceptions;
    this.instrumentationLogger = logger;
    this.signature = sig;
  }

}

