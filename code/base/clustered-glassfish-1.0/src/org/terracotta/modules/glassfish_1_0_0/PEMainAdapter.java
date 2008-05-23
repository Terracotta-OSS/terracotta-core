/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.glassfish_1_0_0;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

import java.lang.reflect.Modifier;

public class PEMainAdapter extends ClassAdapter implements ClassAdapterFactory {

  public PEMainAdapter(ClassVisitor cv) {
    super(cv);
  }

  public PEMainAdapter() {
    super(null);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new PEMainAdapter(visitor);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("main".equals(name) && Modifier.isStatic(access) && "([Ljava/lang/String;)V".equals(desc)) {
      mv = new MainAdapter(mv);
    }
    return mv;
  }

  private static class MainAdapter extends MethodAdapter implements Opcodes {

    public MainAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitCode() {
      super.visitCode();

      Label l0 = new Label();
      Label l1 = new Label();
      Label l2 = new Label();
      mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
      mv.visitLabel(l0);
      mv.visitLdcInsn("com.sun.enterprise.server.PEMain");
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;");
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/SessionsHelper", "injectClasses",
                         "(Ljava/lang/ClassLoader;)V");
      mv.visitLabel(l1);
      Label l3 = new Label();
      mv.visitJumpInsn(GOTO, l3);
      mv.visitLabel(l2);
      mv.visitVarInsn(ASTORE, 0);
      mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
      mv.visitInsn(DUP);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V");
      mv.visitInsn(ATHROW);
      mv.visitLabel(l3);
    }

  }

}
