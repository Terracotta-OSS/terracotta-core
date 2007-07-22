/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.lucene_2_0_0;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class RAMFileExternalAccessAdatper extends ClassAdapter implements ClassAdapterFactory {

  public RAMFileExternalAccessAdatper(ClassVisitor cv) {
    super(cv);
  }

  public RAMFileExternalAccessAdatper() {
    super(null);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new RAMFileExternalAccessAdatper(visitor);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    return new RewriteFieldAccessAdapter(super.visitMethod(access, name, desc, signature, exceptions));
  }

  private static class RewriteFieldAccessAdapter extends MethodAdapter implements Opcodes {

    private static final String RAMFile_CLASS      = "org/apache/lucene/store/RAMFile";

    private boolean             delegateVectorCall = false;

    public RewriteFieldAccessAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if ("java/util/Vector".equals(owner)) {
        if (delegateVectorCall) {
          delegateVectorCall = false;

          // RAMFile instance should still be on the stack here since we removed the field insn
          super.visitMethodInsn(INVOKEVIRTUAL, RAMFile_CLASS, RAMFileAdapter.BUFFERS_FIELD + name, desc);
          return;
        }
      }

      super.visitMethodInsn(opcode, owner, name, desc);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      if (RAMFile_CLASS.equals(owner)) {
        switch (opcode) {
          case GETFIELD: {
            if (RAMFileAdapter.BUFFERS_FIELD.equals(name)) {
              if (delegateVectorCall) { throw new AssertionError("a previous vector call not delegated"); }
              delegateVectorCall = true;
              return;
            }

            super.visitMethodInsn(INVOKEVIRTUAL, RAMFile_CLASS, "get" + name, "()" + desc);
            break;
          }
          case PUTFIELD: {
            super.visitMethodInsn(INVOKEVIRTUAL, RAMFile_CLASS, "set" + name, "(" + desc + ")V");
            break;
          }
          default: {
            throw new AssertionError("unexpected field op: " + opcode + ", " + owner + ", " + name + ", " + desc);
          }
        }
      } else {
        super.visitFieldInsn(opcode, owner, name, desc);
      }
    }

    public void visitEnd() {
      // This assertion will be wrong if there is code that simply reads the vector field from RAMFile,
      // but calls no method on it
      if (delegateVectorCall) { throw new AssertionError("a remaining vector call not delegated"); }
      super.visitEnd();
    }

  }

}
