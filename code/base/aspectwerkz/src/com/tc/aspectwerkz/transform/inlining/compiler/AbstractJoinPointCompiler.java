/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.compiler;

import com.tc.asm.ClassWriter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Label;
import com.tc.asm.Type;

import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.aspect.AdviceInfo;
import com.tc.aspectwerkz.aspect.container.AspectFactoryManager;
import com.tc.aspectwerkz.definition.AspectDefinition;
import com.tc.aspectwerkz.joinpoint.management.AdviceInfoContainer;
import com.tc.aspectwerkz.joinpoint.management.JoinPointType;
import com.tc.aspectwerkz.transform.JoinPointCompiler;
import com.tc.aspectwerkz.transform.Properties;
import com.tc.aspectwerkz.transform.TransformationConstants;
import com.tc.aspectwerkz.transform.inlining.AdviceMethodInfo;
import com.tc.aspectwerkz.transform.inlining.AsmHelper;
import com.tc.aspectwerkz.transform.inlining.AspectInfo;
import com.tc.aspectwerkz.transform.inlining.AspectModelManager;
import com.tc.aspectwerkz.transform.inlining.EmittedJoinPoint;
import com.tc.aspectwerkz.transform.inlining.spi.AspectModel;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base class for the different join point compiler implementations.
 * <p/>
 * Compiles/generates a class that represents a specific join point, a class which invokes the advices
 * and the target join point statically.
 * <p/>
 * FIXME: depending on hotswap needs, remove the implements StaticJP or JP decision
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur </a>
 * @author <a href="mailto:the_mindstorm@evolva.ro">Alex Popescu</a>
 */
public abstract class AbstractJoinPointCompiler implements JoinPointCompiler, TransformationConstants {

  public static final boolean DUMP_JP_CLASSES = Properties.DUMP_JIT_CLOSURES;

  // FIXME get rid of public fields - re architect!!!!!!!!!!!!

  protected final String m_callerClassName;
  protected final String m_calleeClassName;
  public final String m_callerClassSignature;
  public final String m_calleeClassSignature;
  public final String m_joinPointClassName;

  protected final int m_joinPointType;
  protected final int m_joinPointHash;
  protected final String m_callerMethodName;
  protected final String m_callerMethodDesc;
  protected final int m_callerMethodModifiers;
  protected final String m_calleeMemberName;
  protected final String m_calleeMemberDesc;
  protected final int m_calleeMemberModifiers;

  private final CompilationInfo.Model m_model;

  protected ClassWriter m_cw;
  protected AspectInfo[] m_aspectInfos;
  protected AspectModel[] m_aspectModels;
  protected AdviceMethodInfo[] m_aroundAdviceMethodInfos;
  protected AdviceMethodInfo[] m_beforeAdviceMethodInfos;
  protected AdviceMethodInfo[] m_afterFinallyAdviceMethodInfos;
  protected AdviceMethodInfo[] m_afterReturningAdviceMethodInfos;
  protected AdviceMethodInfo[] m_afterThrowingAdviceMethodInfos;

  protected boolean m_hasAroundAdvices = false;
  protected boolean m_requiresThisOrTarget = false;
  protected boolean m_requiresJoinPoint = false;
  protected boolean m_requiresProceedMethod = false;

  public String[] m_fieldNames;
  public Type[] m_argumentTypes;
  protected Type m_returnType;
  protected boolean m_isThisAdvisable = false;

  private CompilerInput m_input;

  /**
   * Creates a new join point compiler instance.
   *
   * @param model the compilation model
   */
  public AbstractJoinPointCompiler(final CompilationInfo.Model model) {
    m_model = model;
    m_joinPointClassName = model.getJoinPointClassName();

    final EmittedJoinPoint emittedJoinPoint = model.getEmittedJoinPoint();

    m_joinPointHash = emittedJoinPoint.getJoinPointHash();
    m_joinPointType = emittedJoinPoint.getJoinPointType();

    m_callerMethodName = emittedJoinPoint.getCallerMethodName();
    m_callerMethodDesc = emittedJoinPoint.getCallerMethodDesc();
    m_callerMethodModifiers = emittedJoinPoint.getCallerMethodModifiers();

    m_calleeMemberName = emittedJoinPoint.getCalleeMemberName();
    m_calleeMemberDesc = emittedJoinPoint.getCalleeMemberDesc();
    m_calleeMemberModifiers = emittedJoinPoint.getCalleeMemberModifiers();

    // NOTE: internal compiler class name format is ALWAYS using '/'
    m_callerClassName = emittedJoinPoint.getCallerClassName().replace('.', '/');
    m_calleeClassName = emittedJoinPoint.getCalleeClassName().replace('.', '/');
    m_callerClassSignature = L + emittedJoinPoint.getCallerClassName().replace('.', '/') + SEMICOLON;
    m_calleeClassSignature = L + emittedJoinPoint.getCalleeClassName().replace('.', '/') + SEMICOLON;

    m_argumentTypes = getJoinPointArgumentTypes();
    m_returnType = getJoinPointReturnType();

    // collect information from the compilation model and setup the aspect model
    initialize(model);

    // prepare a CompilerInput instance that will host most of the arguments visible to the models
    initializeCompilerInput();
  }

  //-- implementation of JoinPointCompiler
  public String getCallerClassName() {
    return m_callerClassName;
  }

  public String getCalleeClassName() {
    return m_calleeClassName;
  }

  public String getCallerClassSignature() {
    return m_callerClassSignature;
  }

  public String getCalleeClassSignature() {
    return m_calleeClassSignature;
  }

  public String getJoinPointClassName() {
    return m_joinPointClassName;
  }

  /**
   * Initializes the the join point compiler.
   *
   * @param model the compilation model
   */
  private void initialize(final CompilationInfo.Model model) {
    // collect the advices
    final AdviceInfoContainer advices = model.getAdviceInfoContainer();
    collectAdviceInfo(advices);

    // setup models at the end so that they can override m_requiresJoinPoint
    setupReferencedAspectModels();

    // compute the optimization we can use
    m_hasAroundAdvices = m_aroundAdviceMethodInfos.length > 0;
    // check if 'caller'(this) is Advisable, e.g. can handle runtime per instance deployment
    m_isThisAdvisable = isCallerAdvisable(model);
    m_requiresThisOrTarget = requiresThisOrTarget();
    m_requiresJoinPoint = requiresJoinPoint();
    m_requiresProceedMethod = requiresProceedMethod();

    m_cw = AsmHelper.newClassWriter(true);
  }

  /**
   * Compute and store index and common information that can be passed thru the aspect model
   */
  private void initializeCompilerInput() {
    m_input = new CompilerInput();

    // signatures
    m_input.calleeClassSignature = m_calleeClassSignature;
    m_input.callerClassSignature = m_callerClassSignature;
    m_input.joinPointClassName = m_joinPointClassName;

    // compute the callee and caller index from the invoke(..) signature
    m_input.calleeIndex = INDEX_NOTAVAILABLE;
    m_input.argStartIndex = 0;
    if (!Modifier.isStatic(m_calleeMemberModifiers) &&
            m_joinPointType != JoinPointType.CONSTRUCTOR_CALL_INT &&
            m_joinPointType != JoinPointType.HANDLER_INT) {
      m_input.calleeIndex = 0;
      m_input.argStartIndex++;
    } else {
      // no callee in the invoke(..) parameters for static call/exe/getDefault/set, ctor call and handler jp
      m_input.calleeIndex = INDEX_NOTAVAILABLE;
    }
    // caller is always last
    m_input.callerIndex = m_input.argStartIndex + AsmHelper.getRegisterDepth(m_argumentTypes);

    // custom logic overrides for handler jp
    if (m_joinPointType == JoinPointType.HANDLER_INT) {
      m_input.calleeIndex = 0;
      m_input.callerIndex = 2;
      m_input.argStartIndex = 1;
    }

    // optimization level
    // do we need to keep track of CALLEE, ARGS etc, if not then completely skip it
    // and make use of the optimized join point instance
    // while not using its fields (does not support reentrancy and thread safety)
    m_input.isOptimizedJoinPoint = !m_requiresJoinPoint && !m_requiresProceedMethod;
    if (m_input.isOptimizedJoinPoint) {
      // we will use the static field that host the sole jp shared instance
      m_input.joinPointInstanceIndex = INDEX_NOTAVAILABLE;
    } else {
      // joinpoint will be new() and stored on first local var
      m_input.joinPointInstanceIndex = m_input.callerIndex + 1;
    }
  }

  /**
   * Collects the advice info.
   *
   * @param advices
   */
  private void collectAdviceInfo(final AdviceInfoContainer advices) {
    //final List aspectQualifiedNames = new ArrayList();// in fact a Set but we need indexOf
    final Map aspectInfoByQualifiedName = new HashMap();
    m_beforeAdviceMethodInfos = getAdviceMethodInfos(
            aspectInfoByQualifiedName, advices.getBeforeAdviceInfos()
    );
    m_aroundAdviceMethodInfos = getAdviceMethodInfos(
            aspectInfoByQualifiedName, advices.getAroundAdviceInfos()
    );
    m_afterReturningAdviceMethodInfos = getAdviceMethodInfos(
            aspectInfoByQualifiedName, advices.getAfterReturningAdviceInfos()
    );
    m_afterFinallyAdviceMethodInfos = getAdviceMethodInfos(
            aspectInfoByQualifiedName, advices.getAfterFinallyAdviceInfos()
    );
    m_afterThrowingAdviceMethodInfos = getAdviceMethodInfos(
            aspectInfoByQualifiedName, advices.getAfterThrowingAdviceInfos()
    );

    m_aspectInfos = (AspectInfo[]) aspectInfoByQualifiedName.values().toArray(new AspectInfo[aspectInfoByQualifiedName.size()]);

    //

  }

