/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.geronimo.transform;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ByteCodeUtil;

public class ProxyMethodInterceptorAdapter extends ClassAdapter {

  public ProxyMethodInterceptorAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    // public final Object intercept(Object object, Method method, Object args[], MethodProxy proxy)

    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if ("intercept".equals(name)) { return new InterceptAdapter(mv); }

    return mv;

  }

  private static class InterceptAdapter extends MethodAdapter implements Opcodes {

    public InterceptAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitCode() {
      super.visitCode();

      // For DSO introduced methods -- no interceptor exists, just call super in this
      mv.visitVarInsn(ALOAD, 2);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "getName", "()Ljava/lang/String;");
      mv.visitLdcInsn(ByteCodeUtil.TC_METHOD_PREFIX);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z");
      Label notTC = new Label();
      mv.visitJumpInsn(IFEQ, notTC);
      mv.visitVarInsn(ALOAD, 4);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitVarInsn(ALOAD, 3);
      mv.visitMethodInsn(INVOKEVIRTUAL, "net/sf/cglib/proxy/MethodProxy", "invokeSuper",
                         "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
      mv.visitInsn(ARETURN);
      mv.visitLabel(notTC);
    }

  }

}
