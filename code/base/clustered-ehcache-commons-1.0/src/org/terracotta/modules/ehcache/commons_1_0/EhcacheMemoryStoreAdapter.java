package org.terracotta.modules.ehcache.commons_1_0;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class EhcacheMemoryStoreAdapter extends ClassAdapter implements ClassAdapterFactory, Opcodes {
  public EhcacheMemoryStoreAdapter() {
    super(null);
  }

  private EhcacheMemoryStoreAdapter(ClassVisitor cv) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new EhcacheMemoryStoreAdapter(visitor);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("create".equals(name)
        && ("(Lnet/sf/ehcache/Ehcache;Lnet/sf/ehcache/store/DiskStore;)Lnet/sf/ehcache/store/MemoryStore;".equals(desc) || "(Lnet/sf/ehcache/Ehcache;Lnet/sf/ehcache/store/Store;)Lnet/sf/ehcache/store/MemoryStore;"
            .equals(desc))) {
      mv = new MemoryStoreCreateMethodAdapter(mv);
    }
    return mv;
  }

  private static class MemoryStoreCreateMethodAdapter extends MethodAdapter implements Opcodes {

    public MemoryStoreCreateMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitCode() {
      super.visitCode();

      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEINTERFACE, "net/sf/ehcache/Ehcache", "getMemoryStoreEvictionPolicy",
                         "()Lnet/sf/ehcache/store/MemoryStoreEvictionPolicy;");
      mv.visitFieldInsn(GETSTATIC, "net/sf/ehcache/store/MemoryStoreEvictionPolicy", "DSO",
                        "Lnet/sf/ehcache/store/MemoryStoreEvictionPolicy;");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z");
      Label l1 = new Label();
      mv.visitJumpInsn(IFEQ, l1);
      mv.visitTypeInsn(NEW, "net/sf/ehcache/store/TimeExpiryMemoryStore");
      mv.visitInsn(DUP);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKESPECIAL, "net/sf/ehcache/store/TimeExpiryMemoryStore", "<init>",
                         "(Lnet/sf/ehcache/Ehcache;Lnet/sf/ehcache/store/Store;)V");
      mv.visitInsn(ARETURN);
      mv.visitLabel(l1);
    }
  }
}
