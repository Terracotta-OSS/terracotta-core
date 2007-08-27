/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.ehcache_1_2_4;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class EhcacheCacheManagerClassAdapter extends ClassAdapter implements ClassAdapterFactory, Opcodes {
  public EhcacheCacheManagerClassAdapter() {
    super(null);
  }

  private EhcacheCacheManagerClassAdapter(ClassVisitor cv) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new EhcacheCacheManagerClassAdapter(visitor);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("init".equals(name)
        && "(Lnet/sf/ehcache/config/Configuration;Ljava/lang/String;Ljava/net/URL;Ljava/io/InputStream;)V".equals(desc)) {
      mv = new InitMethodAdapter(mv);
    }
    return mv;
  }

  private static class InitMethodAdapter extends MethodAdapter implements Opcodes {
    public InitMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if (INVOKESPECIAL == opcode && "net/sf/ehcache/CacheManager".equals(owner) && "addShutdownHook".equals(name)
          && "()V".equals(desc)) {
        super.visitInsn(POP);
        return;
      } else {
        super.visitMethodInsn(opcode, owner, name, desc);
      }
    }
  }
}
