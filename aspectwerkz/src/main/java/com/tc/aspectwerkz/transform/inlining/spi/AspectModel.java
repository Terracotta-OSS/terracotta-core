/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.spi;

import com.tc.asm.ClassWriter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Type;
import com.tc.asm.ClassVisitor;

import com.tc.aspectwerkz.definition.AspectDefinition;
import com.tc.aspectwerkz.transform.JoinPointCompiler;
import com.tc.aspectwerkz.transform.inlining.AdviceMethodInfo;
import com.tc.aspectwerkz.transform.inlining.AspectInfo;
import com.tc.aspectwerkz.transform.inlining.compiler.CompilationInfo;
import com.tc.aspectwerkz.transform.inlining.compiler.CompilerInput;
import com.tc.aspectwerkz.reflect.ClassInfo;

/**
 * An aspect model defines a custom hook for the JoinPointCompiler.
 * <p/>
 * The AspectModel is registered using AspectModelManager, and the AspectDefinition is linked to the model
 * "getAspectModelType()" unique identifier.
 * <p/>
 * An no arg constructor instance of the model will be callback during aspect registration for defineAspect(..)
 * </p>
 * During compilation, different aspect model instance can be instantiated per compilation using the getInstance(..)
 * method (not returning "this").
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public interface AspectModel {

  /**
   * A prototype patttern. Ones may return "this" if singleton / non thread safe instance is enough.
   *
   * @param compilationModel
   * @return
   */
  AspectModel getInstance(CompilationInfo.Model compilationModel);

  /**
   * Returns the aspect model type, which is an id for the the special aspect model, can be anything as long
   * as it is unique.
   *
   * @return the aspect model type id
   */
  String getAspectModelType();

  /**
   * Defines the aspect and adds definition to the aspect definition.
   *
   * @param aspectClassInfo
   * @param aspectDef
   * @param loader
   */
  void defineAspect(ClassInfo aspectClassInfo, AspectDefinition aspectDef, ClassLoader loader);

  /**
   * Returns info about the closure class, name and type (interface or class).
   *
   * @return the closure class info
   */
  AroundClosureClassInfo getAroundClosureClassInfo();

  /**
   * Creates the methods required to implement or extend to implement the closure for the specific aspect model type.
   *
   * @param cw
   * @param compiler
   */
  void createMandatoryMethods(ClassWriter cw, JoinPointCompiler compiler);

  /**
   * Creates invocation of the super class for the around closure.
   * <p/>
   * E.g. the invocation of super(..) in the constructor.
   * <p/>
   * Only needed to be implemented if the around closure base class is really a base class and not an interface.
   *
   * @param cv
   */
  void createInvocationOfAroundClosureSuperClass(MethodVisitor cv);

  /**
   * Creates aspect reference field (field in the jit jointpoint class f.e.) for an aspect instance.
   * Creates instantiation of an aspect instance and stores them if appropriate (see createAspectReferenceField).
   *
   * @param cw                 for the jp class beeing compiled
   * @param cv                 for the <clinit> method
   * @param aspectInfo
   * @param joinPointClassName
   */
  void createAndStoreStaticAspectInstantiation(ClassVisitor cw, MethodVisitor cv, AspectInfo aspectInfo, String joinPointClassName);

  /**
   * Initializes instance level aspects, retrieves them from the target instance through the
   * <code>HasInstanceLevelAspect</code> interfaces.
   * <p/>
   * Use by 'perInstance', 'perThis' and 'perTarget' deployment models.
   *
   * @param cv
   * @param input
   * @param aspectInfo
   */
  void createAndStoreRuntimeAspectInstantiation(final MethodVisitor cv,
                                                final CompilerInput input,
                                                final AspectInfo aspectInfo);

  /**
   * Loads the aspect instance on stack.
   * See loadJoinPointInstance(..) and
   *
   * @param cv
   * @param aspectInfo
   */
  void loadAspect(final MethodVisitor cv,
                  final CompilerInput input,
                  final AspectInfo aspectInfo);

  /**
   * Handles the arguments to the around advice.
   *
   * @param cv
   * @param adviceMethodInfo
   */
  void createAroundAdviceArgumentHandling(MethodVisitor cv,
                                          CompilerInput input,
                                          Type[] joinPointArgumentTypes,
                                          AdviceMethodInfo adviceMethodInfo);

  /**
   * Handles the arguments to the before or after (after XXX) advice.
   *
   * @param cv
   * @param input
   * @param joinPointArgumentTypes
   * @param adviceMethodInfo
   * @param specialArgIndex        index on the stack of the throwned exception / returned value (makes sense for after advice,
   *                               else set to INDEX_NOTAVAILABLE)
   */
  public void createBeforeOrAfterAdviceArgumentHandling(MethodVisitor cv,
                                                        CompilerInput input,
                                                        Type[] joinPointArgumentTypes,
                                                        AdviceMethodInfo adviceMethodInfo,
                                                        int specialArgIndex);

  /**
   * Should return true if the aspect model requires that Runtime Type Information (RTTI) is build up
   * for the join point. Needed for reflective systems and systems that does not support f.e. args() binding.
   *
   * @return
   */
  boolean requiresReflectiveInfo();

  /**
   * Info about the around closure class or interface for this specific aspect model.
   *
   * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
   */
  public static class AroundClosureClassInfo {

    private final String m_superClassName;

    private final String[] m_interfaceNames;

    public AroundClosureClassInfo(final String superClassName, final String[] interfaceNames) {
      if (superClassName != null) {
        m_superClassName = superClassName.replace('.', '/');
      } else {
        m_superClassName = null;
      }
      m_interfaceNames = new String[interfaceNames.length];
      for (int i = 0; i < interfaceNames.length; i++) {
        m_interfaceNames[i] = interfaceNames[i].replace('.', '/');
      }
    }

    public String getSuperClassName() {
      return m_superClassName;
    }

    public String[] getInterfaceNames() {
      return m_interfaceNames;
    }

    /**
     * Type safe enum for the around closure class type.
     */
    public static class Type {
      public static final Type INTERFACE = new Type("INTERFACE");
      public static final Type CLASS = new Type("CLASS");
      private final String m_name;

      private Type(String name) {
        m_name = name;
      }

      public String toString() {
        return m_name;
      }
    }

  }
}
