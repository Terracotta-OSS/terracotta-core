package org.terracotta.modules.ehcache_1_2_4;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class EhcacheMemoryStoreAdapter extends ClassAdapter implements
		ClassAdapterFactory, Opcodes {

	private static final String CLASS_NAME = "net/sf/ehcache/store/MemoryStore";

	private static final String METHOD_NAME = "isFull";

	private static final String METHOD_DESC = "()Z";

	private String className;

	public EhcacheMemoryStoreAdapter() {
		super(null);
	}

	private EhcacheMemoryStoreAdapter(ClassVisitor cv) {
		super(cv);
	}

	public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
		return new EhcacheMemoryStoreAdapter(visitor);
	}

	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		this.className = name;
	}

	public void visitEnd() {
		super.visitEnd();
	}

	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature,
				exceptions);
		/**
		if (className.equals(CLASS_NAME) 
				&& name.equals(METHOD_NAME)
				&& desc.equals(METHOD_DESC)) {
			mv =  foo(mv);
		} 
		*/
		
		return mv;
	}

	private MethodVisitor foo(MethodVisitor mv) {
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitLineNumber(372, l0);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitLdcInsn(".....................................");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLineNumber(373, l1);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
		mv.visitInsn(DUP);
		mv.visitLdcInsn("map: ");
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, "net/sf/ehcache/store/MemoryStore", "map", "Ljava/util/Map;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		Label l2 = new Label();
		mv.visitLabel(l2);
		mv.visitLineNumber(374, l2);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
		mv.visitInsn(DUP);
		mv.visitLdcInsn("cache: ");
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, "net/sf/ehcache/store/MemoryStore", "cache", "Lnet/sf/ehcache/Ehcache;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		Label l3 = new Label();
		mv.visitLabel(l3);
		mv.visitLineNumber(375, l3);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitLdcInsn(".....................................");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		Label l4 = new Label();
		mv.visitLabel(l4);
		mv.visitLineNumber(376, l4);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, "net/sf/ehcache/store/MemoryStore", "map", "Ljava/util/Map;");
		mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "size", "()I");
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, "net/sf/ehcache/store/MemoryStore", "cache", "Lnet/sf/ehcache/Ehcache;");
		mv.visitMethodInsn(INVOKEINTERFACE, "net/sf/ehcache/Ehcache", "getMaxElementsInMemory", "()I");
		Label l5 = new Label();
		mv.visitJumpInsn(IF_ICMPLE, l5);
		mv.visitInsn(ICONST_1);
		mv.visitInsn(IRETURN);
		mv.visitLabel(l5);
		mv.visitInsn(ICONST_0);
		mv.visitInsn(IRETURN);
		Label l6 = new Label();
		mv.visitLabel(l6);
		mv.visitLocalVariable("this", "Lnet/sf/ehcache/store/MemoryStore;", null, l0, l6, 0);
		mv.visitMaxs(4, 1);
		mv.visitEnd();		
		return null;
	}
}
