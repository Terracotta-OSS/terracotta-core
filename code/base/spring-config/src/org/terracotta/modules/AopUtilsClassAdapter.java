/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

/**
 * @author Eugene Kuleshov
 */
public class AopUtilsClassAdapter extends ClassAdapter implements ClassAdapterFactory, Opcodes {

  public AopUtilsClassAdapter() {
    super(null);
  }
  
  public AopUtilsClassAdapter(ClassVisitor cv) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new AopUtilsClassAdapter(visitor);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if("canApply".equals(name) && "(Lorg/springframework/aop/Pointcut;Ljava/lang/Class;Z)Z".equals(desc)) {
      return new MethodAdapter(mv) {
        public void visitMethodInsn(int opcode, String owner, String nameArg, String descArg) {
          super.visitMethodInsn(opcode, owner, nameArg, descArg);
          
          if(opcode==INVOKEVIRTUAL //
              && "java/lang/Class".equals(owner) //
              && "getMethods".equals(nameArg) //
              && "()[Ljava/lang/reflect/Method;".equals(descArg)) {
            super.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ByteCodeUtil", "purgeTCMethods", "([Ljava/lang/reflect/Method;)[Ljava/lang/reflect/Method;");
          }
        }
      };
    }
    return mv;
  }
  
}
