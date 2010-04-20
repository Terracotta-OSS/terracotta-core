/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.MethodAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;

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
    } else if ("enqueue".equals(name) && "(Ljava/lang/Object;)V".equals(desc)) {
      addRedirect("insert", "enqueue", access, desc, signature, exceptions);
    } else if ("dequeue".equals(name) && "()Ljava/lang/Object;".equals(desc)) {
      addRedirect("extract", "dequeue", access, desc, signature, exceptions);
    }
    return new NodeMethodAdapter(mv);
  }
  
  private void addRedirect(String from, String to, int access, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC
                                         , from, desc, signature, exceptions);
    Type[] args = Type.getArgumentTypes(desc);
    mv.visitCode();

    int index = 0;
    mv.visitVarInsn(ALOAD, index++);
    for (Type t : args) {
      mv.visitVarInsn(t.getOpcode(Opcodes.ILOAD), index);
      index += t.getSize();
    }
    
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/LinkedBlockingQueue", to, desc);

    Type ret = Type.getReturnType(desc);    
    mv.visitInsn(ret.getOpcode(IRETURN));
    
    mv.visitMaxs(0, 0);
    mv.visitEnd();    
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
