/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.weaver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Type;

import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.expression.PointcutType;
import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.definition.MixinDefinition;
import com.tc.aspectwerkz.definition.SystemDefinition;
import com.tc.aspectwerkz.exception.DefinitionException;
import com.tc.aspectwerkz.transform.InstrumentationContext;
import com.tc.aspectwerkz.transform.TransformationConstants;
import com.tc.aspectwerkz.transform.inlining.AsmHelper;

/**
 * Adds mixin methods and fields to hold mixin instances to the target class.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class AddMixinMethodsVisitor extends ClassAdapter implements TransformationConstants {

  private final InstrumentationContext m_ctx;
  private String m_declaringTypeName;
  private final ClassInfo m_classInfo;
  private final Set m_addedMethods;
  private ExpressionContext m_expressionContext;
  private boolean m_hasClinit = false;
  private Map m_mixinFields;
  private boolean m_isAdvised = false;

  /**
   * Creates a new class adapter.
   *
   * @param cv
   * @param classInfo
   * @param ctx
   * @param addedMethods
   */
  public AddMixinMethodsVisitor(final ClassVisitor cv,
                                final ClassInfo classInfo,
                                final InstrumentationContext ctx,
                                final Set addedMethods) {
    super(cv);
    m_classInfo = classInfo;
    m_ctx = (InstrumentationContext) ctx;
    m_addedMethods = addedMethods;
    m_expressionContext = new ExpressionContext(PointcutType.WITHIN, m_classInfo, m_classInfo);
  }

  /**
   * Visits the class.
   *
   * @param access
   * @param name
   * @param signature
   * @param superName
   * @param interfaces
   */
  public void visit(final int version,
                    final int access,
                    final String name,
                    final String signature,
                    final String superName,
                    final String[] interfaces) {
    ExpressionContext ctx = new ExpressionContext(PointcutType.WITHIN, m_classInfo, m_classInfo);
    if (!classFilter(m_classInfo, ctx, m_ctx.getDefinitions())) {
      m_declaringTypeName = name;
      m_mixinFields = new HashMap();

      // populate with fields already present for mixins from previous weaving
      for (int i = 0; i < m_classInfo.getFields().length; i++) {
        FieldInfo fieldInfo = m_classInfo.getFields()[i];
        if (fieldInfo.getName().startsWith(MIXIN_FIELD_NAME)) {
          m_mixinFields.put(fieldInfo.getType(), fieldInfo);
        }
      }

      // add fields and method for (not already there) mixins
      addMixinMembers();
    }
    super.visit(version, access, name, signature, superName, interfaces);
  }

  /**
   * Adds mixin fields and methods to the target class.
   */
  private void addMixinMembers() {
    int index = 0;
    for (Iterator it = m_ctx.getDefinitions().iterator(); it.hasNext();) {
      List mixinDefs = ((SystemDefinition) it.next()).getMixinDefinitions(m_expressionContext);

      // check for method clashes
      Set interfaceSet = new HashSet();
      for (Iterator it2 = mixinDefs.iterator(); it2.hasNext();) {
        interfaceSet.addAll(((MixinDefinition) it2.next()).getInterfaceClassNames());
      }
      //TODO refactor to handle precedence injection
//            if (ClassInfoHelper.hasMethodClash(interfaceSet, m_ctx.getLoader())) {
//                return;
//            }

      for (Iterator it2 = mixinDefs.iterator(); it2.hasNext();) {
        final MixinDefinition mixinDef = (MixinDefinition) it2.next();
        final ClassInfo mixinImpl = mixinDef.getMixinImpl();
        final DeploymentModel deploymentModel = mixinDef.getDeploymentModel();

        if (m_mixinFields.containsKey(mixinImpl)) {
          continue;
        }
        final MixinFieldInfo fieldInfo = new MixinFieldInfo();
        fieldInfo.fieldName = MIXIN_FIELD_NAME + index;
        fieldInfo.mixinClassInfo = mixinImpl;

        addMixinField(fieldInfo, deploymentModel, mixinDef);
        addMixinMethods(fieldInfo, mixinDef);

        index++;
        m_isAdvised = true;
      }
    }
  }

  /**
   * Appends mixin instantiation to the clinit method and/or init method.
   *
   * @param access
   * @param name
   * @param desc
   * @param signature
   * @param exceptions
   * @return
   */
  public MethodVisitor visitMethod(final int access,
                                   final String name,
                                   final String desc,
                                   final String signature,
                                   final String[] exceptions) {
    if (m_isAdvised) {
      if (name.equals(CLINIT_METHOD_NAME)) {
        m_hasClinit = true;
        MethodVisitor mv = new PrependToClinitMethodCodeAdapter(
                cv.visitMethod(access, name, desc, signature, exceptions)
        );
        mv.visitMaxs(0, 0);
        return mv;
      } else if (name.equals(INIT_METHOD_NAME)) {
        MethodVisitor mv = new AppendToInitMethodCodeAdapter(
                cv.visitMethod(access, name, desc, signature, exceptions)
        );
        mv.visitMaxs(0, 0);
        return mv;
      }
    }
    return super.visitMethod(access, name, desc, signature, exceptions);
  }

  /**
   * Creates a new clinit method and adds mixin instantiation if it does not exist.
   */
  public void visitEnd() {
    if (m_isAdvised && !m_hasClinit) {
      // add the <clinit> method
      MethodVisitor mv = cv.visitMethod(
              ACC_STATIC, CLINIT_METHOD_NAME, NO_PARAM_RETURN_VOID_SIGNATURE, null, null
      );
      for (Iterator i4 = m_mixinFields.values().iterator(); i4.hasNext();) {
        MixinFieldInfo fieldInfo = (MixinFieldInfo) i4.next();
        if (fieldInfo.isStatic) {
          initializeStaticMixinField(mv, fieldInfo);
        }
      }

      mv.visitInsn(RETURN);
      mv.visitMaxs(0, 0);
    }
    super.visitEnd();
  }

  /**
   * Initializes a static mixin field.
   *
   * @param mv
   * @param fieldInfo
   */
  private void initializeStaticMixinField(final MethodVisitor mv, final MixinFieldInfo fieldInfo) {
    mv.visitLdcInsn(fieldInfo.mixinClassInfo.getName().replace('/', '.'));
    if (fieldInfo.isPerJVM) {
      mv.visitFieldInsn(
              GETSTATIC,
              m_declaringTypeName,
              TARGET_CLASS_FIELD_NAME,
              CLASS_CLASS_SIGNATURE
      );
      mv.visitMethodInsn(
              INVOKEVIRTUAL,
              CLASS_CLASS,
              GETCLASSLOADER_METHOD_NAME,
              CLASS_CLASS_GETCLASSLOADER_METHOD_SIGNATURE
      );
      mv.visitMethodInsn(
              INVOKESTATIC,
              MIXINS_CLASS_NAME,
              MIXIN_OF_METHOD_NAME,
              MIXIN_OF_METHOD_PER_JVM_SIGNATURE
      );
    } else {
      mv.visitFieldInsn(
              GETSTATIC,
              m_declaringTypeName,
              TARGET_CLASS_FIELD_NAME,
              CLASS_CLASS_SIGNATURE
      );
      mv.visitMethodInsn(
              INVOKESTATIC,
              MIXINS_CLASS_NAME,
              MIXIN_OF_METHOD_NAME,
              MIXIN_OF_METHOD_PER_CLASS_SIGNATURE
      );
    }
    mv.visitTypeInsn(CHECKCAST, fieldInfo.mixinClassInfo.getName().replace('.', '/'));
    mv.visitFieldInsn(
            PUTSTATIC,
            m_declaringTypeName,
            fieldInfo.fieldName,
            fieldInfo.mixinClassInfo.getSignature()
    );
  }

  /**
   * Initializes a member mixin field.
   *
   * @param mv
   * @param fieldInfo
   */
  private void initializeMemberMixinField(final MethodVisitor mv, final MixinFieldInfo fieldInfo) {
    mv.visitVarInsn(ALOAD, 0);
    mv.visitLdcInsn(fieldInfo.mixinClassInfo.getName().replace('/', '.'));
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(
            INVOKESTATIC,
            MIXINS_CLASS_NAME,
            MIXIN_OF_METHOD_NAME,
            MIXIN_OF_METHOD_PER_INSTANCE_SIGNATURE
    );
    mv.visitTypeInsn(CHECKCAST, fieldInfo.mixinClassInfo.getName().replace('.', '/'));
    mv.visitFieldInsn(
            PUTFIELD,
            m_declaringTypeName,
            fieldInfo.fieldName,
            fieldInfo.mixinClassInfo.getSignature()
    );
  }

  /**
   * Adds the mixin field to the target class.
   *
   * @param fieldInfo
   * @param deploymentModel
   * @param mixinDef
   */
  private void addMixinField(final MixinFieldInfo fieldInfo,
                             final DeploymentModel deploymentModel,
                             final MixinDefinition mixinDef) {
    final String signature = fieldInfo.mixinClassInfo.getSignature();
    int modifiers = 0;
    if (deploymentModel.equals(DeploymentModel.PER_CLASS) || deploymentModel.equals(DeploymentModel.PER_JVM)) {
      fieldInfo.isStatic = true;
      fieldInfo.isPerJVM = deploymentModel.equals(DeploymentModel.PER_JVM);
      modifiers = ACC_PRIVATE + ACC_FINAL + ACC_STATIC;
//            modifiers = ACC_PRIVATE + ACC_FINAL + ACC_STATIC + ACC_SYNTHETIC;
    } else if (deploymentModel.equals(DeploymentModel.PER_INSTANCE)) {
      fieldInfo.isStatic = false;
      modifiers = ACC_PRIVATE + ACC_FINAL;
//            modifiers = ACC_PRIVATE + ACC_FINAL + ACC_SYNTHETIC;
    } else {
      throw new DefinitionException(
              "deployment model [" + mixinDef.getDeploymentModel() +
                      "] for mixin [" + mixinDef.getMixinImpl().getName() +
                      "] is not supported"
      );

    }
    if (mixinDef.isTransient()) {
      modifiers += ACC_TRANSIENT;
    }
    cv.visitField(modifiers, fieldInfo.fieldName, signature, null, null);
    m_mixinFields.put(mixinDef.getMixinImpl(), fieldInfo);
  }

  /**
   * Adds the mixin methods to the target class.
   *
   * @param fieldInfo
   * @param mixinDef
   */
  private void addMixinMethods(final MixinFieldInfo fieldInfo, final MixinDefinition mixinDef) {
    for (Iterator it3 = mixinDef.getMethodsToIntroduce().iterator(); it3.hasNext();) {
      MethodInfo methodInfo = (MethodInfo) it3.next();
      final String methodName = methodInfo.getName();
      final String methodSignature = methodInfo.getSignature();

      if (m_addedMethods.contains(AlreadyAddedMethodVisitor.getMethodKey(methodName, methodSignature))) {
        continue;
      }

      MethodVisitor mv = cv.visitMethod(
              ACC_PUBLIC + ACC_SYNTHETIC,
              methodName,
              methodSignature,
              null,
              null
      );
      if (fieldInfo.isStatic) {
        mv.visitFieldInsn(
                GETSTATIC,
                m_declaringTypeName,
                fieldInfo.fieldName,
                fieldInfo.mixinClassInfo.getSignature()
        );
      } else {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(
                GETFIELD,
                m_declaringTypeName,
                fieldInfo.fieldName,
                fieldInfo.mixinClassInfo.getSignature()
        );
      }
      AsmHelper.loadArgumentTypes(mv, Type.getArgumentTypes(methodSignature), false);
      mv.visitMethodInsn(
              INVOKEVIRTUAL,
              fieldInfo.mixinClassInfo.getName().replace('.', '/'),
              methodName,
              methodSignature
      );
      AsmHelper.addReturnStatement(mv, Type.getReturnType(methodSignature));
      mv.visitMaxs(0, 0);
    }
  }

  /**
   * Filters the classes to be transformed.
   *
   * @param classInfo   the class to filter
   * @param ctx         the context
   * @param definitions a set with the definitions
   * @return boolean true if the method should be filtered away
   */
  public static boolean classFilter(final ClassInfo classInfo,
                                    final ExpressionContext ctx,
                                    final Set definitions) {
    for (Iterator it = definitions.iterator(); it.hasNext();) {
      SystemDefinition systemDef = (SystemDefinition) it.next();
      if (classInfo.isInterface()) {
        return true;
      }
      String className = classInfo.getName().replace('/', '.');
      if (systemDef.inExcludePackage(className)) {
        return true;
      }
      if (!systemDef.inIncludePackage(className)) {
        return true;
      }
      if (systemDef.hasMixin(ctx)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Adds initialization of static mixin fields to the beginning of the clinit method.
   *
   * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
   */
  public class PrependToClinitMethodCodeAdapter extends MethodAdapter {

    public PrependToClinitMethodCodeAdapter(final MethodVisitor ca) {
      super(ca);
      for (Iterator i4 = m_mixinFields.values().iterator(); i4.hasNext();) {
        MixinFieldInfo fieldInfo = (MixinFieldInfo) i4.next();
        if (fieldInfo.isStatic) {
          initializeStaticMixinField(ca, fieldInfo);
        }
      }
    }
  }

  /**
   * Adds initialization of member mixin fields to end of the init method.
   *
   * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
   */
  public class AppendToInitMethodCodeAdapter extends MethodAdapter {

    public AppendToInitMethodCodeAdapter(final MethodVisitor ca) {
      super(ca);
    }

    public void visitInsn(final int opcode) {
      if (opcode == RETURN) {
        for (Iterator i4 = m_mixinFields.values().iterator(); i4.hasNext();) {
          MixinFieldInfo fieldInfo = (MixinFieldInfo) i4.next();
          if (!fieldInfo.isStatic) {
            initializeMemberMixinField(mv, fieldInfo);
          }
        }
      }
      super.visitInsn(opcode);
    }
  }

  static class MixinFieldInfo {
    private String fieldName;
    private ClassInfo mixinClassInfo;
    private boolean isStatic;
    private boolean isPerJVM = false;
  }
}