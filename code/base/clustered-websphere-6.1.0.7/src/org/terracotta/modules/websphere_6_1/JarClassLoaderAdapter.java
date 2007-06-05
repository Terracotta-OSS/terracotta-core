/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.websphere_6_1;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class JarClassLoaderAdapter extends ClassAdapter implements ClassAdapterFactory, Opcodes {

  public JarClassLoaderAdapter(ClassVisitor cv) {
    super(cv);
  }

  public JarClassLoaderAdapter() {
    super(null);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new JarClassLoaderAdapter(visitor);
  }

  public void visitEnd() {
    // delegate NamedClassLoader methods to the delegate loader in our parent class

    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_getClassLoaderName", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/classloader/JarClassLoader", "getCurrentClassLoader",
                       "()Lcom/ibm/ws/classloader/CompoundClassLoader;");
    mv.visitTypeInsn(CHECKCAST, "com/tc/object/loaders/NamedClassLoader");
    mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/loaders/NamedClassLoader", "__tc_getClassLoaderName",
                       "()Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    mv = super.visitMethod(ACC_PUBLIC, "__tc_setClassLoaderName", "(Ljava/lang/String;)V", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/classloader/JarClassLoader", "getCurrentClassLoader",
                       "()Lcom/ibm/ws/classloader/CompoundClassLoader;");
    mv.visitTypeInsn(CHECKCAST, "com/tc/object/loaders/NamedClassLoader");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/loaders/NamedClassLoader", "__tc_setClassLoaderName",
                       "(Ljava/lang/String;)V");
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    super.visitEnd();
  }
}
