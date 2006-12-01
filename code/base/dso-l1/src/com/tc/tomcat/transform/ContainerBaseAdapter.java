/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.tomcat.transform;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class ContainerBaseAdapter extends ClassAdapter implements Opcodes {

  public ContainerBaseAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("<init>".equals(name)) {
      mv = new PipelineAdapter(mv);
    }
    return mv;

  }

  private static class PipelineAdapter extends MethodAdapter implements Opcodes {

    public PipelineAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      if (PUTFIELD == opcode) {
        if ("org/apache/catalina/core/ContainerBase".equals(owner) && "pipeline".equals(name)) {

          // check if this is an engine (if so, then install the magic TC pipeline)
          mv.visitVarInsn(ALOAD, 0);
          mv.visitTypeInsn(INSTANCEOF, "org/apache/catalina/Engine");
          Label notEngine = new Label();
          mv.visitJumpInsn(IFEQ, notEngine);
          mv.visitInsn(POP);
          mv.visitVarInsn(ALOAD, 0);
          mv.visitMethodInsn(INVOKESTATIC, "com/tc/tomcat/session/VersionHelper", "createTerracottaPipeline",
                             "(Lorg/apache/catalina/Container;)Lorg/apache/catalina/Pipeline;");
          mv.visitLabel(notEngine);
        }
      }

      super.visitFieldInsn(opcode, owner, name, desc);
    }

  }

}
