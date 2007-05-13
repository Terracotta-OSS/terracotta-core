package org.terracotta.modules.ehcache_1_3;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class EhcacheLruMemoryStoreAdapter extends ClassAdapter implements
		ClassAdapterFactory, Opcodes {

	private static final String TC_LINKEDHASHMAP_CLASS_NAME_DOTS = "com.tcclient.util.LinkedHashMap";

	private static final String TC_LINKEDHASHMAP_CLASS_NAME = "com/tcclient/util/LinkedHashMap";

	private static final String LINKEDHASHMAP_CLASS_NAME_DOTS = "java.util.LinkedHashMap";

	private static final String LINKEDHASHMAP_CLASS_NAME = "java/util/LinkedHashMap";

	private static final String LRUMEMORYSTORE_CLASS_NAME = "net/sf/ehcache/store/LruMemoryStore";

	private static final String SPOOLINGLINKEDHASHMAP_CLASS_NAME = "net/sf/ehcache/store/LruMemoryStore$SpoolingLinkedHashMap";

	private String className;

	public EhcacheLruMemoryStoreAdapter() {
		super(null);
	}

	private EhcacheLruMemoryStoreAdapter(ClassVisitor cv) {
		super(cv);
	}

	public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
		return new EhcacheLruMemoryStoreAdapter(visitor);
	}

	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
		if (name.equals(SPOOLINGLINKEDHASHMAP_CLASS_NAME)
				&& superName.equals(LINKEDHASHMAP_CLASS_NAME)) {
			superName = TC_LINKEDHASHMAP_CLASS_NAME;
		}
		this.className = name;
		super.visit(version, access, name, signature, superName, interfaces);
	}

	public void visitEnd() {
		super.visitEnd();
	}

	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature,
				exceptions);

		if (this.className.equals(SPOOLINGLINKEDHASHMAP_CLASS_NAME)
				&& name.equals("<init>")
				&& desc.equals("(Lnet/sf/ehcache/store/LruMemoryStore;)V")) {
			mv = new SpoolingLinkedHashMapConstructorAdapter(mv);
		}
		return mv;
	}

	private static class SpoolingLinkedHashMapConstructorAdapter extends
			MethodAdapter implements Opcodes {

		public SpoolingLinkedHashMapConstructorAdapter(MethodVisitor mv) {
			super(mv);
		}

		public void visitMethodInsn(final int opcode, String owner,
				final String name, final String desc) {
			if ((opcode == INVOKESPECIAL)
					&& owner.equals(LINKEDHASHMAP_CLASS_NAME)
					&& name.equals("<init>") && desc.equals("(IFZ)V")) {
				owner = TC_LINKEDHASHMAP_CLASS_NAME;
			}
			super.visitMethodInsn(opcode, owner, name, desc);
		}
	}
}