  /**
   * Checks if the caller class implements the Advisable interface.
   *
   * @param model
   */
  private boolean isCallerAdvisable(final CompilationInfo.Model model) {
    if (!Modifier.isStatic(m_callerMethodModifiers)) {
      ClassInfo[] interfaces = model.getThisClassInfo().getInterfaces();
      for (int i = 0; i < interfaces.length; i++) {
        if (interfaces[i].getName().equals(ADVISABLE_CLASS_JAVA_NAME)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Retrieves and sets the aspect models that are referenced in this compilation phase.
   */
  private void setupReferencedAspectModels() {
    Map aspectModelInstanceByType = new HashMap();
    for (int i = 0; i < m_aspectInfos.length; i++) {
      AspectDefinition aspectDef = m_aspectInfos[i].getAspectDefinition();
      if (!aspectModelInstanceByType.containsKey(aspectDef.getAspectModel())) {
        AspectModel aspectModel = AspectModelManager.getModelFor(aspectDef.getAspectModel()).getInstance(
                m_model
        );
        aspectModelInstanceByType.put(aspectDef.getAspectModel(), aspectModel);
      }
      // set the model for each aspect info for fast access
      AspectModel aspectModel = (AspectModel) aspectModelInstanceByType.get(aspectDef.getAspectModel());
      if (aspectModel == null) {
        throw new Error("Could not find AspectModel " + aspectDef.getAspectModel() + " for " + m_aspectInfos[i].getAspectQualifiedName());
      }
      m_aspectInfos[i].setAspectModel(aspectModel);
    }

    // keep track of the model instance for fast acccess
    m_aspectModels = (AspectModel[]) aspectModelInstanceByType.values().toArray(new AspectModel[]{});
  }

  /**
   * Returns the join point interface class name.
   *
   * @return
   */
  private String getJoinPointInterface() {
    String joinPointInterface;
    if (m_requiresProceedMethod || m_requiresJoinPoint) {
      joinPointInterface = JOIN_POINT_CLASS_NAME;
    } else {
      joinPointInterface = STATIC_JOIN_POINT_CLASS_NAME;
    }
    return joinPointInterface;
  }

  /**
   * Retrieves the advice method infos.
   *
   * @param aspectInfoByQualifiedName populated
   * @param adviceInfos
   * @return
   */
  private AdviceMethodInfo[] getAdviceMethodInfos(final Map aspectInfoByQualifiedName,
                                                  final AdviceInfo[] adviceInfos) {
    List adviceMethodInfosSet = new ArrayList();
    for (int i = 0; i < adviceInfos.length; i++) {
      AdviceInfo adviceInfo = adviceInfos[i];

      // if we have a perinstance deployed aspect and a static member CALLER -> skip and go on
      DeploymentModel deploymentModel = adviceInfo.getAspectDeploymentModel();

      if (requiresCallerInstance(deploymentModel) && Modifier.isStatic(m_callerMethodModifiers)) {
        continue;
      }
      if (requiresCalleeInstance(deploymentModel) && Modifier.isStatic(m_calleeMemberModifiers)) {
        continue;
      }

      final String aspectClassName = adviceInfo.getAspectClassName().replace('.', '/');

      final AspectInfo aspectInfo;
      if (!aspectInfoByQualifiedName.containsKey(adviceInfo.getAspectQualifiedName())) {
        aspectInfo = new AspectInfo(
                adviceInfo.getAdviceDefinition().getAspectDefinition(),
                ASPECT_FIELD_PREFIX + (aspectInfoByQualifiedName.size()),
                aspectClassName,
                L + aspectClassName + SEMICOLON
        );
        aspectInfoByQualifiedName.put(adviceInfo.getAspectQualifiedName(), aspectInfo);
      } else {
        aspectInfo = (AspectInfo) aspectInfoByQualifiedName.get(adviceInfo.getAspectQualifiedName());
      }

      AdviceMethodInfo adviceMethodInfo = new AdviceMethodInfo(
              aspectInfo,
              adviceInfo,
              m_callerClassSignature,
              m_calleeClassSignature,
              m_joinPointClassName,
              m_calleeMemberDesc
      );
      adviceMethodInfosSet.add(adviceMethodInfo);
    }
    return (AdviceMethodInfo[]) adviceMethodInfosSet.toArray(new AdviceMethodInfo[adviceMethodInfosSet.size()]);
  }

  /**
   * Creates join point specific fields.
   */
  protected abstract void createJoinPointSpecificFields();

  /**
   * Creates the signature for the join point.
   *
   * @param cv
   */
  protected abstract void createSignature(final MethodVisitor cv);

  /**
   * Optimized implementation that does not retrieve the parameters from the join point instance but is passed
   * directly to the method from the input parameters in the 'invoke' method. Can only be used if no around advice
   * exists.
   *
   * @param cv
   * @param input
   */
  protected abstract void createInlinedJoinPointInvocation(final MethodVisitor cv,
                                                           final CompilerInput input);

  /**
   * Creates a call to the target join point, the parameter(s) to the join point are retrieved from the invocation
   * local join point instance.
   *
   * @param cv
   */
  protected abstract void createJoinPointInvocation(final MethodVisitor cv);

  /**
   * Returns the join points return type.
   *
   * @return
   */
  protected abstract Type getJoinPointReturnType();

  /**
   * Returns the join points argument type(s).
   *
   * @return
   */
  protected abstract Type[] getJoinPointArgumentTypes();

  /**
   * Creates the getRtti method
   */
  protected abstract void createGetRttiMethod();

  /**
   * Creates the getSignature method
   */
  protected abstract void createGetSignatureMethod();

  /**
   * Compiles a join point class, one specific class for each distinct join point. The compiled join point class
   * inherits the base join point class.
   *
   * @return the generated, compiled and loaded join point class
   */
  public final byte[] compile() {
    try {
      createClassHeader();
      createFieldsCommonToAllJoinPoints();
      createJoinPointSpecificFields();
      createMandatoryMethodInAspectModels();
      createStaticInitializer();
      createClinit();
      createInit();
      createUtilityMethods();
      createGetSignatureMethod();
      createInvokeMethod();
      if (m_requiresProceedMethod) {
        // prepare a new CompilerInput since jp index changes when in proceed()
        createProceedMethod(m_input.getCopyForProceed());
      }
      if (m_requiresJoinPoint) {
        createGetRttiMethod();
      }
      m_cw.visitEnd();

      if (DUMP_JP_CLASSES) {
        AsmHelper.dumpClass(Properties.DUMP_DIR_CLOSURES, m_joinPointClassName, m_cw);
      }
      return m_cw.toByteArray();

    } catch (Exception e) {
      e.printStackTrace();
      StringBuffer buf = new StringBuffer();
      buf.append("could not compile join point instance for join point with hash [");
      buf.append(m_joinPointHash);
      buf.append("] and declaring class [");
      buf.append(m_callerClassName);
      buf.append("] due to: ");
      if (e instanceof InvocationTargetException) {
        buf.append(((InvocationTargetException) e).getTargetException().toString());
      } else {
        buf.append(e.toString());
      }
      throw new RuntimeException(buf.toString());
    }
  }

  /**
   * Creates join point specific fields.
   */
  private void createFieldsCommonToAllJoinPoints() {
    if (m_returnType.getSort() != Type.VOID) {
      m_cw.visitField(ACC_PRIVATE, RETURN_VALUE_FIELD_NAME, m_returnType.getDescriptor(), null, null);
    }
    m_cw.visitField(
            ACC_PRIVATE + ACC_STATIC,
            TARGET_CLASS_FIELD_NAME_IN_JP,
            CLASS_CLASS_SIGNATURE,
            null,
            null
    );

    m_cw.visitField(
            ACC_PRIVATE + ACC_STATIC + ACC_FINAL,
            THIS_CLASS_FIELD_NAME_IN_JP,
            CLASS_CLASS_SIGNATURE,
            null,
            null
    );

    m_cw.visitField(
            ACC_PRIVATE + ACC_STATIC + ACC_FINAL,
            ENCLOSING_SJP_FIELD_NAME,
            ENCLOSING_SJP_FIELD_CLASS_SIGNATURE,
            null,
            null
    );

    m_cw.visitField(ACC_PRIVATE + ACC_STATIC, META_DATA_FIELD_NAME, MAP_CLASS_SIGNATURE, null, null);
    m_cw.visitField(
            ACC_PRIVATE + ACC_STATIC,
            OPTIMIZED_JOIN_POINT_INSTANCE_FIELD_NAME,
            L + m_joinPointClassName + SEMICOLON,
            null, null
    );
    m_cw.visitField(ACC_PRIVATE, CALLEE_INSTANCE_FIELD_NAME, m_calleeClassSignature, null, null);
    m_cw.visitField(ACC_PRIVATE, CALLER_INSTANCE_FIELD_NAME, m_callerClassSignature, null, null);
    m_cw.visitField(ACC_PRIVATE, STACK_FRAME_COUNTER_FIELD_NAME, I, null, null);

    if (m_isThisAdvisable) {
      m_cw.visitField(ACC_PRIVATE, INTERCEPTOR_INDEX_FIELD_NAME, I, null, null);

      m_cw.visitField(
              ACC_PRIVATE, AROUND_INTERCEPTORS_FIELD_NAME,
              AROUND_ADVICE_ARRAY_CLASS_SIGNATURE, null, null
      );
      m_cw.visitField(ACC_PRIVATE, NR_OF_AROUND_INTERCEPTORS_FIELD_NAME, I, null, null);

      m_cw.visitField(
              ACC_PRIVATE, BEFORE_INTERCEPTORS_FIELD_NAME,
              BEFORE_ADVICE_ARRAY_CLASS_SIGNATURE, null, null
      );
      m_cw.visitField(ACC_PRIVATE, NR_OF_BEFORE_INTERCEPTORS_FIELD_NAME, I, null, null);

      m_cw.visitField(
              ACC_PRIVATE, AFTER_INTERCEPTORS_FIELD_NAME,
              AFTER_ADVICE_ARRAY_CLASS_SIGNATURE, null, null
      );
      m_cw.visitField(ACC_PRIVATE, NR_OF_AFTER_INTERCEPTORS_FIELD_NAME, I, null, null);

      m_cw.visitField(
              ACC_PRIVATE, AFTER_RETURNING_INTERCEPTORS_FIELD_NAME,
              AFTER_RETURNING_ADVICE_ARRAY_CLASS_SIGNATURE, null, null
      );
      m_cw.visitField(ACC_PRIVATE, NR_OF_AFTER_RETURNING_INTERCEPTORS_FIELD_NAME, I, null, null);

      m_cw.visitField(
              ACC_PRIVATE, AFTER_THROWING_INTERCEPTORS_FIELD_NAME,
              AFTER_THROWING_ADVICE_ARRAY_CLASS_SIGNATURE, null, null
      );
      m_cw.visitField(ACC_PRIVATE, NR_OF_AFTER_THROWING_INTERCEPTORS_FIELD_NAME, I, null, null);
    }
  }

  /**
   * Creates the clinit method for the join point.
   */
  private void createClinit() {
    MethodVisitor cv = m_cw.visitMethod(ACC_STATIC, CLINIT_METHOD_NAME, NO_PARAM_RETURN_VOID_SIGNATURE, null, null);
    cv.visitMethodInsn(
            INVOKESTATIC, m_joinPointClassName,
            STATIC_INITIALIZATION_METHOD_NAME, NO_PARAM_RETURN_VOID_SIGNATURE
    );
    cv.visitInsn(RETURN);
    cv.visitMaxs(0, 0);
  }

  /**
   * Creates the init method for the join point.
   */
  private void createInit() {
    MethodVisitor cv = m_cw.visitMethod(ACC_PRIVATE, INIT_METHOD_NAME, NO_PARAM_RETURN_VOID_SIGNATURE, null, null);
    cv.visitVarInsn(ALOAD, 0);

    boolean hasAroundClosureBaseClass = false;
    AspectModel aspectModel = null;

    for (int i = 0; i < m_aspectModels.length; i++) {
      aspectModel = m_aspectModels[i];
      if (aspectModel.getAroundClosureClassInfo().getSuperClassName() != null
              && !OBJECT_CLASS_NAME.equals(aspectModel.getAroundClosureClassInfo().getSuperClassName())) {
        hasAroundClosureBaseClass = true;
        break;
      }
    }

    if (hasAroundClosureBaseClass) {
      // invoke the super class constructor
      aspectModel.createInvocationOfAroundClosureSuperClass(cv);
    } else {
      // invoke the constructor of java.lang.Object
      cv.visitMethodInsn(INVOKESPECIAL, OBJECT_CLASS_NAME, INIT_METHOD_NAME, NO_PARAM_RETURN_VOID_SIGNATURE);
    }

    resetStackFrameCounter(cv);

    cv.visitInsn(RETURN);
    cv.visitMaxs(0, 0);
  }

  /**
   * Creates the class header for the join point.
   */
  private void createClassHeader() {

    Set interfaces = new HashSet();
    String baseClass = OBJECT_CLASS_NAME;

    // getDefault the different aspect models required interfaces
    for (int i = 0; i < m_aspectModels.length; i++) {
      AspectModel aspectModel = m_aspectModels[i];
      AspectModel.AroundClosureClassInfo closureClassInfo = aspectModel.getAroundClosureClassInfo();
      final String superClassName = closureClassInfo.getSuperClassName();
      final String[] interfaceNames = closureClassInfo.getInterfaceNames();
      if (superClassName != null) {
        if (!baseClass.equals(OBJECT_CLASS_NAME)) {
          throw new RuntimeException(
                  "compiled join point can only subclass one around closure base class but more than registered aspect model requires a closure base class"
          );
        }
        baseClass = superClassName;
      }
      if (interfaceNames.length != 0) {
        for (int j = 0; j < interfaceNames.length; j++) {
          interfaces.add(interfaceNames[j]);
        }
      }
    }

    int i = 1;
    String[] interfaceArr = new String[interfaces.size() + 1];
    interfaceArr[0] = getJoinPointInterface();
    for (Iterator it = interfaces.iterator(); it.hasNext(); i++) {
      interfaceArr[i] = (String) it.next();
    }

    m_cw.visit(
            AsmHelper.JAVA_VERSION,
            ACC_PUBLIC + ACC_SUPER,
            m_joinPointClassName,
            null,
            baseClass,
            interfaceArr
    );
  }

  /**
   * Creates the methods that are mandatory methods in the around closure in the different aspect models.
   */
  private void createMandatoryMethodInAspectModels() {
    for (int i = 0; i < m_aspectModels.length; i++) {
      m_aspectModels[i].createMandatoryMethods(m_cw, this);
    }
  }

  /**
   * Creates the static initialization method (not clinit) for the join point.
   */
  private void createStaticInitializer() {
    MethodVisitor cv = m_cw.visitMethod(
            ACC_STATIC | ACC_PUBLIC,
            STATIC_INITIALIZATION_METHOD_NAME,
            NO_PARAM_RETURN_VOID_SIGNATURE,
            null, null
    );

    Label tryLabel = new Label();
    cv.visitLabel(tryLabel);
    cv.visitLdcInsn(m_calleeClassName.replace('/', '.'));
    cv.visitMethodInsn(INVOKESTATIC, CLASS_CLASS, FOR_NAME_METHOD_NAME, FOR_NAME_METHOD_SIGNATURE);
    cv.visitFieldInsn(PUTSTATIC, m_joinPointClassName, TARGET_CLASS_FIELD_NAME_IN_JP, CLASS_CLASS_SIGNATURE);

    cv.visitLdcInsn(m_callerClassName.replace('/', '.'));
    cv.visitMethodInsn(INVOKESTATIC, CLASS_CLASS, FOR_NAME_METHOD_NAME, FOR_NAME_METHOD_SIGNATURE);
    cv.visitFieldInsn(PUTSTATIC, m_joinPointClassName, THIS_CLASS_FIELD_NAME_IN_JP, CLASS_CLASS_SIGNATURE);

    Label finallyLabel = new Label();
    cv.visitLabel(finallyLabel);

    Label gotoFinallyLabel = new Label();
    cv.visitJumpInsn(GOTO, gotoFinallyLabel);

    Label catchLabel = new Label();
    cv.visitLabel(catchLabel);
    cv.visitVarInsn(ASTORE, 0);

    cv.visitVarInsn(ALOAD, 0);
    cv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V");

    cv.visitTypeInsn(NEW, RUNTIME_EXCEPTION_CLASS_NAME);
    cv.visitInsn(DUP);
    cv.visitLdcInsn(
            "could not load target class using Class.forName() in generated join point base class "
                    + m_joinPointClassName
    );

    cv.visitMethodInsn(
            INVOKESPECIAL,
            RUNTIME_EXCEPTION_CLASS_NAME,
            INIT_METHOD_NAME,
            RUNTIME_EXCEPTION_INIT_METHOD_SIGNATURE
    );

    cv.visitInsn(ATHROW);
    cv.visitLabel(gotoFinallyLabel);

    // create the enclosing static joinpoint
    createEnclosingStaticJoinPoint(cv);

    // create the metadata map
    cv.visitTypeInsn(NEW, HASH_MAP_CLASS_NAME);
    cv.visitInsn(DUP);
    cv.visitMethodInsn(INVOKESPECIAL, HASH_MAP_CLASS_NAME, INIT_METHOD_NAME, NO_PARAM_RETURN_VOID_SIGNATURE);
    cv.visitFieldInsn(PUTSTATIC, m_joinPointClassName, META_DATA_FIELD_NAME, MAP_CLASS_SIGNATURE);

    // create the Signature instance
    createSignature(cv);

    // create the static JoinPoint instance
    cv.visitTypeInsn(NEW, m_joinPointClassName);
    cv.visitInsn(DUP);
    cv.visitMethodInsn(INVOKESPECIAL, m_joinPointClassName, INIT_METHOD_NAME, NO_PARAM_RETURN_VOID_SIGNATURE);
    cv.visitFieldInsn(
            PUTSTATIC,
            m_joinPointClassName,
            OPTIMIZED_JOIN_POINT_INSTANCE_FIELD_NAME,
            L + m_joinPointClassName + SEMICOLON
    );

    // ensure aspect factories are all loaded
    for (int i = 0; i < m_aspectInfos.length; i++) {
      AspectInfo m_aspectInfo = m_aspectInfos[i];

      cv.visitLdcInsn(m_aspectInfo.getAspectFactoryClassName());
      cv.visitLdcInsn(m_aspectInfo.getAspectDefinition().getSystemDefinition().getUuid());
      cv.visitLdcInsn(m_aspectInfo.getAspectClassName());
      cv.visitLdcInsn(m_aspectInfo.getAspectQualifiedName());
      AsmHelper.loadStringConstant(cv, m_aspectInfo.getAspectDefinition().getContainerClassName());
      //TODO AVF do it once per aspect def
      StringBuffer sb = new StringBuffer();
      boolean hasOne = false;
      boolean isFirst = true;
      for (Iterator iterator = m_aspectInfo.getAspectDefinition().getParameters().entrySet().iterator(); iterator.hasNext();)
      {
        Map.Entry entry = (Map.Entry) iterator.next();
        if (!isFirst) {
          sb.append(DELIMITER);
        }
        isFirst = false;
        hasOne = true;
        sb.append(entry.getKey()).append(DELIMITER).append(entry.getValue());
      }
      if (hasOne) {
        cv.visitLdcInsn(sb.toString());
      } else {
        cv.visitInsn(ACONST_NULL);
      }
      cv.visitFieldInsn(GETSTATIC, m_joinPointClassName, THIS_CLASS_FIELD_NAME_IN_JP, CLASS_CLASS_SIGNATURE);
      cv.visitMethodInsn(INVOKEVIRTUAL, CLASS_CLASS, GETCLASSLOADER_METHOD_NAME, CLASS_CLASS_GETCLASSLOADER_METHOD_SIGNATURE);
      cv.visitLdcInsn(m_aspectInfo.getDeploymentModel().toString());
      cv.visitMethodInsn(
              INVOKESTATIC,
              Type.getInternalName(AspectFactoryManager.class),
              "loadAspectFactory",
              "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/ClassLoader;Ljava/lang/String;)V"
      );
    }

    // create and initialize the aspect fields
    for (int i = 0; i < m_aspectInfos.length; i++) {
      m_aspectInfos[i].getAspectModel().createAndStoreStaticAspectInstantiation(
              m_cw,
              cv,
              m_aspectInfos[i],
              m_joinPointClassName
      );
    }

    cv.visitInsn(RETURN);
    cv.visitTryCatchBlock(tryLabel, finallyLabel, catchLabel, CLASS_NOT_FOUND_EXCEPTION_CLASS_NAME);
    cv.visitMaxs(0, 0);
  }

  /**
   * Add and initialize the static field for enclosing joint point static part
   *
   * @param cv
   */
  private void createEnclosingStaticJoinPoint(MethodVisitor cv) {
    cv.visitFieldInsn(
            GETSTATIC,
            m_joinPointClassName,
            THIS_CLASS_FIELD_NAME_IN_JP,
            CLASS_CLASS_SIGNATURE
    );
    cv.visitLdcInsn(m_callerMethodName);
    cv.visitLdcInsn(m_callerMethodDesc);

    cv.visitMethodInsn(
            INVOKESTATIC,
            SIGNATURE_FACTORY_CLASS,
            NEW_ENCLOSING_SJP_METHOD_NAME,
            NEW_ENCLOSING_SJP_METHOD_SIGNATURE
    );
    cv.visitFieldInsn(
            PUTSTATIC,
            m_joinPointClassName,
            ENCLOSING_SJP_FIELD_NAME,
            ENCLOSING_SJP_FIELD_CLASS_SIGNATURE
    );
  }

  /**
   * Creates the 'invoke' method. If possible delegates to the target join point directly, e.g. does not invoke the
   * 'proceed' method (Used when a join point has zero around advice).
   * <p/>
   * The implementation here is suitable for regular JP but not for delegating JP upon hotswap
   */
  protected void createInvokeMethod() {

    final String invokeDesc = buildInvokeMethodSignature();

    // create the method
    MethodVisitor cv = m_cw.visitMethod(
            ACC_PUBLIC + ACC_FINAL + ACC_STATIC,
            INVOKE_METHOD_NAME,
            invokeDesc,
            null,
            new String[]{
                    THROWABLE_CLASS_NAME
            }
    );

    if (!m_input.isOptimizedJoinPoint) {
      // create a new JP and makes use of it
      createInvocationLocalJoinPointInstance(cv, m_input);
    }

    //FIXME: see loadAspect and AssociationScopeTest_2_1456425365_738_9001546___AW_JoinPoint f.e.
    // there is redundant checks because
    // the system perObject aspect for a perX aspect will be called *AFTER* the initializeInstanceLevelAspects
    // and thus the aspects.hasAspect will change in the middle of the invoke method
    //
    // flow should be: invoke perObject before aspect, init instance level, invoke all other before aspects
    // we can wether have a createBeforeBefore(...) that checks for this perObject aspect

    // initialize the instance level aspects (perInstance)
    initializeInstanceLevelAspects(cv, m_input);

    // before advices
    createBeforeAdviceInvocations(cv, m_input);

    // handle different combinations of after advice (finally/throwing/returning)
    if (m_afterFinallyAdviceMethodInfos.length == 0 &&
            m_afterThrowingAdviceMethodInfos.length == 0 &&
            !m_isThisAdvisable) {
      createPartOfInvokeMethodWithoutAfterFinallyAndAfterThrowingAdviceTypes(cv, m_input);
    } else if (m_afterThrowingAdviceMethodInfos.length == 0 &&
            !m_isThisAdvisable) {
      createPartOfInvokeMethodWithoutAfterThrowingAdviceTypes(cv, m_input);
    } else {
      createPartOfInvokeMethodWithAllAdviceTypes(cv, m_input);
    }

    cv.visitMaxs(0, 0);
  }

  /**
   * Initializes instance level aspects, retrieves them from the target instance through the
   * <code>HasInstanceLevelAspect</code> interfaces.
   * <p/>
   * Use by 'perInstance', 'perThis' and 'perTarget' deployment models.
   *
   * @param cv
   * @param input
   */
  private void initializeInstanceLevelAspects(final MethodVisitor cv, final CompilerInput input) {
    for (int i = 0; i < m_aspectInfos.length; i++) {
      m_aspectInfos[i].getAspectModel().createAndStoreRuntimeAspectInstantiation(cv, input, m_aspectInfos[i]);
    }
  }


  /**
   * @param cv
   * @param input
   */
  private void createPartOfInvokeMethodWithAllAdviceTypes(final MethodVisitor cv,
                                                          final CompilerInput input) {
    final int returnValueIndex = (input.joinPointInstanceIndex != INDEX_NOTAVAILABLE) ?
            (input.joinPointInstanceIndex + 1) : input.callerIndex + 1;
    final int exceptionIndex1 = returnValueIndex + 1;
    final int exceptionIndex2 = returnValueIndex + 2;

    cv.visitInsn(ACONST_NULL);
    cv.visitVarInsn(ASTORE, returnValueIndex);

    Label tryLabel = new Label();
    cv.visitLabel(tryLabel);
    if (!m_requiresProceedMethod) {
      // if no around advice then optimize by invoking the target JP directly and no call to proceed()
      createInlinedJoinPointInvocation(cv, input);
      int stackIndex = returnValueIndex;//use another int since storeType will update it
      AsmHelper.storeType(cv, stackIndex, m_returnType);
      addReturnedValueToJoinPoint(cv, input, returnValueIndex, false);
    } else {
      createInvocationToProceedMethod(cv, input.joinPointInstanceIndex, returnValueIndex);
    }

    createAfterReturningAdviceInvocations(cv, input);

    Label finallyLabel1 = new Label();
    cv.visitLabel(finallyLabel1);

    if (m_isThisAdvisable) {
      final int registerDepth = input.callerIndex + 2; // caller is using last register + possible return value
      createAfterInterceptorInvocations(cv, input.joinPointInstanceIndex, registerDepth);
    }
    createAfterFinallyAdviceInvocations(cv, input);

    Label gotoFinallyLabel = new Label();
    cv.visitJumpInsn(GOTO, gotoFinallyLabel);

    Label catchLabel = new Label();
    cv.visitLabel(catchLabel);

    // store the exception
    cv.visitVarInsn(ASTORE, exceptionIndex1);

    if (m_isThisAdvisable) {
      createAfterThrowingInterceptorInvocations(cv, input.joinPointInstanceIndex, exceptionIndex1);
    }

    // loop over the after throwing advices
    for (int i = m_afterThrowingAdviceMethodInfos.length - 1; i >= 0; i--) {
      AdviceMethodInfo advice = m_afterThrowingAdviceMethodInfos[i];

      // set the exception argument index
      advice.setSpecialArgumentIndex(exceptionIndex1);

      // if (e instanceof TYPE) {...}
      cv.visitVarInsn(ALOAD, exceptionIndex1);

      final String specialArgTypeName = advice.getSpecialArgumentTypeName();
      if (specialArgTypeName != null) {
        // after throwing <TYPE>
        cv.visitTypeInsn(INSTANCEOF, specialArgTypeName);

        Label ifInstanceOfLabel = new Label();
        cv.visitJumpInsn(IFEQ, ifInstanceOfLabel);

        // after throwing advice invocation
        createAfterAdviceInvocation(cv, input, advice, exceptionIndex1);

        cv.visitLabel(ifInstanceOfLabel);
      } else {
        // after throwing
        createAfterAdviceInvocation(cv, input, advice, INDEX_NOTAVAILABLE);
      }
    }

    // rethrow exception
    cv.visitVarInsn(ALOAD, exceptionIndex1);
    cv.visitInsn(ATHROW);

    // store exception
    Label exceptionLabel = new Label();
    cv.visitLabel(exceptionLabel);
    cv.visitVarInsn(ASTORE, exceptionIndex2);

    // after finally advice invocation
    Label finallyLabel2 = new Label();
    cv.visitLabel(finallyLabel2);

    if (m_isThisAdvisable) {
      final int registerDepth = input.callerIndex + 2; // caller is using last register + possible return value
      createAfterInterceptorInvocations(cv, input.joinPointInstanceIndex, registerDepth);
    }
    createAfterFinallyAdviceInvocations(cv, input);

    // rethrow exception
    cv.visitVarInsn(ALOAD, exceptionIndex2);
    cv.visitInsn(ATHROW);
    cv.visitLabel(gotoFinallyLabel);

    // unwrap if around advice and return in all cases
    if (m_returnType.getSort() != Type.VOID) {
      if (m_requiresProceedMethod) {
        cv.visitVarInsn(ALOAD, returnValueIndex);
        AsmHelper.unwrapType(cv, m_returnType);
      } else {
        AsmHelper.loadType(cv, returnValueIndex, m_returnType);
      }
    }

    AsmHelper.addReturnStatement(cv, m_returnType);

    // build up the exception table
    cv.visitTryCatchBlock(tryLabel, finallyLabel1, catchLabel, THROWABLE_CLASS_NAME);
    cv.visitTryCatchBlock(tryLabel, finallyLabel1, exceptionLabel, null);
    cv.visitTryCatchBlock(catchLabel, finallyLabel2, exceptionLabel, null);
  }

  /**
   * @param cv
   * @param input
   */
  private void createPartOfInvokeMethodWithoutAfterThrowingAdviceTypes(final MethodVisitor cv,
                                                                       final CompilerInput input) {
    final int returnValueIndex = (input.joinPointInstanceIndex != INDEX_NOTAVAILABLE) ?
            (input.joinPointInstanceIndex + 1) : input.callerIndex + 1;
    final int exceptionIndex = returnValueIndex + 1;

    cv.visitInsn(ACONST_NULL);
    cv.visitVarInsn(ASTORE, returnValueIndex);

    Label tryLabel = new Label();
    cv.visitLabel(tryLabel);
    if (!m_requiresProceedMethod) {
      // if no around advice then optimize by invoking the target JP directly and no call to proceed()
      createInlinedJoinPointInvocation(cv, input);
      int stackIndex = returnValueIndex;//use another int since storeType will update it
      AsmHelper.storeType(cv, stackIndex, m_returnType);
      addReturnedValueToJoinPoint(cv, input, returnValueIndex, false);
    } else {
      createInvocationToProceedMethod(cv, input.joinPointInstanceIndex, returnValueIndex);
    }

    createAfterReturningAdviceInvocations(cv, input);

    Label finallyLabel1 = new Label();
    cv.visitLabel(finallyLabel1);

    createAfterFinallyAdviceInvocations(cv, input);

    Label gotoFinallyLabel = new Label();
    cv.visitJumpInsn(GOTO, gotoFinallyLabel);

    Label exceptionLabel = new Label();
    cv.visitLabel(exceptionLabel);
    cv.visitVarInsn(ASTORE, exceptionIndex);

    Label finallyLabel2 = new Label();
    cv.visitLabel(finallyLabel2);

    createAfterFinallyAdviceInvocations(cv, input);

    cv.visitVarInsn(ALOAD, exceptionIndex);
    cv.visitInsn(ATHROW);

    cv.visitLabel(gotoFinallyLabel);

    // unwrap if around advice and return in all cases
    if (m_returnType.getSort() != Type.VOID) {
      if (m_requiresProceedMethod) {
        cv.visitVarInsn(ALOAD, returnValueIndex);
        AsmHelper.unwrapType(cv, m_returnType);
      } else {
        AsmHelper.loadType(cv, returnValueIndex, m_returnType);
      }
    }

    AsmHelper.addReturnStatement(cv, m_returnType);

    cv.visitTryCatchBlock(tryLabel, finallyLabel1, exceptionLabel, null);
    cv.visitTryCatchBlock(exceptionLabel, finallyLabel2, exceptionLabel, null);
  }

  /**
   * @param cv
   * @param input
   */
  private void createPartOfInvokeMethodWithoutAfterFinallyAndAfterThrowingAdviceTypes(final MethodVisitor cv,
                                                                                      final CompilerInput input) {

    final int returnValueIndex = (input.joinPointInstanceIndex != INDEX_NOTAVAILABLE) ?
            (input.joinPointInstanceIndex + 1) : input.callerIndex + 1;
    if (!m_requiresProceedMethod) {
      // if no around advice then optimize by invoking the target JP directly and no call to proceed()
      createInlinedJoinPointInvocation(cv, input);
      int stackIndex = returnValueIndex;//use another int since storeType will update it
      AsmHelper.storeType(cv, stackIndex, m_returnType);
      addReturnedValueToJoinPoint(cv, input, returnValueIndex, false);
    } else {
      createInvocationToProceedMethod(cv, input.joinPointInstanceIndex, returnValueIndex);
    }

    // after returning advice invocations
    createAfterReturningAdviceInvocations(cv, input);

    // unwrap if around advice and return in all cases
    if (m_returnType.getSort() != Type.VOID) {
      if (m_requiresProceedMethod) {
        cv.visitVarInsn(ALOAD, returnValueIndex);
        AsmHelper.unwrapType(cv, m_returnType);
      } else {
        AsmHelper.loadType(cv, returnValueIndex, m_returnType);
      }
    }

    AsmHelper.addReturnStatement(cv, m_returnType);
  }

  /**
   * Creates an invocation to the proceed method.
   *
   * @param cv
   * @param joinPointInstanceIndex
   * @param returnValueIndex
   */
  private void createInvocationToProceedMethod(final MethodVisitor cv,
                                               final int joinPointInstanceIndex,
                                               final int returnValueIndex) {
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitMethodInsn(INVOKEVIRTUAL, m_joinPointClassName, PROCEED_METHOD_NAME, PROCEED_METHOD_SIGNATURE);
    cv.visitVarInsn(ASTORE, returnValueIndex);
  }

  /**
   * Creates an "invocation local" join point instance, e.g. one join point per invocation. Needed for thread-safety
   * when invoking around advice.
   *
   * @param cv
   * @param input
   */
  private void createInvocationLocalJoinPointInstance(final MethodVisitor cv, final CompilerInput input) {
    // create the join point instance
    cv.visitTypeInsn(NEW, m_joinPointClassName);
    cv.visitInsn(DUP);
    cv.visitMethodInsn(INVOKESPECIAL, m_joinPointClassName, INIT_METHOD_NAME, NO_PARAM_RETURN_VOID_SIGNATURE);

    // store the jp on the stack
    cv.visitVarInsn(ASTORE, input.joinPointInstanceIndex);

    // set the argument fields in the join point instance (jp.m_arg<i> = <arg_i>)
    int argStackIndex = input.argStartIndex;
    for (int i = 0; i < m_fieldNames.length; i++) {
      String fieldName = m_fieldNames[i];
      cv.visitVarInsn(ALOAD, input.joinPointInstanceIndex);
      Type type = m_argumentTypes[i];
      argStackIndex = AsmHelper.loadType(cv, argStackIndex, type);
      cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, fieldName, type.getDescriptor());
    }

    // caller (can be assigned to null)
    cv.visitVarInsn(ALOAD, input.joinPointInstanceIndex);
    cv.visitVarInsn(ALOAD, input.callerIndex);
    cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, CALLER_INSTANCE_FIELD_NAME, m_callerClassSignature);

    // callee (can be not available)
    cv.visitVarInsn(ALOAD, input.joinPointInstanceIndex);
    if (input.calleeIndex != INDEX_NOTAVAILABLE) {
      cv.visitVarInsn(ALOAD, 0);
    } else {
      cv.visitInsn(ACONST_NULL);
    }
    cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, CALLEE_INSTANCE_FIELD_NAME, m_calleeClassSignature);

    if (m_isThisAdvisable) {
      createInitializationForAdvisableManagement(cv, input.joinPointInstanceIndex, input.callerIndex);
    }
  }

  /**
   * Create the proceed() method.
   *
   * @param input a slightly different CompilerInput since jp index, is changed and caller and callee are meaningless
   *              in the proceed() method.
   */
  private void createProceedMethod(CompilerInput input) {

    MethodVisitor cv = m_cw.visitMethod(
            ACC_PUBLIC | ACC_FINAL,
            PROCEED_METHOD_NAME,
            PROCEED_METHOD_SIGNATURE,
            null,
            new String[]{
                    THROWABLE_CLASS_NAME
            }
    );

    if (m_isThisAdvisable) {
      createAroundInterceptorInvocations(cv);
    }

    incrementStackFrameCounter(cv);

    // set up the labels
    Label tryLabel = new Label();
    Label defaultCaseLabel = new Label();
    Label gotoLabel = new Label();
    Label handlerLabel = new Label();
    Label endLabel = new Label();

    int nrOfCases = m_aroundAdviceMethodInfos.length;
    if (m_isThisAdvisable) {
      nrOfCases++;
    }

    Label[] caseLabels = new Label[nrOfCases];
    Label[] returnLabels = new Label[nrOfCases];
    int[] caseNumbers = new int[nrOfCases];
    for (int i = 0; i < caseLabels.length; i++) {
      caseLabels[i] = new Label();
      caseNumbers[i] = i;
    }
    for (int i = 0; i < returnLabels.length; i++) {
      returnLabels[i] = new Label();
    }

    // start try-catch block
    cv.visitLabel(tryLabel);

    // start the switch block and set the stackframe as the param to the switch
    cv.visitVarInsn(ALOAD, 0);
    cv.visitFieldInsn(GETFIELD, m_joinPointClassName, STACK_FRAME_COUNTER_FIELD_NAME, I);
    cv.visitLookupSwitchInsn(defaultCaseLabel, caseNumbers, caseLabels);

    // add one case for each around advice invocation
    for (int i = 0; i < m_aroundAdviceMethodInfos.length; i++) {
      cv.visitLabel(caseLabels[i]);

      // gather advice info
      AdviceMethodInfo adviceInfo = m_aroundAdviceMethodInfos[i];

      Label endInstanceOflabel = beginRuntimeCheck(cv, input, adviceInfo.getAdviceInfo());

      // getDefault the aspect instance
      adviceInfo.getAspectInfo().getAspectModel().loadAspect(cv, input, adviceInfo.getAspectInfo());

      // load the arguments to the advice
      adviceInfo.getAspectInfo().getAspectModel().createAroundAdviceArgumentHandling(
              cv,
              input,
              m_argumentTypes,
              adviceInfo
      );

      // invoke the advice method
      cv.visitMethodInsn(
              INVOKEVIRTUAL,
              adviceInfo.getAspectInfo().getAspectClassName(),
              adviceInfo.getAdviceInfo().getMethodName(),
              adviceInfo.getAdviceInfo().getMethodSignature()
      );
      cv.visitVarInsn(ASTORE, 1);

      // we need to handle the case when the advice was skipped due to runtime check
      // that is : if (runtimeCheck) { ret = advice() } else { ret = proceed() }
      if (endInstanceOflabel != null) {
        Label elseInstanceOfLabel = new Label();
        cv.visitJumpInsn(GOTO, elseInstanceOfLabel);
        endRuntimeCheck(cv, adviceInfo.getAdviceInfo(), endInstanceOflabel);
        cv.visitVarInsn(ALOAD, 0);
        cv.visitMethodInsn(INVOKESPECIAL, m_joinPointClassName, PROCEED_METHOD_NAME, PROCEED_METHOD_SIGNATURE);
        cv.visitVarInsn(ASTORE, 1);
        cv.visitLabel(elseInstanceOfLabel);
      }

      cv.visitLabel(returnLabels[i]);

      cv.visitVarInsn(ALOAD, 1);
      cv.visitInsn(ARETURN);
    }

    if (m_isThisAdvisable) {
      int delegationCaseIndex = caseLabels.length - 1;
      cv.visitLabel(caseLabels[delegationCaseIndex]);
      cv.visitVarInsn(ALOAD, 0);
      cv.visitInsn(ICONST_0);
      cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, INTERCEPTOR_INDEX_FIELD_NAME, I);
      cv.visitVarInsn(ALOAD, 0);
      cv.visitMethodInsn(INVOKEVIRTUAL, m_joinPointClassName, PROCEED_METHOD_NAME, PROCEED_METHOD_SIGNATURE);

      cv.visitLabel(returnLabels[delegationCaseIndex]);

      cv.visitInsn(ARETURN);
    }

    // invoke the target join point in the default case
    cv.visitLabel(defaultCaseLabel);

    AsmHelper.prepareWrappingOfPrimitiveType(cv, Type.getReturnType(m_calleeMemberDesc));

    createJoinPointInvocation(cv);

    Type m_returnType = null;
    if (m_joinPointType != JoinPointType.CONSTRUCTOR_CALL_INT) {
      m_returnType = Type.getReturnType(m_calleeMemberDesc);
    } else {
      m_returnType = Type.getType(m_calleeClassSignature);
    }
    AsmHelper.wrapPrimitiveType(cv, m_returnType);
    cv.visitVarInsn(ASTORE, 1);

    // store it in Rtti return value
    addReturnedValueToJoinPoint(cv, input, 1, true);

    // set it as the CALLEE instance for ctor call - TODO refactor somewhere else
    if (m_joinPointType == JoinPointType.CONSTRUCTOR_CALL_INT) {
      cv.visitVarInsn(ALOAD, 0);
      cv.visitVarInsn(ALOAD, 1);
      cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, CALLEE_INSTANCE_FIELD_NAME, m_calleeClassSignature);
    }

