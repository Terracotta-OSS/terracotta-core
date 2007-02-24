/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.plugins.cglib_2_1_3;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class CGLibProxyEnhancerAdapter extends ClassAdapter implements ClassAdapterFactory {

  public CGLibProxyEnhancerAdapter() {
    super(null);
  }
  
  private CGLibProxyEnhancerAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }
  
  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new CGLibProxyEnhancerAdapter(visitor, loader);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    // public final Object intercept(Object object, Method method, Object args[], MethodProxy proxy)

    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if ("getMethods".equals(name) && "(Ljava/lang/Class;[Ljava/lang/Class;Ljava/util/List;Ljava/util/List;Ljava/util/Set;)V".equals(desc)) {
      return new InterceptAdapter(mv);
    }

    return mv;

  }

  private static class InterceptAdapter extends MethodAdapter implements Opcodes {

    public InterceptAdapter(MethodVisitor mv) {
      super(mv);
    }
    
    public void visitInsn(int opcode) {
      if (RETURN == opcode) {
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKESTATIC, "com/tc/util/FilterTCClassPredicate", "filter", "(Ljava/util/Collection;)V");
      }
      super.visitInsn(opcode);
    }
  }
    
}
