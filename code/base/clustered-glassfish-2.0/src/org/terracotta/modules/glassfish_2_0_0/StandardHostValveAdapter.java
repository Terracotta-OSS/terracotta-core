/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.glassfish_2_0_0;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class StandardHostValveAdapter extends ClassAdapter implements ClassAdapterFactory {

  public StandardHostValveAdapter(ClassVisitor cv) {
    super(cv);
  }

  public StandardHostValveAdapter() {
    super(null);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new StandardHostValveAdapter(visitor);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("postInvoke".equals(name)) {
      mv = new PostInvokeAdapter(mv);
    }
    return mv;
  }

  private static class PostInvokeAdapter extends MethodAdapter implements Opcodes {

    public PostInvokeAdapter(MethodVisitor mv) {
      super(mv);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if (INVOKEINTERFACE == opcode && "javax/servlet/ServletResponse".equals(owner) && "isCommitted".equals(name)
          && "()Z".equals(desc)) {
        super.visitMethodInsn(INVOKESTATIC, ResponseCommittedHelper.CLASS, name, "(Ljavax/servlet/ServletResponse;)Z");
      } else {
        super.visitMethodInsn(opcode, owner, name, desc);
      }
    }

  }

}
