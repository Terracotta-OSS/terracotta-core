/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.MethodAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class JavaUtilConcurrentLinkedBlockingQueueClassAdapter extends ClassAdapter implements Opcodes {
  private static final String GET_ITEM_METHOD_NAME   = "getItem";
  private static final String GET_ITEM_METHOD_DESC   = "()Ljava/lang/Object;";

  public JavaUtilConcurrentLinkedBlockingQueueClassAdapter(ClassVisitor cv) {
    super(cv);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if (mv == null) {
      return null;
    } else {
      return new NodeMethodAdapter(mv);
    }
  }
  
  static class NodeMethodAdapter extends MethodAdapter implements Opcodes {
    public NodeMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      if (GETFIELD == opcode && "java/util/concurrent/LinkedBlockingQueue$Node".equals(owner) && "item".equals(name)
          && "Ljava/lang/Object;".equals(desc)) {
        mv.visitMethodInsn(INVOKEVIRTUAL, owner, GET_ITEM_METHOD_NAME, GET_ITEM_METHOD_DESC);
      } else {
        super.visitFieldInsn(opcode, owner, name, desc);
      }
    }
  }
}
