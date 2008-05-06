/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.compiler;

import com.tc.asm.MethodVisitor;
import com.tc.asm.Type;

import com.tc.aspectwerkz.transform.TransformationUtil;

import java.lang.reflect.Modifier;

/**
 * A compiler that compiles/generates a class that represents a specific join point, a class which invokes the advices
 * and the target join point statically.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur </a>
 */
public class ConstructorCallJoinPointCompiler extends AbstractJoinPointCompiler {
  /**
   * Creates a new join point compiler instance.
   *
   * @param model
   */
  ConstructorCallJoinPointCompiler(final CompilationInfo.Model model) {
    super(model);
  }

  /**
   * Creates join point specific fields.
   */
  protected void createJoinPointSpecificFields() {
    String[] fieldNames = null;
    // create the method argument fields
    Type[] argumentTypes = Type.getArgumentTypes(m_calleeMemberDesc);
    fieldNames = new String[argumentTypes.length];
    for (int i = 0; i < argumentTypes.length; i++) {
      Type argumentType = argumentTypes[i];
      String fieldName = ARGUMENT_FIELD + i;
      fieldNames[i] = fieldName;
      m_cw.visitField(ACC_PRIVATE, fieldName, argumentType.getDescriptor(), null, null);
    }
    m_fieldNames = fieldNames;

    m_cw.visitField(
            ACC_PRIVATE + ACC_STATIC,
            SIGNATURE_FIELD_NAME,
            CONSTRUCTOR_SIGNATURE_IMPL_CLASS_SIGNATURE,
            null,
            null
    );
  }

  /**
   * Creates the signature for the join point.
   * <p/>
   * FIXME signature field should NOT be of type Signature but of the specific type (update all refs as well)
   *
   * @param cv
   */
  protected void createSignature(final MethodVisitor cv) {
    cv.visitFieldInsn(GETSTATIC, m_joinPointClassName, TARGET_CLASS_FIELD_NAME_IN_JP, CLASS_CLASS_SIGNATURE);
    cv.visitLdcInsn(new Integer(m_joinPointHash));

    cv.visitMethodInsn(
            INVOKESTATIC,
            SIGNATURE_FACTORY_CLASS,
            NEW_CONSTRUCTOR_SIGNATURE_METHOD_NAME,
            NEW_CONSTRUCTOR_SIGNATURE_METHOD_SIGNATURE
    );
    cv.visitFieldInsn(
            PUTSTATIC, m_joinPointClassName, SIGNATURE_FIELD_NAME,
            CONSTRUCTOR_SIGNATURE_IMPL_CLASS_SIGNATURE
    );
  }

