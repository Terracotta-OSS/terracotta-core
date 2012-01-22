/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.model;

import com.tc.asm.MethodVisitor;
import com.tc.asm.Type;

import com.tc.aspectwerkz.aspect.AdviceType;
import com.tc.aspectwerkz.definition.AspectDefinition;
import com.tc.aspectwerkz.transform.inlining.AdviceMethodInfo;
import com.tc.aspectwerkz.transform.inlining.AsmHelper;
import com.tc.aspectwerkz.transform.inlining.compiler.CompilerInput;
import com.tc.aspectwerkz.transform.inlining.spi.AspectModel;
import com.tc.aspectwerkz.reflect.ClassInfo;

/**
 * Implementation of the AspectModel interface for Spring framework.
 * <p/>
 * Provides methods for definition of aspects and framework specific bytecode generation
 * used by the join point compiler.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bon&#233;r </a>
 */
public class SpringAspectModel extends AopAllianceAspectModel {

  protected static final String ASPECT_MODEL_TYPE = "spring";

  private static final String METHOD_INTERCEPTOR_CLASS = "org.aopalliance.intercept.MethodInterceptor";
  private static final String AFTER_RETURNING_ADVICE_CLASS = "org.springframework.aop.AfterReturningAdvice";
  private static final String METHOD_BEFORE_ADVICE_CLASS = "org.springframework.aop.MethodBeforeAdvice";
  private static final String THROWS_ADVICE_CLASS = "org.springframework.aop.ThrowsAdvice";

  /**
   * Returns the aspect model type, which is an id for the the special aspect model, can be anything as long
   * as it is unique.
   *
   * @return the aspect model type id
   */
  public String getAspectModelType() {
    return ASPECT_MODEL_TYPE;
  }

  /**
   * Defines the aspect.
   */
  public void defineAspect(final ClassInfo classInfo, final AspectDefinition aspectDef, final ClassLoader loader) {
    ClassInfo[] interfaces = classInfo.getInterfaces();
    for (int i = 0; i < interfaces.length; i++) {
      String name = interfaces[i].getName();
      if (METHOD_INTERCEPTOR_CLASS.equals(name) 
          || METHOD_BEFORE_ADVICE_CLASS.equals(name)
          || AFTER_RETURNING_ADVICE_CLASS.equals(name) 
          || THROWS_ADVICE_CLASS.equals(name)) {
        aspectDef.setAspectModel(ASPECT_MODEL_TYPE);
        aspectDef.setContainerClassName(null);
        return;
      }
    }
  }

  /**
   * Returns info about the closure class, name and type (interface or class).
   * 
   * @return the closure class info
   */
  public AroundClosureClassInfo getAroundClosureClassInfo() {
    return new AspectModel.AroundClosureClassInfo(null, 
        new String[] { 
          AOP_ALLIANCE_CLOSURE_CLASS_NAME,
          METHOD_BEFORE_ADVICE_CLASS.replace('.', '/'), 
          AFTER_RETURNING_ADVICE_CLASS.replace('.', '/') });
  }

  public void createBeforeOrAfterAdviceArgumentHandling(MethodVisitor methodVisitor, CompilerInput compilerInput,
      Type[] types, AdviceMethodInfo adviceMethodInfo, int i) {
    if (AdviceType.BEFORE.equals(adviceMethodInfo.getAdviceInfo().getType())) {
      createBeforeAdviceArgumentHandling(methodVisitor, adviceMethodInfo, compilerInput.joinPointInstanceIndex);
    } else {
      // after advice no matter what
      createAfterAdviceArgumentHandling(methodVisitor, adviceMethodInfo, compilerInput.joinPointInstanceIndex);
    }
  }

