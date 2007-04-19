package org.terracotta.modules.ehcache_1_2_4;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class EhcacheLruMemoryStoreAdapter extends ClassAdapter implements
		ClassAdapterFactory, Opcodes {

	private static final String CLASS_NAME = "net/sf/ehcache/store/LruMemoryStore$SpoolingLinkedHashMap";

	private static final String METHOD_NAME = "removeEldestEntry";

	private static final String METHOD_DESC = "(Ljava/util/Map$Entry;)Z";

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
			mv =  recreateRemoveEldestEntry(mv);
		} else if (className.equals(CLASS_NAME)
				&& name.equals("removeLeastRecentlyUsedElement")
				&& desc.equals("(Lnet/sf/ehcache/Element;)Z")) {
			mv = foo(mv);
		}
		*/
		
		return mv;
	}

	public MethodVisitor recreateRemoveEldestEntry(MethodVisitor mv) {
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitLineNumber(191, l0);
		mv.visitInsn(ACONST_NULL);
		mv.visitVarInsn(ASTORE, 2);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLineNumber(192, l1);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getValue", "()Ljava/lang/Object;");
		mv.visitVarInsn(ASTORE, 3);
		Label l2 = new Label();
		mv.visitLabel(l2);
		mv.visitLineNumber(193, l2);
		mv.visitVarInsn(ALOAD, 3);
		mv.visitTypeInsn(INSTANCEOF, "com/tc/object/ObjectID");
		Label l3 = new Label();
		mv.visitJumpInsn(IFEQ, l3);
		Label l4 = new Label();
		mv.visitLabel(l4);
		mv.visitLineNumber(194, l4);
		mv.visitVarInsn(ALOAD, 3);
		mv.visitTypeInsn(CHECKCAST, "com/tc/object/ObjectID");
		mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "lookupObject", "(Lcom/tc/object/ObjectID;)Ljava/lang/Object;");
		mv.visitTypeInsn(CHECKCAST, "net/sf/ehcache/Element");
		mv.visitVarInsn(ASTORE, 2);
		Label l5 = new Label();
		mv.visitLabel(l5);
		mv.visitLineNumber(195, l5);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitLdcInsn("--------------------------");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		Label l6 = new Label();
		mv.visitLabel(l6);
		mv.visitLineNumber(196, l6);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
		mv.visitInsn(DUP);
		mv.visitLdcInsn("objId  : ");
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
		mv.visitVarInsn(ALOAD, 3);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		Label l7 = new Label();
		mv.visitLabel(l7);
		mv.visitLineNumber(197, l7);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
		mv.visitInsn(DUP);
		mv.visitLdcInsn("element: ");
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
		mv.visitVarInsn(ALOAD, 2);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		Label l8 = new Label();
		mv.visitLabel(l8);
		mv.visitLineNumber(198, l8);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
		mv.visitInsn(DUP);
		mv.visitLdcInsn("         ");
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
		mv.visitVarInsn(ALOAD, 2);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		Label l9 = new Label();
		mv.visitLabel(l9);
		mv.visitLineNumber(199, l9);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitLdcInsn("--------------------------");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		Label l10 = new Label();
		mv.visitJumpInsn(GOTO, l10);
		mv.visitLabel(l3);
		mv.visitLineNumber(201, l3);
		mv.visitVarInsn(ALOAD, 3);
		mv.visitTypeInsn(CHECKCAST, "net/sf/ehcache/Element");
		mv.visitVarInsn(ASTORE, 2);
		mv.visitLabel(l10);
		mv.visitLineNumber(203, l10);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitMethodInsn(INVOKESPECIAL, "net/sf/ehcache/store/LruMemoryStore$SpoolingLinkedHashMap", "removeLeastRecentlyUsedElement", "(Lnet/sf/ehcache/Element;)Z");
		mv.visitInsn(IRETURN);
		Label l11 = new Label();
		mv.visitLabel(l11);
		mv.visitLocalVariable("this", "Lnet/sf/ehcache/store/LruMemoryStore$SpoolingLinkedHashMap;", null, l0, l11, 0);
		mv.visitLocalVariable("eldest", "Ljava/util/Map$Entry;", null, l0, l11, 1);
		mv.visitLocalVariable("element", "Lnet/sf/ehcache/Element;", null, l1, l11, 2);
		mv.visitLocalVariable("obj", "Ljava/lang/Object;", null, l2, l11, 3);
		mv.visitMaxs(4, 4);
		mv.visitEnd();
		return null;
	}
	
	private MethodVisitor foo(MethodVisitor mv) {
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitLineNumber(214, l0);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
		mv.visitInsn(DUP);
		mv.visitLdcInsn("element: ");
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLineNumber(215, l1);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitLdcInsn("checking if expired");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		Label l2 = new Label();
		mv.visitLabel(l2);
		mv.visitLineNumber(216, l2);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKEVIRTUAL, "net/sf/ehcache/Element", "isExpired", "()Z");
		Label l3 = new Label();
		mv.visitJumpInsn(IFEQ, l3);
		Label l4 = new Label();
		mv.visitLabel(l4);
		mv.visitLineNumber(217, l4);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitLdcInsn("it is expired - notifying expiration");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		Label l5 = new Label();
		mv.visitLabel(l5);
		mv.visitLineNumber(218, l5);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, "net/sf/ehcache/store/LruMemoryStore$SpoolingLinkedHashMap", "this$0", "Lnet/sf/ehcache/store/LruMemoryStore;");
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKEVIRTUAL, "net/sf/ehcache/store/LruMemoryStore", "notifyExpiry", "(Lnet/sf/ehcache/Element;)V");
		Label l6 = new Label();
		mv.visitLabel(l6);
		mv.visitLineNumber(219, l6);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitLdcInsn("notification complete");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		Label l7 = new Label();
		mv.visitLabel(l7);
		mv.visitLineNumber(220, l7);
		mv.visitInsn(ICONST_1);
		mv.visitInsn(IRETURN);
		mv.visitLabel(l3);
		mv.visitLineNumber(223, l3);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitLdcInsn("not expired");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		Label l8 = new Label();
		mv.visitLabel(l8);
		mv.visitLineNumber(224, l8);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitLdcInsn("checking if full");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		Label l9 = new Label();
		mv.visitLabel(l9);
		mv.visitLineNumber(225, l9);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, "net/sf/ehcache/store/LruMemoryStore$SpoolingLinkedHashMap", "this$0", "Lnet/sf/ehcache/store/LruMemoryStore;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "net/sf/ehcache/store/LruMemoryStore", "isFull", "()Z");
		Label l10 = new Label();
		mv.visitJumpInsn(IFEQ, l10);
		Label l11 = new Label();
		mv.visitLabel(l11);
		mv.visitLineNumber(226, l11);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitLdcInsn("it is full - evicting");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		Label l12 = new Label();
		mv.visitLabel(l12);
		mv.visitLineNumber(227, l12);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, "net/sf/ehcache/store/LruMemoryStore$SpoolingLinkedHashMap", "this$0", "Lnet/sf/ehcache/store/LruMemoryStore;");
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKEVIRTUAL, "net/sf/ehcache/store/LruMemoryStore", "evict", "(Lnet/sf/ehcache/Element;)V");
		Label l13 = new Label();
		mv.visitLabel(l13);
		mv.visitLineNumber(228, l13);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitLdcInsn("eviction complete");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		Label l14 = new Label();
		mv.visitLabel(l14);
		mv.visitLineNumber(229, l14);
		mv.visitInsn(ICONST_1);
		mv.visitInsn(IRETURN);
		mv.visitLabel(l10);
		mv.visitLineNumber(231, l10);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitLdcInsn("it is not full");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		Label l15 = new Label();
		mv.visitLabel(l15);
		mv.visitLineNumber(232, l15);
		mv.visitInsn(ICONST_0);
		mv.visitInsn(IRETURN);
		Label l16 = new Label();
		mv.visitLabel(l16);
		mv.visitLocalVariable("this", "Lnet/sf/ehcache/store/LruMemoryStore$SpoolingLinkedHashMap;", null, l0, l16, 0);
		mv.visitLocalVariable("element", "Lnet/sf/ehcache/Element;", null, l0, l16, 1);
		mv.visitMaxs(4, 2);
		mv.visitEnd();		
		return null;
	}
}
