/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.weaver;

import java.util.Set;
import java.util.Iterator;
import java.util.Collection;

import com.tc.asm.*;

import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.expression.PointcutType;
import com.tc.aspectwerkz.expression.ExpressionInfo;
import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.definition.AdviceDefinition;
import com.tc.aspectwerkz.definition.DeploymentScope;
import com.tc.aspectwerkz.definition.SystemDefinition;
import com.tc.aspectwerkz.perx.PerObjectAspect;
import com.tc.aspectwerkz.transform.InstrumentationContext;
import com.tc.aspectwerkz.transform.TransformationConstants;

/**
 * Adds an instance level aspect management to the target class.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a>
 */
public class InstanceLevelAspectVisitor extends ClassAdapter implements TransformationConstants {

  private final InstrumentationContext m_ctx;
  private final ClassInfo m_classInfo;
  private boolean m_isAdvised = false;

  /**
   * Creates a new add interface class adapter.
   *
   * @param cv
   * @param classInfo
   * @param ctx
   */
  public InstanceLevelAspectVisitor(final ClassVisitor cv,
                                    final ClassInfo classInfo,
                                    final InstrumentationContext ctx) {
    super(cv);
    m_classInfo = classInfo;
    m_ctx = (InstrumentationContext) ctx;
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

    if (classFilter(m_classInfo, m_ctx.getDefinitions())) {
      super.visit(version, access, name, signature, superName, interfaces);
      return;
    }

    for (int i = 0; i < interfaces.length; i++) {
      String anInterface = interfaces[i];
      if (anInterface.equals(HAS_INSTANCE_LEVEL_ASPECT_INTERFACE_NAME)) {
        super.visit(version, access, name, signature, superName, interfaces);
        return;
      }
    }
    String[] newInterfaceArray = new String[interfaces.length + 1];
    System.arraycopy(interfaces, 0, newInterfaceArray, 0, interfaces.length);
    newInterfaceArray[interfaces.length] = HAS_INSTANCE_LEVEL_ASPECT_INTERFACE_NAME;

    // add the interface
    super.visit(version, access, name, signature, superName, newInterfaceArray);

    // add the field with the aspect instance map
    addAspectMapField();

    // add the getAspect(..) method
    addGetAspectMethod(name);

    // add the hasAspect(...) method
    addHasAspectMethod(name);

    addBindAspectMethod(name);
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
      if (name.equals(INIT_METHOD_NAME)) {
        MethodVisitor mv = new AppendToInitMethodCodeAdapter(
                cv.visitMethod(access, name, desc, signature, exceptions),
                name
        );
        mv.visitMaxs(0, 0);
        return mv;
      }
    }
    return cv.visitMethod(access, name, desc, signature, exceptions);
  }

  /**
   * Adds the aspect map field to the target class.
   */
  private void addAspectMapField() {
    super.visitField(
            ACC_PRIVATE + ACC_SYNTHETIC + ACC_TRANSIENT,
            INSTANCE_LEVEL_ASPECT_MAP_FIELD_NAME,
            INSTANCE_LEVEL_ASPECT_MAP_FIELD_SIGNATURE,
            null, null
    );
  }

  /**
   * Adds the getAspect(..) method to the target class.
   *
   * @param name the class name of the target class
   */
  private void addGetAspectMethod(final String name) {
    MethodVisitor cv = super.visitMethod(
            ACC_PUBLIC + ACC_SYNTHETIC,
            INSTANCE_LEVEL_GETASPECT_METHOD_NAME,
            INSTANCE_LEVEL_GETASPECT_METHOD_SIGNATURE,
            null, null
    );

    cv.visitVarInsn(ALOAD, 0);
    cv.visitFieldInsn(
            GETFIELD,
            name,
            INSTANCE_LEVEL_ASPECT_MAP_FIELD_NAME,
            INSTANCE_LEVEL_ASPECT_MAP_FIELD_SIGNATURE
    );
    //--
    cv.visitInsn(DUP);
    Label ifMapNonNull = new Label();
    cv.visitJumpInsn(IFNONNULL, ifMapNonNull);
    cv.visitInsn(ACONST_NULL);
    cv.visitInsn(ARETURN);
    cv.visitLabel(ifMapNonNull);

//        // if == null, field = new HashMap()
//        Label ifFieldNullNotLabel = new Label();
//        cv.visitJumpInsn(IFNONNULL, ifFieldNullNotLabel);
//        cv.visitVarInsn(ALOAD, 0);
//        cv.visitTypeInsn(NEW, HASH_MAP_CLASS_NAME);
//        cv.visitInsn(DUP);
//        cv.visitMethodInsn(
//                INVOKESPECIAL,
//                HASH_MAP_CLASS_NAME,
//                INIT_METHOD_NAME,
//                NO_PARAM_RETURN_VOID_SIGNATURE
//        );
//        cv.visitFieldInsn(
//                PUTFIELD,
//                name,
//                INSTANCE_LEVEL_ASPECT_MAP_FIELD_NAME,
//                INSTANCE_LEVEL_ASPECT_MAP_FIELD_SIGNATURE
//        );
//        cv.visitLabel(ifFieldNullNotLabel);
//
//        cv.visitVarInsn(ALOAD, 0);
//        cv.visitFieldInsn(
//                GETFIELD,
//                name,
//                INSTANCE_LEVEL_ASPECT_MAP_FIELD_NAME,
//                INSTANCE_LEVEL_ASPECT_MAP_FIELD_SIGNATURE
//        );
//
//        cv.visitVarInsn(ALOAD, 2);//qName
//        cv.visitMethodInsn(INVOKEINTERFACE, MAP_CLASS_NAME, GET_METHOD_NAME, GET_METHOD_SIGNATURE);
//        cv.visitVarInsn(ASTORE, 4);
//        cv.visitVarInsn(ALOAD, 4);
//        Label ifNullNotLabel = new Label();
//        cv.visitJumpInsn(IFNONNULL, ifNullNotLabel);
//        cv.visitVarInsn(ALOAD, 2);//qName
//        cv.visitVarInsn(ALOAD, 3);//containerClassName
//        cv.visitVarInsn(ALOAD, 0);//this (perInstance)
//        cv.visitMethodInsn(
//                INVOKESTATIC,
//                ASPECTS_CLASS_NAME,
//                ASPECT_OF_METHOD_NAME,
//                ASPECT_OF_PER_INSTANCE_METHOD_SIGNATURE
//        );
//        cv.visitVarInsn(ASTORE, 4);
    //cv.visitVarInsn(ALOAD, 0);
    //--
    cv.visitVarInsn(ALOAD, 1);
    cv.visitMethodInsn(INVOKEINTERFACE, MAP_CLASS_NAME, GET_METHOD_NAME, GET_METHOD_SIGNATURE);
    //--
//        cv.visitFieldInsn(
//                GETFIELD,
//                name,
//                INSTANCE_LEVEL_ASPECT_MAP_FIELD_NAME,
//                INSTANCE_LEVEL_ASPECT_MAP_FIELD_SIGNATURE
//        );
    cv.visitInsn(ARETURN);
//        cv.visitVarInsn(ALOAD, 2);
//        cv.visitVarInsn(ALOAD, 4);
//        cv.visitMethodInsn(INVOKEINTERFACE, MAP_CLASS_NAME, PUT_METHOD_NAME, PUT_METHOD_SIGNATURE);
//        cv.visitInsn(POP);
//        cv.visitLabel(ifNullNotLabel);
//        cv.visitVarInsn(ALOAD, 4);
//        cv.visitInsn(ARETURN);
    cv.visitMaxs(0, 0);

    m_ctx.markAsAdvised();
    m_isAdvised = true;
  }

  private void addHasAspectMethod(String mapFieldName) {
    MethodVisitor cv = super.visitMethod(ACC_PUBLIC + ACC_SYNTHETIC,
            INSTANCE_LEVEL_HASASPECT_METHOD_NAME,
            INSTANCE_LEVEL_HASASPECT_METHOD_SIGNATURE,
            null,
            null
    );

    cv.visitVarInsn(ALOAD, 0);
    cv.visitFieldInsn(GETFIELD,
            mapFieldName,
            INSTANCE_LEVEL_ASPECT_MAP_FIELD_NAME,
            INSTANCE_LEVEL_ASPECT_MAP_FIELD_SIGNATURE
    );
    cv.visitInsn(DUP);
    Label ifMapNonNull = new Label();
    cv.visitJumpInsn(IFNONNULL, ifMapNonNull);
    cv.visitInsn(ICONST_0);
    cv.visitInsn(IRETURN);
    cv.visitLabel(ifMapNonNull);
    cv.visitVarInsn(ALOAD, 1);
    cv.visitMethodInsn(INVOKEINTERFACE, MAP_CLASS_NAME, "containsKey", "(Ljava/lang/Object;)Z");
    //cv.visitMethodInsn(INVOKEINTERFACE, MAP_CLASS_NAME, GET_METHOD_NAME, GET_METHOD_SIGNATURE);
//
//        Label ifNullLabel = new Label();
//        cv.visitJumpInsn(IFNULL, ifNullLabel);
//        cv.visitInsn(ICONST_1);
//        cv.visitInsn(IRETURN);
//        cv.visitLabel(ifNullLabel);
//        cv.visitInsn(ICONST_0);
    cv.visitInsn(IRETURN);
    cv.visitMaxs(0, 0);

    m_ctx.markAsAdvised();
    m_isAdvised = true;
  }

  private void addBindAspectMethod(final String name) {
    MethodVisitor cv = super.visitMethod(
            ACC_PUBLIC + ACC_SYNTHETIC,
            INSTANCE_LEVEL_BINDASPECT_METHOD_NAME,
            INSTANCE_LEVEL_BINDASPECT_METHOD_SIGNATURE,
            null, null
    );

    cv.visitVarInsn(ALOAD, 0);
    cv.visitFieldInsn(
            GETFIELD,
            name,
            INSTANCE_LEVEL_ASPECT_MAP_FIELD_NAME,
            INSTANCE_LEVEL_ASPECT_MAP_FIELD_SIGNATURE
    );
    // if == null, field = new HashMap()
    Label ifFieldNullNotLabel = new Label();
    cv.visitJumpInsn(IFNONNULL, ifFieldNullNotLabel);
    cv.visitVarInsn(ALOAD, 0);
    cv.visitTypeInsn(NEW, HASH_MAP_CLASS_NAME);
    cv.visitInsn(DUP);
    cv.visitMethodInsn(
            INVOKESPECIAL,
            HASH_MAP_CLASS_NAME,
            INIT_METHOD_NAME,
            NO_PARAM_RETURN_VOID_SIGNATURE
    );
    cv.visitFieldInsn(
            PUTFIELD,
            name,
            INSTANCE_LEVEL_ASPECT_MAP_FIELD_NAME,
            INSTANCE_LEVEL_ASPECT_MAP_FIELD_SIGNATURE
    );
    cv.visitLabel(ifFieldNullNotLabel);

    cv.visitVarInsn(ALOAD, 0);
    cv.visitFieldInsn(
            GETFIELD,
            name,
            INSTANCE_LEVEL_ASPECT_MAP_FIELD_NAME,
            INSTANCE_LEVEL_ASPECT_MAP_FIELD_SIGNATURE
    );
    cv.visitVarInsn(ALOAD, 1);
    cv.visitVarInsn(ALOAD, 2);
    cv.visitMethodInsn(INVOKEINTERFACE, MAP_CLASS_NAME, PUT_METHOD_NAME, PUT_METHOD_SIGNATURE);
    cv.visitVarInsn(ALOAD, 2);
    cv.visitInsn(ARETURN);
  }

  /**
   * Filters the classes to be transformed.
   *
   * @param classInfo   the class to filter
   * @param definitions a set with the definitions
   * @return boolean true if the method should be filtered away
   */
  public static boolean classFilter(final ClassInfo classInfo, final Set definitions) {
    if (classInfo.isInterface()) {
      return true;
    }

    ExpressionContext ctx = new ExpressionContext(PointcutType.WITHIN, null, classInfo);

    for (Iterator it = definitions.iterator(); it.hasNext();) {
      SystemDefinition systemDef = (SystemDefinition) it.next();
      String className = classInfo.getName().replace('/', '.');
      if (systemDef.inExcludePackage(className)) {
        return true;
      }
      if (!systemDef.inIncludePackage(className)) {
        return true;
      }

      Collection adviceDefs = systemDef.getAdviceDefinitions();
      for (Iterator defs = adviceDefs.iterator(); defs.hasNext();) {
        AdviceDefinition adviceDef = (AdviceDefinition) defs.next();
        ExpressionInfo expressionInfo = adviceDef.getExpressionInfo();
        if (expressionInfo == null) {
          continue;
        }
        DeploymentModel deploymentModel = adviceDef.getDeploymentModel();

        // match on perinstance deployed aspects
        if (DeploymentModel.PER_INSTANCE.equals(deploymentModel)) {
          if (expressionInfo.getAdvisedClassFilterExpression().match(ctx)) {
            return false;
          }
        }

        // match on perthis/pertarget perX X pointcuts
        if (adviceDef.getAspectClassName().equals(PerObjectAspect.PEROBJECT_ASPECT_NAME)) {
          ExpressionInfo perXExpressionInfo = adviceDef.getExpressionInfo();
          if (perXExpressionInfo.getAdvisedClassFilterExpression().match(ctx)) {
            return false;
          }
        }
      }

      // match on deployment scopes, e.g. potential perinstance deployment aspects
      Collection deploymentScopes = systemDef.getDeploymentScopes();
      for (Iterator scopes = deploymentScopes.iterator(); scopes.hasNext();) {
        DeploymentScope deploymentScope = (DeploymentScope) scopes.next();
        ExpressionInfo expression = new ExpressionInfo(
                deploymentScope.getExpression(),
                systemDef.getUuid()
        );
        if (expression.getAdvisedClassFilterExpression().match(ctx)) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Adds initialization of aspect map field to end of the init method.
   *
   * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
   */
  private class AppendToInitMethodCodeAdapter extends AfterObjectInitializationCodeAdapter {

    private boolean m_done = false;

    public AppendToInitMethodCodeAdapter(final MethodVisitor ca, String callerMemberName) {
      super(ca, callerMemberName);
    }

    /**
     * Inserts the init of the aspect field right after the call to super(..) of this(..).
     *
     * @param opcode
     * @param owner
     * @param name
     * @param desc
     */
    public void visitMethodInsn(int opcode,
                                String owner,
                                String name,
                                String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
      if (opcode == INVOKESPECIAL && m_isObjectInitialized && !m_done) {
        m_done = true;

        // initialize aspect map field
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, HASH_MAP_CLASS_NAME);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(
                INVOKESPECIAL,
                HASH_MAP_CLASS_NAME,
                INIT_METHOD_NAME,
                NO_PARAM_RETURN_VOID_SIGNATURE
        );
        mv.visitFieldInsn(
                PUTFIELD,
                m_classInfo.getName().replace('.', '/'),
                INSTANCE_LEVEL_ASPECT_MAP_FIELD_NAME,
                INSTANCE_LEVEL_ASPECT_MAP_FIELD_SIGNATURE
        );
      }
    }
  }
}
