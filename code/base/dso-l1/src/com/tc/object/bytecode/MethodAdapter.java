/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;


import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.object.logging.InstrumentationLogger;

/**
 *
 */
public interface MethodAdapter {
  public MethodVisitor adapt(ClassVisitor classVisitor);

  public boolean doesOriginalNeedAdapting();

  public void initialize(ManagerHelper managerHelper, int access, String owner, String methodName,
                         String originalMethodName, String description, String sig, String[] exceptions,
                         InstrumentationLogger instrumentationLogger);

}
