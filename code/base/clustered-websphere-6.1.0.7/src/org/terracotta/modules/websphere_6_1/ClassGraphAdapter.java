/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.websphere_6_1;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;

public class ClassGraphAdapter extends ClassAdapter implements ClassAdapterFactory, Opcodes {

  public ClassGraphAdapter(ClassVisitor cv) {
    super(cv);
  }

  public ClassGraphAdapter() {
    super(null);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new ClassGraphAdapter(visitor);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    if ("processWARModule".equals(name)) {
      addProcessWARModuleWrapper(access, name, desc, signature, exceptions);
      return super.visitMethod(access, ByteCodeUtil.METHOD_RENAME_PREFIX + name, desc, signature, exceptions);
    }

    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("createClassLoaders".equals(name)) {
      return new CreateClassLoadersAdapter(mv);
    }
    return mv;
  }

  public void visitEnd() {
    super.visitEnd();
  }

  private void addProcessWARModuleWrapper(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    for (int i = 1; i <= 9; i++) {
      mv.visitVarInsn(ALOAD, i);
    }
    mv.visitMethodInsn(INVOKESPECIAL, "com/ibm/ws/classloader/ClassGraph", //
                       ByteCodeUtil.METHOD_RENAME_PREFIX + name, desc);

    mv.visitVarInsn(ALOAD, 6);  // modulenode
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/classloader/ClassGraph$ModuleNode", "classLoader",
                      "Lcom/ibm/ws/classloader/JarClassLoader;");
    mv.visitVarInsn(ALOAD, 1);  // earfile
    mv.visitVarInsn(ALOAD, 4);  // moduleref
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/websphere/WebsphereLoaderNaming", "registerWebAppLoader",
                       "(Lcom/tc/object/loaders/NamedClassLoader;Ljava/lang/Object;Ljava/lang/Object;)V");

    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private static class CreateClassLoadersAdapter extends MethodAdapter implements Opcodes {

    public CreateClassLoadersAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
      if (INVOKESPECIAL == opcode && "com/ibm/ws/classloader/JarClassLoader".equals(owner) && "<init>".equals(name)) {
        super.visitInsn(DUP);
        super.visitVarInsn(ALOAD, 1);
        super.visitMethodInsn(INVOKESTATIC, "com/tc/websphere/WebsphereLoaderNaming",
                              "nameAndRegisterDependencyLoader",
                              "(Lcom/tc/object/loaders/NamedClassLoader;Ljava/lang/Object;)V");
      }
    }
  }

}
