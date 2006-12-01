/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.tomcat.transform;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class CatalinaAdapter extends ClassAdapter implements Opcodes {

  public CatalinaAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if ("start".equals(name) && "()V".equals(desc)) { return new StartAdapter(mv); }

    return mv;
  }

  private static class StartAdapter extends MethodAdapter implements Opcodes {

    public StartAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitInsn(int opcode) {
      if (opcode == RETURN) {
        mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper", "shutdown", "()V");
      }
      super.visitInsn(opcode);
    }

  }

}
