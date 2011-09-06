/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class VectorAdapter {

  public static class ElementsAdapter extends AbstractMethodAdapter {
    public MethodVisitor adapt(ClassVisitor classVisitor) {
      MethodVisitor mv = visitOriginal(classVisitor);
      return new Adapter(mv);
    }

    public boolean doesOriginalNeedAdapting() {
      return false;
    }

    private static class Adapter extends MethodAdapter implements Opcodes {

      public Adapter(MethodVisitor mv) {
        super(mv);
        mv.visitTypeInsn(NEW, "com/tc/util/EnumerationWrapper");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
      }

      public void visitInsn(int opcode) {
        if (ARETURN == opcode) {
          mv.visitMethodInsn(INVOKESPECIAL, "com/tc/util/EnumerationWrapper", "<init>", "(Ljava/util/Vector;Ljava/util/Enumeration;)V");
        }
        super.visitInsn(opcode);
      }
    }
  }
}
