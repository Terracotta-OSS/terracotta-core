package org.terracotta.modules.ehcache_1_3;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class EhcacheLruMemoryStoreAdapter extends ClassAdapter 
	implements ClassAdapterFactory, Opcodes, IConstants {
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
		if (this.className.equals(LRUMEMORYSTORE_CLASS_NAME_SLASH)
				&& name.equals("loadMapInstance")
				&& desc.equals("()Ljava/util/Map;")) {
		    mv = recreateLoadMapInstanceMethod(mv);
		}
		return mv;
	}

	/**
	 * Replace the LruMemoryStore.loadMapInstanceMethod(..) so that it will always
	 * return a Map interface referring to a java.util.HashMap instance.
	 * @param mv
	 * @return
	 */
	private MethodVisitor recreateLoadMapInstanceMethod(MethodVisitor mv) {
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitLineNumber(64, l0);
		mv.visitTypeInsn(NEW, "java/util/HashMap");
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V");
		mv.visitInsn(ARETURN);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", "Lnet/sf/ehcache/store/LruMemoryStore;", null, l0, l1, 0);
		mv.visitMaxs(2, 1);
		mv.visitEnd();
		return null;
	}

	/**
	 * Force the LruMemoryStore.loadMapInstanceMethod(..) so that it will always
	 * return a Map interface referring to a LruMemoryStore.SpoolingLRUMap instance.
	 * @deprecated
	 */
	private static class LruMemoryStoreLoadMapInstanceMethodAdapter extends
			MethodAdapter implements Opcodes {

		public LruMemoryStoreLoadMapInstanceMethodAdapter(MethodVisitor mv) {
			super(mv);
		}

		public void visitLdcInsn(Object cst) {
			if (cst instanceof java.lang.String) {
				if (cst.toString().equals(LINKEDHASHMAP_CLASS_NAME_DOTS)) {
					cst = LRUMAP_CLASS_NAME_DOTS;
				} else if (cst.toString().equals(
						" Cache: Using SpoolingLinkedHashMap implementation")) {
					cst = " Cache: Using SpoolingLRUMap implementation";
				} else if (cst.toString().equals(
						" Cache: Cannot find " + LINKEDHASHMAP_CLASS_NAME_DOTS)) {
					cst = " Cache: Cannot find " + LRUMAP_CLASS_NAME_DOTS;
				}
			}
			super.visitLdcInsn(cst);
		}

		public void visitTypeInsn(int opcode, String desc) {
			if ((opcode == NEW)
					&& (desc.equals(SPOOLINGLINKEDHASHMAP_CLASS_NAME_SLASH))) {
				desc = SPOOLINGLRUMAP_CLASS_NAME_SLASH;
			}
			super.visitTypeInsn(opcode, desc);
		}
		
		public void visitMethodInsn(int opcode, String owner, String name,
				String desc) {
			if ((opcode == INVOKESPECIAL)
					&& owner.equals(SPOOLINGLINKEDHASHMAP_CLASS_NAME_SLASH)
					&& (name.equals("<init>") && desc
							.equals("(Lnet/sf/ehcache/store/LruMemoryStore;)V"))) {
				owner = SPOOLINGLRUMAP_CLASS_NAME_SLASH;
			}
			super.visitMethodInsn(opcode, owner, name, desc);
		}
	}
}
