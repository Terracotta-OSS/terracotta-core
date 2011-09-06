/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.transform;

import java.lang.reflect.Modifier;

import com.tc.aspectwerkz.joinpoint.management.JoinPointType;

/**
 * Utility method used by the transformers.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public final class TransformationUtil {

  /**
   * Return the prefixed clinit method name
   *
   * @param className
   * @return
   */
  public static String getPrefixedOriginalClinitName(final String className) {
    return getPrefixedOriginalMethodName(
            TransformationConstants.STATICINITIALIZER_WRAPPER_METHOD_KEY,//need to not clash with a user method named "clinit"
            className
    );
  }

  /**
   * Returns the prefixed method name.
   *
   * @param methodName the method name
   * @param className  the class name
   * @return the name of the join point
   */
  public static String getPrefixedOriginalMethodName(final String methodName,
                                                     final String className) {
    final StringBuffer buf = new StringBuffer();
    buf.append(TransformationConstants.ORIGINAL_METHOD_PREFIX);
    buf.append(methodName);
    buf.append(TransformationConstants.DELIMITER);
    buf.append(className.replace('.', '_').replace('/', '_'));
    return buf.toString();
  }

  /**
   * Returns the prefixed method name.
   *
   * @param methodName the method name
   * @param methodDesc the method desc
   * @param className  the class name
   * @return the name of the join point
   */
  public static String getWrapperMethodName(final String methodName,
                                            final String methodDesc,
                                            final String className,
                                            final String prefix) {
    final StringBuffer buf = new StringBuffer();
    //FIXME: double check me
    // we use the javaC convention for hidden synthetic method
    // is the methodSequence enough ?
    // [ Alex: looks like it will change between each RW since tied to ctx match ]
    buf.append(TransformationConstants.WRAPPER_METHOD_PREFIX);
    buf.append(prefix);
    buf.append(methodName.replace('<', '$').replace('>', '$'));// can be <init> for ctor wrapping
    buf.append(methodDesc.hashCode());//??
    buf.append(className.replace('.', '_').replace('/', '_'));
    return buf.toString().replace('-', '_');
  }

  /**
   * Build the join point invoke method descriptor for code (method or constructor) join points.
   * Depends if the target method is static or not.
   *
   * @param codeModifiers
   * @param codeDesc
   * @param callerTypeName
   * @param calleeTypeName
   * @return
   */
  public static String getInvokeSignatureForCodeJoinPoints(final int codeModifiers,
                                                           final String codeDesc,
                                                           final String callerTypeName,
                                                           final String calleeTypeName) {
    StringBuffer sig = new StringBuffer("(");
    if (!Modifier.isStatic(codeModifiers)) {
      // callee is arg0 for non static target method invoke call
      // else it is skept
      sig.append('L');
      sig.append(calleeTypeName);
      sig.append(';');
    }
    int index = codeDesc.lastIndexOf(')');
    sig.append(codeDesc.substring(1, index));
    sig.append('L');
    sig.append(callerTypeName);
    sig.append(';');
    sig.append(codeDesc.substring(index, codeDesc.length()));
    return sig.toString();
  }

  /**
   * Build the join point invoke method descriptor for field join points.
   * Depends if the target field is static or not.
   *
   * @param fieldModifiers
   * @param fieldDesc
   * @param callerTypeName
   * @param calleeTypeName
   * @return the signature
   */
  public static String getInvokeSignatureForFieldJoinPoints(final int fieldModifiers,
                                                            final String fieldDesc,
                                                            final String callerTypeName,
                                                            final String calleeTypeName) {
    StringBuffer sig = new StringBuffer("(");
    if (!Modifier.isStatic(fieldModifiers)) {
      // callee is arg0 for non static target method invoke call
      // else it is skept
      sig.append('L');
      sig.append(calleeTypeName);
      sig.append(';');
    }
    sig.append(fieldDesc);
    sig.append('L');
    sig.append(callerTypeName);
    sig.append(';');
    sig.append(')');
    sig.append(fieldDesc);
    return sig.toString();
  }

  /**
   * Build the join point invoke method descriptor for handler join points.
   * "Exception invoke(Exception, WithinInstance[can be null])"
   *
   * @param withinTypeName
   * @param exceptionTypeName
   * @return the signature
   */
  public static String getInvokeSignatureForHandlerJoinPoints(final String withinTypeName,
                                                              final String exceptionTypeName) {
    StringBuffer sig = new StringBuffer("(");
    sig.append('L');
    sig.append(exceptionTypeName);
    sig.append(';');
    sig.append('L');//TODO check me [callee + arg0 or just arg0?]
    sig.append(exceptionTypeName);
    sig.append(';');
    sig.append('L');
    sig.append(withinTypeName);
    sig.append(';');
    sig.append(')');
    sig.append('L');
    sig.append(exceptionTypeName);
    sig.append(';');
    return sig.toString();
  }

  /**
   * Build the join point invoke method descriptor for ctor call join points.
   *
   * @param calleeConstructorDesc
   * @param callerTypeName
   * @param calleeTypeName
   * @return the signature
   */
  public static String getInvokeSignatureForConstructorCallJoinPoints(final String calleeConstructorDesc,
                                                                      final String callerTypeName,
                                                                      final String calleeTypeName) {
    StringBuffer sig = new StringBuffer("(");
    int index = calleeConstructorDesc.lastIndexOf(')');
    // callee ctor args
    sig.append(calleeConstructorDesc.substring(1, index));
    // caller
    sig.append('L');
    sig.append(callerTypeName);
    sig.append(';');
    sig.append(")L");
    sig.append(calleeTypeName);
    sig.append(';');
    return sig.toString();
  }

  /**
   * Returns the method name used for constructor body
   *
   * @param calleeTypeName
   * @return
   */
  public static String getConstructorBodyMethodName(final String calleeTypeName) {
    final StringBuffer buf = new StringBuffer();
    buf.append(TransformationConstants.ORIGINAL_METHOD_PREFIX);
    buf.append("init");
    buf.append(TransformationConstants.DELIMITER);
    buf.append(calleeTypeName.replace('.', '_').replace('/', '_'));
    return buf.toString();
  }

  /**
   * Returns the method used for constructor body signature
   * The callee type name is prepended to the constructor signature
   *
   * @param ctorDesc
   * @param calleeTypeName
   * @return
   */
  public static String getConstructorBodyMethodSignature(final String ctorDesc, final String calleeTypeName) {
    StringBuffer sig = new StringBuffer("(L");
    sig.append(calleeTypeName);
    sig.append(";");
    sig.append(ctorDesc.substring(1));
    return sig.toString();
  }

  /**
   * Computes the joinpoint classname : "caller/class_type_hash_suffix"
   * For constructor call joinpoints, the hash of callee name is used as well.
   *
   * @param thisClassName
   * @param thisMemberName
   * @param thisMemberDesc
   * @param targetClassName
   * @param joinPointType
   * @param joinPointHash
   * @return the JIT joinpoint classname
   */
  public static String getJoinPointClassName(final String thisClassName,
                                             final String thisMemberName,
                                             final String thisMemberDesc,
                                             final String targetClassName,
                                             final int joinPointType,
                                             final int joinPointHash) {
    StringBuffer classNameBuf = new StringBuffer(thisClassName);
    // TODO: INNER CLASS OR NOT?
//        classNameBuf.append("$");
    classNameBuf.append('_');
    classNameBuf.append(joinPointType);
    classNameBuf.append('_');
    // whithincode support
    classNameBuf.append((thisMemberName + thisMemberDesc).hashCode());
    classNameBuf.append('_');
    classNameBuf.append(joinPointHash);
    //FIXME needed for other jp ? f.e. Handler ??
    if (joinPointType == JoinPointType.CONSTRUCTOR_CALL_INT || joinPointType == JoinPointType.METHOD_CALL_INT
            || joinPointType == JoinPointType.FIELD_GET_INT
            || joinPointType == JoinPointType.FIELD_SET_INT
            ) {
      classNameBuf.append('_').append(targetClassName.hashCode());
    }
    classNameBuf.append(TransformationConstants.JOIN_POINT_CLASS_SUFFIX);

    //replace minus signs on m_joinPointHash
    return classNameBuf.toString().replace('-', '_').replace('.', '/');
  }

}