/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;
import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.asm.commons.LocalVariablesSorter;
import com.tc.object.bytecode.JavaUtilConcurrentLinkedBlockingQueueClassAdapter.NodeMethodAdapter;

public class JavaUtilConcurrentLinkedBlockingQueueIteratorClassAdapter extends ClassAdapter implements Opcodes {

  public JavaUtilConcurrentLinkedBlockingQueueIteratorClassAdapter(ClassVisitor cv) {
    super(cv);
  }
  
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    mv = new NodeMethodAdapter(mv);
    if ("remove".equals(name) && "()V".equals(desc)) {
      mv = new RemoveMethodAdapter(access, desc, mv);
    }
    return mv;
  }
  
  // This is a hack for the remove() method of the LinkedBlockingQueue.iterator.
  private static class RemoveMethodAdapter extends LocalVariablesSorter implements Opcodes {
    private final int newLocalVar;
    private boolean incNext = false;
    private boolean incDone = false;
    private boolean logicalInvoke = false;
    
    public RemoveMethodAdapter(int access, String desc, MethodVisitor mv) {
      super(access, desc, mv);
      newLocalVar = newLocal(Type.INT_TYPE);
    }
    
    public void visitCode() {
      super.visitCode();
      
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ISTORE, newLocalVar);
    }
    
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      super.visitFieldInsn(opcode, owner, name, desc);
      if (GETFIELD == opcode  && "next".equals(name)) {
        if (!incNext) {
          incNext = true;
        } else if (!incDone) {
          mv.visitIincInsn(newLocalVar, 1);
          incDone = true;
        }
      } else if (PUTFIELD == opcode && "item".equals(name)) {
        if (!logicalInvoke) {
          addLogicalInvokeMethod();
          logicalInvoke = true;
        }
      }
    }
    
    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
      if ("unlink".equals(name)) {
        if (!logicalInvoke) {
          addLogicalInvokeMethod();
          logicalInvoke = true;
        }
      }
    }
    
    private void addLogicalInvokeMethod() {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue$Itr", "this$0", "Ljava/util/concurrent/LinkedBlockingQueue;");
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isManaged", "(Ljava/lang/Object;)Z");
      Label l21 = new Label();
      mv.visitJumpInsn(IFEQ, l21);
      Label l22 = new Label();
      mv.visitLabel(l22);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue$Itr", "this$0", "Ljava/util/concurrent/LinkedBlockingQueue;");
      mv.visitLdcInsn("remove(I)Ljava/lang/Object;");
      mv.visitInsn(ICONST_1);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitTypeInsn(NEW, "java/lang/Integer");
      mv.visitInsn(DUP);
      mv.visitVarInsn(ILOAD, newLocalVar);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke", "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      mv.visitLabel(l21);
    }
  }
  
}