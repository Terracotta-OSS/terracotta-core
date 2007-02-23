/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode.struts;


import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;

/**
 * This is a work-around for a problem with the <bean:include> tag in struts. Basically the tag can create a new
 * response back to the same web application *and* it passes along the session cookie. Given the way we do locking on
 * the session object, this creates a deadlock. The solution implemented here is to release the session lock before
 * processing the tag
 */
public class IncludeTagAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {

  public IncludeTagAdapter() {
    super(null);
  }
  
  private IncludeTagAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new IncludeTagAdapter(visitor, loader);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    if ("doStartTag".equals(name) && Type.getArgumentTypes(desc).length == 0) {
      // rename existing method and make it private for good measure
      return super.visitMethod(ACC_PRIVATE, ByteCodeUtil.METHOD_RENAME_PREFIX + name, desc, signature, exceptions);
    }

    return super.visitMethod(access, name, desc, signature, exceptions);
  }

  public void visitEnd() {
    addSessionLockingSupport();
    super.visitEnd();
  }

  private void addSessionLockingSupport() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "doStartTag", "()I", null,
                                         new String[] { "javax/servlet/jsp/JspException" });
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "org/apache/struts/taglib/bean/IncludeTag", "pageContext",
                      "Ljavax/servlet/jsp/PageContext;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "javax/servlet/jsp/PageContext", "getSession",
                       "()Ljavax/servlet/http/HttpSession;");
    mv.visitVarInsn(ASTORE, 1);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitTypeInsn(INSTANCEOF, "com/tc/session/SessionSupport");
    Label l2 = new Label();
    mv.visitJumpInsn(IFEQ, l2);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitTypeInsn(CHECKCAST, "com/tc/session/SessionSupport");
    mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/session/SessionSupport", "pauseRequest", "()V");
    mv.visitLabel(l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "org/apache/struts/taglib/bean/IncludeTag", ByteCodeUtil.METHOD_RENAME_PREFIX
                                                                                  + "doStartTag", "()I");
    mv.visitVarInsn(ISTORE, 4);
    Label l4 = new Label();
    mv.visitJumpInsn(JSR, l4);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitVarInsn(ILOAD, 4);
    mv.visitInsn(IRETURN);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitVarInsn(ASTORE, 3);
    mv.visitJumpInsn(JSR, l4);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitInsn(ATHROW);
    mv.visitLabel(l4);
    mv.visitVarInsn(ASTORE, 2);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitTypeInsn(INSTANCEOF, "com/tc/session/SessionSupport");
    Label l9 = new Label();
    mv.visitJumpInsn(IFEQ, l9);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitTypeInsn(CHECKCAST, "com/tc/session/SessionSupport");
    mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/session/SessionSupport", "resumeRequest", "()V");
    mv.visitLabel(l9);
    mv.visitVarInsn(RET, 2);
    mv.visitTryCatchBlock(l2, l5, l6, null);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

}