  /**
   * Optimized implementation that does not retrieve the parameters from the join point instance but is passed
   * directly to the method from the input parameters in the 'invoke' method. Can only be used if no around advice
   * exists.
   *
   * @param cv
   * @param input
   */
  protected void createInlinedJoinPointInvocation(final MethodVisitor cv, final CompilerInput input) {
    if (!Modifier.isPublic(m_calleeMemberModifiers)) {
      loadArgumentMemberFields(cv, input.argStartIndex);
      cv.visitMethodInsn(
              INVOKESTATIC,
              m_calleeClassName,
              TransformationUtil.getWrapperMethodName(
                      m_calleeMemberName,
                      m_calleeMemberDesc,
                      m_calleeClassName,
                      INVOKE_WRAPPER_METHOD_PREFIX
              ),
              Type.getMethodDescriptor(Type.getType(m_calleeClassSignature), m_argumentTypes)
      );
    } else {
      cv.visitTypeInsn(NEW, m_calleeClassName);
      cv.visitInsn(DUP);
      loadArgumentMemberFields(cv, input.argStartIndex);
      cv.visitMethodInsn(
              INVOKESPECIAL, m_calleeClassName, INIT_METHOD_NAME,
              m_calleeMemberDesc
      );
    }
    // assign to CALLEE
    //TODO - might not be needed / feasible for optimized jp - we should ensure that it is affected to target for
    // after advice that comes after (but should we support target on ctor call)
    cv.visitInsn(DUP);
    loadJoinPointInstance(cv, input);
    cv.visitInsn(SWAP);
    cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, CALLEE_INSTANCE_FIELD_NAME, m_calleeClassSignature);
  }

  /**
   * Creates a call to the target join point, the parameter(s) to the join point are retrieved from the invocation
   * local join point instance.
   *
   * @param cv
   */
  protected void createJoinPointInvocation(final MethodVisitor cv) {
    if (!Modifier.isPublic(m_calleeMemberModifiers)) {
      loadArguments(cv);
      cv.visitMethodInsn(
              INVOKESTATIC,
              m_calleeClassName,
              TransformationUtil.getWrapperMethodName(
                      m_calleeMemberName,
                      m_calleeMemberDesc,
                      m_calleeClassName,
                      INVOKE_WRAPPER_METHOD_PREFIX
              ),
              Type.getMethodDescriptor(Type.getType(m_calleeClassSignature), m_argumentTypes)
      );
    } else {
      cv.visitTypeInsn(NEW, m_calleeClassName);
      cv.visitInsn(DUP);
      loadArguments(cv);
      cv.visitMethodInsn(
              INVOKESPECIAL, m_calleeClassName, INIT_METHOD_NAME,
              m_calleeMemberDesc
      );
    }

    // put it in CALLEE field
    cv.visitInsn(DUP);
    cv.visitVarInsn(ALOAD, 0);
    cv.visitInsn(SWAP);
    cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, CALLEE_INSTANCE_FIELD_NAME, m_calleeClassSignature);
  }

  /**
   * Returns the join points return type.
   *
   * @return
   */
  protected Type getJoinPointReturnType() {
    return Type.getReturnType(m_calleeClassSignature);
  }

  /**
   * Returns the join points argument type(s).
   *
   * @return
   */
  protected Type[] getJoinPointArgumentTypes() {
    return Type.getArgumentTypes(m_calleeMemberDesc);
  }

  /**
   * Creates the getRtti method
   */
  protected void createGetRttiMethod() {
    MethodVisitor cv = m_cw.visitMethod(ACC_PUBLIC, GET_RTTI_METHOD_NAME, GET_RTTI_METHOD_SIGNATURE, null, null);

    // new CtorRttiImpl( .. )
    cv.visitTypeInsn(NEW, CONSTRUCTOR_RTTI_IMPL_CLASS_NAME);
    cv.visitInsn(DUP);
    cv.visitFieldInsn(
            GETSTATIC, m_joinPointClassName, SIGNATURE_FIELD_NAME, CONSTRUCTOR_SIGNATURE_IMPL_CLASS_SIGNATURE
    );
    cv.visitVarInsn(ALOAD, 0);
    cv.visitFieldInsn(GETFIELD, m_joinPointClassName, CALLER_INSTANCE_FIELD_NAME, m_callerClassSignature);
    cv.visitVarInsn(ALOAD, 0);
    // use RETURNED field instead
    cv.visitFieldInsn(GETFIELD, m_joinPointClassName, RETURN_VALUE_FIELD_NAME, m_calleeClassSignature);
    cv.visitMethodInsn(
            INVOKESPECIAL, CONSTRUCTOR_RTTI_IMPL_CLASS_NAME, INIT_METHOD_NAME,
            CONSTRUCTOR_RTTI_IMPL_INIT_SIGNATURE
    );

    // set the arguments
    cv.visitInsn(DUP);
    createArgumentArrayAt(cv, 1);
    cv.visitVarInsn(ALOAD, 1);
    cv.visitMethodInsn(
            INVOKEVIRTUAL, CONSTRUCTOR_RTTI_IMPL_CLASS_NAME, SET_PARAMETER_VALUES_METHOD_NAME,
            SET_PARAMETER_VALUES_METHOD_SIGNATURE
    );

    cv.visitInsn(ARETURN);
    cv.visitMaxs(0, 0);
  }

  /**
   * Creates the getSignature method.
   */
  protected void createGetSignatureMethod() {
    MethodVisitor cv = m_cw.visitMethod(
            ACC_PUBLIC,
            GET_SIGNATURE_METHOD_NAME,
            GET_SIGNATURE_METHOD_SIGNATURE,
            null,
            null
    );
    cv.visitFieldInsn(
            GETSTATIC, m_joinPointClassName,
            SIGNATURE_FIELD_NAME, CONSTRUCTOR_SIGNATURE_IMPL_CLASS_SIGNATURE
    );
    cv.visitInsn(ARETURN);
    cv.visitMaxs(0, 0);
  }
}