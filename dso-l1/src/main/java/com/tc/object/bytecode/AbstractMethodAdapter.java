/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.object.logging.InstrumentationLogger;

public abstract class AbstractMethodAdapter implements MethodAdapter, Opcodes {

  protected int                   access;
  protected String                methodName;
  protected String                originalMethodName;
  protected String                signature;
  protected String                description;
  protected String[]              exceptions;
  protected InstrumentationLogger instrumentationLogger;
  protected String                ownerDots;
  protected MemberInfo            memberInfo;

  protected MethodVisitor visitOriginal(ClassVisitor classVisitor) {
    return classVisitor.visitMethod(access, methodName, description, signature, exceptions);
  }

  public abstract MethodVisitor adapt(ClassVisitor classVisitor);

  public abstract boolean doesOriginalNeedAdapting();

  public void initialize(int acc, String own, String method, String origMethodName,
                         String desc, String sig, String[] ex, InstrumentationLogger instLogger, MemberInfo info) {
    this.access = acc;
    this.ownerDots = own;
    this.methodName = method;
    this.originalMethodName = origMethodName;
    this.signature = sig;
    this.description = desc;
    this.exceptions = ex;
    this.instrumentationLogger = instLogger;
    this.memberInfo = info;
  }

}
