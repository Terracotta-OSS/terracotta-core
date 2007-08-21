/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.weblogic.transform;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class FilterManagerAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {

  public FilterManagerAdapter() {
    super(null);
  }

  private FilterManagerAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new FilterManagerAdapter(visitor, loader);
  }

  public void visitEnd() {
    addGetContextMethod();
    super.visitEnd();
  }

  // see the dummy FilterManager class in dso-weblogic-stubs for this method post-asm
  private void addGetContextMethod() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_getContext",
                                      "()Lweblogic/servlet/internal/WebAppServletContext;", null, null);
    
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(8, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "weblogic/servlet/internal/FilterManager", "context", "Lweblogic/servlet/internal/WebAppServletContext;");
    mv.visitInsn(ARETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lweblogic/servlet/internal/FilterManager;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

}
