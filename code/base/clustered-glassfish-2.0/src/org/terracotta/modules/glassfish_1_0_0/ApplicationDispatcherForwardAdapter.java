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

public class ApplicationDispatcherForwardAdapter extends ClassAdapter implements ClassAdapterFactory, Opcodes {

  public ApplicationDispatcherForwardAdapter(ClassVisitor cv) {
    super(cv);
  }

  public ApplicationDispatcherForwardAdapter() {
    super(null);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new ApplicationDispatcherForwardAdapter(visitor);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    // for glassfish-v1 [ START [
    if ("getStatus".equals(name) && "(Ljavax/servlet/ServletResponse;)I".equals(desc)) {
      mv = new GetStatusAdapter(mv, access, name, desc);
    }
    if ("getMessage".equals(name) && "(Ljavax/servlet/ServletResponse;)Ljava/lang/String;".equals(desc)) {
      mv = new GetMessageAdapter(mv, access, name, desc);
    }
    if ("resetResponse".equals(name) && "(Ljavax/servlet/ServletResponse;)V".equals(desc)) {
      mv = new ResetResponseAdapter(mv, access, name, desc);
    }
    // ] END] for glassfish-v1

    // for glassfish-v2 [ START [
    if ("getResponseFacade".equals(name) && "(Ljavax/servlet/ServletResponse;)Lorg/apache/coyote/tomcat5/CoyoteResponseFacade;".equals(desc)) {
      mv = new GetResponseFacadeAdapter(mv, access, name, desc);
    }
    // ] END ] for glassfish-v2

    return mv;
  }

  private static class SessionResponseAdviceAdapter extends AdviceAdapter implements Opcodes {
    private SessionResponseAdviceAdapter(final MethodVisitor mv, final int access, final String name, final String desc) {
      super(mv, access, name, desc);
    }

    protected int tcUnwrapAndStoreInLocal() {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/tomcat50/session/SessionResponse50", "tcUnwrap",
        "(Ljavax/servlet/ServletResponse;)Lorg/apache/coyote/tomcat5/CoyoteResponse;");

      int slot = newLocal(Type.getType("Lorg/apache/coyote/tomcat5/CoyoteResponse;"));
      mv.visitVarInsn(ASTORE, slot);
      return slot;
    }
  }

  private static class GetStatusAdapter extends SessionResponseAdviceAdapter {

    private GetStatusAdapter(final MethodVisitor mv, final int access, final String name, final String desc) {
      super(mv, access, name, desc);
    }

    protected void onMethodEnter() {
      int slot = tcUnwrapAndStoreInLocal();
      mv.visitVarInsn(ALOAD, slot);
      Label l2 = new Label();
      mv.visitJumpInsn(IFNULL, l2);
      mv.visitVarInsn(ALOAD, slot);
      mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/coyote/tomcat5/CoyoteResponse", "getStatus", "()I");
      mv.visitInsn(IRETURN);
      mv.visitLabel(l2);
    }
  }

  private static class GetMessageAdapter extends SessionResponseAdviceAdapter {

    private GetMessageAdapter(final MethodVisitor mv, final int access, final String name, final String desc) {
      super(mv, access, name, desc);
    }

    protected void onMethodEnter() {
      int slot = tcUnwrapAndStoreInLocal();
      mv.visitVarInsn(ALOAD, slot);
      Label l2 = new Label();
      mv.visitJumpInsn(IFNULL, l2);
      mv.visitVarInsn(ALOAD, slot);
      mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/coyote/tomcat5/CoyoteResponse", "getMessage", "()Ljava/lang/String;");
      mv.visitInsn(ARETURN);
      mv.visitLabel(l2);
    }
  }

  private static class ResetResponseAdapter extends SessionResponseAdviceAdapter {

    private ResetResponseAdapter(final MethodVisitor mv, final int access, final String name, final String desc) {
      super(mv, access, name, desc);
    }

    protected void onMethodEnter() {
      int slot = tcUnwrapAndStoreInLocal();
      mv.visitVarInsn(ALOAD, slot);
      Label l2 = new Label();
      mv.visitJumpInsn(IFNULL, l2);
      mv.visitVarInsn(ALOAD, slot);
      mv.visitInsn(ICONST_0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/coyote/tomcat5/CoyoteResponse", "setSuspended", "(Z)V");
      mv.visitVarInsn(ALOAD, slot);
      mv.visitInsn(ICONST_0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/coyote/tomcat5/CoyoteResponse", "setAppCommitted", "(Z)V");
      mv.visitInsn(RETURN);
      mv.visitLabel(l2);
    }
  }


  private class GetResponseFacadeAdapter extends SessionResponseAdviceAdapter {
    public GetResponseFacadeAdapter(final MethodVisitor mv, final int access, final String name, final String desc) {
      super(mv, access, name, desc);
    }

    protected void onMethodEnter() {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/tomcat50/session/SessionResponse50", "tcUnwrapCoyoteResponseFacade",
        "(Ljavax/servlet/ServletResponse;)Lorg/apache/coyote/tomcat5/CoyoteResponseFacade;");

      int slot = newLocal(Type.getType("Lorg/apache/coyote/tomcat5/CoyoteResponseFacade;"));
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