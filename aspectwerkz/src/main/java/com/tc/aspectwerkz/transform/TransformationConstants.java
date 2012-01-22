/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.transform;

import com.tc.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface with common constants used in the transformation process.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public interface TransformationConstants extends Opcodes {
  public static final List EMTPTY_ARRAY_LIST = new ArrayList();
  public static final String[] EMPTY_STRING_ARRAY = new String[0];

  // prefixes
  public static final String DELIMITER = "$_AW_$";
  public static final String ASPECTWERKZ_PREFIX = "___AW_";
  public static final String WRAPPER_METHOD_PREFIX = "aw$";
  public static final String SYNTHETIC_MEMBER_PREFIX = "aw$";
  public static final String ORIGINAL_METHOD_PREFIX = WRAPPER_METHOD_PREFIX + "original" + DELIMITER;
  public static final String STATICINITIALIZER_WRAPPER_METHOD_KEY = "aw_clinit";
  public static final String INVOKE_WRAPPER_METHOD_PREFIX = "INVOKE" + DELIMITER;
  public static final String PUTFIELD_WRAPPER_METHOD_PREFIX = "PUTFIELD" + DELIMITER;
  public static final String GETFIELD_WRAPPER_METHOD_PREFIX = "GETFIELD" + DELIMITER;
  public static final String JOIN_POINT_CLASS_SUFFIX = ASPECTWERKZ_PREFIX + "JoinPoint";

  // internal fields
  public static final String SERIAL_VERSION_UID_FIELD_NAME = "serialVersionUID";
  public static final String TARGET_CLASS_FIELD_NAME = SYNTHETIC_MEMBER_PREFIX + "clazz";
  public static final String EMITTED_JOINPOINTS_FIELD_NAME = SYNTHETIC_MEMBER_PREFIX + "emittedJoinPoints";

  // internal methods
  public static final String INIT_JOIN_POINTS_METHOD_NAME = WRAPPER_METHOD_PREFIX + "initJoinPoints";
  public static final String STATIC_INITIALIZATION_METHOD_NAME = WRAPPER_METHOD_PREFIX + "staticinitialization";

  // method and class names
  public static final String INIT_METHOD_NAME = "<init>";
  public static final String CLINIT_METHOD_NAME = "<clinit>";
  public static final String CLINIT_METHOD_SIGNATURE = "()V";
  public static final String CLASS_LOADER_REFLECT_CLASS_NAME = "java.lang.ClassLoader";
  public static final String CLASS_LOADER_CLASS_NAME = "java/lang/ClassLoader";
  public static final String DEFINE_CLASS_METHOD_NAME = "defineClass";
  public static final String INVOKE_METHOD_NAME = "invoke";
  public static final String FOR_NAME_METHOD_NAME = "forName";
  public static final String LOAD_JOIN_POINT_METHOD_NAME = "loadJoinPoint";
  public static final String MIXINS_CLASS_NAME = "com/tc/aspectwerkz/aspect/management/Mixins";
  public static final String MIXIN_OF_METHOD_NAME = "mixinOf";
  public static final String MIXIN_OF_METHOD_PER_JVM_SIGNATURE = "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/Object;";
  public static final String MIXIN_OF_METHOD_PER_CLASS_SIGNATURE = "(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;";
  public static final String MIXIN_OF_METHOD_PER_INSTANCE_SIGNATURE = "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;";

  // java types and signatures
  public static final String SHORT_CLASS_NAME = "java/lang/Short";
  public static final String INTEGER_CLASS_NAME = "java/lang/Integer";
  public static final String LONG_CLASS_NAME = "java/lang/Long";
  public static final String FLOAT_CLASS_NAME = "java/lang/Float";
  public static final String DOUBLE_CLASS_NAME = "java/lang/Double";
  public static final String BYTE_CLASS_NAME = "java/lang/Byte";
  public static final String BOOLEAN_CLASS_NAME = "java/lang/Boolean";
  public static final String CHARACTER_CLASS_NAME = "java/lang/Character";
  public static final String OBJECT_CLASS_SIGNATURE = "Ljava/lang/Object;";
  public static final String OBJECT_CLASS_NAME = "java/lang/Object";
  public static final String CLASS_CLASS_SIGNATURE = "Ljava/lang/Class;";
  public static final String CLASS_CLASS = "java/lang/Class";
  public static final String THROWABLE_CLASS_NAME = "java/lang/Throwable";
  public static final String SHORT_VALUE_METHOD_NAME = "shortValue";
  public static final String INT_VALUE_METHOD_NAME = "intValue";
  public static final String LONG_VALUE_METHOD_NAME = "longValue";
  public static final String FLOAT_VALUE_METHOD_NAME = "floatValue";
  public static final String DOUBLE_VALUE_METHOD_NAME = "doubleValue";
  public static final String BYTE_VALUE_METHOD_NAME = "byteValue";
  public static final String BOOLEAN_VALUE_METHOD_NAME = "booleanValue";
  public static final String CHAR_VALUE_METHOD_NAME = "charValue";
  public static final String CHAR_VALUE_METHOD_SIGNATURE = "()C";
  public static final String BOOLEAN_VALUE_METHOD_SIGNATURE = "()Z";
  public static final String BYTE_VALUE_METHOD_SIGNATURE = "()B";
  public static final String DOUBLE_VALUE_METHOD_SIGNATURE = "()D";
  public static final String FLOAT_VALUE_METHOD_SIGNATURE = "()F";
  public static final String LONG_VALUE_METHOD_SIGNATURE = "()J";
  public static final String INT_VALUE_METHOD_SIGNATURE = "()I";
  public static final String SHORT_VALUE_METHOD_SIGNATURE = "()S";
  public static final String SHORT_CLASS_INIT_METHOD_SIGNATURE = "(S)V";
  public static final String INTEGER_CLASS_INIT_METHOD_SIGNATURE = "(I)V";
  public static final String LONG_CLASS_INIT_METHOD_SIGNATURE = "(J)V";
  public static final String FLOAT_CLASS_INIT_METHOD_SIGNATURE = "(F)V";
  public static final String DOUBLE_CLASS_INIT_METHOD_SIGNATURE = "(D)V";
  public static final String BYTE_CLASS_INIT_METHOD_SIGNATURE = "(B)V";
  public static final String BOOLEAN_CLASS_INIT_METHOD_SIGNATURE = "(Z)V";
  public static final String CHARACTER_CLASS_INIT_METHOD_SIGNATURE = "(C)V";
  public static final String CLASS_CLASS_GETCLASSLOADER_METHOD_SIGNATURE = "()Ljava/lang/ClassLoader;";
  public static final String ENCLOSING_SJP_FIELD_CLASS_SIGNATURE = "Lcom/tc/aspectwerkz/joinpoint/EnclosingStaticJoinPoint;";
  public static final String L = "L";
  public static final String I = "I";
  public static final String SEMICOLON = ";";

  public static final byte EMPTY_WRAPPER_ATTRIBUTE_VALUE_EMPTY = Byte.MIN_VALUE;
  public static final byte EMPTY_WRAPPER_ATTRIBUTE_VALUE_NOTEMPTY = Byte.MAX_VALUE;

  // optim flags
  public static final boolean OPTIMIZED_JOIN_POINT = true;
  public static final boolean NON_OPTIMIZED_JOIN_POINT = false;

  // static and member field names
//    public static final String MIXIN_FIELD_NAME = "MIXIN_";
  public static final String MIXIN_FIELD_NAME = SYNTHETIC_MEMBER_PREFIX + "MIXIN_";
  public static final String SIGNATURE_FIELD_NAME = "SIGNATURE";
  public static final String META_DATA_FIELD_NAME = "META_DATA";
  public static final String ASPECT_FIELD_PREFIX = "ASPECT_";
  public static final String STACK_FRAME_COUNTER_FIELD_NAME = "STACK_FRAME_COUNTER";
  public static final String INTERCEPTOR_INDEX_FIELD_NAME = "INTERCEPTOR_INDEX";
  public static final String CALLEE_INSTANCE_FIELD_NAME = "CALLEE";
  public static final String CALLER_INSTANCE_FIELD_NAME = "CALLER";
  public static final String ARGUMENT_FIELD = "ARGUMENT_";
  public static final String RETURN_VALUE_FIELD_NAME = "RETURN_VALUE";
  public static final String OPTIMIZED_JOIN_POINT_INSTANCE_FIELD_NAME = "OPTIMIZED_JOIN_POINT";
  public static final String ENCLOSING_SJP_FIELD_NAME = "ENCLOSINGSJP";

  public static final String AROUND_INTERCEPTORS_FIELD_NAME = "AROUND_INTERCEPTORS";
  public static final String NR_OF_AROUND_INTERCEPTORS_FIELD_NAME = "NR_OF_AROUND_INTERCEPTORS";
  public static final String BEFORE_INTERCEPTORS_FIELD_NAME = "BEFORE_INTERCEPTORS";
  public static final String NR_OF_BEFORE_INTERCEPTORS_FIELD_NAME = "NR_OF_BEFORE_INTERCEPTORS";
  public static final String AFTER_INTERCEPTORS_FIELD_NAME = "AFTER_INTERCEPTORS";
  public static final String NR_OF_AFTER_INTERCEPTORS_FIELD_NAME = "NR_OF_AFTER_INTERCEPTORS";
  public static final String AFTER_RETURNING_INTERCEPTORS_FIELD_NAME = "AFTER_RETURNING_INTERCEPTORS";
  public static final String NR_OF_AFTER_RETURNING_INTERCEPTORS_FIELD_NAME = "NR_OF_AFTER_RETURNING_INTERCEPTORS";
  public static final String AFTER_THROWING_INTERCEPTORS_FIELD_NAME = "AFTER_THROWING_INTERCEPTORS";
  public static final String NR_OF_AFTER_THROWING_INTERCEPTORS_FIELD_NAME = "NR_OF_AFTER_THROWING_INTERCEPTORS";

  // runtime system signatures and types

  public static final String JOIN_POINT_MANAGER_CLASS_NAME = "com/tc/aspectwerkz/joinpoint/management/JoinPointManager";
  public static final String LOAD_JOIN_POINT_METHOD_SIGNATURE = "(ILjava/lang/Class;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;IILjava/lang/String;)V";
  public static final String FOR_NAME_METHOD_SIGNATURE = "(Ljava/lang/String;)Ljava/lang/Class;";
  public static final String METHOD_SIGNATURE_IMPL_CLASS_NAME = "com/tc/aspectwerkz/joinpoint/impl/MethodSignatureImpl";
  public static final String METHOD_SIGNATURE_IMPL_CLASS_SIGNATURE = "Lcom/tc/aspectwerkz/joinpoint/impl/MethodSignatureImpl;";
  public static final String CONSTRUCTOR_SIGNATURE_IMPL_CLASS_SIGNATURE = "Lcom/tc/aspectwerkz/joinpoint/impl/ConstructorSignatureImpl;";
  public static final String FIELD_SIGNATURE_IMPL_CLASS_SIGNATURE = "Lcom/tc/aspectwerkz/joinpoint/impl/FieldSignatureImpl;";
  public static final String HANDLER_SIGNATURE_IMPL_CLASS_SIGNATURE = "Lcom/tc/aspectwerkz/joinpoint/impl/CatchClauseSignatureImpl;";
  public static final String NEW_METHOD_SIGNATURE_METHOD_SIGNATURE = "(Ljava/lang/Class;I)Lcom/tc/aspectwerkz/joinpoint/impl/MethodSignatureImpl;";
  public static final String NEW_CONSTRUCTOR_SIGNATURE_METHOD_SIGNATURE = "(Ljava/lang/Class;I)Lcom/tc/aspectwerkz/joinpoint/impl/ConstructorSignatureImpl;";
  public static final String NEW_FIELD_SIGNATURE_METHOD_SIGNATURE = "(Ljava/lang/Class;I)Lcom/tc/aspectwerkz/joinpoint/impl/FieldSignatureImpl;";
  public static final String NEW_HANDLER_SIGNATURE_METHOD_SIGNATURE = "(Ljava/lang/Class;)Lcom/tc/aspectwerkz/joinpoint/impl/CatchClauseSignatureImpl;";
  public static final String SIGNATURE_FACTORY_CLASS = "com/tc/aspectwerkz/joinpoint/management/SignatureFactory";
  public static final String GETCLASSLOADER_METHOD_NAME = "getClassLoader";
  public static final String ASPECT_OF_PER_CLASS_METHOD_SIGNATURE = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;";
  public static final String ASPECT_OF_PER_INSTANCE_METHOD_SIGNATURE = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;";
  public static final String GET_CFLOW_STACK_METHOD_NAME = "getCflowStack";
  public static final String GET_CFLOW_STACK_METHOD_SIGNATURE = "(Ljava/lang/Class;)Lcom/tc/aspectwerkz/CflowStack;";
  public static final String GET_ENCLOSING_SJP_METHOD_NAME = "getEnclosingStaticJoinPoint";
  public static final String GET_ENCLOSING_SJP_METHOD_SIGNATURE = "()Lcom/tc/aspectwerkz/joinpoint/EnclosingStaticJoinPoint;";
  public static final String GET_SIGNATURE_METHOD_NAME = "getSignature";
  public static final String GET_SIGNATURE_METHOD_SIGNATURE = "()Lcom/tc/aspectwerkz/joinpoint/Signature;";
  public static final String GET_RTTI_METHOD_NAME = "getRtti";
  public static final String GET_RTTI_METHOD_SIGNATURE = "()Lcom/tc/aspectwerkz/joinpoint/Rtti;";
  public static final String PROCEED_METHOD_NAME = "proceed";
  public static final String PROCEED_METHOD_SIGNATURE = "()Ljava/lang/Object;";
  public static final String COPY_METHOD_NAME = "copy";
  public static final String COPY_METHOD_SIGNATURE = "()Lcom/tc/aspectwerkz/joinpoint/StaticJoinPoint;";
  public static final String ADD_META_DATA_METHOD_NAME = "addMetaData";
  public static final String ADD_META_DATA_METHOD_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Object;)V";
  public static final String MAP_CLASS_SIGNATURE = "Ljava/util/Map;";
  public static final String MAP_CLASS_NAME = "java/util/Map";
  public static final String PUT_METHOD_NAME = "put";
  public static final String PUT_METHOD_SIGNATURE = "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
  public static final String GET_META_DATA_METHOD_NAME = "getMetaData";
  public static final String GET_TARGET_METHOD_NAME = "getTarget";
  public static final String GET_THIS_METHOD_NAME = "getThis";
  public static final String GET_CALLER_METHOD_NAME = "getCaller";
  public static final String GET_CALLEE_METHOD_NAME = "getCallee";
  public static final String GET_METHOD_NAME = "getDefault";
  public static final String GET_METHOD_SIGNATURE = "(Ljava/lang/Object;)Ljava/lang/Object;";
  public static final String GET_META_DATA_METHOD_SIGNATURE = "(Ljava/lang/Object;)Ljava/lang/Object;";
  public static final String NEW_METHOD_SIGNATURE_METHOD_NAME = "newMethodSignature";
  public static final String NEW_CONSTRUCTOR_SIGNATURE_METHOD_NAME = "newConstructorSignature";
  public static final String NEW_FIELD_SIGNATURE_METHOD_NAME = "newFieldSignature";
  public static final String NEW_CATCH_CLAUSE_SIGNATURE_METHOD_NAME = "newCatchClauseSignature";
  public static final String NEW_ENCLOSING_SJP_METHOD_NAME = "newEnclosingStaticJoinPoint";
  public static final String NEW_ENCLOSING_SJP_METHOD_SIGNATURE = "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)Lcom/tc/aspectwerkz/joinpoint/EnclosingStaticJoinPoint;";
  public static final String HASH_MAP_CLASS_NAME = "java/util/HashMap";
  public static final String NO_PARAM_RETURN_VOID_SIGNATURE = "()V";
  public static final String NO_PARAM_RETURN_BOOLEAN_SIGNATURE = "()Z";
  public static final String CLASS_NOT_FOUND_EXCEPTION_CLASS_NAME = "java/lang/ClassNotFoundException";
  public static final String GET_CALLER_CLASS_METHOD_NAME = "getCallerClass";
  public static final String GET_CALLER_CLASS_METHOD_SIGNATURE = "()Ljava/lang/Class;";
  public static final String GET_CALLEE_CLASS_METHOD_NAME = "getCalleeClass";
  public static final String GET_CALLEE_CLASS_METHOD_SIGNATURE = "()Ljava/lang/Class;";
  public static final String GET_TARGET_CLASS_METHOD_NAME = "getTargetClass";
  public static final String GET_TARGET_CLASS_METHOD_SIGNATURE = "()Ljava/lang/Class;";
  public static final String GET_TYPE_METHOD_NAME = "getType";
  public static final String GET_TYPE_METHOD_SIGNATURE = "()Lcom/tc/aspectwerkz/joinpoint/management/JoinPointType;";
  public static final String RESET_METHOD_NAME = "reset";
  public static final String RUNTIME_EXCEPTION_CLASS_NAME = "java/lang/RuntimeException";
  public static final String RUNTIME_EXCEPTION_INIT_METHOD_SIGNATURE = "(Ljava/lang/String;)V";
  public static final String IS_IN_CFLOW_METOD_NAME = "isInCflow";
  public static final String IS_IN_CFLOW_METOD_SIGNATURE = "()Z";
  public static final String STATIC_JOIN_POINT_CLASS_NAME = "com/tc/aspectwerkz/joinpoint/StaticJoinPoint";
  public static final String STATIC_JOIN_POINT_JAVA_CLASS_NAME = "com.tc.aspectwerkz.joinpoint.StaticJoinPoint";
  public static final String JOIN_POINT_CLASS_NAME = "com/tc/aspectwerkz/joinpoint/JoinPoint";
  public static final String JOIN_POINT_CLASS_SIGNATURE = "Lcom/tc/aspectwerkz/joinpoint/JoinPoint;";
  public static final String JOIN_POINT_JAVA_CLASS_NAME = "com.tc.aspectwerkz.joinpoint.JoinPoint";
  public static final String NO_PARAMS_SIGNATURE = "()";

  public static final String METHOD_RTTI_IMPL_CLASS_SIGNATURE = "Lcom/tc/aspectwerkz/joinpoint/impl/MethodRttiImpl;";
  public static final String METHOD_RTTI_IMPL_CLASS_NAME = "com/tc/aspectwerkz/joinpoint/impl/MethodRttiImpl";
  public static final String METHOD_RTTI_IMPL_INIT_SIGNATURE = "(Lcom/tc/aspectwerkz/joinpoint/impl/MethodSignatureImpl;Ljava/lang/Object;Ljava/lang/Object;)V";
  public static final String CONSTRUCTOR_RTTI_IMPL_CLASS_SIGNATURE = "Lcom/tc/aspectwerkz/joinpoint/impl/ConstructorRttiImpl;";
  public static final String CONSTRUCTOR_RTTI_IMPL_CLASS_NAME = "com/tc/aspectwerkz/joinpoint/impl/ConstructorRttiImpl";
  public static final String CONSTRUCTOR_RTTI_IMPL_INIT_SIGNATURE = "(Lcom/tc/aspectwerkz/joinpoint/impl/ConstructorSignatureImpl;Ljava/lang/Object;Ljava/lang/Object;)V";
  public static final String FIELD_RTTI_IMPL_CLASS_SIGNATURE = "Lcom/tc/aspectwerkz/joinpoint/impl/FieldRttiImpl;";
  public static final String FIELD_RTTI_IMPL_CLASS_NAME = "com/tc/aspectwerkz/joinpoint/impl/FieldRttiImpl";
  public static final String FIELD_RTTI_IMPL_INIT_SIGNATURE = "(Lcom/tc/aspectwerkz/joinpoint/impl/FieldSignatureImpl;Ljava/lang/Object;Ljava/lang/Object;)V";
  public static final String HANDLER_RTTI_IMPL_CLASS_SIGNATURE = "Lcom/tc/aspectwerkz/joinpoint/impl/CatchClauseRttiImpl;";
  public static final String HANDLER_RTTI_IMPL_CLASS_NAME = "com/tc/aspectwerkz/joinpoint/impl/CatchClauseRttiImpl";
  public static final String HANDLER_RTTI_IMPL_INIT_SIGNATURE = "(Lcom/tc/aspectwerkz/joinpoint/impl/CatchClauseSignatureImpl;Ljava/lang/Object;Ljava/lang/Object;)V";
  public static final String SET_PARAMETER_VALUES_METHOD_NAME = "setParameterValues";
  public static final String SET_PARAMETER_VALUES_METHOD_SIGNATURE = "([Ljava/lang/Object;)V";
  public static final String SET_PARAMETER_VALUE_METHOD_NAME = "setParameterValue";
  public static final String SET_PARAMETER_VALUE_METHOD_SIGNATURE = "(Ljava/lang/Object;)V";
  public static final String SET_FIELD_VALUE_METHOD_NAME = "setFieldValue";
  public static final String SET_FIELD_VALUE_METHOD_SIGNATURE = "(Ljava/lang/Object;)V";
  public static final String SET_RETURN_VALUE_METHOD_NAME = "setReturnValue";
  public static final String SET_RETURN_VALUE_METHOD_SIGNATURE = "(Ljava/lang/Object;)V";
  public static final String STATICINITIALIZATION_RTTI_IMPL_CLASS_NAME = "com/tc/aspectwerkz/joinpoint/impl/StaticInitializationRttiImpl";
  public static final String STATICINITIALIZATION_RTTI_IMPL_INIT_SIGNATURE = "(Lcom/tc/aspectwerkz/joinpoint/impl/StaticInitializerSignatureImpl;)V";

  public static final String HAS_INSTANCE_LEVEL_ASPECT_INTERFACE_NAME = "com/tc/aspectwerkz/aspect/management/HasInstanceLevelAspect";
  public static final String INSTANCE_LEVEL_ASPECT_MAP_FIELD_NAME = SYNTHETIC_MEMBER_PREFIX + "instanceLevelAspects";
  public static final String INSTANCE_LEVEL_ASPECT_MAP_FIELD_SIGNATURE = "Ljava/util/Map;";
  public static final String INSTANCE_LEVEL_GETASPECT_METHOD_NAME = WRAPPER_METHOD_PREFIX + "getAspect";
  public static final String INSTANCE_LEVEL_GETASPECT_METHOD_SIGNATURE = "(Ljava/lang/Class;)Ljava/lang/Object;";
  public static final String INSTANCE_LEVEL_HASASPECT_METHOD_NAME = WRAPPER_METHOD_PREFIX + "hasAspect";
  public static final String INSTANCE_LEVEL_HASASPECT_METHOD_SIGNATURE = "(Ljava/lang/Class;)Z";
  public static final String INSTANCE_LEVEL_BINDASPECT_METHOD_NAME = WRAPPER_METHOD_PREFIX + "bindAspect";
  public static final String INSTANCE_LEVEL_BINDASPECT_METHOD_SIGNATURE = "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/Object;";


  public static final String ADVISABLE_CLASS_JAVA_NAME = "com.tc.aspectwerkz.intercept.Advisable";
  public static final String ADVISABLE_CLASS_NAME = "com/tc/aspectwerkz/intercept/Advisable";
  public static final String INTERCEPT_INVOKE_METHOD_NAME = "invoke";
  public static final String AROUND_ADVICE_INVOKE_METHOD_SIGNATURE = "(Lcom/tc/aspectwerkz/joinpoint/JoinPoint;)Ljava/lang/Object;";
  public static final String BEFORE_ADVICE_INVOKE_METHOD_SIGNATURE = "(Lcom/tc/aspectwerkz/joinpoint/JoinPoint;)V";
  public static final String AFTER_ADVICE_INVOKE_METHOD_SIGNATURE = "(Lcom/tc/aspectwerkz/joinpoint/JoinPoint;)V";
  public static final String AFTER_RETURNING_ADVICE_INVOKE_METHOD_SIGNATURE = "(Lcom/tc/aspectwerkz/joinpoint/JoinPoint;Ljava/lang/Object;)V";
  public static final String AFTER_THROWING_ADVICE_INVOKE_METHOD_SIGNATURE = "(Lcom/tc/aspectwerkz/joinpoint/JoinPoint;Ljava/lang/Throwable;)V";
  public static final String AROUND_ADVICE_CLASS_NAME = "com/tc/aspectwerkz/intercept/AroundAdvice";
  public static final String BEFORE_ADVICE_CLASS_NAME = "com/tc/aspectwerkz/intercept/BeforeAdvice";
  public static final String AFTER_ADVICE_CLASS_NAME = "com/tc/aspectwerkz/intercept/AfterAdvice";
  public static final String AFTER_RETURNING_ADVICE_CLASS_NAME = "com/tc/aspectwerkz/intercept/AfterReturningAdvice";
  public static final String AFTER_THROWING_ADVICE_CLASS_NAME = "com/tc/aspectwerkz/intercept/AfterThrowingAdvice";
  public static final String AROUND_ADVICE_ARRAY_CLASS_SIGNATURE = "[Lcom/tc/aspectwerkz/intercept/AroundAdvice;";
  public static final String BEFORE_ADVICE_ARRAY_CLASS_SIGNATURE = "[Lcom/tc/aspectwerkz/intercept/BeforeAdvice;";
  public static final String AFTER_ADVICE_ARRAY_CLASS_SIGNATURE = "[Lcom/tc/aspectwerkz/intercept/AfterAdvice;";
  public static final String AFTER_RETURNING_ADVICE_ARRAY_CLASS_SIGNATURE = "[Lcom/tc/aspectwerkz/intercept/AfterReturningAdvice;";
  public static final String AFTER_THROWING_ADVICE_ARRAY_CLASS_SIGNATURE = "[Lcom/tc/aspectwerkz/intercept/AfterThrowingAdvice;";
  public static final String GET_AROUND_ADVICE_METHOD_NAME = "aw$getAroundAdvice";
  public static final String GET_AROUND_ADVICE_METHOD_SIGNATURE = "(I)[Lcom/tc/aspectwerkz/intercept/AroundAdvice;";
  public static final String GET_BEFORE_ADVICE_METHOD_NAME = "aw$getBeforeAdvice";
  public static final String GET_BEFORE_ADVICE_METHOD_SIGNATURE = "(I)[Lcom/tc/aspectwerkz/intercept/BeforeAdvice;";
  public static final String GET_AFTER_ADVICE_METHOD_NAME = "aw$getAfterAdvice";
  public static final String GET_AFTER_ADVICE_METHOD_SIGNATURE = "(I)[Lcom/tc/aspectwerkz/intercept/AfterAdvice;";
  public static final String GET_AFTER_RETURNING_ADVICE_METHOD_NAME = "aw$getAfterReturningAdvice";
  public static final String GET_AFTER_RETURNING_ADVICE_METHOD_SIGNATURE = "(I)[Lcom/tc/aspectwerkz/intercept/AfterReturningAdvice;";
  public static final String GET_AFTER_THROWING_ADVICE_METHOD_NAME = "aw$getAfterThrowingAdvice";
  public static final String GET_AFTER_THROWING_ADVICE_METHOD_SIGNATURE = "(I)[Lcom/tc/aspectwerkz/intercept/AfterThrowingAdvice;";

  public static final int MODIFIER_INVOKEINTERFACE = 0x10000000;
  public static final int INDEX_NOTAVAILABLE = -1;


  public static final String STATICINITIALIZATION_SIGNATURE_IMPL_CLASS_NAME = "com/tc/aspectwerkz/joinpoint/impl/StaticInitializerSignatureImpl";
  public static final String STATICINITIALIZATION_SIGNATURE_IMPL_CLASS_SIGNATURE = "Lcom/tc/aspectwerkz/joinpoint/impl/StaticInitializerSignatureImpl;";
  public static final String NEW_STATICINITIALIZATION_SIGNATURE_METHOD_SIGNATURE = "(Ljava/lang/Class;)Lcom/tc/aspectwerkz/joinpoint/impl/StaticInitializerSignatureImpl;";
  public static final String NEW_STATICINITIALIZATION_SIGNATURE_METHOD_NAME = "newStaticInitializationSignature";

  public static final String TARGET_CLASS_FIELD_NAME_IN_JP = "TARGET_CLASS";
  public static final String THIS_CLASS_FIELD_NAME_IN_JP = "THIS_CLASS";

  public static final String FACTORY_CLASS_FIELD_NAME = "FACTORY_CLASS";
  public static final String FACTORY_CONTAINER_FIELD_NAME = "CONTAINER";
  public static final String FACTORY_SINGLE_ASPECT_FIELD_NAME = "ASPECT";
  public static final String FACTORY_ASPECTS_FIELD_NAME = "ASPECTS";
  public static final String FACTORY_PARAMS_FIELD_NAME = "PARAMS";
  public static final String FACTORY_ASPECTOF_METHOD_NAME = "aspectOf";
  public static final String FACTORY_HASASPECT_METHOD_NAME = "hasAspect";
  public static final String FACTORY_HASASPECT_PEROBJECT_METHOD_SIGNATURE = "(Ljava/lang/Object;)Z";
  public static final String NO_ASPECT_BOUND_EXCEPTION_CLASS_NAME = "com/tc/aspectwerkz/aspect/management/NoAspectBoundException";
  public static final String ASPECT_CONTAINER_CLASS_NAME = "com/tc/aspectwerkz/aspect/AspectContainer";
  public static final String ASPECT_CONTAINER_CLASS_SIGNATURE = "Lcom/tc/aspectwerkz/aspect/AspectContainer;";
  public static final String ASPECT_CONTAINER_OPTIONAL_INIT_SIGNATURE = "(Ljava/lang/Class;Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/lang/String;Ljava/util/Map;)V";
  public static final String ASPECT_CONTAINER_ASPECTOF_METHOD_NAME = "aspectOf";
  public static final String ASPECT_CONTAINER_ASPECTOF_PERJVM_METHOD_SIGNATURE = "()Ljava/lang/Object;";

}
