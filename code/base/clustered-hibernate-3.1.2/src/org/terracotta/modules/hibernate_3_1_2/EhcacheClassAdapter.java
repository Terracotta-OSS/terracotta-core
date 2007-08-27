/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.hibernate_3_1_2;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class EhcacheClassAdapter extends ClassAdapter implements ClassAdapterFactory, Opcodes {
  public EhcacheClassAdapter() {
    super(null);
  }

  private EhcacheClassAdapter(ClassVisitor cv) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new EhcacheClassAdapter(visitor);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    if ("destroy".equals(name) && "()V".equals(desc) && exceptions.length == 1
        && "org/hibernate/cache/CacheException".equals(exceptions[0])) {
      recreateDestroyMethod(access, name, desc, signature, exceptions);
      return null;
    } else {
      return super.visitMethod(access, name, desc, signature, exceptions);
    }
  }

  private void recreateDestroyMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(150, l0);
    mv.visitInsn(RETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lorg/hibernate/cache/EhCache;", null, l0, l1, 0);
    mv.visitMaxs(0, 1);
    mv.visitEnd();  }
}
