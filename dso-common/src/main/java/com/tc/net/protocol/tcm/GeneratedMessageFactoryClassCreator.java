/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.asm.ClassWriter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class GeneratedMessageFactoryClassCreator implements Opcodes {

  public static byte[] create(String className, Class msgType) {

    final String msgClass = msgType.getName().replace('.', '/');

    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    MethodVisitor mv;

    cw.visit(V1_4, ACC_PUBLIC + ACC_SUPER, className.replace('.', '/'), null, "java/lang/Object",
             new String[] { GeneratedMessageFactory.class.getName().replace('.', '/') });

    {
      mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
      mv.visitInsn(RETURN);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }

    {
      mv = cw
          .visitMethod(
                       ACC_PUBLIC,
                       "createMessage",
                       "(Lcom/tc/object/session/SessionID;Lcom/tc/net/protocol/tcm/MessageMonitor;Lcom/tc/io/TCByteBufferOutputStream;Lcom/tc/net/protocol/tcm/MessageChannel;Lcom/tc/net/protocol/tcm/TCMessageType;)Lcom/tc/net/protocol/tcm/TCMessage;",
                       null, null);
      mv.visitCode();
      mv.visitTypeInsn(NEW, msgClass);
      mv.visitInsn(DUP);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitVarInsn(ALOAD, 3);
      mv.visitVarInsn(ALOAD, 4);
      mv.visitVarInsn(ALOAD, 5);
      mv
          .visitMethodInsn(
                           INVOKESPECIAL,
                           msgClass,
                           "<init>",
                           "(Lcom/tc/object/session/SessionID;Lcom/tc/net/protocol/tcm/MessageMonitor;Lcom/tc/io/TCByteBufferOutputStream;Lcom/tc/net/protocol/tcm/MessageChannel;Lcom/tc/net/protocol/tcm/TCMessageType;)V");
      mv.visitInsn(ARETURN);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }
    {
      mv = cw
          .visitMethod(
                       ACC_PUBLIC,
                       "createMessage",
                       "(Lcom/tc/object/session/SessionID;Lcom/tc/net/protocol/tcm/MessageMonitor;Lcom/tc/net/protocol/tcm/MessageChannel;Lcom/tc/net/protocol/tcm/TCMessageHeader;[Lcom/tc/bytes/TCByteBuffer;)Lcom/tc/net/protocol/tcm/TCMessage;",
                       null, null);
      mv.visitCode();
      mv.visitTypeInsn(NEW, msgClass);
      mv.visitInsn(DUP);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitVarInsn(ALOAD, 3);
      mv.visitVarInsn(ALOAD, 4);
      mv.visitVarInsn(ALOAD, 5);
      mv
          .visitMethodInsn(
                           INVOKESPECIAL,
                           msgClass,
                           "<init>",
                           "(Lcom/tc/object/session/SessionID;Lcom/tc/net/protocol/tcm/MessageMonitor;Lcom/tc/net/protocol/tcm/MessageChannel;Lcom/tc/net/protocol/tcm/TCMessageHeader;[Lcom/tc/bytes/TCByteBuffer;)V");
      mv.visitInsn(ARETURN);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }
    cw.visitEnd();

    return cw.toByteArray();
  }
}
