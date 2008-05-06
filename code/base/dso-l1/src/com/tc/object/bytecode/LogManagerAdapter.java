/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class LogManagerAdapter extends ClassAdapter {

  public LogManagerAdapter(ClassVisitor cv) {
    super(cv);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if ("<clinit>".equals(name)) {
      mv = new StaticInitAdapter(mv);
    }

    return mv;
  }

  private static class StaticInitAdapter extends MethodAdapter implements Opcodes {

    public StaticInitAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitInsn(int opcode) {
      if (RETURN == opcode) {
        super.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper", "loggingInitialized",                              "()V");
      }

      super.visitInsn(opcode);
    }
  }

}