    cv.visitLabel(gotoLabel);

    cv.visitVarInsn(ALOAD, 1);
    cv.visitInsn(ARETURN);

    // finally clause
    cv.visitLabel(handlerLabel);
    cv.visitVarInsn(ASTORE, 2);
    cv.visitLabel(endLabel);

    cv.visitVarInsn(ALOAD, 2);
    cv.visitInsn(ATHROW);

    // set up the label table
    cv.visitTryCatchBlock(tryLabel, returnLabels[0], handlerLabel, null);
    for (int i = 1; i < caseLabels.length; i++) {
      Label caseLabel = caseLabels[i];
      Label returnLabel = returnLabels[i];
      cv.visitTryCatchBlock(caseLabel, returnLabel, handlerLabel, null);
    }
    cv.visitTryCatchBlock(defaultCaseLabel, gotoLabel, handlerLabel, null);
    cv.visitTryCatchBlock(handlerLabel, endLabel, handlerLabel, null);
    cv.visitMaxs(0, 0);
  }

  /**
   * Adds before advice invocations.
   *
   * @param cv
   */
  private void createBeforeAdviceInvocations(final MethodVisitor cv, CompilerInput input) {
    for (int i = 0; i < m_beforeAdviceMethodInfos.length; i++) {
      AdviceMethodInfo adviceMethodInfo = m_beforeAdviceMethodInfos[i];
      AspectInfo aspectInfo = adviceMethodInfo.getAspectInfo();

      if (requiresCallerInstance(aspectInfo.getDeploymentModel())
              && input.callerIndex < 0) {
        continue;
      }
      if (requiresCalleeInstance(aspectInfo.getDeploymentModel())
              && input.calleeIndex < 0) {
        continue;
      }

      // runtime check for target() etc
      Label endInstanceOflabel = beginRuntimeCheck(cv, input, adviceMethodInfo.getAdviceInfo());

      //getDefault the aspect instance
      final AspectModel aspectModel = adviceMethodInfo.getAspectInfo().getAspectModel();
      aspectModel.loadAspect(cv, input, adviceMethodInfo.getAspectInfo());

      // push any needed arguments for the advice invocation
      aspectModel.createBeforeOrAfterAdviceArgumentHandling(
              cv, input, m_argumentTypes, adviceMethodInfo, INDEX_NOTAVAILABLE
      );

      // invoke the advice
      cv.visitMethodInsn(
              INVOKEVIRTUAL,
              adviceMethodInfo.getAspectInfo().getAspectClassName(),
              adviceMethodInfo.getAdviceInfo().getMethodName(),
              adviceMethodInfo.getAdviceInfo().getMethodSignature()
      );

      // end label of runtime checks
      endRuntimeCheck(cv, adviceMethodInfo.getAdviceInfo(), endInstanceOflabel);
    }

    if (m_isThisAdvisable) {
      createBeforeInterceptorInvocations(
              cv,
              input.joinPointInstanceIndex,
              input.callerIndex + 1
      );
    }
  }

  /**
   * Adds after advice invocations.
   *
   * @param cv
   * @param input
   */
  private void createAfterFinallyAdviceInvocations(final MethodVisitor cv,
                                                   final CompilerInput input) {
    // add after advice in reverse order
    for (int i = m_afterFinallyAdviceMethodInfos.length - 1; i >= 0; i--) {
      AdviceMethodInfo advice = m_afterFinallyAdviceMethodInfos[i];
      createAfterAdviceInvocation(cv, input, advice, INDEX_NOTAVAILABLE);
    }
  }

  /**
   * Adds after returning advice invocations.
   *
   * @param cv
   * @param input
   */
  private void createAfterReturningAdviceInvocations(final MethodVisitor cv,
                                                     final CompilerInput input) {
    final int returnValueIndex = (input.joinPointInstanceIndex != INDEX_NOTAVAILABLE) ?
            (input.joinPointInstanceIndex + 1) : input.callerIndex + 1;

    if (m_isThisAdvisable) {
      createAfterReturningInterceptorInvocations(cv, input.joinPointInstanceIndex, returnValueIndex);
    }

    boolean hasPoppedReturnValueFromStack = false;
    for (int i = m_afterReturningAdviceMethodInfos.length - 1; i >= 0; i--) {
      AdviceMethodInfo advice = m_afterReturningAdviceMethodInfos[i];

      // set the return value index that will be used as arg to advice
      advice.setSpecialArgumentIndex(returnValueIndex);

      String specialArgDesc = advice.getSpecialArgumentTypeDesc();
      if (specialArgDesc == null) {
        // after returning
        createAfterAdviceInvocation(cv, input, advice, INDEX_NOTAVAILABLE);
      } else {
        // after returning <TYPE>
        if (AsmHelper.isPrimitive(m_returnType)) {
          if (m_returnType.getDescriptor().equals(specialArgDesc)) {
            createAfterAdviceInvocation(cv, input, advice, returnValueIndex);
          }
        } else {
          cv.visitVarInsn(ALOAD, returnValueIndex);

          cv.visitTypeInsn(INSTANCEOF, advice.getSpecialArgumentTypeName());

          Label label = new Label();
          cv.visitJumpInsn(IFEQ, label);

          createAfterAdviceInvocation(cv, input, advice, returnValueIndex);

          cv.visitLabel(label);
        }
      }
    }

    // need the return value in return operation
    if (!m_requiresProceedMethod && hasPoppedReturnValueFromStack) {
      cv.visitVarInsn(ALOAD, returnValueIndex);
    }
  }

  /**
   * Adds a single generic after advice invocation.
   *
   * @param cv
   * @param input
   * @param adviceMethodInfo
   * @param specialArgIndex  for afterReturning / Throwing when binding is used
   */
  private void createAfterAdviceInvocation(final MethodVisitor cv,
                                           final CompilerInput input,
                                           final AdviceMethodInfo adviceMethodInfo,
                                           final int specialArgIndex) {
    AspectInfo aspectInfo = adviceMethodInfo.getAspectInfo();

    if (requiresCallerInstance(aspectInfo.getDeploymentModel())
            && input.callerIndex < 0) {
      return; // without callER instance we cannot load a PER_THIS aspect instance
    }
    if (requiresCalleeInstance(aspectInfo.getDeploymentModel())
            && input.calleeIndex < 0) {
      return;
    }
    // runtime check for target() etc
    Label endInstanceOflabel = beginRuntimeCheck(cv, input, adviceMethodInfo.getAdviceInfo());

    // getDefault the aspect instance
    final AspectModel aspectModel = adviceMethodInfo.getAspectInfo().getAspectModel();
    aspectModel.loadAspect(cv, input, aspectInfo);

    aspectModel.createBeforeOrAfterAdviceArgumentHandling(
            cv, input, m_argumentTypes, adviceMethodInfo, specialArgIndex
    );

    cv.visitMethodInsn(
            INVOKEVIRTUAL,
            adviceMethodInfo.getAspectInfo().getAspectClassName(),
            adviceMethodInfo.getAdviceInfo().getMethodName(),
            adviceMethodInfo.getAdviceInfo().getMethodSignature()
    );

    // end label of runtime checks
    endRuntimeCheck(cv, adviceMethodInfo.getAdviceInfo(), endInstanceOflabel);
  }

  /**
   * Adds the return value to the RETURNED field.
   *
   * @param cv
   * @param input
   * @param returnValueIndex
   * @param unwrap           set to true if already wrapped on the stack (within proceed() code)
   */
  private void addReturnedValueToJoinPoint(final MethodVisitor cv,
                                           final CompilerInput input,
                                           final int returnValueIndex,
                                           final boolean unwrap) {
    if (m_requiresJoinPoint && m_returnType.getSort() != Type.VOID) {
      if (m_joinPointType == JoinPointType.METHOD_EXECUTION_INT
              || m_joinPointType == JoinPointType.METHOD_CALL_INT
              || m_joinPointType == JoinPointType.CONSTRUCTOR_CALL_INT) {
        //TODO should we do something for field getDefault / set
        loadJoinPointInstance(cv, input);
        if (unwrap && AsmHelper.isPrimitive(m_returnType)) {
          cv.visitVarInsn(ALOAD, returnValueIndex);
          AsmHelper.unwrapType(cv, m_returnType);
        } else {
          AsmHelper.loadType(cv, returnValueIndex, m_returnType);
        }
        cv.visitFieldInsn(
                PUTFIELD, m_joinPointClassName,
                RETURN_VALUE_FIELD_NAME, m_returnType.getDescriptor()
        );
      }
    }
  }

  /**
   * Loads the join point instance, takes static/non-static join point access into account.
   *
   * @param cv
   * @param input
   */
  public static void loadJoinPointInstance(final MethodVisitor cv,
                                           final CompilerInput input) {
    if (input.isOptimizedJoinPoint) {
      cv.visitFieldInsn(
              GETSTATIC, input.joinPointClassName,
              OPTIMIZED_JOIN_POINT_INSTANCE_FIELD_NAME,
              L + input.joinPointClassName + SEMICOLON
      );
    } else {
      cv.visitVarInsn(ALOAD, input.joinPointInstanceIndex);
    }
  }

  /**
   * Loads the argument member fields.
   *
   * @param cv
   * @param argStartIndex
   */
  protected final void loadArgumentMemberFields(final MethodVisitor cv, final int argStartIndex) {
    int argStackIndex = argStartIndex;
    for (int index = 0; index < m_argumentTypes.length; index++) {
      Type argumentType = m_argumentTypes[index];
      argStackIndex = AsmHelper.loadType(cv, argStackIndex, argumentType);
    }
  }

  /**
   * Loads the arguments.
   *
   * @param cv
   */
  protected final void loadArguments(final MethodVisitor cv) {
    for (int i = 0; i < m_fieldNames.length; i++) {
      String fieldName = m_fieldNames[i];
      Type argumentType = m_argumentTypes[i];
      cv.visitVarInsn(ALOAD, 0);
      cv.visitFieldInsn(GETFIELD, m_joinPointClassName, fieldName, argumentType.getDescriptor());
    }
  }

  /**
   * Resets the stack frame counter.
   *
   * @param cv
   */
  private void resetStackFrameCounter(final MethodVisitor cv) {
    cv.visitVarInsn(ALOAD, 0);
    cv.visitInsn(ICONST_M1);
    cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, STACK_FRAME_COUNTER_FIELD_NAME, I);
  }

  /**
   * Handles the incrementation of the stack frame.
   *
   * @param cv
   */
  private void incrementStackFrameCounter(final MethodVisitor cv) {
    cv.visitVarInsn(ALOAD, 0);
    cv.visitInsn(DUP);
    cv.visitFieldInsn(GETFIELD, m_joinPointClassName, STACK_FRAME_COUNTER_FIELD_NAME, I);
    cv.visitInsn(ICONST_1);
    cv.visitInsn(IADD);
    cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, STACK_FRAME_COUNTER_FIELD_NAME, I);
  }

  /**
   * Create and load a structure (f.e. array of Object) where args are stored, before setting the Rtti
   * with it (See addParametersToRttiInstance). The structure is stored at the given stackFreeIndex.
   * <p/>
   * Note: public for createMandatoryMethods in the models
   *
   * @param cv
   * @param stackFreeIndex
   */
  public final void createArgumentArrayAt(final MethodVisitor cv, final int stackFreeIndex) {
    AsmHelper.loadIntegerConstant(cv, m_fieldNames.length);
    cv.visitTypeInsn(ANEWARRAY, OBJECT_CLASS_NAME);
    cv.visitVarInsn(ASTORE, stackFreeIndex);

    for (int i = 0; i < m_argumentTypes.length; i++) {
      cv.visitVarInsn(ALOAD, stackFreeIndex);
      AsmHelper.loadIntegerConstant(cv, i);
      AsmHelper.prepareWrappingOfPrimitiveType(cv, m_argumentTypes[i]);
      cv.visitVarInsn(ALOAD, 0);
      cv.visitFieldInsn(GETFIELD, m_joinPointClassName, ARGUMENT_FIELD + i, m_argumentTypes[i].getDescriptor());
      AsmHelper.wrapPrimitiveType(cv, m_argumentTypes[i]);
      cv.visitInsn(AASTORE);
    }
  }

  /**
   * Creates utility methods for the join point (getter, setters etc.).
   */
  private void createUtilityMethods() {
    MethodVisitor cv;

    // addMetaData
    {
      cv = m_cw.visitMethod(ACC_PUBLIC, ADD_META_DATA_METHOD_NAME, ADD_META_DATA_METHOD_SIGNATURE, null, null);
      cv.visitFieldInsn(GETSTATIC, m_joinPointClassName, META_DATA_FIELD_NAME, MAP_CLASS_SIGNATURE);
      cv.visitVarInsn(ALOAD, 1);
      cv.visitVarInsn(ALOAD, 2);
      cv.visitMethodInsn(
              INVOKEINTERFACE,
              MAP_CLASS_NAME,
              PUT_METHOD_NAME,
              PUT_METHOD_SIGNATURE
      );
      cv.visitInsn(POP);
      cv.visitInsn(RETURN);
      cv.visitMaxs(0, 0);
    }

    // getMetaData
    {
      cv = m_cw.visitMethod(ACC_PUBLIC, GET_META_DATA_METHOD_NAME, GET_META_DATA_METHOD_SIGNATURE, null, null);
      cv.visitFieldInsn(GETSTATIC, m_joinPointClassName, META_DATA_FIELD_NAME, MAP_CLASS_SIGNATURE);
      cv.visitVarInsn(ALOAD, 1);
      cv.visitMethodInsn(INVOKEINTERFACE, MAP_CLASS_NAME, GET_METHOD_NAME, GET_METHOD_SIGNATURE);
      cv.visitInsn(ARETURN);
      cv.visitMaxs(0, 0);
    }

    // getCallee
    {
      cv = m_cw.visitMethod(
              ACC_PUBLIC,
              GET_CALLEE_METHOD_NAME,
              NO_PARAMS_SIGNATURE + OBJECT_CLASS_SIGNATURE,
              null, null
      );
      cv.visitVarInsn(ALOAD, 0);
      cv.visitFieldInsn(GETFIELD, m_joinPointClassName, CALLEE_INSTANCE_FIELD_NAME, m_calleeClassSignature);
      cv.visitInsn(ARETURN);
      cv.visitMaxs(0, 0);
    }

    // getCaller
    {
      cv = m_cw.visitMethod(
              ACC_PUBLIC,
              GET_CALLER_METHOD_NAME,
              NO_PARAMS_SIGNATURE + OBJECT_CLASS_SIGNATURE,
              null, null
      );
      cv.visitVarInsn(ALOAD, 0);
      cv.visitFieldInsn(GETFIELD, m_joinPointClassName, CALLER_INSTANCE_FIELD_NAME, m_callerClassSignature);
      cv.visitInsn(ARETURN);
      cv.visitMaxs(0, 0);
    }

    // getTarget
    {
      cv = m_cw.visitMethod(
              ACC_PUBLIC,
              GET_TARGET_METHOD_NAME,
              NO_PARAMS_SIGNATURE + OBJECT_CLASS_SIGNATURE,
              null, null
      );
      cv.visitVarInsn(ALOAD, 0);
      cv.visitFieldInsn(GETFIELD, m_joinPointClassName, CALLEE_INSTANCE_FIELD_NAME, m_calleeClassSignature);
      cv.visitInsn(ARETURN);
      cv.visitMaxs(0, 0);
    }

    // getThis
    {
      cv = m_cw.visitMethod(
              ACC_PUBLIC,
              GET_THIS_METHOD_NAME,
              NO_PARAMS_SIGNATURE + OBJECT_CLASS_SIGNATURE,
              null, null
      );
      cv.visitVarInsn(ALOAD, 0);
      cv.visitFieldInsn(GETFIELD, m_joinPointClassName, CALLER_INSTANCE_FIELD_NAME, m_callerClassSignature);
      cv.visitInsn(ARETURN);
      cv.visitMaxs(0, 0);
    }

    // getCallerClass
    {
      cv =
              m_cw.visitMethod(
                      ACC_PUBLIC,
                      GET_CALLER_CLASS_METHOD_NAME,
                      GET_CALLER_CLASS_METHOD_SIGNATURE,
                      null,
                      null
              );
      cv.visitFieldInsn(GETSTATIC, m_joinPointClassName, THIS_CLASS_FIELD_NAME_IN_JP, CLASS_CLASS_SIGNATURE);
      cv.visitInsn(ARETURN);
      cv.visitMaxs(0, 0);
    }

    // getCalleeClass
    {
      cv =
              m_cw.visitMethod(
                      ACC_PUBLIC,
                      GET_CALLEE_CLASS_METHOD_NAME,
                      GET_CALLEE_CLASS_METHOD_SIGNATURE,
                      null,
                      null
              );
      cv.visitFieldInsn(GETSTATIC, m_joinPointClassName, TARGET_CLASS_FIELD_NAME_IN_JP, CLASS_CLASS_SIGNATURE);
      cv.visitInsn(ARETURN);
      cv.visitMaxs(0, 0);
    }

    // getTargetClass, deprecated but still there
    {
      cv =
              m_cw.visitMethod(
                      ACC_PUBLIC,
                      GET_TARGET_CLASS_METHOD_NAME,
                      GET_TARGET_CLASS_METHOD_SIGNATURE,
                      null,
                      null
              );
      cv.visitFieldInsn(GETSTATIC, m_joinPointClassName, TARGET_CLASS_FIELD_NAME_IN_JP, CLASS_CLASS_SIGNATURE);
      cv.visitInsn(ARETURN);
      cv.visitMaxs(0, 0);
    }

    // getType
    {
      cv = m_cw.visitMethod(ACC_PUBLIC, GET_TYPE_METHOD_NAME, GET_TYPE_METHOD_SIGNATURE, null, null);
      AsmHelper.loadIntegerConstant(cv, m_joinPointType);
      cv.visitMethodInsn(
              INVOKESTATIC, Type.getType(JoinPointType.class).getInternalName(), "fromInt",
              "(I)" + Type.getType(JoinPointType.class).getDescriptor()
      );
      cv.visitInsn(ARETURN);
      cv.visitMaxs(0, 0);
    }

    // getEnclosingStaticJoinPoint
    {
      cv = m_cw.visitMethod(
              ACC_PUBLIC,
              GET_ENCLOSING_SJP_METHOD_NAME,
              GET_ENCLOSING_SJP_METHOD_SIGNATURE,
              null,
              null
      );
      cv.visitVarInsn(ALOAD, 0);
      cv.visitFieldInsn(
              GETSTATIC,
              m_joinPointClassName,
              ENCLOSING_SJP_FIELD_NAME,
              ENCLOSING_SJP_FIELD_CLASS_SIGNATURE
      );
      cv.visitInsn(ARETURN);
      cv.visitMaxs(0, 0);
    }

  }

  // TODO might be a use-case now in Uber Container, keep code for now
