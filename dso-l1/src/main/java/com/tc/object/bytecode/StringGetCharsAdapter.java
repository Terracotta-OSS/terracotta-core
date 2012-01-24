/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class StringGetCharsAdapter extends ClassAdapter {

  private final String[] includePatterns;

  public StringGetCharsAdapter(ClassVisitor cv, String[] includePatterns) {
    super(cv);
    this.includePatterns = includePatterns;
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    for (int i = 0; i < includePatterns.length; i++) {
      if (name.matches(includePatterns[i])) { return new RewriteStringGetChars(mv); }
    }

    return mv;
  }

  private static class RewriteStringGetChars extends MethodAdapter implements Opcodes {

    public RewriteStringGetChars(MethodVisitor mv) {
      super(mv);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if ((INVOKEVIRTUAL == opcode)
          && ("java/lang/String".equals(owner) && "getChars".equals(name) && ("(II[CI)V".equals(desc) || "([CI)V"
              .equals(desc)))) {
        super.visitMethodInsn(opcode, owner, "getCharsFast", desc);
      } else {
        super.visitMethodInsn(opcode, owner, name, desc);
      }
    }
  }

}
