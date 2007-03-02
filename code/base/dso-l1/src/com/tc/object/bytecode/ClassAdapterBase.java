/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.FieldVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.asm.commons.LocalVariablesSorter;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.object.Portability;
import com.tc.object.config.TransparencyClassSpec;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Common base class for Terracotta class adapters
 */
public abstract class ClassAdapterBase extends ClassAdapter implements Opcodes {
  public static final String          MANAGED_FIELD_NAME                      = ByteCodeUtil.TC_FIELD_PREFIX
                                                                                + "MANAGED";
  public static final String          MANAGED_FIELD_TYPE                      = "Lcom/tc/object/TCObject;";
  public static final String          MANAGED_METHOD                          = ByteCodeUtil.TC_METHOD_PREFIX
                                                                                + "managed";
  public static final String          IS_MANAGED_METHOD                       = ByteCodeUtil.TC_METHOD_PREFIX
                                                                                + "isManaged";
  public static final String          IS_MANAGED_DESCRIPTION                  = "()Z";

  public static final String          VALUES_GETTER                           = ByteCodeUtil.VALUES_GETTER;
  public static final String          VALUES_GETTER_DESCRIPTION               = ByteCodeUtil.VALUES_GETTER_DESCRIPTION;
  public static final String          VALUES_SETTER                           = ByteCodeUtil.VALUES_SETTER;
  public static final String          VALUES_SETTER_DESCRIPTION               = ByteCodeUtil.VALUES_SETTER_DESCRIPTION;
  public static final String          MANAGED_VALUES_GETTER                   = ByteCodeUtil.MANAGED_VALUES_GETTER;
  public static final String          MANAGED_VALUES_GETTER_DESCRIPTION       = ByteCodeUtil.MANAGED_VALUES_GETTER_DESCRIPTION;
  public static final String          MANAGED_VALUES_SETTER                   = ByteCodeUtil.MANAGED_VALUES_SETTER;

  private static final String         LOGICAL_TYPE_DELEGATE_FIELD_NAME_PREFIX = "__delegate_tc_";
  private static final int            LOGICAL_TYPE_DELEGATE_FIELD_MODIFIER    = ACC_PRIVATE + ACC_TRANSIENT;

  private final Map                   fields                                  = new HashMap();
  protected final InstrumentationSpec spec;
  private final Portability           portability;
  private boolean                     hasVisitedDelegateField                 = false;

  public static String getDelegateFieldName(String logicalExtendingClassName) {
    return LOGICAL_TYPE_DELEGATE_FIELD_NAME_PREFIX
           + logicalExtendingClassName.replace('.', '_').replace('/', '_').replace('$', '_');
  }

  public static boolean isDelegateFieldName(String fieldName) {
    return fieldName.startsWith(LOGICAL_TYPE_DELEGATE_FIELD_NAME_PREFIX);
  }

  protected TransparencyClassSpec getTransparencyClassSpec() {
    return spec.getTransparencyClassSpec();
  }

  protected InstrumentationSpec getInstrumentationSpec() {
    return spec;
  }

  private boolean isRoot(String fieldName) {
    return getTransparencyClassSpec().isRootInThisClass(fieldName);
  }

  public ClassAdapterBase(ClassInfo classInfo,  TransparencyClassSpec spec, ClassVisitor delegate, ManagerHelper mgrHelper,
                          ClassLoader caller, Portability p) {
    super(delegate);
    this.portability = p;
    this.spec = new InstrumentationSpec(classInfo, spec, mgrHelper, caller);
  }

  public final void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    spec.initialize(version, access, name, signature, superName, interfaces, portability);

    if (spec.isClassNotAdaptable()) {
      super.visit(version, access, name, signature, superName, interfaces);
      return;
    }

    if (spec.isClassPortable()) {
      interfaces = getNewInterfacesForPortableObject(interfaces);
    }

