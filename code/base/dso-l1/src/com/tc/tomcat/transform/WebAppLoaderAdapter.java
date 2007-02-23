/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.tomcat.transform;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.loaders.NamedClassLoader;

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
        && "()Lorg/apache/catalina/loader/WebappClassLoader;".equals(desc)) { return new CreateClassLoaderAdapter(mv); }
    return mv;
  }

  private static class CreateClassLoaderAdapter extends MethodAdapter implements Opcodes {

    public CreateClassLoaderAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitInsn(int opcode) {
      if (ARETURN == opcode) {
        // name the web app loader
        mv.visitInsn(DUP);
        mv.visitTypeInsn(CHECKCAST, NamedClassLoader.CLASS);
        mv.visitFieldInsn(GETSTATIC, "com/tc/object/loaders/Namespace", "TOMCAT_NAMESPACE", "Ljava/lang/String;");
        mv.visitLdcInsn("context:");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEINTERFACE, "org/apache/catalina/Loader", "getContainer",
                           "()Lorg/apache/catalina/Container;");
        mv.visitMethodInsn(INVOKEINTERFACE, "org/apache/catalina/Container", "getName", "()Ljava/lang/String;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;");
        mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/loaders/Namespace", "createLoaderName",
                           "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
        mv.visitMethodInsn(INVOKEINTERFACE, NamedClassLoader.CLASS, "__tc_setClassLoaderName", "(Ljava/lang/String;)V");

        // register the web app loader
        mv.visitInsn(DUP);
        mv.visitTypeInsn(CHECKCAST, NamedClassLoader.CLASS);
        mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper",
                           "registerGlobalLoader", "(" + NamedClassLoader.TYPE + ")V");
      }
      super.visitInsn(opcode);
    }

  }

}
