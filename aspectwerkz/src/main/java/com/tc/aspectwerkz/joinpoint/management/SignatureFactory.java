/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.joinpoint.management;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.tc.aspectwerkz.joinpoint.EnclosingStaticJoinPoint;
import com.tc.aspectwerkz.joinpoint.impl.CatchClauseSignatureImpl;
import com.tc.aspectwerkz.joinpoint.impl.ConstructorSignatureImpl;
import com.tc.aspectwerkz.joinpoint.impl.EnclosingStaticJoinPointImpl;
import com.tc.aspectwerkz.joinpoint.impl.FieldSignatureImpl;
import com.tc.aspectwerkz.joinpoint.impl.MethodSignatureImpl;
import com.tc.aspectwerkz.joinpoint.impl.StaticInitializerSignatureImpl;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfoRepository;
import com.tc.aspectwerkz.reflect.ReflectHelper;
import com.tc.aspectwerkz.transform.TransformationConstants;
import com.tc.aspectwerkz.transform.inlining.AsmHelper;

/**
 * Factory class for the signature hierarchy.
 * The helper methods here are called by the JIT jp.
 * <p/>
 * TODO may be worth having a cache
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 * @author <a href="mailto:the_mindstorm@evolva.ro">Alex Popescu</a>
 */
public final class SignatureFactory {

  /**
   * Method signature factory
   *
   * @param declaringClass
   * @param joinPointHash
   * @return
   */
  public static final MethodSignatureImpl newMethodSignature(final Class declaringClass, final int joinPointHash) {
    AsmClassInfoRepository.getRepository(declaringClass.getClassLoader()).removeClassInfo(declaringClass.getName().replace('.', '/'));
    Method[] methods = declaringClass.getDeclaredMethods();
    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      if (ReflectHelper.calculateHash(method) == joinPointHash) {
        return new MethodSignatureImpl(declaringClass, method);
      }
    }
    // lookup in the hierarchy
    MethodSignatureImpl signature = null;
    for (int i = 0; i < declaringClass.getInterfaces().length; i++) {
      signature = newMethodSignature(declaringClass.getInterfaces()[i], joinPointHash);
      if (signature != null) {
        return signature;
      }
    }
    if (declaringClass.getSuperclass() != null) {
      signature = newMethodSignature(declaringClass.getSuperclass(), joinPointHash);
    } else {
      return null;
    }
    return signature;
  }

  /**
   * Field signature factory
   *
   * @param declaringClass
   * @param joinPointHash
   * @return
   */
  public static final FieldSignatureImpl newFieldSignature(final Class declaringClass, final int joinPointHash) {
    Field[] fields = declaringClass.getDeclaredFields();
    for (int i = 0; i < fields.length; i++) {
      Field field = fields[i];
      if (ReflectHelper.calculateHash(field) == joinPointHash) {
        return new FieldSignatureImpl(declaringClass, field);
      }
    }
    // lookup in the hierarchy
    if (declaringClass.getSuperclass() != null) {
      return newFieldSignature(declaringClass.getSuperclass(), joinPointHash);
    } else {
      return null;
    }
  }

  /**
   * Constructor signature factory
   *
   * @param declaringClass
   * @param joinPointHash
   * @return
   */
  public static final ConstructorSignatureImpl newConstructorSignature(final Class declaringClass,
                                                                       final int joinPointHash) {
    for (int i = 0; i < declaringClass.getDeclaredConstructors().length; i++) {
      Constructor c = declaringClass.getDeclaredConstructors()[i];
      if (ReflectHelper.calculateHash(c) == joinPointHash) {
        return new ConstructorSignatureImpl(declaringClass, c);
      }
    }
    // lookup in the hierarchy
    if (declaringClass.getSuperclass() != null) {
      return newConstructorSignature(declaringClass.getSuperclass(), joinPointHash);
    } else {
      return null;
    }
  }

  /**
   * Handler signature factory
   *
   * @param exceptionClass
   * @return
   */
  public static final CatchClauseSignatureImpl newCatchClauseSignature(final Class exceptionClass) {
    return new CatchClauseSignatureImpl(exceptionClass);
  }

  /**
   * Enclosing signature factory, wrapped behind an EnclosingStaticJoinPoint for syntax consistency
   *
   * @param declaringClass
   * @param name
   * @param description
   * @return
   */
  public static EnclosingStaticJoinPoint newEnclosingStaticJoinPoint(
          final Class declaringClass,
          final String name,
          final String description) {
    if (TransformationConstants.CLINIT_METHOD_NAME.equals(name)) {
      return new EnclosingStaticJoinPointImpl(
              new StaticInitializerSignatureImpl(declaringClass),
              JoinPointType.STATIC_INITIALIZATION
      );
    } else if (TransformationConstants.INIT_METHOD_NAME.equals(name)) {
      return new EnclosingStaticJoinPointImpl(
              newConstructorSignature(declaringClass, AsmHelper.calculateConstructorHash(description)),
              JoinPointType.CONSTRUCTOR_EXECUTION
      );
    } else {
      // regular method
      return new EnclosingStaticJoinPointImpl(
              newMethodSignature(declaringClass, AsmHelper.calculateMethodHash(name, description)),
              JoinPointType.METHOD_EXECUTION
      );
    }
  }

  /**
   * Static initialization factory
   *
   * @param declaringClass
   * @return
   */
  public static StaticInitializerSignatureImpl newStaticInitializationSignature(final Class declaringClass) {
    return new StaticInitializerSignatureImpl(declaringClass);
  }

}