//    /**
//     * Creates the copy method.
//     * <p/>
//     * TODO refactor and put in subclasses
//     */
//    protected void createCopyMethod() {
//
//        MethodVisitor cv = m_cw.visitMethod(ACC_PUBLIC, COPY_METHOD_NAME, COPY_METHOD_SIGNATURE, null, null);
//
//        // create a new join point instance
//        cv.visitTypeInsn(NEW, m_joinPointClassName);
//        cv.visitInsn(DUP);
//        int joinPointCloneIndex = 1;
//        cv.visitMethodInsn(INVOKESPECIAL, m_joinPointClassName, INIT_METHOD_NAME, NO_PARAM_RETURN_VOID_SIGNATURE);
//        cv.visitVarInsn(ASTORE, joinPointCloneIndex);
//
//        // set stack frame index
//        cv.visitVarInsn(ALOAD, joinPointCloneIndex);
//        cv.visitVarInsn(ALOAD, 0);
//        cv.visitFieldInsn(GETFIELD, m_joinPointClassName, STACK_FRAME_COUNTER_FIELD_NAME, I);
//        cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, STACK_FRAME_COUNTER_FIELD_NAME, I);
//
//        if (m_isThisAdvisable) {
//            // set interceptor index
//            cv.visitVarInsn(ALOAD, joinPointCloneIndex);
//            cv.visitVarInsn(ALOAD, 0);
//            cv.visitFieldInsn(GETFIELD, m_joinPointClassName, INTERCEPTOR_INDEX_FIELD_NAME, I);
//            cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, INTERCEPTOR_INDEX_FIELD_NAME, I);
//
//            // set array length fields
//            cv.visitVarInsn(ALOAD, joinPointCloneIndex);
//            cv.visitVarInsn(ALOAD, 0);
//            cv.visitFieldInsn(GETFIELD, m_joinPointClassName, NR_OF_BEFORE_INTERCEPTORS_FIELD_NAME, I);
//            cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, NR_OF_BEFORE_INTERCEPTORS_FIELD_NAME, I);
//            cv.visitVarInsn(ALOAD, joinPointCloneIndex);
//            cv.visitVarInsn(ALOAD, 0);
//            cv.visitFieldInsn(GETFIELD, m_joinPointClassName, NR_OF_AROUND_INTERCEPTORS_FIELD_NAME, I);
//            cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, NR_OF_AROUND_INTERCEPTORS_FIELD_NAME, I);
//            cv.visitVarInsn(ALOAD, joinPointCloneIndex);
//            cv.visitVarInsn(ALOAD, 0);
//            cv.visitFieldInsn(GETFIELD, m_joinPointClassName, NR_OF_AFTER_INTERCEPTORS_FIELD_NAME, I);
//            cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, NR_OF_AFTER_INTERCEPTORS_FIELD_NAME, I);
//            cv.visitVarInsn(ALOAD, joinPointCloneIndex);
//            cv.visitVarInsn(ALOAD, 0);
//            cv.visitFieldInsn(GETFIELD, m_joinPointClassName, NR_OF_AFTER_RETURNING_INTERCEPTORS_FIELD_NAME, I);
//            cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, NR_OF_AFTER_RETURNING_INTERCEPTORS_FIELD_NAME, I);
//            cv.visitVarInsn(ALOAD, joinPointCloneIndex);
//            cv.visitVarInsn(ALOAD, 0);
//            cv.visitFieldInsn(GETFIELD, m_joinPointClassName, NR_OF_AFTER_THROWING_INTERCEPTORS_FIELD_NAME, I);
//            cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, NR_OF_AFTER_THROWING_INTERCEPTORS_FIELD_NAME, I);
//
//            // set arrays
//            cv.visitVarInsn(ALOAD, joinPointCloneIndex);
//            cv.visitVarInsn(ALOAD, 0);
//            cv.visitFieldInsn(
//                    GETFIELD, m_joinPointClassName, BEFORE_INTERCEPTORS_FIELD_NAME,
//                    BEFORE_ADVICE_ARRAY_CLASS_SIGNATURE
//            );
//            cv.visitFieldInsn(
//                    PUTFIELD, m_joinPointClassName, BEFORE_INTERCEPTORS_FIELD_NAME,
//                    BEFORE_ADVICE_ARRAY_CLASS_SIGNATURE
//            );
//            cv.visitVarInsn(ALOAD, joinPointCloneIndex);
//            cv.visitVarInsn(ALOAD, 0);
//            cv.visitFieldInsn(
//                    GETFIELD, m_joinPointClassName, AROUND_INTERCEPTORS_FIELD_NAME,
//                    AROUND_ADVICE_ARRAY_CLASS_SIGNATURE
//            );
//            cv.visitFieldInsn(
//                    PUTFIELD, m_joinPointClassName, AROUND_INTERCEPTORS_FIELD_NAME,
//                    AROUND_ADVICE_ARRAY_CLASS_SIGNATURE
//            );
//            cv.visitVarInsn(ALOAD, joinPointCloneIndex);
//            cv.visitVarInsn(ALOAD, 0);
//            cv.visitFieldInsn(
//                    GETFIELD, m_joinPointClassName, AFTER_INTERCEPTORS_FIELD_NAME, AFTER_ADVICE_ARRAY_CLASS_SIGNATURE
//            );
//            cv.visitFieldInsn(
//                    PUTFIELD, m_joinPointClassName, AFTER_INTERCEPTORS_FIELD_NAME, AFTER_ADVICE_ARRAY_CLASS_SIGNATURE
//            );
//            cv.visitVarInsn(ALOAD, joinPointCloneIndex);
//            cv.visitVarInsn(ALOAD, 0);
//            cv.visitFieldInsn(
//                    GETFIELD, m_joinPointClassName, AFTER_RETURNING_INTERCEPTORS_FIELD_NAME,
//                    AFTER_RETURNING_ADVICE_ARRAY_CLASS_SIGNATURE
//            );
//            cv.visitFieldInsn(
//                    PUTFIELD, m_joinPointClassName, AFTER_RETURNING_INTERCEPTORS_FIELD_NAME,
//                    AFTER_RETURNING_ADVICE_ARRAY_CLASS_SIGNATURE
//            );
//            cv.visitVarInsn(ALOAD, joinPointCloneIndex);
//            cv.visitVarInsn(ALOAD, 0);
//            cv.visitFieldInsn(
//                    GETFIELD, m_joinPointClassName, AFTER_THROWING_INTERCEPTORS_FIELD_NAME,
//                    AFTER_THROWING_ADVICE_ARRAY_CLASS_SIGNATURE
//            );
//            cv.visitFieldInsn(
//                    PUTFIELD, m_joinPointClassName, AFTER_THROWING_INTERCEPTORS_FIELD_NAME,
//                    AFTER_THROWING_ADVICE_ARRAY_CLASS_SIGNATURE
//            );
//        }
//
//        // set callee
//        cv.visitVarInsn(ALOAD, joinPointCloneIndex);
//        cv.visitVarInsn(ALOAD, 0);
//        cv.visitFieldInsn(GETFIELD, m_joinPointClassName, CALLEE_INSTANCE_FIELD_NAME, m_calleeClassSignature);
//        cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, CALLEE_INSTANCE_FIELD_NAME, m_calleeClassSignature);
//
//        // set caller
//        cv.visitVarInsn(ALOAD, joinPointCloneIndex);
//        cv.visitVarInsn(ALOAD, 0);
//        cv.visitFieldInsn(GETFIELD, m_joinPointClassName, CALLER_INSTANCE_FIELD_NAME, m_callerClassSignature);
//        cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, CALLER_INSTANCE_FIELD_NAME, m_callerClassSignature);
//
//        // set the arguments
//        for (int i = 0; i < m_fieldNames.length; i++) {
//            String fieldName = m_fieldNames[i];
//            cv.visitVarInsn(ALOAD, joinPointCloneIndex);
//            cv.visitVarInsn(ALOAD, 0);
//            cv.visitFieldInsn(GETFIELD, m_joinPointClassName, fieldName, m_argumentTypes[i].getDescriptor());
//            cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, fieldName, m_argumentTypes[i].getDescriptor());
//        }
//
//        // set the returned field if any
//        if (m_returnType.getSort() != Type.VOID) {
//            cv.visitVarInsn(ALOAD, joinPointCloneIndex);
//            cv.visitVarInsn(ALOAD, 0);
//            cv.visitFieldInsn(GETFIELD, m_joinPointClassName, RETURN_VALUE_FIELD_NAME, m_returnType.getDescriptor());
//            cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, RETURN_VALUE_FIELD_NAME, m_returnType.getDescriptor());
//        }
//
//        cv.visitVarInsn(ALOAD, joinPointCloneIndex);
//        cv.visitInsn(ARETURN);
//        cv.visitMaxs(0, 0);
//    }

  /**
   * Build up the signature of the 'invoke' methods.
   *
   * @return
   */
  protected String buildInvokeMethodSignature() {
    StringBuffer invokeDescBuf = new StringBuffer();
    invokeDescBuf.append('(');
    if (m_joinPointType != JoinPointType.CONSTRUCTOR_CALL_INT) {
      if (!Modifier.isStatic(m_calleeMemberModifiers)) {
        // callee
        invokeDescBuf.append(m_calleeClassSignature);
      }
    }
    // args
    for (int i = 0; i < m_argumentTypes.length; i++) {
      Type type = m_argumentTypes[i];
      invokeDescBuf.append(type.getDescriptor());
    }
    // caller
    invokeDescBuf.append(m_callerClassSignature);
    invokeDescBuf.append(')');
    invokeDescBuf.append(m_returnType.getDescriptor());
    return invokeDescBuf.toString();
  }

  /**
   * Checks if at least one advice is using this or target (bounded or runtime check)
   *
   * @return true if so
   */
  private boolean requiresThisOrTarget() {
    return m_isThisAdvisable ||
            requiresThisOrTarget(m_aroundAdviceMethodInfos) ||
            requiresThisOrTarget(m_beforeAdviceMethodInfos) ||
            requiresThisOrTarget(m_afterFinallyAdviceMethodInfos) ||
            requiresThisOrTarget(m_afterReturningAdviceMethodInfos) ||
            requiresThisOrTarget(m_afterThrowingAdviceMethodInfos);
  }

  /**
   * Checks if at least one advice is using the non static JoinPoint explicitly
   *
   * @return true if so
   */
  private boolean requiresJoinPoint() {
    if (m_isThisAdvisable ||
            requiresJoinPoint(m_aroundAdviceMethodInfos) ||
            requiresJoinPoint(m_beforeAdviceMethodInfos) ||
            requiresJoinPoint(m_afterFinallyAdviceMethodInfos) ||
            requiresJoinPoint(m_afterReturningAdviceMethodInfos) ||
            requiresJoinPoint(m_afterThrowingAdviceMethodInfos)) {
      return true;
    }

    // query the models to know which level of optimization we can use
    for (int i = 0; i < m_aspectModels.length; i++) {
      if (m_aspectModels[i].requiresReflectiveInfo()) {
        // if at least one model requries RTTI then build it
        return true;
      }
    }

    return false;
  }

  /**
   * Checks if at least one advice is using target or this (bounded or runtime check)
   *
   * @param adviceMethodInfos
   * @return true if so
   */
  private boolean requiresThisOrTarget(final AdviceMethodInfo[] adviceMethodInfos) {
    for (int i = 0; i < adviceMethodInfos.length; i++) {
      if (adviceMethodInfos[i].requiresThisOrTarget()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if at least one advice is using non static JoinPoint explicitly
   *
   * @param adviceMethodInfos
   * @return true if so
   */
  private boolean requiresJoinPoint(final AdviceMethodInfo[] adviceMethodInfos) {
    for (int i = 0; i < adviceMethodInfos.length; i++) {
      if (adviceMethodInfos[i].requiresJoinPoint()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Handles the if case for runtime check (target instanceof, cflow)
   *
   * @param cv
   * @param adviceInfo
   * @return the label for endIf or null if the adviceInfo did not required runtime check
   */
  private Label beginRuntimeCheck(final MethodVisitor cv,
                                  final CompilerInput input,
                                  final AdviceInfo adviceInfo) {
    Label endRuntimeCheckLabel = null;
    DeploymentModel deploymentModel = adviceInfo.getAspectDeploymentModel();
    if (adviceInfo.hasTargetWithRuntimeCheck()
            || adviceInfo.getAdviceDefinition().hasCflowOrCflowBelow()
            || DeploymentModel.PER_THIS.equals(deploymentModel)
            || DeploymentModel.PER_TARGET.equals(deploymentModel)) {

      int perObjectCheckType = RuntimeCheckVisitor.NULL_PER_OBJECT_TYPE;

      if (DeploymentModel.PER_THIS.equals(deploymentModel)) {
        perObjectCheckType = RuntimeCheckVisitor.PER_THIS_TYPE;
      } else if (DeploymentModel.PER_TARGET.equals(deploymentModel)) {
        perObjectCheckType = RuntimeCheckVisitor.PER_TARGET_TYPE;
      }

      endRuntimeCheckLabel = new Label();
      // create a specific visitor everytime
      RuntimeCheckVisitor runtimeCheckVisitor = new RuntimeCheckVisitor(
              cv,
              adviceInfo.getExpressionInfo(),
              input,
              perObjectCheckType,
              adviceInfo.getAspectQualifiedName()
      );
      runtimeCheckVisitor.pushCheckOnStack(adviceInfo);
      cv.visitJumpInsn(IFEQ, endRuntimeCheckLabel);
    }
    return endRuntimeCheckLabel;
  }

  /**
   * Ends the ifLabel of a runtime check
   *
   * @param cv
   * @param adviceInfo
   * @param label      if null, then do nothing (means we did not had a runtime check)
   */
  private void endRuntimeCheck(final MethodVisitor cv, final AdviceInfo adviceInfo, final Label label) {
    DeploymentModel deployModel = adviceInfo.getAspectDeploymentModel();

    if (adviceInfo.hasTargetWithRuntimeCheck()
            || adviceInfo.getAdviceDefinition().hasCflowOrCflowBelow()
            || DeploymentModel.PER_THIS.equals(deployModel)
            || DeploymentModel.PER_TARGET.equals(deployModel)) {

      cv.visitLabel(label);
    }
  }

  /**
   * Helper method to load the callee on the stack
   *
   * @param cv
   * @param input
   */
  public static void loadCallee(final MethodVisitor cv,
                                final CompilerInput input) {
    if (input.isOptimizedJoinPoint) {
      // grab the callee from the invoke parameters directly
      cv.visitVarInsn(ALOAD, input.calleeIndex);
    } else {
      loadJoinPointInstance(cv, input);
      cv.visitFieldInsn(
              GETFIELD, input.joinPointClassName, CALLEE_INSTANCE_FIELD_NAME, input.calleeClassSignature
      );
    }
  }

  /**
   * Helper method to load the caller on the stack
   *
   * @param cv
   * @param input
   */
  public static void loadCaller(final MethodVisitor cv,
                                final CompilerInput input) {
    if (input.isOptimizedJoinPoint) {
      // grab the callee from the invoke parameters directly
      cv.visitVarInsn(ALOAD, input.callerIndex);
    } else {
      loadJoinPointInstance(cv, input);
      cv.visitFieldInsn(
              GETFIELD, input.joinPointClassName, CALLER_INSTANCE_FIELD_NAME, input.callerClassSignature
      );
    }
  }

//    /**
//     * Creates an invocation to Aspects.aspectOf(..).
//     *
//     * @param cv
//     * @param isOptimizedJoinPoint
//     * @param joinPointIndex
//     * @param callerIndex
//     * @param calleeIndex
//     * @param aspectInfo
//     */
//    public void createInvocationToAspectOf(final MethodVisitor cv,
//                                           final boolean isOptimizedJoinPoint,
//                                           final int joinPointIndex,
//                                           final int callerIndex,
//                                           final int calleeIndex,
//                                           final AspectInfo aspectInfo) {
//        if (DeploymentModel.PER_INSTANCE.equals(aspectInfo.getDeploymentModel())) {
//
//            //generates code: aspectField = (cast) Aspects.aspect$Of(aspectQN, containerClassName, callee)
//            loadJoinPointInstance(cv, m_joinPointClassName, isOptimizedJoinPoint, joinPointIndex);
//            cv.visitLdcInsn(aspectInfo.getAspectQualifiedName());
//            if (calleeIndex >= 0) {
//                cv.visitVarInsn(ALOAD, calleeIndex);
//                cv.visitLdcInsn(aspectInfo.getAspectDefinition().getContainerClassName());
//                cv.visitMethodInsn(
//                        INVOKESTATIC,
//                        ASPECTS_CLASS_NAME,
//                        ASPECT_OF_METHOD_NAME,
//                        ASPECT_OF_PER_INSTANCE_METHOD_SIGNATURE
//                );
//            } else {
//                // TODO: should this really happen? we are filtering at early stage now. - REMOVE CODE BLOCK
//                // fallback to perClass
//                //aspectField = (cast) Aspects.aspectOf(aspectQN, containerClass, calleeClass)
//                cv.visitFieldInsn(GETSTATIC, m_joinPointClassName, TARGET_CLASS_FIELD_NAME_IN_JP, CLASS_CLASS_SIGNATURE);
//                cv.visitLdcInsn(aspectInfo.getAspectDefinition().getContainerClassName());
//                cv.visitMethodInsn(
//                        INVOKESTATIC,
//                        ASPECTS_CLASS_NAME,
//                        ASPECT_OF_METHOD_NAME,
//                        ASPECT_OF_PER_CLASS_METHOD_SIGNATURE
//                );
//            }
//            cv.visitTypeInsn(CHECKCAST, aspectInfo.getAspectClassName());
//            cv.visitFieldInsn(
//                    PUTFIELD, m_joinPointClassName, aspectInfo.getAspectFieldName(),
//                    aspectInfo.getAspectClassSignature()
//            );
//        }
//    }

  /**
   * Generates code needed for handling Advisable management for the target class.
   *
   * @param cv
   * @param joinPointInstanceIndex
   * @param advisableIndex
   */
  private void createInitializationForAdvisableManagement(final MethodVisitor cv,
                                                          final int joinPointInstanceIndex,
                                                          final int advisableIndex) {
    // interceptor index
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitInsn(ICONST_M1);
    cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, INTERCEPTOR_INDEX_FIELD_NAME, I);

    initializeAroundInterceptors(cv, joinPointInstanceIndex, advisableIndex);
    initializeBeforeInterceptors(cv, joinPointInstanceIndex, advisableIndex);
    initializeAfterInterceptors(cv, joinPointInstanceIndex, advisableIndex);
    initializeAfterReturningInterceptors(cv, joinPointInstanceIndex, advisableIndex);
    initializeAfterThrowingInterceptors(cv, joinPointInstanceIndex, advisableIndex);
  }

  /**
   * Handle the around interceptor init.
   *
   * @param cv
   * @param joinPointInstanceIndex
   * @param advisableIndex
   */
  private void initializeAroundInterceptors(final MethodVisitor cv,
                                            final int joinPointInstanceIndex,
                                            final int advisableIndex) {
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitVarInsn(ALOAD, advisableIndex);
    cv.visitTypeInsn(CHECKCAST, ADVISABLE_CLASS_NAME);
    cv.visitLdcInsn(new Integer(m_joinPointClassName.hashCode()));
    cv.visitMethodInsn(
            INVOKEINTERFACE,
            ADVISABLE_CLASS_NAME,
            GET_AROUND_ADVICE_METHOD_NAME,
            GET_AROUND_ADVICE_METHOD_SIGNATURE
    );
    cv.visitFieldInsn(
            PUTFIELD,
            m_joinPointClassName,
            AROUND_INTERCEPTORS_FIELD_NAME,
            AROUND_ADVICE_ARRAY_CLASS_SIGNATURE
    );

    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitFieldInsn(
            GETFIELD,
            m_joinPointClassName,
            AROUND_INTERCEPTORS_FIELD_NAME,
            AROUND_ADVICE_ARRAY_CLASS_SIGNATURE
    );
    cv.visitInsn(ARRAYLENGTH);
    cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, NR_OF_AROUND_INTERCEPTORS_FIELD_NAME, I);
  }

  /**
   * Handle the before interceptor init.
   *
   * @param cv
   * @param joinPointInstanceIndex
   * @param advisableIndex
   */
  private void initializeBeforeInterceptors(final MethodVisitor cv,
                                            final int joinPointInstanceIndex,
                                            final int advisableIndex) {
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitVarInsn(ALOAD, advisableIndex);
    cv.visitTypeInsn(CHECKCAST, ADVISABLE_CLASS_NAME);
    cv.visitLdcInsn(new Integer(m_joinPointClassName.hashCode()));
    cv.visitMethodInsn(
            INVOKEINTERFACE,
            ADVISABLE_CLASS_NAME,
            GET_BEFORE_ADVICE_METHOD_NAME,
            GET_BEFORE_ADVICE_METHOD_SIGNATURE
    );
    cv.visitFieldInsn(
            PUTFIELD,
            m_joinPointClassName,
            BEFORE_INTERCEPTORS_FIELD_NAME,
            BEFORE_ADVICE_ARRAY_CLASS_SIGNATURE
    );

    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitFieldInsn(
            GETFIELD,
            m_joinPointClassName,
            BEFORE_INTERCEPTORS_FIELD_NAME,
            BEFORE_ADVICE_ARRAY_CLASS_SIGNATURE
    );
    cv.visitInsn(ARRAYLENGTH);
    cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, NR_OF_BEFORE_INTERCEPTORS_FIELD_NAME, I);
  }

  /**
   * Handle the after finally interceptor init.
   *
   * @param cv
   * @param joinPointInstanceIndex
   * @param advisableIndex
   */
  private void initializeAfterInterceptors(final MethodVisitor cv,
                                           final int joinPointInstanceIndex,
                                           final int advisableIndex) {
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitVarInsn(ALOAD, advisableIndex);
    cv.visitTypeInsn(CHECKCAST, ADVISABLE_CLASS_NAME);
    cv.visitLdcInsn(new Integer(m_joinPointClassName.hashCode()));
    cv.visitMethodInsn(
            INVOKEINTERFACE,
            ADVISABLE_CLASS_NAME,
            GET_AFTER_ADVICE_METHOD_NAME,
            GET_AFTER_ADVICE_METHOD_SIGNATURE
    );
    cv.visitFieldInsn(
            PUTFIELD,
            m_joinPointClassName,
            AFTER_INTERCEPTORS_FIELD_NAME,
            AFTER_ADVICE_ARRAY_CLASS_SIGNATURE
    );

    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitFieldInsn(
            GETFIELD,
            m_joinPointClassName,
            AFTER_INTERCEPTORS_FIELD_NAME,
            AFTER_ADVICE_ARRAY_CLASS_SIGNATURE
    );
    cv.visitInsn(ARRAYLENGTH);
    cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, NR_OF_AFTER_INTERCEPTORS_FIELD_NAME, I);
  }

  /**
   * Handle the after returning interceptor init.
   *
   * @param cv
   * @param joinPointInstanceIndex
   * @param advisableIndex
   */
  private void initializeAfterReturningInterceptors(final MethodVisitor cv,
                                                    final int joinPointInstanceIndex,
                                                    final int advisableIndex) {
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitVarInsn(ALOAD, advisableIndex);
    cv.visitTypeInsn(CHECKCAST, ADVISABLE_CLASS_NAME);
    cv.visitLdcInsn(new Integer(m_joinPointClassName.hashCode()));
    cv.visitMethodInsn(
            INVOKEINTERFACE,
            ADVISABLE_CLASS_NAME,
            GET_AFTER_RETURNING_ADVICE_METHOD_NAME,
            GET_AFTER_RETURNING_ADVICE_METHOD_SIGNATURE
    );
    cv.visitFieldInsn(
            PUTFIELD,
            m_joinPointClassName,
            AFTER_RETURNING_INTERCEPTORS_FIELD_NAME,
            AFTER_RETURNING_ADVICE_ARRAY_CLASS_SIGNATURE
    );

    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitFieldInsn(
            GETFIELD,
            m_joinPointClassName,
            AFTER_RETURNING_INTERCEPTORS_FIELD_NAME,
            AFTER_RETURNING_ADVICE_ARRAY_CLASS_SIGNATURE
    );
    cv.visitInsn(ARRAYLENGTH);
    cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, NR_OF_AFTER_RETURNING_INTERCEPTORS_FIELD_NAME, I);
  }

  /**
   * Handle the after throwing interceptor init.
   *
   * @param cv
   * @param joinPointInstanceIndex
   * @param advisableIndex
   */
  private void initializeAfterThrowingInterceptors(final MethodVisitor cv,
                                                   final int joinPointInstanceIndex,
                                                   final int advisableIndex) {
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitVarInsn(ALOAD, advisableIndex);
    cv.visitTypeInsn(CHECKCAST, ADVISABLE_CLASS_NAME);
    cv.visitLdcInsn(new Integer(m_joinPointClassName.hashCode()));
    cv.visitMethodInsn(
            INVOKEINTERFACE,
            ADVISABLE_CLASS_NAME,
            GET_AFTER_THROWING_ADVICE_METHOD_NAME,
            GET_AFTER_THROWING_ADVICE_METHOD_SIGNATURE
    );
    cv.visitFieldInsn(
            PUTFIELD,
            m_joinPointClassName,
            AFTER_THROWING_INTERCEPTORS_FIELD_NAME,
            AFTER_THROWING_ADVICE_ARRAY_CLASS_SIGNATURE
    );

    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitFieldInsn(
            GETFIELD,
            m_joinPointClassName,
            AFTER_THROWING_INTERCEPTORS_FIELD_NAME,
            AFTER_THROWING_ADVICE_ARRAY_CLASS_SIGNATURE
    );
    cv.visitInsn(ARRAYLENGTH);
    cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, NR_OF_AFTER_THROWING_INTERCEPTORS_FIELD_NAME, I);
  }

  /**
   * Handles the around interceptor invocations.
   *
   * @param cv
   */
  private void createAroundInterceptorInvocations(final MethodVisitor cv) {
    cv.visitVarInsn(ALOAD, 0);
    cv.visitFieldInsn(GETFIELD, m_joinPointClassName, INTERCEPTOR_INDEX_FIELD_NAME, I);
    cv.visitInsn(ICONST_M1);
    Label ifStatementLabel = new Label();
    cv.visitJumpInsn(IF_ICMPEQ, ifStatementLabel);
    cv.visitVarInsn(ALOAD, 0);
    cv.visitFieldInsn(GETFIELD, m_joinPointClassName, INTERCEPTOR_INDEX_FIELD_NAME, I);
    cv.visitVarInsn(ALOAD, 0);
    cv.visitFieldInsn(GETFIELD, m_joinPointClassName, NR_OF_AROUND_INTERCEPTORS_FIELD_NAME, I);
    cv.visitJumpInsn(IF_ICMPGE, ifStatementLabel);
    cv.visitVarInsn(ALOAD, 0);
    cv.visitFieldInsn(
            GETFIELD,
            m_joinPointClassName,
            AROUND_INTERCEPTORS_FIELD_NAME,
            AROUND_ADVICE_ARRAY_CLASS_SIGNATURE
    );
    cv.visitVarInsn(ALOAD, 0);
    cv.visitInsn(DUP);
    cv.visitFieldInsn(GETFIELD, m_joinPointClassName, INTERCEPTOR_INDEX_FIELD_NAME, I);
    cv.visitInsn(DUP_X1);
    cv.visitInsn(ICONST_1);
    cv.visitInsn(IADD);
    cv.visitFieldInsn(PUTFIELD, m_joinPointClassName, INTERCEPTOR_INDEX_FIELD_NAME, I);
    cv.visitInsn(AALOAD);
    cv.visitVarInsn(ALOAD, 0);
    cv.visitMethodInsn(
            INVOKEINTERFACE,
            AROUND_ADVICE_CLASS_NAME,
            INTERCEPT_INVOKE_METHOD_NAME,
            AROUND_ADVICE_INVOKE_METHOD_SIGNATURE
    );
    cv.visitInsn(ARETURN);
    cv.visitLabel(ifStatementLabel);
  }

  /**
   * Creates invocations fo the before interceptors.
   *
   * @param cv
   * @param joinPointInstanceIndex
   * @param registerDepth
   */
  private void createBeforeInterceptorInvocations(final MethodVisitor cv,
                                                  final int joinPointInstanceIndex,
                                                  final int registerDepth) {
    final int loopIndex = registerDepth + 1;
    cv.visitInsn(ICONST_0);
    cv.visitVarInsn(ISTORE, loopIndex);
    Label loopStartLabel = new Label();
    cv.visitLabel(loopStartLabel);
    cv.visitVarInsn(ILOAD, loopIndex);
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitFieldInsn(
            GETFIELD,
            m_joinPointClassName,
            NR_OF_BEFORE_INTERCEPTORS_FIELD_NAME,
            I
    );
    Label loopCheckCondLabel = new Label();
    cv.visitJumpInsn(IF_ICMPGE, loopCheckCondLabel);
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitFieldInsn(
            GETFIELD,
            m_joinPointClassName,
            BEFORE_INTERCEPTORS_FIELD_NAME,
            BEFORE_ADVICE_ARRAY_CLASS_SIGNATURE
    );
    cv.visitVarInsn(ILOAD, loopIndex);
    cv.visitInsn(AALOAD);
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitMethodInsn(
            INVOKEINTERFACE,
            BEFORE_ADVICE_CLASS_NAME,
            INTERCEPT_INVOKE_METHOD_NAME,
            BEFORE_ADVICE_INVOKE_METHOD_SIGNATURE
    );
    cv.visitIincInsn(loopIndex, 1);
    cv.visitJumpInsn(GOTO, loopStartLabel);
    cv.visitLabel(loopCheckCondLabel);
  }

  /**
   * Creates invocations fo the after finally interceptors.
   *
   * @param cv
   * @param joinPointInstanceIndex
   * @param registerDepth
   */
  private void createAfterInterceptorInvocations(final MethodVisitor cv,
                                                 final int joinPointInstanceIndex,
                                                 final int registerDepth) {
    final int loopIndex = registerDepth + 1;
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitFieldInsn(
            GETFIELD,
            m_joinPointClassName,
            NR_OF_AFTER_INTERCEPTORS_FIELD_NAME,
            I
    );
    cv.visitInsn(ICONST_1);
    cv.visitInsn(ISUB);
    cv.visitVarInsn(ISTORE, loopIndex);
    Label loopLabel1 = new Label();
    cv.visitLabel(loopLabel1);
    cv.visitVarInsn(ILOAD, loopIndex);
    Label loopLabel2 = new Label();
    cv.visitJumpInsn(IFLT, loopLabel2);
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitFieldInsn(
            GETFIELD,
            m_joinPointClassName,
            AFTER_INTERCEPTORS_FIELD_NAME,
            AFTER_ADVICE_ARRAY_CLASS_SIGNATURE
    );
    cv.visitVarInsn(ILOAD, loopIndex);
    cv.visitInsn(AALOAD);
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitMethodInsn(
            INVOKEINTERFACE,
            AFTER_ADVICE_CLASS_NAME,
            INTERCEPT_INVOKE_METHOD_NAME,
            AFTER_ADVICE_INVOKE_METHOD_SIGNATURE
    );
    cv.visitIincInsn(loopIndex, -1);
    cv.visitJumpInsn(GOTO, loopLabel1);
    cv.visitLabel(loopLabel2);
  }

  /**
   * Creates invocations fo the after returning interceptors.
   *
   * @param cv
   * @param joinPointInstanceIndex
   * @param returnValueInstanceIndex
   */
  private void createAfterReturningInterceptorInvocations(final MethodVisitor cv,
                                                          final int joinPointInstanceIndex,
                                                          final int returnValueInstanceIndex) {
    final int loopIndex = returnValueInstanceIndex + 1;
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitFieldInsn(
            GETFIELD,
            m_joinPointClassName,
            NR_OF_AFTER_RETURNING_INTERCEPTORS_FIELD_NAME,
            I
    );
    cv.visitInsn(ICONST_1);
    cv.visitInsn(ISUB);
    cv.visitVarInsn(ISTORE, loopIndex);
    Label loopLabel1 = new Label();
    cv.visitLabel(loopLabel1);
    cv.visitVarInsn(ILOAD, loopIndex);
    Label loopLabel2 = new Label();
    cv.visitJumpInsn(IFLT, loopLabel2);
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitFieldInsn(
            GETFIELD,
            m_joinPointClassName,
            AFTER_RETURNING_INTERCEPTORS_FIELD_NAME,
            AFTER_RETURNING_ADVICE_ARRAY_CLASS_SIGNATURE
    );
    cv.visitVarInsn(ILOAD, loopIndex);
    cv.visitInsn(AALOAD);
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitVarInsn(ALOAD, returnValueInstanceIndex);
    cv.visitMethodInsn(
            INVOKEINTERFACE,
            AFTER_RETURNING_ADVICE_CLASS_NAME,
            INTERCEPT_INVOKE_METHOD_NAME,
            AFTER_RETURNING_ADVICE_INVOKE_METHOD_SIGNATURE
    );
    cv.visitIincInsn(loopIndex, -1);
    cv.visitJumpInsn(GOTO, loopLabel1);
    cv.visitLabel(loopLabel2);
  }

  /**
   * Creates invocations fo the after returning interceptors.
   *
   * @param cv
   * @param joinPointInstanceIndex
   * @param exceptionInstanceIndex
   */
  private void createAfterThrowingInterceptorInvocations(final MethodVisitor cv,
                                                         final int joinPointInstanceIndex,
                                                         final int exceptionInstanceIndex) {
    final int loopIndex = exceptionInstanceIndex + 1;
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitFieldInsn(
            GETFIELD,
            m_joinPointClassName,
            NR_OF_AFTER_THROWING_INTERCEPTORS_FIELD_NAME,
            I
    );
    cv.visitInsn(ICONST_1);
    cv.visitInsn(ISUB);
    cv.visitVarInsn(ISTORE, loopIndex);
    Label loopLabel1 = new Label();
    cv.visitLabel(loopLabel1);
    cv.visitVarInsn(ILOAD, loopIndex);
    Label loopLabel2 = new Label();
    cv.visitJumpInsn(IFLT, loopLabel2);
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitFieldInsn(
            GETFIELD,
            m_joinPointClassName,
            AFTER_THROWING_INTERCEPTORS_FIELD_NAME,
            AFTER_THROWING_ADVICE_ARRAY_CLASS_SIGNATURE
    );
    cv.visitVarInsn(ILOAD, loopIndex);
    cv.visitInsn(AALOAD);
    cv.visitVarInsn(ALOAD, joinPointInstanceIndex);
    cv.visitVarInsn(ALOAD, exceptionInstanceIndex);
    cv.visitMethodInsn(
            INVOKEINTERFACE,
            AFTER_THROWING_ADVICE_CLASS_NAME,
            INTERCEPT_INVOKE_METHOD_NAME,
            AFTER_THROWING_ADVICE_INVOKE_METHOD_SIGNATURE
    );
    cv.visitIincInsn(loopIndex, -1);
    cv.visitJumpInsn(GOTO, loopLabel1);
    cv.visitLabel(loopLabel2);
  }

  /**
   * Checks if the join point requires a proceed() method.
   *
   * @return
   */
  private boolean requiresProceedMethod() {
    return m_hasAroundAdvices || m_isThisAdvisable;
  }

  public static boolean requiresCallerInstance(DeploymentModel deployModel) {
    return DeploymentModel.PER_INSTANCE.equals(deployModel)
            || DeploymentModel.PER_THIS.equals(deployModel);
  }

  public static boolean requiresCalleeInstance(DeploymentModel deployModel) {
    return DeploymentModel.PER_TARGET.equals(deployModel);
  }

  public static boolean requiresCallerOrCallee(DeploymentModel deploymentModel) {
    return requiresCallerInstance(deploymentModel)
            || requiresCalleeInstance(deploymentModel);
  }

  public final AspectModel[] getAspectModels() {
    return m_aspectModels;
  }

}