    basicVisit(version, access, name, signature, superName, interfaces);
  }

  private void visitDelegateFieldIfNecessary() {
    if (!hasVisitedDelegateField) {
      hasVisitedDelegateField = true;
      String superClassNameSlashes = spec.getSuperClassNameSlashes();
      String delegateFieldName = getDelegateFieldName(superClassNameSlashes);
      String delegateFieldType = "L" + superClassNameSlashes + ";";
      visitField(LOGICAL_TYPE_DELEGATE_FIELD_MODIFIER, delegateFieldName, delegateFieldType, null, null);
    }
  }

  private String[] getNewInterfacesForPortableObject(String[] interfaces) {
    Set ifaces = new LinkedHashSet(Arrays.asList(interfaces));
    if (!ifaces.contains(Manageable.CLASS)) {
      ifaces.add(Manageable.CLASS);
    }

    if (!spec.isLogical() && !ifaces.contains(TransparentAccess.CLASS)) {
      ifaces.add(TransparentAccess.CLASS);
    }

    return (String[]) ifaces.toArray(interfaces);
  }

  public final FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    spec.handleSubclassOfLogicalClassWithFieldsIfNecessary(access);
    if (spec.needDelegateField()) {
      visitDelegateFieldIfNecessary();
    }

    spec.recordExistingFields(name, desc, signature);

    if (spec.isClassNotAdaptable() || name.startsWith(ByteCodeUtil.TC_FIELD_PREFIX)) { return super
        .visitField(access, name, desc, signature, value); }
    if (!Modifier.isStatic(access)) {
      fields.put(name, desc);
    }
    return basicVisitField(access, name, desc, signature, value);
  }

  /**
   * This MethodAdapter is to instrument the constructor for a subclass of Logical class which contains one or more
   * field. In such situation, we need to instantiate the added delegate field in the constructor.
   */
  private class LogicalInitMethodAdapter extends LocalVariablesSorter implements Opcodes {
    private int[] localVariablesForMethodCall;

    public LogicalInitMethodAdapter(int access, String methodDesc, MethodVisitor mv) {
      super(access, methodDesc, mv);

    }

    private void storeStackValuesToLocalVariables(String methodInsnDesc) {
      Type[] types = Type.getArgumentTypes(methodInsnDesc);
      localVariablesForMethodCall = new int[types.length];
      for (int i = 0; i < types.length; i++) {
        localVariablesForMethodCall[i] = newLocal(types[i].getSize());
      }
      for (int i = types.length - 1; i >= 0; i--) {
        super.visitVarInsn(types[i].getOpcode(ISTORE), localVariablesForMethodCall[i]);
      }
    }

    private void loadLocalVariables(String methodInsnDesc) {
      Type[] types = Type.getArgumentTypes(methodInsnDesc);
      for (int i = 0; i < types.length; i++) {
        super.visitVarInsn(types[i].getOpcode(ILOAD), localVariablesForMethodCall[i]);
      }
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      String superClassNameSlashes = spec.getSuperClassNameSlashes();
      if (INVOKESPECIAL == opcode && owner.equals(superClassNameSlashes) && "<init>".equals(name)) {
        storeStackValuesToLocalVariables(desc);
        loadLocalVariables(desc);
        super.visitMethodInsn(opcode, owner, name, desc);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, spec.getSuperClassNameSlashes());
        mv.visitInsn(DUP);
        loadLocalVariables(desc);

        String delegateFieldName = getDelegateFieldName(superClassNameSlashes);
        mv.visitMethodInsn(INVOKESPECIAL, superClassNameSlashes, "<init>", desc);
        mv.visitMethodInsn(INVOKESPECIAL, spec.getClassNameSlashes(),
                           ByteCodeUtil.fieldSetterMethod(delegateFieldName), "(L" + superClassNameSlashes + ";)V");

      } else {
        super.visitMethodInsn(opcode, owner, name, desc);
      }
    }
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    spec.shouldProceedInstrumentation(access, name, desc);

    if (spec.isClassNotAdaptable()) { return super.visitMethod(access, name, desc, signature, exceptions); }
    MethodVisitor mv;

    spec.recordExistingMethods(name, desc, signature);

    if (spec.shouldVisitMethod(access, name)) {
      mv = basicVisitMethod(access, name, desc, signature, exceptions);
      if (spec.hasDelegatedToLogicalClass()) {
        String logicalExtendingClassName = spec.getSuperClassNameSlashes();
        mv = new LogicalClassSerializationAdapter.LogicalSubclassSerializationMethodAdapter(mv, name + desc, spec
            .getClassNameSlashes(), logicalExtendingClassName, getDelegateFieldName(logicalExtendingClassName));
      }
    } else {
      mv = super.visitMethod(access, name, desc, signature, exceptions);
    }

    if (spec.hasDelegatedToLogicalClass() && "<init>".equals(name)) {
      mv = new LogicalInitMethodAdapter(access, desc, mv);
    }

    return mv;
  }

  private void revisitLogicalSubclassDefinition() {
    spec.moveToLogicalIfNecessary();

    String[] interfaces = spec.getClassInterfaces();
    interfaces = getNewInterfacesForPortableObject(interfaces);

    basicVisit(spec.getClassVersion(), spec.getClassAccess(), spec.getClassNameSlashes(), spec.getClassSignature(),
               spec.getSuperClassNameSlashes(), interfaces);
  }

  public final void visitEnd() {
    if (spec.isClassNotAdaptable()) {
      super.visitEnd();
      return;
    }

    if (spec.isSubclassofLogicalClass()) {
      // If this is a subclass of a logical class, we need to determine if we need the
      // TransparentAccess interface, depending if the subclass has defined any field or
      // not.
      revisitLogicalSubclassDefinition();
    }

    // We will add overriding method if there is any if this is a subclass of a logical
    // class.
    addOverridenLogicalMethods();

    addRetrieveValuesMethod();
    addSetValueMethod();
    addRetrieveManagedValueMethod();
    addSetManagedValueMethod();

    // Condition for generating __tc_managed() method and $__tc_managed field should be the same.
    if (spec.isManagedFieldNeeded() && spec.generateNonStaticTCFields()) {
      addCachedManagedMethods();

      visitField(ACC_PRIVATE | ACC_VOLATILE | ACC_TRANSIENT | ACC_SYNTHETIC, MANAGED_FIELD_NAME, MANAGED_FIELD_TYPE,
                 null, null);
    }

    basicVisitEnd();
  }

  private void addOverridenLogicalMethods() {
    Collection needToOverrideMethods = spec.getShouldOverrideMethods();
    for (Iterator i = needToOverrideMethods.iterator(); i.hasNext();) {
      Method m = (Method) i.next();
      int modifier = m.getModifiers();
      String methodName = m.getName();
      if (!spec.shouldVisitMethod(modifier, methodName) || Modifier.isFinal(modifier)) {
        continue;
      }

      String methodDesc = Type.getMethodDescriptor(m);
      Type returnType = Type.getReturnType(m);

      Class[] exceptionTypes = m.getExceptionTypes();
      String[] exceptions = new String[exceptionTypes.length];
      for (int j = 0; j < exceptionTypes.length; j++) {
        exceptions[j] = Type.getInternalName(exceptionTypes[j]);
      }

      String logicalExtendingClassName = spec.getSuperClassNameSlashes();

      MethodVisitor mv = cv.visitMethod(modifier & ~ACC_ABSTRACT, methodName, methodDesc, null, exceptions);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESPECIAL, spec.getClassNameSlashes(), ByteCodeUtil
          .fieldGetterMethod(getDelegateFieldName(logicalExtendingClassName)), "()L" + logicalExtendingClassName + ";");
      ByteCodeUtil.pushMethodArguments(ACC_PUBLIC, methodDesc, mv);
      mv.visitMethodInsn(INVOKEVIRTUAL, logicalExtendingClassName, methodName, methodDesc);

      mv.visitInsn(returnType.getOpcode(IRETURN));
      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }
    if (spec.hasDelegatedToLogicalClass()) {
      addReadObjectMethod();
      addWriteObjectMethod();
      addSerializationOverrideMethod();
    }
  }

  private void addSerializationOverrideMethod() {
    LogicalClassSerializationAdapter.addCheckSerializationOverrideMethod(cv, true);
  }

  private void addWriteObjectMethod() {
    if (!spec.isWriteObjectMethodNeeded()) { return; }
    String logicalExtendingClassName = spec.getSuperClassNameSlashes();

    MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, "writeObject", "(Ljava/io/ObjectOutputStream;)V", null,
                                      new String[] { "java/io/IOException" });
    mv.visitCode();
    LogicalClassSerializationAdapter.addDelegateFieldWriteObjectCode(mv, spec.getClassNameSlashes(),
                                                                     logicalExtendingClassName,
                                                                     getDelegateFieldName(logicalExtendingClassName));
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addReadObjectMethod() {
    if (!spec.isReadObjectMethodNeeded()) { return; }
    String logicalExtendingClassName = spec.getSuperClassNameSlashes();

    MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, "readObject", "(Ljava/io/ObjectInputStream;)V", null, new String[] {
        "java/io/IOException", "java/lang/ClassNotFoundException" });
    mv.visitCode();
    LogicalClassSerializationAdapter.addDelegateFieldReadObjectCode(mv, spec.getClassNameSlashes(),
                                                                    logicalExtendingClassName,
                                                                    getDelegateFieldName(logicalExtendingClassName));
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addCachedManagedMethods() {
    if (spec.isManagedMethodsNeeded()) {
      // add getter
      MethodVisitor mv = visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, MANAGED_METHOD, "()" + MANAGED_FIELD_TYPE, null, null);
      if (spec.generateNonStaticTCFields()) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, spec.getClassNameSlashes(), MANAGED_FIELD_NAME, MANAGED_FIELD_TYPE);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
      } else {
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
      }

      // add setter
      mv = visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, MANAGED_METHOD, "(" + MANAGED_FIELD_TYPE + ")V", null, null);
      if (spec.generateNonStaticTCFields()) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, spec.getClassNameSlashes(), MANAGED_FIELD_NAME, MANAGED_FIELD_TYPE);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
      } else {
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
      }

      // add isManaged() method
      // XXX::FIXME:: This method need to handle TCClonableObjects and TCNonDistributableObjects
      mv = visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, IS_MANAGED_METHOD, IS_MANAGED_DESCRIPTION, null, null);
      if (spec.generateNonStaticTCFields()) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, spec.getClassNameSlashes(), MANAGED_FIELD_NAME, MANAGED_FIELD_TYPE);
        Label l1 = new Label();
        mv.visitJumpInsn(IFNULL, l1);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(IRETURN);
        mv.visitLabel(l1);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(1, 1);
      } else {
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(1, 1);
      }
    }
  }

  /**
   * Creates a method that takes all of the fields in the class and the super classes and puts them into the passed in
   * hashmap
   */
  private void addRetrieveValuesMethod() {
    if (spec.isValuesGetterMethodNeeded()) {
      MethodVisitor mv = visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, VALUES_GETTER, VALUES_GETTER_DESCRIPTION, null, null);
      if (!portability.isInstrumentationNotNeeded(spec.getSuperClassNameDots())
          && getTransparencyClassSpec().hasPhysicallyPortableSpecs(spec.getClassInfo().getSuperclass())) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, spec.getSuperClassNameSlashes(), VALUES_GETTER, VALUES_GETTER_DESCRIPTION);
      }

      for (Iterator i = fields.keySet().iterator(); i.hasNext();) {
        String fieldName = (String) i.next();
        String fieldDescription = (String) fields.get(fieldName);

        mv.visitVarInsn(ALOAD, 1);
        mv.visitLdcInsn(spec.getClassNameDots() + "." + fieldName);
        addValueToStackForField(mv, fieldName, fieldDescription);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put",
                           "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        mv.visitInsn(POP);
      }
      mv.visitInsn(RETURN);
      mv.visitMaxs(0, 0);
    }
  }

  private void addRetrieveManagedValueMethod() {
    if (spec.isManagedValuesGetterMethodNeeded()) {
      MethodVisitor mv = visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, MANAGED_VALUES_GETTER,
                                     MANAGED_VALUES_GETTER_DESCRIPTION, null, null);

      for (Iterator i = fields.keySet().iterator(); i.hasNext();) {
        String fieldName = (String) i.next();
        String fieldDescription = (String) fields.get(fieldName);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitLdcInsn(spec.getClassNameDots() + "." + fieldName);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z");
        Label l1 = new Label();
        mv.visitJumpInsn(IFEQ, l1);

        if (ByteCodeUtil.isSynthetic(fieldName)) {
          addValueToStackForField(mv, fieldName, fieldDescription);
        } else {
          addManagedValueToStackForField(mv, fieldName, fieldDescription);
        }
        mv.visitInsn(ARETURN);
        mv.visitLabel(l1);
      }

      
      if (!portability.isInstrumentationNotNeeded(spec.getSuperClassNameDots())
          && getTransparencyClassSpec().hasPhysicallyPortableSpecs(spec.getClassInfo().getSuperclass())) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, spec.getSuperClassNameSlashes(), MANAGED_VALUES_GETTER,
                           MANAGED_VALUES_GETTER_DESCRIPTION);
      } else {
        mv.visitInsn(ACONST_NULL);
      }
      mv.visitInsn(ARETURN);
      mv.visitMaxs(0, 0);
    }
  }

  /**
   * Creates a method that allows the setting of any field in this class or it's super classes.
   */
  private void addSetValueMethod() {
    if (spec.isValuesSetterMethodNeeded()) {
      MethodVisitor mv = visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, VALUES_SETTER, VALUES_SETTER_DESCRIPTION, null, null);
      Label l1 = new Label();
      for (Iterator i = fields.keySet().iterator(); i.hasNext();) {
        String fieldName = (String) i.next();
        if (ByteCodeUtil.isTCSynthetic(fieldName)) {
          continue;
        }

        String fieldDescription = (String) fields.get(fieldName);
        Type t = Type.getType(fieldDescription);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitLdcInsn(spec.getClassNameDots() + "." + fieldName);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z");
        Label l2 = new Label();
        mv.visitJumpInsn(IFEQ, l2);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 2);
        if (t.getSort() != Type.OBJECT && t.getSort() != Type.ARRAY) {
          mv.visitTypeInsn(CHECKCAST, ByteCodeUtil.sortToWrapperName(t.getSort()));
          mv.visitMethodInsn(INVOKEVIRTUAL, ByteCodeUtil.sortToWrapperName(t.getSort()), ByteCodeUtil
              .sortToPrimitiveMethodName(t.getSort()), "()" + fieldDescription);
        } else {
          mv.visitTypeInsn(CHECKCAST, convertToCheckCastDesc(fieldDescription));
        }
        mv.visitFieldInsn(PUTFIELD, spec.getClassNameSlashes(), fieldName, fieldDescription);
        mv.visitJumpInsn(GOTO, l1);
        mv.visitLabel(l2);
      }
      if (!portability.isInstrumentationNotNeeded(spec.getSuperClassNameDots())
          && getTransparencyClassSpec().hasPhysicallyPortableSpecs(spec.getClassInfo().getSuperclass())) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKESPECIAL, spec.getSuperClassNameSlashes(), VALUES_SETTER, VALUES_SETTER_DESCRIPTION);
      }
      mv.visitLabel(l1);

      mv.visitInsn(RETURN);
      mv.visitMaxs(0, 0);
    }
  }

  private void addSetManagedValueMethod() {
    if (spec.isManagedValuesSetterMethodNeeded()) {
      MethodVisitor mv = visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, MANAGED_VALUES_SETTER, VALUES_SETTER_DESCRIPTION,
                                     null, null);

      Label l1 = new Label();
      for (Iterator i = fields.keySet().iterator(); i.hasNext();) {
        String fieldName = (String) i.next();
        if (ByteCodeUtil.isSynthetic(fieldName)) {
          continue;
        }

        String fieldDescription = (String) fields.get(fieldName);
        Type t = Type.getType(fieldDescription);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitLdcInsn(spec.getClassNameDots() + "." + fieldName);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z");
        Label l2 = new Label();
        mv.visitJumpInsn(IFEQ, l2);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 2);
        if (t.getSort() != Type.OBJECT && t.getSort() != Type.ARRAY) {
          mv.visitTypeInsn(CHECKCAST, ByteCodeUtil.sortToWrapperName(t.getSort()));
          mv.visitMethodInsn(INVOKEVIRTUAL, ByteCodeUtil.sortToWrapperName(t.getSort()), ByteCodeUtil
              .sortToPrimitiveMethodName(t.getSort()), "()" + fieldDescription);
        } else {
          mv.visitTypeInsn(CHECKCAST, convertToCheckCastDesc(fieldDescription));
        }
        mv.visitMethodInsn(INVOKEVIRTUAL, spec.getClassNameSlashes(), ByteCodeUtil.fieldSetterMethod(fieldName),
                           "(" + fieldDescription + ")V");
        mv.visitJumpInsn(GOTO, l1);
        mv.visitLabel(l2);
      }

      if (!portability.isInstrumentationNotNeeded(spec.getSuperClassNameDots())
          && getTransparencyClassSpec().hasPhysicallyPortableSpecs(spec.getClassInfo().getSuperclass())) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKESPECIAL, spec.getSuperClassNameSlashes(), MANAGED_VALUES_SETTER,
                           VALUES_SETTER_DESCRIPTION);
      }

      mv.visitLabel(l1);
      mv.visitInsn(RETURN);
      mv.visitMaxs(0, 0);
    }
  }

  protected String convertToCheckCastDesc(String desc) {
    if (desc.startsWith("[")) return desc;
    return desc.substring(1, desc.length() - 1);
  }

  private void addValueToStackForField(MethodVisitor mv, String fieldName, String fieldDescription) {
    Type t = Type.getType(fieldDescription);

    if (t.getSort() == Type.OBJECT || t.getSort() == Type.ARRAY) {
      ByteCodeUtil.pushThis(mv);
      mv.visitFieldInsn(GETFIELD, spec.getClassNameSlashes(), fieldName, fieldDescription);
    } else {
      mv.visitTypeInsn(NEW, ByteCodeUtil.sortToWrapperName(t.getSort()));
      mv.visitInsn(DUP);
      ByteCodeUtil.pushThis(mv);
      mv.visitFieldInsn(GETFIELD, spec.getClassNameSlashes(), fieldName, fieldDescription);
      mv.visitMethodInsn(INVOKESPECIAL, ByteCodeUtil.sortToWrapperName(t.getSort()), "<init>", "(" + fieldDescription
                                                                                               + ")V");
    }
  }

  private void addManagedValueToStackForField(MethodVisitor mv, String fieldName, String fieldDescription) {
    Type t = Type.getType(fieldDescription);
    if (t.getSort() == Type.OBJECT || t.getSort() == Type.ARRAY) {
      ByteCodeUtil.pushThis(mv);
      mv.visitMethodInsn(INVOKESPECIAL, spec.getClassNameSlashes(), ByteCodeUtil.fieldGetterMethod(fieldName),
                         "()" + fieldDescription);
    } else {
      mv.visitTypeInsn(NEW, ByteCodeUtil.sortToWrapperName(t.getSort()));
      mv.visitInsn(DUP);
      ByteCodeUtil.pushThis(mv);
      if (isRoot(fieldName)) {
        mv.visitMethodInsn(INVOKESPECIAL, spec.getClassNameSlashes(), ByteCodeUtil.fieldGetterMethod(fieldName),
                           "()" + fieldDescription);
      } else {
        mv.visitFieldInsn(GETFIELD, spec.getClassNameSlashes(), fieldName, fieldDescription);
      }
      mv.visitMethodInsn(INVOKESPECIAL, ByteCodeUtil.sortToWrapperName(t.getSort()), "<init>", "(" + fieldDescription
                                                                                               + ")V");
    }
  }

  protected void basicVisit(int version, int access, String name, String signature, String superName,
                            String[] interfaces) {
    // override me if you need to access to the standard visit() method
    cv.visit(version, access, name, signature, superName, interfaces);
  }

  protected FieldVisitor basicVisitField(int access, String name, String desc, String signature, Object value) {
    // override me if you need to access to the standard visitField() method
    return cv.visitField(access, name, desc, signature, value);
  }

  protected MethodVisitor basicVisitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    // override me if you need to access to the standard visitMethod() method
    return cv.visitMethod(access, name, desc, signature, exceptions);
  }

  protected void basicVisitEnd() {
    // override me if you need to access to the standard visitMethod() method
    cv.visitEnd();
  }

}
