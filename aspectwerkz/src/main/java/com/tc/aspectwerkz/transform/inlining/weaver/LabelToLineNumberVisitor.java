/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.weaver;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.Label;

import com.tc.aspectwerkz.transform.InstrumentationContext;

/**
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class LabelToLineNumberVisitor extends ClassAdapter {

  private InstrumentationContext m_ctx;

  public LabelToLineNumberVisitor(ClassVisitor cv, InstrumentationContext ctx) {
    super(cv);
    m_ctx = ctx;
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    return new MethodAdapter(super.visitMethod(access, name, desc, signature, exceptions)) {
      public void visitLineNumber(int i, Label label) {
        super.visitLineNumber(i, label);
        m_ctx.addLineNumberInfo(label, i);
      }
    };
  }
}
