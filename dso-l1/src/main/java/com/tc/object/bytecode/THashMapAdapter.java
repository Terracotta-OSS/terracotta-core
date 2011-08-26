/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;


import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class THashMapAdapter {
  
  public static class TransformValuesAdapter extends AbstractMethodAdapter {
    private final static String STATIC_EXECUTE_DESC = "(Lgnu/trove/TObjectFunction;Ljava/lang/Object;Lgnu/trove/THashMap;Ljava/lang/Object;)Ljava/lang/Object;";
    
    public MethodVisitor adapt(ClassVisitor classVisitor) {
      addStaticExecuteMethod(classVisitor);
      MethodVisitor mv = visitOriginal(classVisitor);
      return new Adapter(mv);
    }
    
    private void addStaticExecuteMethod(ClassVisitor cv) {
      MethodVisitor mv = cv.visitMethod(ACC_PRIVATE + ACC_STATIC + ACC_SYNTHETIC, "execute", STATIC_EXECUTE_DESC, null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(468, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKEINTERFACE, "gnu/trove/TObjectFunction", "execute", "(Ljava/lang/Object;)Ljava/lang/Object;");
      mv.visitVarInsn(ASTORE, 4);
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLineNumber(469, l1);
      mv.visitVarInsn(ALOAD, 4);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z");
      Label l2 = new Label();
      mv.visitJumpInsn(IFNE, l2);
      Label l3 = new Label();
      mv.visitLabel(l3);
      mv.visitLineNumber(470, l3);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitLdcInsn("put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
      mv.visitInsn(ICONST_2);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 3);
      mv.visitInsn(AASTORE);
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_1);
      mv.visitVarInsn(ALOAD, 4);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke", "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      mv.visitLabel(l2);
      mv.visitLineNumber(472, l2);
      mv.visitVarInsn(ALOAD, 4);
      mv.visitInsn(ARETURN);
      Label l4 = new Label();
      mv.visitLabel(l4);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }

    public boolean doesOriginalNeedAdapting() {
      return false;
    }

    private static class Adapter extends MethodAdapter implements Opcodes {

      public Adapter(MethodVisitor mv) {
        super(mv);
      }
      
      public void visitCode() {
        super.visitCode();
        ByteCodeUtil.pushThis(mv);
        mv.visitMethodInsn(INVOKEVIRTUAL, "gnu/trove/THashMap", "__tc_managed", "()Lcom/tc/object/TCObject;");
        Label l1 = new Label();
        mv.visitJumpInsn(IFNULL, l1);
        ByteCodeUtil.pushThis(mv);
        mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "checkWriteAccess", "(Ljava/lang/Object;)V");
        mv.visitLabel(l1);
      }

      public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        if ((opcode == INVOKEINTERFACE) && "gnu/trove/TObjectFunction".equals(owner) && "execute".equals(name) &&
            "(Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
          mv.visitVarInsn(ALOAD, 0);
          mv.visitVarInsn(ALOAD, 3);
          mv.visitVarInsn(ILOAD, 4);
          mv.visitInsn(AALOAD);
          opcode = INVOKESTATIC;
          owner = "gnu/trove/THashMap";
          desc = STATIC_EXECUTE_DESC; 
        }
        super.visitMethodInsn(opcode, owner, name, desc);
      }
    }
  }

}
