/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.Label;
import com.tc.asm.Opcodes;
import com.tc.asm.tree.MethodNode;

public class HashtableClassAdapter implements Opcodes {

  public static MethodNode createMethod() {
    MethodNode mv = new MethodNode(ACC_PROTECTED, "getEntry",
                                              "(Ljava/lang/Object;)Ljava/util/Map$Entry;", null, null);
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/Hashtable", "table", "[Ljava/util/Hashtable$Entry;");
    mv.visitVarInsn(ASTORE, 2);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I");
    mv.visitVarInsn(ISTORE, 3);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitVarInsn(ILOAD, 3);
    mv.visitLdcInsn(new Integer(2147483647));
    mv.visitInsn(IAND);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInsn(ARRAYLENGTH);
    mv.visitInsn(IREM);
    mv.visitVarInsn(ISTORE, 4);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitVarInsn(ILOAD, 4);
    mv.visitInsn(AALOAD);
    mv.visitVarInsn(ASTORE, 5);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitVarInsn(ALOAD, 5);
    Label l5 = new Label();
    mv.visitJumpInsn(IFNULL, l5);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitFieldInsn(GETFIELD, "java/util/Hashtable$Entry", "hash", "I");
    mv.visitVarInsn(ILOAD, 3);
    Label l7 = new Label();
    mv.visitJumpInsn(IF_ICMPNE, l7);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitFieldInsn(GETFIELD, "java/util/Hashtable$Entry", "key", "Ljava/lang/Object;");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z");
    mv.visitJumpInsn(IFEQ, l7);
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitTypeInsn(CHECKCAST, "java/util/Hashtable$Entry");
    mv.visitInsn(ARETURN);
    mv.visitLabel(l7);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitFieldInsn(GETFIELD, "java/util/Hashtable$Entry", "next", "Ljava/util/Hashtable$Entry;");
    mv.visitVarInsn(ASTORE, 5);
    mv.visitJumpInsn(GOTO, l4);
    mv.visitLabel(l5);
    mv.visitInsn(ACONST_NULL);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    return mv;
  }

}
