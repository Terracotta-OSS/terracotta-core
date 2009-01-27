/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;


import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.object.logging.InstrumentationLogger;

/**
 * Interface for something that can modify methods
 */
public interface MethodAdapter {

  /**
   * Initialize the method adapter with resources
   * @param access Access modifier for method
   * @param owner Owner class name
   * @param methodName Modified method name
   * @param originalMethodName Original method name
   * @param description Method description (params and return type)
   * @param sig The method's signature. May be null if the
   *        method parameters, return type and exceptions do not use generic
   *        types.
   * @param exceptions Exceptions thrown by the method
   * @param instrumentationLogger The logger
   * @param memberInfo Member info
   */
  public void initialize(int access, String owner, String methodName,
                         String originalMethodName, String description, String sig, String[] exceptions,
                         InstrumentationLogger instrumentationLogger, MemberInfo memberInfo);

  /**
   * Create a method visitor from the class visitor
   * @param classVisitor Modifies classes
   * @return Method visitor that can modify methods
   */
  public MethodVisitor adapt(ClassVisitor classVisitor);

  /**
   * Checks whether original needs adapting
   * @return True to adapt
   */
  public boolean doesOriginalNeedAdapting();


}
