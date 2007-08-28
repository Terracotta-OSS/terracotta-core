/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.tomcat.transform;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.asm.commons.LocalVariablesSorter;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;

public class WebAppLoaderAdapter extends ClassAdapter implements ClassAdapterFactory {

  public WebAppLoaderAdapter() {
    super(null);
  }

  private WebAppLoaderAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new WebAppLoaderAdapter(visitor, loader);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("createClassLoader".equals(name) //
        && "()Lorg/apache/catalina/loader/WebappClassLoader;".equals(desc)) { return new CreateClassLoaderAdapter(
                                                                                                                  access,
                                                                                                                  desc,
                                                                                                                  mv); }
    return mv;
  }

  private static class CreateClassLoaderAdapter extends LocalVariablesSorter implements Opcodes {

    public CreateClassLoaderAdapter(int access, String desc, MethodVisitor mv) {
      super(access, desc, mv);
    }

    public void visitInsn(int opcode) {
      if (ARETURN == opcode) {

        int slot = newLocal(Type.getObjectType("java/lang/Object"));
        mv.visitVarInsn(ASTORE, slot);

        // name the web app loader
        mv.visitVarInsn(ALOAD, slot);
        mv.visitFieldInsn(GETSTATIC, "com/tc/object/loaders/Namespace", "TOMCAT_NAMESPACE", "Ljava/lang/String;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEINTERFACE, "org/apache/catalina/Loader", "getContainer",
                           "()Lorg/apache/catalina/Container;");
        mv.visitMethodInsn(INVOKESTATIC, "com/tc/tomcat/LoaderNaming", "getFullyQualifiedName",
                           "(Lorg/apache/catalina/Container;)Ljava/lang/String;");
        mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/loaders/Namespace", "createLoaderName",
                           "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
        mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/loaders/NamedClassLoader", "__tc_setClassLoaderName",
                           "(Ljava/lang/String;)V");

        // register the web app loader
        mv.visitVarInsn(ALOAD, slot);
        mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper",
                           "registerGlobalLoader", "(" + ByteCodeUtil.NAMEDCLASSLOADER_TYPE + ")V");

        // prepare for ARETURN
        mv.visitVarInsn(ALOAD, slot);
      }
      super.visitInsn(opcode);
    }

  }

}
