/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
  private static MethodNode __tc_getEntryMethod() {
    MethodNode mv = new MethodNode(ACC_PROTECTED, //
                                   "__tc_getEntry", //
                                   "(Ljava/lang/Object;)Ljava/util/Map$Entry;", null, null);
    mv.visitCode();

    if (Vm.isIBM()) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Hashtable", "getEntry",
                         "(Ljava/lang/Object;)Ljava/util/Hashtable$Entry;");
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

  /**
   * Just like Hashtable.remove(Object) except that it returns the entry (or null)
   *
   * <pre>
   * protected synchronized Entry __tc_removeEntryForKey(Object key) {
   *   Entry tab[] = table;
   *   int hash = key.hashCode();
   *   int index = (hash &amp; 0x7FFFFFFF) % tab.length;
   *   for (Entry e = tab[index], prev = null; e != null; prev = e, e = e.next) {
   *     if ((e.hash == hash) &amp;&amp; e.key.equals(key)) {
   *       modCount++;
   *       if (prev != null) {
   *         prev.next = e.next;
   *       } else {
   *         tab[index] = e.next;
   *       }
   *       count--;
   *       return e;
   *     }
   *   }
   *   return null;
   * }
   *
   */
  private static MethodNode __tc_removeEntryForKeyMethodSun() {
    MethodNode mv = new MethodNode(ACC_PROTECTED + ACC_SYNCHRONIZED, "__tc_removeEntryForKey",
                                   "(Ljava/lang/Object;)Ljava/util/Map$Entry;", null, null);
    mv.visitCode();
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
    mv.visitInsn(ACONST_NULL);
    mv.visitVarInsn(ASTORE, 6);
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
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(DUP);
    mv.visitFieldInsn(GETFIELD, "java/util/Hashtable", "modCount", "I");
    mv.visitInsn(ICONST_1);
    mv.visitInsn(IADD);
    mv.visitFieldInsn(PUTFIELD, "java/util/Hashtable", "modCount", "I");
    mv.visitVarInsn(ALOAD, 6);
    Label l10 = new Label();
    mv.visitJumpInsn(IFNULL, l10);
    mv.visitVarInsn(ALOAD, 6);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitFieldInsn(GETFIELD, "java/util/Hashtable$Entry", "next", "Ljava/util/Hashtable$Entry;");
    mv.visitFieldInsn(PUTFIELD, "java/util/Hashtable$Entry", "next", "Ljava/util/Hashtable$Entry;");
    Label l12 = new Label();
    mv.visitJumpInsn(GOTO, l12);
    mv.visitLabel(l10);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitVarInsn(ILOAD, 4);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitFieldInsn(GETFIELD, "java/util/Hashtable$Entry", "next", "Ljava/util/Hashtable$Entry;");
    mv.visitInsn(AASTORE);
    mv.visitLabel(l12);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(DUP);
    mv.visitFieldInsn(GETFIELD, "java/util/Hashtable", "count", "I");
    mv.visitInsn(ICONST_1);
    mv.visitInsn(ISUB);
    mv.visitFieldInsn(PUTFIELD, "java/util/Hashtable", "count", "I");
    mv.visitVarInsn(ALOAD, 5);
    mv.visitInsn(ARETURN);
    mv.visitLabel(l7);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitVarInsn(ASTORE, 6);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitFieldInsn(GETFIELD, "java/util/Hashtable$Entry", "next", "Ljava/util/Hashtable$Entry;");
    mv.visitVarInsn(ASTORE, 5);
    mv.visitJumpInsn(GOTO, l4);
    mv.visitLabel(l5);
    mv.visitInsn(ACONST_NULL);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    return mv;
  }

  private static MethodNode __tc_removeEntryForKeyMethodIBM() {
    MethodNode mv = new MethodNode(ACC_PROTECTED + ACC_SYNCHRONIZED, "__tc_removeEntryForKey",
                                   "(Ljava/lang/Object;)Ljava/util/Map$Entry;", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I");
    mv.visitVarInsn(ISTORE, 2);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitLdcInsn(new Integer(2147483647));
    mv.visitInsn(IAND);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/Hashtable", "elementData", "[Ljava/util/Hashtable$Entry;");
    mv.visitInsn(ARRAYLENGTH);
    mv.visitInsn(IREM);
    mv.visitVarInsn(ISTORE, 3);
    mv.visitInsn(ACONST_NULL);
    mv.visitVarInsn(ASTORE, 4);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/Hashtable", "elementData", "[Ljava/util/Hashtable$Entry;");
    mv.visitVarInsn(ILOAD, 3);
    mv.visitInsn(AALOAD);
    mv.visitVarInsn(ASTORE, 5);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitVarInsn(ALOAD, 5);
    Label l5 = new Label();
    mv.visitJumpInsn(IFNULL, l5);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Hashtable$Entry", "equalsKey", "(Ljava/lang/Object;I)Z");
    mv.visitJumpInsn(IFNE, l5);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitVarInsn(ASTORE, 4);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitFieldInsn(GETFIELD, "java/util/Hashtable$Entry", "next", "Ljava/util/Hashtable$Entry;");
    mv.visitVarInsn(ASTORE, 5);
    mv.visitJumpInsn(GOTO, l4);
    mv.visitLabel(l5);
    mv.visitVarInsn(ALOAD, 5);
    Label l9 = new Label();
    mv.visitJumpInsn(IFNULL, l9);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(DUP);
    mv.visitFieldInsn(GETFIELD, "java/util/Hashtable", "modCount", "I");
    mv.visitInsn(ICONST_1);
    mv.visitInsn(IADD);
    mv.visitFieldInsn(PUTFIELD, "java/util/Hashtable", "modCount", "I");
    mv.visitVarInsn(ALOAD, 4);
    Label l12 = new Label();
    mv.visitJumpInsn(IFNONNULL, l12);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/Hashtable", "elementData", "[Ljava/util/Hashtable$Entry;");
    mv.visitVarInsn(ILOAD, 3);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitFieldInsn(GETFIELD, "java/util/Hashtable$Entry", "next", "Ljava/util/Hashtable$Entry;");
    mv.visitInsn(AASTORE);
    Label l13 = new Label();
    mv.visitJumpInsn(GOTO, l13);
    mv.visitLabel(l12);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitFieldInsn(GETFIELD, "java/util/Hashtable$Entry", "next", "Ljava/util/Hashtable$Entry;");
    mv.visitFieldInsn(PUTFIELD, "java/util/Hashtable$Entry", "next", "Ljava/util/Hashtable$Entry;");
    mv.visitLabel(l13);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(DUP);
    mv.visitFieldInsn(GETFIELD, "java/util/Hashtable", "elementCount", "I");
    mv.visitInsn(ICONST_1);
    mv.visitInsn(ISUB);
    mv.visitFieldInsn(PUTFIELD, "java/util/Hashtable", "elementCount", "I");
    mv.visitVarInsn(ALOAD, 5);
    mv.visitInsn(ARETURN);
    mv.visitLabel(l9);
    mv.visitInsn(ACONST_NULL);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
    return mv;
  }

  public static MethodNode[] getMethods() {
    return new MethodNode[] { __tc_getEntryMethod(),
        Vm.isIBM() ? __tc_removeEntryForKeyMethodIBM() : __tc_removeEntryForKeyMethodSun() };
  }

}
