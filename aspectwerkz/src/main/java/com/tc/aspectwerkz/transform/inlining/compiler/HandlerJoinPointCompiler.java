/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.compiler;

import com.tc.asm.MethodVisitor;
import com.tc.asm.Type;

/**
 * A compiler that compiles/generates a class that represents a specific join point, a class which invokes the advices
 * and the target join point statically.
 * <p/>
 * In this case, CALLEE is the catched exception instance itself.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur </a>
 */
public class HandlerJoinPointCompiler extends AbstractJoinPointCompiler {

  /**
   * Creates a new join point compiler instance.
   *
   * @param model
   */
  HandlerJoinPointCompiler(final CompilationInfo.Model model) {
    super(model);
  }

  /**
   * Creates join point specific fields.
   */
  protected void createJoinPointSpecificFields() {
    // create the field argument field
    String[] fieldNames = null;
    Type fieldType = Type.getType(m_calleeClassSignature);
    fieldNames = new String[1];
    String fieldName = ARGUMENT_FIELD + 0;
    fieldNames[0] = fieldName;
    m_cw.visitField(ACC_PRIVATE, fieldName, fieldType.getDescriptor(), null, null);
    m_fieldNames = fieldNames;
    m_cw.visitField(
            ACC_PRIVATE + ACC_STATIC,
            SIGNATURE_FIELD_NAME,
            HANDLER_SIGNATURE_IMPL_CLASS_SIGNATURE,
            null,
            null
    );
  }

  /**
   * Creates the signature for the join point.
   *
   * @param cv
   */
  protected void createSignature(final MethodVisitor cv) {
    cv.visitFieldInsn(GETSTATIC, m_joinPointClassName, TARGET_CLASS_FIELD_NAME_IN_JP, CLASS_CLASS_SIGNATURE);

    cv.visitMethodInsn(
            INVOKESTATIC,
            SIGNATURE_FACTORY_CLASS,
            NEW_CATCH_CLAUSE_SIGNATURE_METHOD_NAME,
            NEW_HANDLER_SIGNATURE_METHOD_SIGNATURE
    );
    cv.visitFieldInsn(
            PUTSTATIC, m_joinPointClassName, SIGNATURE_FIELD_NAME, HANDLER_SIGNATURE_IMPL_CLASS_SIGNATURE
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
  protected void createInlinedJoinPointInvocation(final MethodVisitor cv,
                                                  final CompilerInput input) {
    // load the exception
    cv.visitVarInsn(ALOAD, 0);//TODO if changed perhaps load CALLEE instead that host the exception ?
  }

  /**
   * Creates a call to the target join point, the parameter(s) to the join point are retrieved from the invocation
   * local join point instance.
   *
   * @param cv
   */
  protected void createJoinPointInvocation(final MethodVisitor cv) {
    cv.visitInsn(ACONST_NULL);
  }

  /**
   * Returns the join points return type.
   *
   * @return
   */
  protected Type getJoinPointReturnType() {
    return Type.getType(m_calleeClassSignature);
  }

  /**
   * Returns the join points argument type(s).
   *
   * @return
   */
  protected Type[] getJoinPointArgumentTypes() {
    return new Type[]{Type.getType(m_calleeClassSignature)};//TODO should callee be arg instead ? to bind it later ?
  }

  /**
   * Creates the getRtti method
   */
  protected void createGetRttiMethod() {
    MethodVisitor cv = m_cw.visitMethod(ACC_PUBLIC, GET_RTTI_METHOD_NAME, GET_RTTI_METHOD_SIGNATURE, null, null);

    // new CtorRttiImpl( .. )
    cv.visitTypeInsn(NEW, HANDLER_RTTI_IMPL_CLASS_NAME);
    cv.visitInsn(DUP);
    cv.visitFieldInsn(
            GETSTATIC, m_joinPointClassName, SIGNATURE_FIELD_NAME, HANDLER_SIGNATURE_IMPL_CLASS_SIGNATURE
    );
    cv.visitVarInsn(ALOAD, 0);
    cv.visitFieldInsn(GETFIELD, m_joinPointClassName, CALLER_INSTANCE_FIELD_NAME, m_callerClassSignature);
    cv.visitVarInsn(ALOAD, 0);
    cv.visitFieldInsn(GETFIELD, m_joinPointClassName, CALLEE_INSTANCE_FIELD_NAME, m_calleeClassSignature);
    cv.visitMethodInsn(
            INVOKESPECIAL, HANDLER_RTTI_IMPL_CLASS_NAME, INIT_METHOD_NAME, HANDLER_RTTI_IMPL_INIT_SIGNATURE
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
            SIGNATURE_FIELD_NAME, HANDLER_SIGNATURE_IMPL_CLASS_SIGNATURE
    );
    cv.visitInsn(ARETURN);
    cv.visitMaxs(0, 0);
  }
}
