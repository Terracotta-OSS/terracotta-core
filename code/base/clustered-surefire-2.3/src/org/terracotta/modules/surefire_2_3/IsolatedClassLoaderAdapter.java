/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.surefire_2_3;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

/**
 * ClassLoader adapter for Surefire org.apache.maven.surefire.booter.IsolatedClassLoader 
 *
 * @author Eugene Kuleshov
 */
public class IsolatedClassLoaderAdapter extends ClassAdapter implements ClassAdapterFactory, Opcodes {

  public IsolatedClassLoaderAdapter() {
    super(null);
  }

  public IsolatedClassLoaderAdapter(ClassVisitor cv) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new IsolatedClassLoaderAdapter(visitor);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    
    if (name.equals("<init>")) {
      return new MethodAdapter(mv) {
        public void visitInsn(int opcode) {
          if(opcode==RETURN) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(CHECKCAST, "com/tc/object/loaders/NamedClassLoader");
            mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper", "registerGlobalLoader", "(Lcom/tc/object/loaders/NamedClassLoader;)V");
          }
          super.visitInsn(opcode);
        }
      };
    }

    return mv;
  }
  
  public void visitEnd() {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "__tc_getClassLoaderName", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    mv.visitLdcInsn("Maven.IsolatedClassLoader");
    mv.visitInsn(ARETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd();

    super.visitEnd();
  }

}
