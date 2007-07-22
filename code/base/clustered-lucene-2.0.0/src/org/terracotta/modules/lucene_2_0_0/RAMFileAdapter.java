package org.terracotta.modules.lucene_2_0_0;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.FieldVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

public class RAMFileAdapter extends ClassAdapter implements ClassAdapterFactory, Opcodes {

  public static final String BUFFERS_FIELD   = "buffers";

  private final Set          vectorDelegates = new HashSet();
  private final Map          fields          = new HashMap();
  private String             className;

  public RAMFileAdapter(ClassVisitor cv) {
    super(cv);

    // Only delegate the needed methods on the "buffers" vector
    vectorDelegates.add("elementAt(I)Ljava/lang/Object;");
    vectorDelegates.add("addElement(Ljava/lang/Object;)V");
    vectorDelegates.add("size()I");
  }

  public RAMFileAdapter() {
    super(null);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new RAMFileAdapter(visitor);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    this.className = name;
  }

  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    if (!name.startsWith(ByteCodeUtil.TC_FIELD_PREFIX) && !Modifier.isStatic(access)) {
      // make all fields private -- one of the purposes of this adapter is to make all field access go through methods.
      // Making fields private will let us know if we missed an outside class
      access &= ~ACC_PUBLIC;
      access &= ~ACC_PROTECTED;
      access |= ACC_PRIVATE;

      fields.put(name, desc);
    }

    return super.visitField(access, name, desc, signature, value);
  }

  public void visitEnd() {
    addGetterSetter();
    addBufferDelegates();
    super.visitEnd();
  }

  private void addBufferDelegates() {
    String desc = (String) fields.get(BUFFERS_FIELD);

    if (desc != null & desc.equals("Ljava/util/Vector;")) {
      Method[] vectorMetods = Vector.class.getMethods();
      for (int i = 0; i < vectorMetods.length; i++) {
        Method method = vectorMetods[i];
        if (Modifier.isStatic(method.getModifiers())) {
          continue;
        }

        String methodName = method.getName();
        String methodDesc = Type.getMethodDescriptor(method);

        if (methodName.startsWith(ByteCodeUtil.TC_METHOD_PREFIX)) {
          continue;
        }

        if (!vectorDelegates.contains(methodName + methodDesc)) {
          continue;
        }

        MethodVisitor mv = super.visitMethod(ACC_PUBLIC, BUFFERS_FIELD + methodName, methodDesc, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, BUFFERS_FIELD, desc);

        int index = 1;
        Type[] args = Type.getArgumentTypes(methodDesc);
        for (int j = 0; j < args.length; j++) {
          mv.visitVarInsn(args[j].getOpcode(ILOAD), index);
          index += args[j].getSize();
        }

        mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Vector", methodName, methodDesc);
        mv.visitInsn(Type.getReturnType(method).getOpcode(IRETURN));
        mv.visitCode();
        mv.visitMaxs(0, 0);
        mv.visitEnd();
      }
    }
  }

  private void addGetterSetter() {
    for (Iterator i = fields.entrySet().iterator(); i.hasNext();) {
      Map.Entry entry = (Entry) i.next();
      String name = (String) entry.getKey();
      String desc = (String) entry.getValue();

      Type type = Type.getType(desc);

      MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "set" + name, "(" + desc + ")V", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(type.getOpcode(ILOAD), 1);
      mv.visitFieldInsn(PUTFIELD, className, name, desc);
      mv.visitInsn(RETURN);
      mv.visitCode();
      mv.visitMaxs(0, 0);
      mv.visitEnd();

      mv = super.visitMethod(ACC_PUBLIC, "get" + name, "()" + desc, null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, className, name, desc);
      mv.visitInsn(type.getOpcode(IRETURN));
      mv.visitCode();
      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }

  }

}
