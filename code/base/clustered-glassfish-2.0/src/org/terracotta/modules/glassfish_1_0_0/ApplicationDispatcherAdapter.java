/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.glassfish_1_0_0;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.asm.commons.AdviceAdapter;
import com.tc.object.bytecode.ClassAdapterFactory;

public class ApplicationDispatcherAdapter extends ClassAdapter implements ClassAdapterFactory, Opcodes {

  public ApplicationDispatcherAdapter(ClassVisitor cv) {
    super(cv);
  }

  public ApplicationDispatcherAdapter() {
    super(null);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new ApplicationDispatcherAdapter(visitor);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("getCoyoteRequest".equals(name) && "(Ljavax/servlet/ServletRequest;)Lorg/apache/coyote/tomcat5/CoyoteRequest;".equals(desc)) {
      mv = new GetCoyoteRequestAdapter(mv, access, name, desc);
    }
    return mv;
  }

  private static class GetCoyoteRequestAdapter extends AdviceAdapter implements Opcodes {

    private GetCoyoteRequestAdapter(final MethodVisitor mv, final int access, final String name, final String desc) {
      super(mv, access, name, desc);
    }

    protected void onMethodEnter() {
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/tomcat50/session/SessionRequest50", "tcUnwrap",
        "(Ljavax/servlet/ServletRequest;)Lorg/apache/coyote/tomcat5/CoyoteRequest;");

      int slot = newLocal(Type.getType("Lorg/apache/coyote/tomcat5/CoyoteRequest;"));
      mv.visitVarInsn(ASTORE, slot);
      mv.visitVarInsn(ALOAD, slot);
      Label l2 = new Label();
      mv.visitJumpInsn(IFNULL, l2);
      mv.visitVarInsn(ALOAD, slot);
      mv.visitInsn(ARETURN);
      mv.visitLabel(l2);
    }
  }
}