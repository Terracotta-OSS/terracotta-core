/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.Label;
import com.tc.asm.Opcodes;
import com.tc.asm.tree.MethodNode;
import com.tc.util.runtime.Vm;

public class HashtableClassAdapter implements Opcodes {

  /**
   * Creates the following method code
   * 
   * For Sun JRE:
   * 
   * <pre>
   * protected Map.Entry __tc_getEntry(Object obj) {
   *   Entry aentry[] = table;
   *   int i = obj.hashCode();
   *   int j = (i &amp; 0x7fffffff) % aentry.length;
   *   for (Entry entry = aentry[j]; entry != null; entry = entry.next) {
   *     if (entry.hash == i &amp;&amp; entry.key.equals(obj)) { return (Entry) entry; }
   *   }
   *   return null;
   * }
   * </pre>
   * 
   * For IBM JRE:
   * 
   * <pre>
   * protected Map.Entry __tc_getEntry(Object obj) {
   *   return getEntry();
   * }
   * </pre>
   * 
   */
  public static MethodNode createMethod() {
    MethodNode mv = new MethodNode(ACC_PROTECTED, //
                                   "__tc_getEntry", //
                                   "(Ljava/lang/Object;)Ljava/util/Map$Entry;", null, null);
    mv.visitCode();
    
    if (Vm.isIBM()) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Hashtable", "getEntry", "(Ljava/lang/Object;)Ljava/util/Hashtable$Entry;");
    } else {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "java/util/Hashtable", "table", "[Ljava/util/Hashtable$Entry;");
      mv.visitVarInsn(ASTORE, 2);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I");
      mv.visitVarInsn(ISTORE, 3);
      mv.visitVarInsn(ILOAD, 3);
      mv.visitLdcInsn(new Integer(2147483647));
      mv.visitInsn(IAND);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitInsn(ARRAYLENGTH);
      mv.visitInsn(IREM);
      mv.visitVarInsn(ISTORE, 4);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitVarInsn(ILOAD, 4);
      mv.visitInsn(AALOAD);
      mv.visitVarInsn(ASTORE, 5);
      Label l4 = new Label();
      mv.visitLabel(l4);
      mv.visitVarInsn(ALOAD, 5);
      Label l5 = new Label();
      mv.visitJumpInsn(IFNULL, l5);
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
    }

    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
    
    return mv;
  }

}
