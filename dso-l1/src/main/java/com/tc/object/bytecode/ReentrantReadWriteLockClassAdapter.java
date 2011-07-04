/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;

public class ReentrantReadWriteLockClassAdapter extends ClassAdapter implements Opcodes {
  public ReentrantReadWriteLockClassAdapter(ClassVisitor cv) {
    super(cv);
  }

  public final MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    if ("hasQueuedThreads".equals(name) || "hasQueuedThread".equals(name) || "getQueueLength".equals(name)) {
      name = getNewName(name);
    }
    
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("<init>".equals(name)) {
      mv = new InitMethodAdapter(mv);
    }
    return mv;
  }
  
  private String getNewName(String methodName) {
    return ByteCodeUtil.TC_METHOD_PREFIX + methodName;
  }
  
  public void visitEnd() {
    createHasQueuedThreadsMethod("hasQueuedThreads", "()Z");
    createHasQueuedThreadMethod("hasQueuedThread", "(Ljava/lang/Thread;)Z");
    createGetQueueLengthMethod("getQueueLength", "()I");
    super.visitEnd();
  }
  
  private void createGetQueueLengthMethod(String methodName, String methodDesc) {
    Type ret = Type.getReturnType(methodDesc);
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_FINAL, methodName, methodDesc, null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(258, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isManaged", "(Ljava/lang/Object;)Z");
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(259, l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/locks/ReentrantReadWriteLock", "sync", "Ljava/util/concurrent/locks/ReentrantReadWriteLock$Sync;");
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "queueLength", "(Ljava/lang/Object;)I");
    mv.visitInsn(ret.getOpcode(IRETURN));
    mv.visitLabel(l1);
    mv.visitLineNumber(261, l1);
    mv.visitVarInsn(ALOAD, 0);
    Type[] params = Type.getArgumentTypes(methodDesc);
    for (int i = 0; i < params.length; i++) {
      mv.visitVarInsn(params[i].getOpcode(ILOAD), i + 1);
    }
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/locks/ReentrantReadWriteLock", getNewName(methodName), methodDesc);
    mv.visitInsn(ret.getOpcode(IRETURN));
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }
  
  private void createHasQueuedThreadMethod(String methodName, String methodDesc) {
    Type ret = Type.getReturnType(methodDesc);
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_FINAL, methodName, methodDesc, null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(258, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isManaged", "(Ljava/lang/Object;)Z");
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(259, l2);
    mv.visitTypeInsn(NEW, "com/tc/exception/TCNotSupportedMethodException");
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, "com/tc/exception/TCNotSupportedMethodException", "<init>", "()V");
    mv.visitInsn(ATHROW);
    mv.visitLabel(l1);
    mv.visitLineNumber(261, l1);
    mv.visitVarInsn(ALOAD, 0);
    Type[] params = Type.getArgumentTypes(methodDesc);
    for (int i = 0; i < params.length; i++) {
      mv.visitVarInsn(params[i].getOpcode(ILOAD), i + 1);
    }
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/locks/ReentrantReadWriteLock", getNewName(methodName), methodDesc);
    mv.visitInsn(ret.getOpcode(IRETURN));
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void createHasQueuedThreadsMethod(String methodName, String methodDesc) {
    Type ret = Type.getReturnType(methodDesc);
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_FINAL, methodName, methodDesc, null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(258, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isManaged", "(Ljava/lang/Object;)Z");
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(259, l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/locks/ReentrantReadWriteLock", "sync", "Ljava/util/concurrent/locks/ReentrantReadWriteLock$Sync;");
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "queueLength", "(Ljava/lang/Object;)I");
    Label l3 = new Label();
    mv.visitJumpInsn(IFLE, l3);
    mv.visitInsn(ICONST_1);
    mv.visitInsn(ret.getOpcode(IRETURN));
    mv.visitLabel(l3);
    mv.visitInsn(ICONST_0);
    mv.visitInsn(ret.getOpcode(IRETURN));
    mv.visitLabel(l1);
    mv.visitLineNumber(261, l1);
    mv.visitVarInsn(ALOAD, 0);
    Type[] params = Type.getArgumentTypes(methodDesc);
    for (int i = 0; i < params.length; i++) {
      mv.visitVarInsn(params[i].getOpcode(ILOAD), i + 1);
    }
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/locks/ReentrantReadWriteLock", getNewName(methodName), methodDesc);
    mv.visitInsn(ret.getOpcode(IRETURN));
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }
  
  private final static class InitMethodAdapter extends MethodAdapter implements Opcodes {
    public InitMethodAdapter(MethodVisitor mv) {
      super(mv);
    }
    
    public void visitTypeInsn(int opcode, String desc) {
      if (NEW == opcode) {
        if ("java/util/concurrent/locks/ReentrantReadWriteLock$NonfairSync".equals(desc)) {
          desc = "java/util/concurrent/locks/ReentrantReadWriteLock$FairSync";
        } else if ("java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock".equals(desc)) {
          desc = "java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock";
        } else if ("java/util/concurrent/locks/ReentrantReadWriteLock$WriteLock".equals(desc)) {
          desc = "java/util/concurrent/locks/ReentrantReadWriteLock$WriteLock";
        }
      } 
      super.visitTypeInsn(opcode, desc);
    }
    
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if (opcode == INVOKESPECIAL) {
        if ("java/util/concurrent/locks/ReentrantReadWriteLock$NonfairSync".equals(owner) &&
          "<init>".equals(name) && "()V".equals(desc)) {
          owner = "java/util/concurrent/locks/ReentrantReadWriteLock$FairSync";
        } else if ("java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock".equals(owner) &&
            "<init>".equals(name) && "(Ljava/util/concurrent/locks/ReentrantReadWriteLock;)V".equals(desc)) {
          mv.visitVarInsn(ALOAD, 0);
          mv.visitFieldInsn(GETFIELD, "java/util/concurrent/locks/ReentrantReadWriteLock", "sync", "Ljava/util/concurrent/locks/ReentrantReadWriteLock$Sync;");
          //owner = "java/util/concurrent/locks/ReentrantReadWriteLock$ReadLockTC";
          owner = "java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock";
          desc = "(Ljava/util/concurrent/locks/ReentrantReadWriteLock;Ljava/util/concurrent/locks/ReentrantReadWriteLock$Sync;)V";
        } else if ("java/util/concurrent/locks/ReentrantReadWriteLock$WriteLock".equals(owner) &&
            "<init>".equals(name) && "(Ljava/util/concurrent/locks/ReentrantReadWriteLock;)V".equals(desc)) {
          mv.visitVarInsn(ALOAD, 0);
          mv.visitFieldInsn(GETFIELD, "java/util/concurrent/locks/ReentrantReadWriteLock", "sync", "Ljava/util/concurrent/locks/ReentrantReadWriteLock$Sync;");
          //owner = "java/util/concurrent/locks/ReentrantReadWriteLock$WriteLockTC";
          owner = "java/util/concurrent/locks/ReentrantReadWriteLock$WriteLock";
          desc = "(Ljava/util/concurrent/locks/ReentrantReadWriteLock;Ljava/util/concurrent/locks/ReentrantReadWriteLock$Sync;)V";
        }
      }
      super.visitMethodInsn(opcode, owner, name, desc);
    }
  }
}
