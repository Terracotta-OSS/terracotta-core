/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;


import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Type;
import com.tc.object.logging.InstrumentationLogger;

/**
 * TODO Apr 19, 2005: I, steve, am too lazy to write a single sentence describing what this class is for.
 */
public class DistributedMethodCallAdapter implements MethodAdapter {
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
    MethodVisitor codeVisitor = classVisitor.visitMethod(access, methodName, description, signature, exceptions);
    addDistributedCall(codeVisitor, methodName, description);
    return codeVisitor;
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

  public void initialize(ManagerHelper aManagerHelper, int anAccess, String aClassName, String aMethodName, String aOriginalMethodName,
                         String aDescription, String sig, String[] anExceptions, InstrumentationLogger logger) {
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
