/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.surefire_2_3;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
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

  @Override
  public MethodVisitor visitMethod(int access, final String name, final String desc, String signature,
                                   String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if (name.equals("<init>")) { return new MethodAdapter(mv) {
      @Override
      public void visitInsn(int opcode) {
        if (opcode == RETURN) {
          if (Type.getArgumentTypes(desc).length > 0) {
//            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
//            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
//            mv.visitInsn(DUP);
//            mv.visitLdcInsn("### isolated class loader " + name + desc + " ");
//            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
//            mv.visitVarInsn(ALOAD, 0);
//            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
//                               "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
//            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
//            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
//
//            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
//            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
//            mv.visitInsn(DUP);
//            mv.visitLdcInsn("### isolated class loader parent ");
//            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
//            mv.visitVarInsn(ALOAD, 1);
//            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
//                               "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
//            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
//            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
//
//            mv.visitTypeInsn(NEW, "java/lang/Throwable");
//            mv.visitInsn(DUP);
//            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Throwable", "<init>", "()V");
//            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V");

            mv.visitVarInsn(ALOAD, 1);
            Label l1 = new Label();
            mv.visitJumpInsn(IFNONNULL, l1);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(CHECKCAST, "com/tc/object/loaders/NamedClassLoader");
            mv.visitInsn(ACONST_NULL); // this is not a webapp context classloader
            mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper",
                               "registerGlobalLoader", "(Lcom/tc/object/loaders/NamedClassLoader;Ljava/lang/String;)V");

            mv.visitLabel(l1);
          }
        }
        super.visitInsn(opcode);
      }
    }; }

    return mv;
  }

  @Override
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
