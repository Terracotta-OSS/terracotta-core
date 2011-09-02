/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.weaver;

import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;

import com.tc.aspectwerkz.transform.TransformationConstants;

/**
 * A visitor that keeps track of NEW and INVOKESPECIAL when within a constructor
 * to flag when the object initialization has been reached (after this/super call).
 * <p/>
 * No regular weaving should occur before it since this(XXJP.invoke(this)) is not allowed Java code
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class AfterObjectInitializationCodeAdapter extends MethodAdapter implements TransformationConstants {

  private String m_callerMemberName;
  private int m_newCount = 0;
  private int m_invokeSpecialCount = 0;
  protected boolean m_isObjectInitialized = false;

  public AfterObjectInitializationCodeAdapter(MethodVisitor cv, String callerMemberName) {
    super(cv);
    m_callerMemberName = callerMemberName;
    // object initialization matters within constructors only
    if (!m_callerMemberName.equals(INIT_METHOD_NAME)) {
      m_isObjectInitialized = true;
    }
  }

  public void visitTypeInsn(int opcode, String desc) {
    if (opcode == NEW) {
      m_newCount++;
    }
    super.visitTypeInsn(opcode, desc);
  }

  protected boolean queryCurrentMethodInsn(final int opcode,
                                           final String calleeClassName,
                                           final String calleeMethodName,
                                           final String calleeMethodDesc) {
    int localInvokeSpecialCount = m_invokeSpecialCount;
    int localNewCount = m_newCount;

    if (opcode == INVOKESPECIAL) {
      localInvokeSpecialCount++;
    }

    if (m_callerMemberName.equals(INIT_METHOD_NAME)) {
      // in ctor
      // make sure we are after object initialization ie after
      // the INVOKESPECIAL for this(..) / super(..)
      // that is we have seen an INVOKESPECIAL while newCount == 0
      // or while newCount == invokeSpecialCount - 1
      // [ ie same with numberOfInvokeSpecialCount = 1 ]
      if (opcode == INVOKESPECIAL) {
        if (localNewCount == localInvokeSpecialCount - 1) {
          return true;
        }
      }
      return false;
    }
    return false;
  }

  public void visitMethodInsn(final int opcode,
                              final String calleeClassName,
                              final String calleeMethodName,
                              final String calleeMethodDesc) {
    if (opcode == INVOKESPECIAL) {
      m_invokeSpecialCount++;
    }

    if (m_callerMemberName.equals(INIT_METHOD_NAME)) {
      // in ctor
      // make sure we are after object initialization ie after
      // the INVOKESPECIAL for this(..) / super(..)
      // that is we have seen an INVOKESPECIAL while newCount == 0
      // or while newCount == invokeSpecialCount - 1
      // [ ie same with numberOfInvokeSpecialCount = 1 ]
      if (opcode == INVOKESPECIAL) {
        if (m_newCount == m_invokeSpecialCount - 1) {
          m_isObjectInitialized = true;
        }
      }
    }
    super.visitMethodInsn(opcode, calleeClassName, calleeMethodName, calleeMethodDesc);
  }
}
