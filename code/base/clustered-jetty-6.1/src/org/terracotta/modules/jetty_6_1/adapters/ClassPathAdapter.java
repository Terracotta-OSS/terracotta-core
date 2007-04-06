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

public class ClassPathAdapter extends ClassAdapter implements ClassAdapterFactory {

  public ClassPathAdapter(ClassVisitor cv) {
    super(cv);
  }

  public ClassPathAdapter() {
    super(null);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new ClassPathAdapter(visitor);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if ("getClassLoader".equals(name) & "()Ljava/lang/ClassLoader;".equals(desc)) {
      mv = new GetClassLoaderAdapter(mv);
    }

    return mv;
  }

  private static class GetClassLoaderAdapter extends MethodAdapter implements Opcodes {

    public GetClassLoaderAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitInsn(int opcode) {
      if (opcode == ARETURN) {
        super.visitInsn(DUP);
        super.visitMethodInsn(INVOKESTATIC, "com/tc/jetty/JettyLoaderNaming", "nameAndRegisterClasspathLoader",
                              "(Ljava/lang/ClassLoader;)V");
      }

      super.visitInsn(opcode);
    }
  }

}
