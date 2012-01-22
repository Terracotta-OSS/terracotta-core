/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.weaver;

import java.util.Set;

import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

import com.tc.aspectwerkz.transform.TransformationConstants;
import com.tc.aspectwerkz.transform.inlining.AsmNullAdapter;

/**
 * A read only visitor to gather wrapper methods and proxy methods
 * Makes use of the NullVisitors
 *
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur</a>
 */
public class AlreadyAddedMethodAdapter extends AsmNullAdapter.NullClassAdapter implements Opcodes, TransformationConstants {

  /**
   * Set of "<methodName><methodDesc>" strings populated with wrapper methods, prefixed originals
   * and ctor body wrappers to allow multiweaving support.
   */
  private final Set m_addedMethods;

  /**
   * Creates a new class adapter.
   *
   * @param wrappers
   */
  public AlreadyAddedMethodAdapter(final Set wrappers) {
    m_addedMethods = wrappers;
  }

  /**
   * Visits the methods.
   *
   * @param access
   * @param name
   * @param desc
   * @param signature
   * @param exceptions
   * @return
   */
  public MethodVisitor visitMethod(final int access,
                                   final String name,
                                   final String desc,
                                   final String signature,
                                   final String[] exceptions) {
    if (name.startsWith(WRAPPER_METHOD_PREFIX)
            || name.startsWith(ORIGINAL_METHOD_PREFIX)) {
      m_addedMethods.add(getMethodKey(name, desc));
    }
    return super.visitMethod(access, name, desc, signature, exceptions);
  }

  static String getMethodKey(String name, String desc) {
    StringBuffer sb = new StringBuffer(name);
    return sb.append(desc).toString();
  }
}
