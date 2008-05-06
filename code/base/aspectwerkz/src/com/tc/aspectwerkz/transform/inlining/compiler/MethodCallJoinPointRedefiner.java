/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.compiler;

import com.tc.asm.MethodVisitor;
import com.tc.asm.Type;

import com.tc.aspectwerkz.transform.inlining.AsmHelper;

/**
 * Redefines the existing join point class and turns it into a delegation class delegating to the newly created
 * replacement join point class.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public class MethodCallJoinPointRedefiner extends MethodCallJoinPointCompiler {
  /**
   * The redefined model.
   */
  private final CompilationInfo.Model m_redefinedModel;

  /**
   * Creates a new join point compiler instance.
   *
   * @param model
   */
  MethodCallJoinPointRedefiner(final CompilationInfo model) {
    super(model.getInitialModel());
    m_redefinedModel = model.getRedefinedModel();
  }

  /**
   * Creates the 'invoke' method.
   */
  protected void createInvokeMethod() {
    String invokeDesc = buildInvokeMethodSignature();
    MethodVisitor cv = m_cw.visitMethod(
            ACC_PUBLIC + ACC_FINAL + ACC_STATIC,
            INVOKE_METHOD_NAME,
            invokeDesc,
            null,
            new String[]{
                    THROWABLE_CLASS_NAME
            }
    );
    AsmHelper.loadArgumentTypes(cv, Type.getArgumentTypes(invokeDesc), true);
    cv.visitMethodInsn(INVOKESTATIC, m_redefinedModel.getJoinPointClassName(), INVOKE_METHOD_NAME, invokeDesc);
    AsmHelper.addReturnStatement(cv, Type.getReturnType(invokeDesc));
    cv.visitMaxs(0, 0);
  }

  /**
   * Creates the 'invoke' method.
   */
  protected void createInlinedInvokeMethod() {
    createInvokeMethod();
  }

}