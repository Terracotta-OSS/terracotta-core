/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.FieldVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.object.Portability;
import com.tc.object.config.TransparencyClassSpec;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

  private static final Set            ALWAYS_REWRITE_IF_PRESENT               = getAlwaysRewrite();

  private final Map                   fields                                  = new HashMap();
  protected final InstrumentationSpec spec;
  private final Portability           portability;
  private boolean                     hasVisitedDelegateField                 = false;

  public static String getDelegateFieldName(String logicalExtendingClassName) {
    return LOGICAL_TYPE_DELEGATE_FIELD_NAME_PREFIX
           + logicalExtendingClassName.replace('.', '_').replace('/', '_').replace('$', '_');
  }

  private static Set getAlwaysRewrite() {
    Set s = new HashSet();
    s.addAll(getInterfaceMethodDescriptions(Manageable.class));
    s.addAll(getInterfaceMethodDescriptions(TransparentAccess.class));
    return Collections.unmodifiableSet(s);
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
    FieldInfo fieldInfo = getInstrumentationSpec().getFieldInfo(fieldName);
    if (fieldInfo == null) { return false; }

    return getTransparencyClassSpec().isRootInThisClass(fieldInfo);
  }

  public ClassAdapterBase(ClassInfo classInfo, TransparencyClassSpec spec2, ClassVisitor delegate, ClassLoader caller,
                          Portability p) {
    super(delegate);
    this.portability = p;
    this.spec = new InstrumentationSpec(classInfo, spec2, caller);
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
    if (!ifaces.contains(ByteCodeUtil.MANAGEABLE_CLASS)) {
      ifaces.add(ByteCodeUtil.MANAGEABLE_CLASS);
    }

    if (!spec.isLogical() && !ifaces.contains(ByteCodeUtil.TRANSPARENT_ACCESS_CLASS)) {
      ifaces.add(ByteCodeUtil.TRANSPARENT_ACCESS_CLASS);
    }

    return (String[]) ifaces.toArray(interfaces);
  }

  public final FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    spec.handleSubclassOfLogicalClassWithFieldsIfNecessary(access, name);
    if (spec.needDelegateField()) {
      visitDelegateFieldIfNecessary();
    }

    // always add a known definition for this field if it already exists
    if (MANAGED_FIELD_NAME.equals(name) && !getTransparencyClassSpec().isIgnoreRewrite()) { return null; }

    spec.recordExistingFields(name, desc, signature);

    if (spec.isClassNotAdaptable() || name.startsWith(ByteCodeUtil.TC_FIELD_PREFIX)) { return super
        .visitField(access, name, desc, signature, value); }
    if (!Modifier.isStatic(access)) {
      fields.put(name, desc);
    }
    return basicVisitField(access, name, desc, signature, value);
  }

  public final MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    spec.shouldProceedInstrumentation(access, name, desc);

    if (spec.isClassNotAdaptable()) { return super.visitMethod(access, name, desc, signature, exceptions); }
    MethodVisitor mv;

    // always add a known implementation for these methods even if they already exist
    if (ALWAYS_REWRITE_IF_PRESENT.contains(name + desc) && !getTransparencyClassSpec().isIgnoreRewrite()) { return null; }

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
    if (spec.isManagedFieldNeeded()) {
      addCachedManagedMethods();

      if (shouldAddField(MANAGED_FIELD_NAME + MANAGED_FIELD_TYPE)) {
        super.visitField(ACC_PRIVATE | ACC_VOLATILE | ACC_TRANSIENT | ACC_SYNTHETIC, MANAGED_FIELD_NAME,
                         MANAGED_FIELD_TYPE, null, null);
      }
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

      if (shouldAddMethod(MANAGED_METHOD + "()" + MANAGED_FIELD_TYPE)) {
        // add getter
        MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, MANAGED_METHOD, "()" + MANAGED_FIELD_TYPE,
                                             null, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, spec.getClassNameSlashes(), MANAGED_FIELD_NAME, MANAGED_FIELD_TYPE);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
      }

      if (shouldAddMethod(MANAGED_METHOD + "(" + MANAGED_FIELD_TYPE + ")V")) {
        // add setter
        MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, MANAGED_METHOD, "(" + MANAGED_FIELD_TYPE
                                                                                         + ")V", null, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, spec.getClassNameSlashes(), MANAGED_FIELD_NAME, MANAGED_FIELD_TYPE);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
      }

      if (shouldAddMethod(IS_MANAGED_METHOD + IS_MANAGED_DESCRIPTION)) {
        // add isManaged() method
        // XXX::FIXME:: This method need to handle TCClonableObjects and TCNonDistributableObjects
        MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, IS_MANAGED_METHOD, IS_MANAGED_DESCRIPTION,
                                             null, null);
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
        mv.visitEnd();
      }
    }
  }

  /**
   * Creates a method that takes all of the fields in the class and the super classes and puts them into the passed in
   * hashmap
   */
  private void addRetrieveValuesMethod() {
    if (spec.isValuesGetterMethodNeeded() && shouldAddMethod(VALUES_GETTER + VALUES_GETTER_DESCRIPTION)) {
      MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, VALUES_GETTER, VALUES_GETTER_DESCRIPTION, null,
                                           null);
      if (!portability.isInstrumentationNotNeeded(spec.getSuperClassNameDots())
          && getTransparencyClassSpec().hasPhysicallyPortableSpecs(
                                                                   AsmClassInfo.getClassInfo(spec
                                                                       .getSuperClassNameDots(), spec.getClassInfo()
                                                                       .getClassLoader()))) {
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
      mv.visitEnd();
    }
  }

  private boolean shouldAddMethod(String nameAndDesc) {
    return !getTransparencyClassSpec().isIgnoreRewrite() || !spec.hasMethod(nameAndDesc);
  }

  private boolean shouldAddField(String nameAndDesc) {
    return !getTransparencyClassSpec().isIgnoreRewrite() || !spec.hasField(nameAndDesc);
  }

  private void addRetrieveManagedValueMethod() {
    if (spec.isManagedValuesGetterMethodNeeded()
        && shouldAddMethod(MANAGED_VALUES_GETTER + MANAGED_VALUES_GETTER_DESCRIPTION)) {
      MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, MANAGED_VALUES_GETTER,
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
          && getTransparencyClassSpec().hasPhysicallyPortableSpecs(
                                                                   AsmClassInfo.getClassInfo(spec
                                                                       .getSuperClassNameDots(), spec.getClassInfo()
                                                                       .getClassLoader()))) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, spec.getSuperClassNameSlashes(), MANAGED_VALUES_GETTER,
                           MANAGED_VALUES_GETTER_DESCRIPTION);
      } else {
        mv.visitInsn(ACONST_NULL);
      }
      mv.visitInsn(ARETURN);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }
  }

  /**
   * Creates a method that allows the setting of any field in this class or it's super classes.
   */
  private void addSetValueMethod() {
    if (spec.isValuesSetterMethodNeeded() && shouldAddMethod(VALUES_SETTER + VALUES_SETTER_DESCRIPTION)) {
      MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, VALUES_SETTER, VALUES_SETTER_DESCRIPTION, null,
                                           null);
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
          && getTransparencyClassSpec().hasPhysicallyPortableSpecs(
                                                                   AsmClassInfo.getClassInfo(spec
                                                                       .getSuperClassNameDots(), spec.getClassInfo()
                                                                       .getClassLoader()))) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKESPECIAL, spec.getSuperClassNameSlashes(), VALUES_SETTER, VALUES_SETTER_DESCRIPTION);
      }
      mv.visitLabel(l1);

      mv.visitInsn(RETURN);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }
  }

  private void addSetManagedValueMethod() {
    if (spec.isManagedValuesSetterMethodNeeded()
        && shouldAddMethod(MANAGED_VALUES_SETTER + VALUES_SETTER_DESCRIPTION)) {
      MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, MANAGED_VALUES_SETTER,
                                           VALUES_SETTER_DESCRIPTION, null, null);

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
          && getTransparencyClassSpec().hasPhysicallyPortableSpecs(
                                                                   AsmClassInfo.getClassInfo(spec
                                                                       .getSuperClassNameDots(), spec.getClassInfo()
                                                                       .getClassLoader()))) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKESPECIAL, spec.getSuperClassNameSlashes(), MANAGED_VALUES_SETTER,
                           VALUES_SETTER_DESCRIPTION);
      }

      mv.visitLabel(l1);
      mv.visitInsn(RETURN);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
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

  private static Collection getInterfaceMethodDescriptions(Class iface) {
    Set rv = new HashSet();
    Method[] methods = iface.getMethods();
    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      rv.add(method.getName() + Type.getMethodDescriptor(method));
    }

    return rv;
  }

}
