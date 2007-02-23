/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class JavaUtilWeakHashMapAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {
  private static final String HASH_MAP_CLASS          = "java/util/HashMap";
  private static final String WEAK_HASH_MAP_CLASS     = "java/util/WeakHashMap";

  private static final int    SYNTHETIC_METHOD_ACCESS = ACC_PROTECTED | ACC_SYNTHETIC;
  private static final String HASH_METHOD_NAME        = "hash";
  private static final String HASH_METHOD_DESCRIPTION = "(Ljava/lang/Object;)I";
  private static final String EQUAL_METHOD_NAME       = "equal";
  private static final String EQ_METHOD_NAME          = "eq";
  private static final String EQ_METHOD_DESCRIPTION   = "(Ljava/lang/Object;Ljava/lang/Object;)Z";

  public JavaUtilWeakHashMapAdapter() {
    super(null);
  }
  
  private JavaUtilWeakHashMapAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new JavaUtilWeakHashMapAdapter(visitor, loader);
  }
  
  public void visitEnd() {
    generateSyntheticHashMethod();
    generateSyntheticEqualMethod();
    super.visitEnd();
  }

  private void generateSyntheticHashMethod() {
    MethodVisitor mv = cv.visitMethod(SYNTHETIC_METHOD_ACCESS, HASH_METHOD_NAME, HASH_METHOD_DESCRIPTION, null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKESTATIC, HASH_MAP_CLASS, HASH_METHOD_NAME, HASH_METHOD_DESCRIPTION);
    mv.visitInsn(IRETURN);
    mv.visitMaxs(1, 2);
    mv.visitEnd();
  }

  private void generateSyntheticEqualMethod() {
    MethodVisitor mv = cv.visitMethod(SYNTHETIC_METHOD_ACCESS, EQUAL_METHOD_NAME, EQ_METHOD_DESCRIPTION, null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKESTATIC, WEAK_HASH_MAP_CLASS, EQ_METHOD_NAME, EQ_METHOD_DESCRIPTION);
    mv.visitInsn(IRETURN);
    mv.visitMaxs(1, 2);
    mv.visitEnd();
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if (!HASH_METHOD_NAME.equals(name) && !EQUAL_METHOD_NAME.equals(name)) {
      return new JavaUtilWeakHashMapMethodAdapter(mv);
    } else {
      return mv;
    }
  }

  private static class JavaUtilWeakHashMapMethodAdapter extends MethodAdapter {
    public JavaUtilWeakHashMapMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if (INVOKESTATIC == opcode && HASH_MAP_CLASS.equals(owner) && HASH_METHOD_NAME.equals(name)
          && HASH_METHOD_DESCRIPTION.equals(desc)) {
        ByteCodeUtil.pushThis(mv);
        mv.visitInsn(SWAP);
        mv.visitMethodInsn(INVOKEVIRTUAL, WEAK_HASH_MAP_CLASS, name, desc);
      } else if (INVOKESTATIC == opcode && WEAK_HASH_MAP_CLASS.equals(owner) && EQ_METHOD_NAME.equals(name)
                 && EQ_METHOD_DESCRIPTION.equals(desc)) {
        ByteCodeUtil.pushThis(mv);
        mv.visitInsn(SWAP);
        mv.visitInsn(DUP2_X1);
        mv.visitInsn(POP2);
        mv.visitInsn(SWAP);
        mv.visitMethodInsn(INVOKEVIRTUAL, WEAK_HASH_MAP_CLASS, EQUAL_METHOD_NAME, desc);
      } else {
        mv.visitMethodInsn(opcode, owner, name, desc);
      }
    }
  }
}