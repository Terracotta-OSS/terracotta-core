/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.jboss.transform;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.commons.LocalVariablesSorter;
import com.tc.object.bytecode.ClassAdapterFactory;

public class MainAdapter extends ClassAdapter implements ClassAdapterFactory {

  public MainAdapter() {
    super(null);
  }

  private MainAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new MainAdapter(visitor, loader);
  }
  
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if (!"boot".equals(name)) {
      return mv;
    }
    return new BootAdapter(access, desc, mv);
  }

  private static class BootAdapter extends LocalVariablesSorter implements Opcodes {

    private int serverSlot;
    private int configSlot;

    public BootAdapter(int access, String desc, MethodVisitor mv) {
      super(access, desc, mv);
      serverSlot = newLocal(1);
      configSlot = newLocal(1);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);

      if ((opcode == INVOKEVIRTUAL) && "org/jboss/system/server/ServerLoader".equals(owner) && "load".equals(name)) {
        super.visitInsn(DUP);
        super.visitVarInsn(ASTORE, serverSlot);
      } else if ((opcode == INVOKEINTERFACE) && "org/jboss/system/server/Server".equals(owner) && "init".equals(name)) {
        super.visitVarInsn(ALOAD, serverSlot);
        super.visitMethodInsn(INVOKEINTERFACE, "org/jboss/system/server/Server", "getConfig",
                              "()Lorg/jboss/system/server/ServerConfig;");
        super.visitVarInsn(ASTORE, configSlot);
        super.visitVarInsn(ALOAD, serverSlot);
        super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
        super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;");
        super.visitVarInsn(ALOAD, configSlot);
        super.visitMethodInsn(INVOKEINTERFACE, "org/jboss/system/server/ServerConfig", "getServerHomeDir",
                              "()Ljava/io/File;");
        super.visitVarInsn(ALOAD, configSlot);
        super.visitMethodInsn(INVOKEINTERFACE, "org/jboss/system/server/ServerConfig", "getServerBaseDir",
                              "()Ljava/io/File;");
        super.visitVarInsn(ALOAD, configSlot);
        super.visitMethodInsn(INVOKEINTERFACE, "org/jboss/system/server/ServerConfig", "getServerTempDir",
                              "()Ljava/io/File;");
        super.visitMethodInsn(INVOKESTATIC, "com/tc/jboss/JBossLoaderNaming", "initialize",
                              "(Ljava/lang/ClassLoader;Ljava/io/File;Ljava/io/File;Ljava/io/File;)V");
      }
    }
  }
}
