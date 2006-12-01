/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;


import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tcclient.util.MapEntrySetWrapper;

public class HashMapAdapter {

  public static class EntrySetAdapter extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor classVisitor) {
      MethodVisitor mv = visitOriginal(classVisitor);
      if ("entrySet".equals(methodName)) {
        return new Adapter(mv);
      }
      return mv;
    }

    public boolean doesOriginalNeedAdapting() {
      return false;
    }
    
    private static class Adapter extends MethodAdapter implements Opcodes {

      public Adapter(MethodVisitor mv) {
        super(mv);
      }

      public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        super.visitMethodInsn(opcode, owner, name, desc);

        if ((opcode == INVOKESPECIAL)) {
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

}
