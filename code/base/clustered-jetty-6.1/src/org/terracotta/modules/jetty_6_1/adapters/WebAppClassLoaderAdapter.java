/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.jetty_6_1.adapters;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class WebAppClassLoaderAdapter extends ClassAdapter implements ClassAdapterFactory {

  public WebAppClassLoaderAdapter(ClassVisitor cv) {
    super(cv);
  }

  public WebAppClassLoaderAdapter() {
    super(null);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new WebAppClassLoaderAdapter(visitor);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("<init>".equals(name)) {
      mv = new InitAdapter(mv);
    }

    return mv;
  }

  private static class InitAdapter extends MethodAdapter implements Opcodes {

    public InitAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitInsn(int opcode) {
      if (opcode == RETURN) {
        super.visitVarInsn(ALOAD, 0);
        super.visitMethodInsn(INVOKESTATIC, "com/tc/jetty/JettyLoaderNaming", "nameAndRegisterWebAppLoader",
                              "(Ljava/lang/ClassLoader;)V");
      }

      super.visitInsn(opcode);
    }
  }

}
