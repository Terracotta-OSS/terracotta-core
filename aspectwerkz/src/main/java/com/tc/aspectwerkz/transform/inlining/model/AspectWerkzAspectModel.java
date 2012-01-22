/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining.model;

import com.tc.asm.ClassVisitor;
import com.tc.asm.ClassWriter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Label;
import com.tc.asm.Type;

import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.aspect.AdviceInfo;
import com.tc.aspectwerkz.aspect.AdviceType;
import com.tc.aspectwerkz.cflow.CflowCompiler;
import com.tc.aspectwerkz.definition.AspectDefinition;
import com.tc.aspectwerkz.exception.DefinitionException;
import com.tc.aspectwerkz.joinpoint.management.AdviceInfoContainer;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.reflect.ClassInfoHelper;
import com.tc.aspectwerkz.transform.JoinPointCompiler;
import com.tc.aspectwerkz.transform.TransformationConstants;
import com.tc.aspectwerkz.transform.inlining.AdviceMethodInfo;
import com.tc.aspectwerkz.transform.inlining.AsmHelper;
import com.tc.aspectwerkz.transform.inlining.AspectInfo;
import com.tc.aspectwerkz.transform.inlining.compiler.AbstractJoinPointCompiler;
import com.tc.aspectwerkz.transform.inlining.compiler.CompilationInfo;
import com.tc.aspectwerkz.transform.inlining.compiler.CompilerInput;
import com.tc.aspectwerkz.transform.inlining.spi.AspectModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * TODO doc
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class AspectWerkzAspectModel implements AspectModel, Opcodes, TransformationConstants {

  protected final List m_customProceedMethodStructs;


  public AspectWerkzAspectModel() {
    m_customProceedMethodStructs = null;
    //prototype
  }

  private AspectWerkzAspectModel(CompilationInfo.Model compilationModel) {
    m_customProceedMethodStructs = new ArrayList(0);
    collectCustomProceedMethods(compilationModel, compilationModel.getAdviceInfoContainer());
  }

  public static final String TYPE = AspectWerkzAspectModel.class.getName();

  public AspectModel getInstance(CompilationInfo.Model compilationModel) {
    // return a new instance to handle custom proceed
    return new AspectWerkzAspectModel(compilationModel);
  }

  public String getAspectModelType() {
    return TYPE;
  }

  public void defineAspect(ClassInfo aspectClassInfo, AspectDefinition aspectDef, ClassLoader loader) {
  }

  public AroundClosureClassInfo getAroundClosureClassInfo() {
    if (m_customProceedMethodStructs.isEmpty()) {
      //let the compiler deal with JP / SJP interface
      return new AroundClosureClassInfo(OBJECT_CLASS_NAME, new String[0]);
    } else {
      // getDefault the custom join point interfaces
      Set interfaces = new HashSet();
      for (Iterator it = m_customProceedMethodStructs.iterator(); it.hasNext();) {
        MethodInfo methodInfo = ((CustomProceedMethodStruct) it.next()).customProceed;
        interfaces.add(methodInfo.getDeclaringType().getName().replace('.', '/'));
      }
      return new AroundClosureClassInfo(OBJECT_CLASS_NAME, (String[]) interfaces.toArray(new String[]{}));
    }
  }

  public void createMandatoryMethods(ClassWriter cw, JoinPointCompiler compiler) {
    createCustomProceedMethods(cw, (AbstractJoinPointCompiler) compiler);
  }

  public void createInvocationOfAroundClosureSuperClass(MethodVisitor cv) {
    ;// AW model has no super class apart Object, which is handled by the compiler
  }

  /**
   * Create and initialize the aspect field for a specific aspect (qualified since it depends
   * on the param, deployment model, container etc).
   * And creates instantiation of aspects using the Aspects.aspectOf() methods which uses the AspectContainer impls.
   * We are using the THIS_CLASS classloader since the aspect can be visible from that one only f.e. for getDefault/set/call
   * <p/>
   * TODO for perJVM and perClass aspect this means we eagerly load the aspect. Different from AJ
   *
   * @param cw
   * @param cv
   * @param aspectInfo
   * @param joinPointClassName
   */
  public void createAndStoreStaticAspectInstantiation(ClassVisitor cw, MethodVisitor cv, AspectInfo aspectInfo, String joinPointClassName) {
    String aspectClassSignature = aspectInfo.getAspectClassSignature();
    String aspectClassName = aspectInfo.getAspectClassName();
    // retrieve the aspect set it to the field
    DeploymentModel deploymentModel = aspectInfo.getDeploymentModel();
    if (CflowCompiler.isCflowClass(aspectClassName)) {
      cw.visitField(ACC_PRIVATE + ACC_STATIC, aspectInfo.getAspectFieldName(), aspectClassSignature, null, null);
      // handle Cflow native aspectOf
      //TODO: would be better done with a custom aspectModel for cflow, or with default factory handling
      //FIXME AVF what does factory do there ?
      cv.visitMethodInsn(
              INVOKESTATIC,
              aspectClassName,
              CflowCompiler.CFLOW_ASPECTOF_METHOD_NAME,
              "()" + aspectClassSignature
      );
      cv.visitFieldInsn(PUTSTATIC, joinPointClassName, aspectInfo.getAspectFieldName(), aspectClassSignature);
    } else if (deploymentModel.equals(DeploymentModel.PER_JVM)) {
      cw.visitField(ACC_PRIVATE + ACC_STATIC, aspectInfo.getAspectFieldName(), aspectClassSignature, null, null);
      cv.visitMethodInsn(
              INVOKESTATIC,
              aspectInfo.getAspectFactoryClassName(),
              "aspectOf",
              "()" + aspectClassSignature
      );
      cv.visitFieldInsn(PUTSTATIC, joinPointClassName, aspectInfo.getAspectFieldName(), aspectClassSignature);
    } else if (deploymentModel.equals(DeploymentModel.PER_CLASS)) {
      cw.visitField(ACC_PRIVATE + ACC_STATIC, aspectInfo.getAspectFieldName(), aspectClassSignature, null, null);
      cv.visitFieldInsn(GETSTATIC, joinPointClassName, THIS_CLASS_FIELD_NAME_IN_JP, CLASS_CLASS_SIGNATURE);
      cv.visitMethodInsn(
              INVOKESTATIC,
              aspectInfo.getAspectFactoryClassName(),
              "aspectOf",
              "(Ljava/lang/Class;)" + aspectClassSignature
      );
      cv.visitFieldInsn(PUTSTATIC, joinPointClassName, aspectInfo.getAspectFieldName(), aspectClassSignature);
    } else if (AbstractJoinPointCompiler.requiresCallerOrCallee(deploymentModel)) {
      cw.visitField(ACC_PRIVATE, aspectInfo.getAspectFieldName(), aspectClassSignature, null, null);
    } else {
      throw new UnsupportedOperationException(
              "unsupported deployment model - " +
                      aspectInfo.getAspectClassName() + " " +
                      deploymentModel
      );
    }
  }

  /**
   * Initializes instance level aspects, retrieves them from the target instance through the
   * <code>HasInstanceLevelAspect</code> interfaces.
   * <p/>
   * Use by 'perInstance', 'perThis' and 'perTarget' deployment models.
   *
   * @param cv
   * @param aspectInfo
   * @param input
   */
  public void createAndStoreRuntimeAspectInstantiation(final MethodVisitor cv,
                                                       final CompilerInput input,
                                                       final AspectInfo aspectInfo) {
    // gen code: if (Aspects.hasAspect(...) { aspectField = (<TYPE>)((HasInstanceLocalAspect)CALLER).aw$getAspect(className, qualifiedName, containerClassName) }
    if (DeploymentModel.PER_INSTANCE.equals(aspectInfo.getDeploymentModel())) {//TODO && callerIndex >= 0
      //storeAspectInstance(cv, input, aspectInfo, input.callerIndex);
    } else if (DeploymentModel.PER_THIS.equals(aspectInfo.getDeploymentModel())
            && input.callerIndex >= 0) {
      Label hasAspectCheck = pushPerXCondition(cv, input.callerIndex, aspectInfo);
      storeAspectInstance(cv, input, aspectInfo, input.callerIndex);
      cv.visitLabel(hasAspectCheck);
    } else if (DeploymentModel.PER_TARGET.equals(aspectInfo.getDeploymentModel())
            && input.calleeIndex >= 0) {
      Label hasAspectCheck = pushPerXCondition(cv, input.calleeIndex, aspectInfo);
      storeAspectInstance(cv, input, aspectInfo, input.calleeIndex);
      cv.visitLabel(hasAspectCheck);
    }

    if (aspectInfo.getDeploymentModel() == DeploymentModel.PER_INSTANCE) {//TODO refactor with previous if block
      // gen code: aspectField = (<TYPE>)((HasInstanceLocalAspect)CALLER).aw$getAspect(className, qualifiedName, containerClassName)
      AbstractJoinPointCompiler.loadJoinPointInstance(cv, input);
      if (input.callerIndex >= 0) {
        cv.visitVarInsn(ALOAD, input.callerIndex);
      } else {
        // caller instance not available - skipping
        //TODO clean up should not occur
      }
      cv.visitMethodInsn(
              INVOKESTATIC,
              aspectInfo.getAspectFactoryClassName(),
              "aspectOf",
              "(Ljava/lang/Object;)" + aspectInfo.getAspectClassSignature()
      );
//            cv.visitLdcInsn(aspectInfo.getAspectClassName().replace('/', '.'));
//            cv.visitLdcInsn(aspectInfo.getAspectQualifiedName());
//            AsmHelper.loadStringConstant(cv, aspectInfo.getAspectDefinition().getContainerClassName());
//            cv.visitMethodInsn(
//                    INVOKEINTERFACE,
//                    HAS_INSTANCE_LEVEL_ASPECT_INTERFACE_NAME,
//                    INSTANCE_LEVEL_GETASPECT_METHOD_NAME,
//                    INSTANCE_LEVEL_GETASPECT_METHOD_SIGNATURE
//            );
//            cv.visitTypeInsn(CHECKCAST, aspectInfo.getAspectClassName());
      cv.visitFieldInsn(
              PUTFIELD,
              input.joinPointClassName,
              aspectInfo.getAspectFieldName(),
              aspectInfo.getAspectClassSignature()
      );
    }
  }


  /**
   * Load aspect instance on stack
   *
   * @param cv
   * @param input
   * @param aspectInfo
   */
  public void loadAspect(final MethodVisitor cv,
                         final CompilerInput input,
                         final AspectInfo aspectInfo) {
    DeploymentModel deploymentModel = aspectInfo.getDeploymentModel();
    if (DeploymentModel.PER_JVM.equals(deploymentModel)
            || DeploymentModel.PER_CLASS.equals(deploymentModel)) {
      cv.visitFieldInsn(
              GETSTATIC, input.joinPointClassName, aspectInfo.getAspectFieldName(),
              aspectInfo.getAspectClassSignature()
      );
    } else if (DeploymentModel.PER_INSTANCE.equals(deploymentModel)) {
      AbstractJoinPointCompiler.loadJoinPointInstance(cv, input);
      cv.visitFieldInsn(
              GETFIELD, input.joinPointClassName, aspectInfo.getAspectFieldName(),
              aspectInfo.getAspectClassSignature()
      );
    } else if (DeploymentModel.PER_THIS.equals(deploymentModel)) {
      AbstractJoinPointCompiler.loadJoinPointInstance(cv, input);
      cv.visitFieldInsn(
              GETFIELD,
              input.joinPointClassName,
              aspectInfo.getAspectFieldName(),
              aspectInfo.getAspectClassSignature()
      );

      //FIXME see FIXME on aspect instantion
      Label nullCheck = new Label();
      cv.visitJumpInsn(IFNONNULL, nullCheck);
      storeAspectInstance(cv, input, aspectInfo, input.callerIndex);
      cv.visitLabel(nullCheck);

      AbstractJoinPointCompiler.loadJoinPointInstance(cv, input);
      cv.visitFieldInsn(
              GETFIELD,
              input.joinPointClassName,
              aspectInfo.getAspectFieldName(),
              aspectInfo.getAspectClassSignature()
      );
    } else if (DeploymentModel.PER_TARGET.equals(deploymentModel)) {
      AbstractJoinPointCompiler.loadJoinPointInstance(cv, input);
      cv.visitFieldInsn(
              GETFIELD,
              input.joinPointClassName,
              aspectInfo.getAspectFieldName(),
              aspectInfo.getAspectClassSignature()
      );
      //FIXME see FIXME on aspect instantion

      Label nullCheck = new Label();
      cv.visitJumpInsn(IFNONNULL, nullCheck);
      storeAspectInstance(cv, input, aspectInfo, input.calleeIndex);
      cv.visitLabel(nullCheck);

      AbstractJoinPointCompiler.loadJoinPointInstance(cv, input);
      cv.visitFieldInsn(
              GETFIELD,
              input.joinPointClassName,
              aspectInfo.getAspectFieldName(),
              aspectInfo.getAspectClassSignature()
      );
    } else {
      throw new DefinitionException("deployment model [" + deploymentModel + "] is not supported");
    }
  }

  public void createAroundAdviceArgumentHandling(MethodVisitor cv,
                                                 CompilerInput input,
                                                 Type[] joinPointArgumentTypes,
                                                 AdviceMethodInfo adviceMethodInfo) {
    defaultCreateAroundAdviceArgumentHandling(
            cv,
            input,
            joinPointArgumentTypes,
            adviceMethodInfo
    );
  }

  public void createBeforeOrAfterAdviceArgumentHandling(MethodVisitor cv,
                                                        CompilerInput input,
                                                        Type[] joinPointArgumentTypes,
                                                        AdviceMethodInfo adviceMethodInfo,
                                                        int specialArgIndex) {
    defaultCreateBeforeOrAfterAdviceArgumentHandling(
            cv,
            input,
            joinPointArgumentTypes,
            adviceMethodInfo,
            specialArgIndex
    );
  }

  public boolean requiresReflectiveInfo() {
    // custom proceed() JoinPoint will not be recognize by the default logic
    return m_customProceedMethodStructs.size() > 0;
  }

  ///////---------- Helpers

  /**
   * Generate a "if Aspects.hasAspect(qName, instance)"
   *
   * @param cv
   * @param perInstanceIndex
   * @param aspectInfo
   * @return
   */
  private Label pushPerXCondition(final MethodVisitor cv,
                                  final int perInstanceIndex,
                                  final AspectInfo aspectInfo) {
    Label hasAspectCheck = new Label();

    cv.visitVarInsn(ALOAD, perInstanceIndex);
    cv.visitMethodInsn(
            INVOKESTATIC,
            aspectInfo.getAspectFactoryClassName(),
            FACTORY_HASASPECT_METHOD_NAME,
            FACTORY_HASASPECT_PEROBJECT_METHOD_SIGNATURE
    );
    cv.visitJumpInsn(IFEQ, hasAspectCheck);

    return hasAspectCheck;
  }

  /**
   * Creates the instance of an aspect by invoking
   * "HasInstanceLevelAspect.aw$getAspect(String, String)" on perInstanceIndex variable
   * and stores the aspect instance in the joinpoint instance field
   */
  private void storeAspectInstance(final MethodVisitor cv,
                                   final CompilerInput input,
                                   final AspectInfo aspectInfo,
                                   final int perInstanceIndex) {
    AbstractJoinPointCompiler.loadJoinPointInstance(cv, input);

    if (perInstanceIndex >= 0) {
      cv.visitVarInsn(ALOAD, perInstanceIndex);
    }

    cv.visitMethodInsn(
            INVOKESTATIC,
            aspectInfo.getAspectFactoryClassName(),
            FACTORY_ASPECTOF_METHOD_NAME,
            "(Ljava/lang/Object;)" + aspectInfo.getAspectClassSignature()
    );
//
//        cv.visitLdcInsn(aspectInfo.getAspectClassName().replace('/', '.'));
//        cv.visitLdcInsn(aspectInfo.getAspectQualifiedName());
//        AsmHelper.loadStringConstant(cv, aspectInfo.getAspectDefinition().getContainerClassName());
//        cv.visitMethodInsn(
//                INVOKEINTERFACE,
//                HAS_INSTANCE_LEVEL_ASPECT_INTERFACE_NAME,
//                INSTANCE_LEVEL_GETASPECT_METHOD_NAME,
//                INSTANCE_LEVEL_GETASPECT_METHOD_SIGNATURE
//        );
//
//        cv.visitTypeInsn(CHECKCAST, aspectInfo.getAspectClassName());

    cv.visitFieldInsn(
            PUTFIELD,
            input.joinPointClassName,
            aspectInfo.getAspectFieldName(),
            aspectInfo.getAspectClassSignature()
    );
  }

  /**
   * Creates the custom proceed methods.
   */
  private void createCustomProceedMethods(ClassWriter cw, AbstractJoinPointCompiler compiler) {
    Set addedMethodSignatures = new HashSet();
    for (Iterator it = m_customProceedMethodStructs.iterator(); it.hasNext();) {
      CustomProceedMethodStruct customProceedStruct = (CustomProceedMethodStruct) it.next();
      MethodInfo methodInfo = customProceedStruct.customProceed;
      final String desc = methodInfo.getSignature();

      if (addedMethodSignatures.contains(desc)) {
        continue;
      }
      addedMethodSignatures.add(desc);

      MethodVisitor cv = cw.visitMethod(
              ACC_PUBLIC | ACC_FINAL,
              PROCEED_METHOD_NAME,
              desc,
              null,
              new String[]{
                      THROWABLE_CLASS_NAME
              }
      );

      // update the joinpoint instance with the given values
      // starts at 1 since first arg is the custom join point by convention
      //TODO see JoinPointManage for this custom jp is first convention
      int argStackIndex = 1;
      for (int i = 1; i < customProceedStruct.adviceToTargetArgs.length; i++) {
        int targetArg = customProceedStruct.adviceToTargetArgs[i];
        if (targetArg >= 0) {
          // regular arg
          String fieldName = compiler.m_fieldNames[targetArg];
          cv.visitVarInsn(ALOAD, 0);
          Type type = compiler.m_argumentTypes[targetArg];
          argStackIndex = AsmHelper.loadType(cv, argStackIndex, type);
          cv.visitFieldInsn(PUTFIELD, compiler.m_joinPointClassName, fieldName, type.getDescriptor());
        } else if (targetArg == AdviceInfo.TARGET_ARG) {
          cv.visitVarInsn(ALOAD, 0);
          argStackIndex = AsmHelper.loadType(
                  cv, argStackIndex, Type.getType(compiler.m_calleeClassSignature)
          );
          cv.visitFieldInsn(
                  PUTFIELD,
                  compiler.m_joinPointClassName,
                  CALLEE_INSTANCE_FIELD_NAME,
                  compiler.m_calleeClassSignature
          );
        } else if (targetArg == AdviceInfo.THIS_ARG) {
          cv.visitVarInsn(ALOAD, 0);
          argStackIndex = AsmHelper.loadType(
                  cv, argStackIndex, Type.getType(compiler.m_callerClassSignature)
          );
          cv.visitFieldInsn(
                  PUTFIELD,
                  compiler.m_joinPointClassName,
                  CALLER_INSTANCE_FIELD_NAME,
                  compiler.m_callerClassSignature
          );
        } else {
          ;//skip it
        }
      }

      // call proceed()
      // and handles unwrapping for returning primitive
      Type returnType = Type.getType(customProceedStruct.customProceed.getReturnType().getSignature());
      if (AsmHelper.isPrimitive(returnType)) {
        cv.visitVarInsn(ALOAD, 0);
        cv.visitMethodInsn(
                INVOKESPECIAL,
                compiler.m_joinPointClassName,
                PROCEED_METHOD_NAME,
                PROCEED_METHOD_SIGNATURE
        );
        AsmHelper.unwrapType(cv, returnType);
      } else {
        cv.visitVarInsn(ALOAD, 0);
        cv.visitMethodInsn(
                INVOKESPECIAL,
                compiler.m_joinPointClassName,
                PROCEED_METHOD_NAME,
                PROCEED_METHOD_SIGNATURE
        );
        if (!returnType.getClassName().equals(OBJECT_CLASS_SIGNATURE)) {
          cv.visitTypeInsn(CHECKCAST, returnType.getInternalName());
        }
      }
      AsmHelper.addReturnStatement(cv, returnType);
      cv.visitMaxs(0, 0);
    }
  }

  //---------- Model specific methods

  /**
   * Collects the custom proceed methods used in the advice specified.
   *
   * @param model
   * @param advices
   */
  private void collectCustomProceedMethods(final CompilationInfo.Model model,
                                           final AdviceInfoContainer advices) {
    ClassLoader loader = model.getThisClassInfo().getClassLoader();
    final AdviceInfo[] beforeAdviceInfos = advices.getBeforeAdviceInfos();
    for (int i = 0; i < beforeAdviceInfos.length; i++) {
      collectCustomProceedMethods(beforeAdviceInfos[i], loader);
    }
    final AdviceInfo[] aroundAdviceInfos = advices.getAroundAdviceInfos();
    for (int i = 0; i < aroundAdviceInfos.length; i++) {
      collectCustomProceedMethods(aroundAdviceInfos[i], loader);
    }
    final AdviceInfo[] afterFinallyAdviceInfos = advices.getAfterFinallyAdviceInfos();
    for (int i = 0; i < afterFinallyAdviceInfos.length; i++) {
      collectCustomProceedMethods(afterFinallyAdviceInfos[i], loader);
    }
    final AdviceInfo[] afterReturningAdviceInfos = advices.getAfterReturningAdviceInfos();
    for (int i = 0; i < afterReturningAdviceInfos.length; i++) {
      collectCustomProceedMethods(afterReturningAdviceInfos[i], loader);
    }
    final AdviceInfo[] afterThrowingAdviceInfos = advices.getAfterThrowingAdviceInfos();
    for (int i = 0; i < afterThrowingAdviceInfos.length; i++) {
      collectCustomProceedMethods(afterThrowingAdviceInfos[i], loader);
    }
  }

  /**
   * Collects the custom proceed methods used in the advice specified.
   *
   * @param adviceInfo
   * @param loader
   */
  private void collectCustomProceedMethods(final AdviceInfo adviceInfo, final ClassLoader loader) {
    final Type[] paramTypes = adviceInfo.getMethodParameterTypes();
    if (paramTypes.length != 0) {
      Type firstParam = paramTypes[0];
      //TODO should we support JP at other positions or lock the other advice models then so that JP..
      // ..is not there or first only ?
      // check if first param is an object but not a JP or SJP
      if (firstParam.getSort() == Type.OBJECT &&
              !firstParam.getClassName().equals(JOIN_POINT_JAVA_CLASS_NAME) &&
              !firstParam.getClassName().equals(STATIC_JOIN_POINT_JAVA_CLASS_NAME)) {
        ClassInfo classInfo = AsmClassInfo.getClassInfo(firstParam.getClassName(), loader);
        if (ClassInfoHelper.implementsInterface(classInfo, JOIN_POINT_JAVA_CLASS_NAME) ||
                ClassInfoHelper.implementsInterface(classInfo, STATIC_JOIN_POINT_JAVA_CLASS_NAME)) {
          // we have ourselves a custom joinpoint
          MethodInfo[] methods = classInfo.getMethods();
          for (int j = 0; j < methods.length; j++) {
            MethodInfo method = methods[j];
            if (method.getName().equals(PROCEED_METHOD_NAME)) {
              // we inherit the binding from the advice that actually use us
              // for now the first advice sets the rule
              // it is up to the user to ensure consistency if the custom proceed
              // is used more than once in different advices.
              m_customProceedMethodStructs.add(
                      new CustomProceedMethodStruct(
                              method,
                              adviceInfo.getMethodToArgIndexes()
                      )
              );
            }
          }
        }
      }
    }
  }

  private static class CustomProceedMethodStruct {
    MethodInfo customProceed;
    int[] adviceToTargetArgs;

    public CustomProceedMethodStruct(MethodInfo customProceed, int[] adviceToTargetArgs) {
      this.customProceed = customProceed;
      this.adviceToTargetArgs = adviceToTargetArgs;
    }
  }

  public static void defaultCreateBeforeOrAfterAdviceArgumentHandling(MethodVisitor cv,
                                                                      CompilerInput input,
                                                                      Type[] joinPointArgumentTypes,
                                                                      AdviceMethodInfo adviceMethodInfo,
                                                                      int specialArgIndex) {
    int[] argIndexes = adviceMethodInfo.getAdviceMethodArgIndexes();
    // if empty, we consider for now that we have to push JoinPoint for old advice with JoinPoint as sole arg
    for (int j = 0; j < argIndexes.length; j++) {
      int argIndex = argIndexes[j];
      if (argIndex >= 0) {
        Type argumentType = joinPointArgumentTypes[argIndex];
        int argStackIndex = AsmHelper.getRegisterIndexOf(joinPointArgumentTypes, argIndex) + input.argStartIndex;
        AsmHelper.loadType(cv, argStackIndex, argumentType);
      } else if (argIndex == AdviceInfo.JOINPOINT_ARG || argIndex == AdviceInfo.STATIC_JOINPOINT_ARG) {
        AbstractJoinPointCompiler.loadJoinPointInstance(cv, input);
      } else if (argIndex == AdviceInfo.TARGET_ARG) {
        AbstractJoinPointCompiler.loadCallee(cv, input);
        // add a cast if runtime check was used
        if (adviceMethodInfo.getAdviceInfo().hasTargetWithRuntimeCheck()) {
          cv.visitTypeInsn(
                  CHECKCAST,
                  adviceMethodInfo.getAdviceInfo().getMethodParameterTypes()[j].getInternalName()
          );
        }
      } else if (argIndex == AdviceInfo.THIS_ARG) {
        AbstractJoinPointCompiler.loadCaller(cv, input);
      } else if (argIndex == AdviceInfo.SPECIAL_ARGUMENT && specialArgIndex != INDEX_NOTAVAILABLE) {
        Type argumentType = adviceMethodInfo.getAdviceInfo().getMethodParameterTypes()[j];
        AsmHelper.loadType(cv, specialArgIndex, argumentType);
        if (AdviceType.AFTER_THROWING.equals(adviceMethodInfo.getAdviceInfo().getAdviceDefinition().getType())
                || AdviceType.AFTER_RETURNING.equals(adviceMethodInfo.getAdviceInfo().getAdviceDefinition().getType()))
        {
          cv.visitTypeInsn(CHECKCAST, argumentType.getInternalName());
        }
      } else {
        throw new Error("magic index is not supported: " + argIndex);
      }
    }
  }

  public static void defaultCreateAroundAdviceArgumentHandling(MethodVisitor cv,
                                                               CompilerInput input,
                                                               Type[] joinPointArgumentTypes,
                                                               AdviceMethodInfo adviceMethodInfo) {
    int[] argIndexes = adviceMethodInfo.getAdviceMethodArgIndexes();
    for (int j = 0; j < argIndexes.length; j++) {
      int argIndex = argIndexes[j];
      if (argIndex >= 0) {
        Type argumentType = joinPointArgumentTypes[argIndex];
        cv.visitVarInsn(ALOAD, 0);
        cv.visitFieldInsn(
                GETFIELD,
                input.joinPointClassName,
                ARGUMENT_FIELD + argIndex,
                argumentType.getDescriptor()
        );
      } else if (argIndex == AdviceInfo.JOINPOINT_ARG ||
              argIndex == AdviceInfo.STATIC_JOINPOINT_ARG ||
              argIndex == AdviceInfo.VALID_NON_AW_AROUND_CLOSURE_TYPE ||
              argIndex == AdviceInfo.CUSTOM_JOIN_POINT_ARG) {
        cv.visitVarInsn(ALOAD, 0);
      } else if (argIndex == AdviceInfo.TARGET_ARG) {
        AbstractJoinPointCompiler.loadCallee(cv, input);
        // add a cast if runtime check was used
        if (adviceMethodInfo.getAdviceInfo().hasTargetWithRuntimeCheck()) {
          cv.visitTypeInsn(
                  CHECKCAST,
                  adviceMethodInfo.getAdviceInfo().getMethodParameterTypes()[j].getInternalName()
          );
        }
      } else if (argIndex == AdviceInfo.THIS_ARG) {
        AbstractJoinPointCompiler.loadCaller(cv, input);
      } else {
        throw new Error("advice method argument index type is not supported: " + argIndex);
      }
    }
  }

}
