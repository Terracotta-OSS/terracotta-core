/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;

public class JavaUtilWeakHashMapAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {
  private static final String HASH_MAP_CLASS            = "java/util/HashMap";
  private static final String WEAK_HASH_MAP_CLASS       = "java/util/WeakHashMap";

  private static final int    SYNTHETIC_METHOD_ACCESS   = ACC_PROTECTED | ACC_SYNTHETIC;

  private static final String HASH_MAP_HASH_METHOD_NAME = "hash";

  private static final String TC_HASH_METHOD_NAME       = ByteCodeUtil.TC_METHOD_PREFIX + "hash";
  private static final String HASH_METHOD_DESCRIPTION   = "(Ljava/lang/Object;)I";

  private static final String TC_EQUAL_METHOD_NAME      = ByteCodeUtil.TC_METHOD_PREFIX + "equal";
  private static final String EQ_METHOD_DESCRIPTION     = "(Ljava/lang/Object;Ljava/lang/Object;)Z";

  private static final String EQ_METHOD_NAME            = "eq";

  private String              hashMapHashMethodDesc     = null;

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
    MethodVisitor mv = super.visitMethod(SYNTHETIC_METHOD_ACCESS, TC_HASH_METHOD_NAME, HASH_METHOD_DESCRIPTION, null,
                                         null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 1);

    if (hashMapHashMethodDesc == null) { throw new AssertionError(); }
    Type[] args = Type.getArgumentTypes(hashMapHashMethodDesc);
    if (args.length != 1) { throw new AssertionError("unexpected HashMap.hash() signature: " + hashMapHashMethodDesc); }

    if (args[0].getSort() == Type.INT) {
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I");
    } else if (!args[0].getInternalName().equals("java/lang/Object")) { throw new AssertionError("unexpected type: "
                                                                                                 + args[0]); }

    mv.visitMethodInsn(INVOKESTATIC, HASH_MAP_CLASS, HASH_MAP_HASH_METHOD_NAME, hashMapHashMethodDesc);
    mv.visitInsn(IRETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void generateSyntheticEqualMethod() {
    MethodVisitor mv = super.visitMethod(SYNTHETIC_METHOD_ACCESS, TC_EQUAL_METHOD_NAME, EQ_METHOD_DESCRIPTION, null,
                                         null);
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
    return new JavaUtilWeakHashMapMethodAdapter(mv);
  }

  private class JavaUtilWeakHashMapMethodAdapter extends MethodAdapter {
    public JavaUtilWeakHashMapMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if ((INVOKEVIRTUAL == opcode) && "java/lang/Object".equals(owner) && "hashCode".equals(name)) { return; }

      if (INVOKESTATIC == opcode && HASH_MAP_CLASS.equals(owner) && HASH_MAP_HASH_METHOD_NAME.equals(name)) {
        hashMapHashMethodDesc = desc;
        ByteCodeUtil.pushThis(this);
        super.visitInsn(SWAP);
        super.visitMethodInsn(INVOKEVIRTUAL, WEAK_HASH_MAP_CLASS, TC_HASH_METHOD_NAME, HASH_METHOD_DESCRIPTION);
      } else if (INVOKESTATIC == opcode && WEAK_HASH_MAP_CLASS.equals(owner) && EQ_METHOD_NAME.equals(name)
                 && EQ_METHOD_DESCRIPTION.equals(desc)) {
        ByteCodeUtil.pushThis(mv);
        super.visitInsn(SWAP);
        super.visitInsn(DUP2_X1);
        super.visitInsn(POP2);
        super.visitInsn(SWAP);
        super.visitMethodInsn(INVOKEVIRTUAL, WEAK_HASH_MAP_CLASS, TC_EQUAL_METHOD_NAME, desc);
      } else {
        super.visitMethodInsn(opcode, owner, name, desc);
      }
    }
  }
}