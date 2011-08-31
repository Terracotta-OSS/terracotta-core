/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.object.SerializationUtil;
import com.tcclient.util.MapEntrySetWrapper;

public class TreeMapAdapter {

  public static class EntrySetAdapter extends AbstractMethodAdapter {

    @Override
    public MethodVisitor adapt(ClassVisitor classVisitor) {
      return new Adapter(visitOriginal(classVisitor));
    }

    @Override
    public boolean doesOriginalNeedAdapting() {
      return false;
    }

    private static class Adapter extends MethodAdapter implements Opcodes {
      public Adapter(MethodVisitor mv) {
        super(mv);
      }

      @Override
      public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        super.visitMethodInsn(opcode, owner, name, desc);

        if ((opcode == INVOKESPECIAL) && "<init>".equals(name) && "java/util/TreeMap$3".equals(owner)) {
          mv.visitVarInsn(ASTORE, 1);
          mv.visitTypeInsn(NEW, MapEntrySetWrapper.CLASS_SLASH);
          mv.visitInsn(DUP);
          mv.visitVarInsn(ALOAD, 0);
          mv.visitVarInsn(ALOAD, 1);
          mv.visitMethodInsn(INVOKESPECIAL, MapEntrySetWrapper.CLASS_SLASH, "<init>",
                             "(Ljava/util/Map;Ljava/util/Set;)V");
        }
      }
    }

  }

  public static class DeleteEntryAdapter extends AbstractMethodAdapter {

    @Override
    public MethodVisitor adapt(ClassVisitor classVisitor) {
      return new Adapter(visitOriginal(classVisitor));
    }

    @Override
    public boolean doesOriginalNeedAdapting() {
      return false;
    }

    private static class Adapter extends MethodAdapter implements Opcodes {
      public Adapter(MethodVisitor mv) {
        super(mv);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(SerializationUtil.REMOVE_KEY_SIGNATURE);
        mv.visitLdcInsn(Integer.valueOf(1));
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        mv.visitInsn(DUP);
        mv.visitLdcInsn(Integer.valueOf(0));
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/TreeMap$Entry", "getKey", "()Ljava/lang/Object;");
        mv.visitInsn(AASTORE);
        mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "logicalInvoke",
                           "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      }
    }
  }

  public static class PutAdapter extends AbstractMethodAdapter {

    @Override
    public MethodVisitor adapt(ClassVisitor classVisitor) {
      return new Adapter(visitOriginal(classVisitor));
    }

    @Override
    protected MethodVisitor visitOriginal(ClassVisitor classVisitor) {
      MethodVisitor mv = super.visitOriginal(classVisitor);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "checkWriteAccess", "(Ljava/lang/Object;)V");

      return mv;
    }

    @Override
    public boolean doesOriginalNeedAdapting() {
      return false;
    }

    private class Adapter extends MethodAdapter implements Opcodes {

      public Adapter(MethodVisitor mv) {
        super(mv);
      }

      @Override
      public void visitMethodInsn(int opcode, String className, String method, String desc) {
        super.visitMethodInsn(opcode, className, method, desc);

        if ((INVOKESPECIAL == opcode) && "<init>".equals(method) && "java/util/TreeMap$Entry".equals(className)) {
          mv.visitVarInsn(ALOAD, 0);
          mv.visitLdcInsn(methodName + description);
          ByteCodeUtil.createParametersToArrayByteCode(mv, Type.getArgumentTypes(description));
          mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "logicalInvoke",
                             "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
        }

        if ((INVOKEVIRTUAL == opcode) && "setValue".equals(method) && "java/util/TreeMap$Entry".equals(className)) {
          mv.visitInsn(DUP);
          mv.visitVarInsn(ALOAD, 0);
          mv.visitLdcInsn(methodName + description);
          mv.visitLdcInsn(Integer.valueOf(2));
          mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
          mv.visitInsn(DUP);
          mv.visitInsn(DUP);
          mv.visitLdcInsn(Integer.valueOf(0));
          mv.visitVarInsn(ALOAD, 3);
          mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/TreeMap$Entry", "getKey", "()Ljava/lang/Object;");
          mv.visitInsn(AASTORE);
          mv.visitLdcInsn(Integer.valueOf(1));
          mv.visitVarInsn(ALOAD, 2);
          mv.visitInsn(AASTORE);
          mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "logicalInvoke",
                             "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
        }

      }
    }
  }
}