  /**
   * Handles the arguments to the before advice.
   */
  public void createBeforeAdviceArgumentHandling(final MethodVisitor cv, final AdviceMethodInfo adviceMethodInfo,
      final int joinPointInstanceIndex) {
    final String joinPointClassName = adviceMethodInfo.getJoinPointClassName();
    final int joinPointIndex = joinPointInstanceIndex;
    cv.visitFieldInsn(GETSTATIC, joinPointClassName, SIGNATURE_FIELD_NAME, METHOD_SIGNATURE_IMPL_CLASS_SIGNATURE);
    cv.visitMethodInsn(INVOKEVIRTUAL, METHOD_SIGNATURE_IMPL_CLASS_NAME, GET_METHOD_METHOD_NAME,
        GET_METHOD_METHOD_SIGNATURE);

    if (Type.getArgumentTypes(adviceMethodInfo.getCalleeMemberDesc()).length == 0) {
      cv.visitInsn(ACONST_NULL);
    } else {
      cv.visitVarInsn(ALOAD, joinPointIndex);
      cv.visitMethodInsn(INVOKEVIRTUAL, joinPointClassName, GET_RTTI_METHOD_NAME, GET_RTTI_METHOD_SIGNATURE);
      cv.visitTypeInsn(CHECKCAST, METHOD_RTTI_IMPL_CLASS_NAME);
      cv.visitMethodInsn(INVOKEVIRTUAL, METHOD_RTTI_IMPL_CLASS_NAME, GET_PARAMETER_VALUES_METHOD_NAME,
          GET_ARGUMENTS_METHOD_SIGNATURE);
    }
    cv.visitVarInsn(ALOAD, joinPointIndex);
    cv.visitFieldInsn(GETFIELD, joinPointClassName, CALLEE_INSTANCE_FIELD_NAME, adviceMethodInfo
        .getCalleeClassSignature());
  }

  /**
   * Handles the arguments to the after advice.
   */
  public void createAfterAdviceArgumentHandling(final MethodVisitor cv, final AdviceMethodInfo adviceMethodInfo,
      final int joinPointInstanceIndex) {
    final String joinPointClassName = adviceMethodInfo.getJoinPointClassName();
    final int joinPointIndex = joinPointInstanceIndex;
    final String specArgDesc = adviceMethodInfo.getSpecialArgumentTypeDesc();
    if (specArgDesc == null) {
      cv.visitInsn(ACONST_NULL);
    } else {
      cv.visitVarInsn(ALOAD, adviceMethodInfo.getSpecialArgumentIndex());
      AsmHelper.wrapPrimitiveType(cv, Type.getType(specArgDesc));
    }
    cv.visitFieldInsn(GETSTATIC, joinPointClassName, SIGNATURE_FIELD_NAME, METHOD_SIGNATURE_IMPL_CLASS_SIGNATURE);
    cv.visitMethodInsn(INVOKEVIRTUAL, METHOD_SIGNATURE_IMPL_CLASS_NAME, GET_METHOD_METHOD_NAME,
        GET_METHOD_METHOD_SIGNATURE);

    if (Type.getArgumentTypes(adviceMethodInfo.getCalleeMemberDesc()).length == 0) {
      cv.visitInsn(ACONST_NULL);
    } else {
      cv.visitVarInsn(ALOAD, joinPointIndex);
      cv.visitMethodInsn(INVOKEVIRTUAL, joinPointClassName, GET_RTTI_METHOD_NAME, GET_RTTI_METHOD_SIGNATURE);
      cv.visitTypeInsn(CHECKCAST, METHOD_RTTI_IMPL_CLASS_NAME);
      cv.visitMethodInsn(INVOKEVIRTUAL, METHOD_RTTI_IMPL_CLASS_NAME, GET_PARAMETER_VALUES_METHOD_NAME,
          GET_ARGUMENTS_METHOD_SIGNATURE);
    }

    cv.visitVarInsn(ALOAD, joinPointIndex);
    cv.visitFieldInsn(GETFIELD, joinPointClassName, CALLEE_INSTANCE_FIELD_NAME, adviceMethodInfo
        .getCalleeClassSignature());
  }
}
