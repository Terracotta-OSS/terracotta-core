/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import com.tc.asm.ClassVisitor;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;

import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.bytecode.ManagerHelper;
import com.tc.object.bytecode.MethodAdapter;
import com.tc.object.bytecode.MethodCreator;
import com.tc.object.logging.InstrumentationLogger;

public interface TransparencyClassSpec {

  public boolean hasPhysicallyPortableSpecs(ClassInfo classInfo);

  public TransparencyClassSpec addRoot(String variableName, String rootName);

  public TransparencyClassSpec addRoot(String variableName, String rootName, boolean dsoFinal);

  public void addDoNotInstrument(String methodName);

  public boolean doNotInstrument(String methodName);

  public TransparencyClassSpec markPreInstrumented();

  public boolean isPreInstrumented();

  public LockDefinition[] lockDefinitionsFor(MemberInfo memberInfo);

  public LockDefinition autoLockDefinitionFor(MethodInfo methodInfo);

  public LockDefinition getAutoLockDefinition(LockDefinition lds[]);

  public LockDefinition getNonAutoLockDefinition(LockDefinition lds[]);

  public TransparencyClassSpec addSupportMethodCreator(MethodCreator creator);

  public TransparencyClassSpec addDistributedMethodCall(String methodName, String description, boolean runOnAllNodes);

  public TransparencyClassSpec addTransient(String variableName);

  public TransparencyClassSpec addMethodAdapter(String method, MethodAdapter adapter);

  public String getClassName();

  public void createClassSupportMethods(ClassVisitor classVisitor);

  public boolean isLogical();

  public boolean isPhysical();

  public boolean ignoreChecks();

  public boolean isRootInThisClass(FieldInfo fieldInfo);

  public boolean isRoot(FieldInfo fieldInfo);

  public boolean isRootDSOFinal(FieldInfo fieldInfo);

  public boolean isTransient(int access, ClassInfo classInfo, String fieldName);

  public boolean isVolatile(int access, ClassInfo classInfo, String fieldName);

  public String rootNameFor(FieldInfo fieldInfo);

  public boolean isLockMethod(MemberInfo memberInfo);

  public LockDefinition getLockMethodLockDefinition(int access, LockDefinition lds[]);

  public boolean hasCustomMethodAdapter(MemberInfo memberInfo);

  public MethodAdapter customMethodAdapterFor(ManagerHelper managerHelper, int access, String methodName,
                                              String origMethodName, String description, String signature,
                                              String[] exceptions, InstrumentationLogger logger, MemberInfo memberInfo);

  public ChangeApplicatorSpec getChangeApplicatorSpec();

  public String getLogicalExtendingClassName();

  public void moveToLogical(TransparencyClassSpec superClassSpec);

  public void addAlwaysLogSpec(String name);

  public void addIfTrueLogSpec(String name);

  public void addSetIteratorWrapperSpec(String name);

  public void addViewSetWrapperSpec(String name);

  public void addEntrySetWrapperSpec(String name);

  public void addKeySetWrapperSpec(String name);

  public void addValuesWrapperSpec(String name);

  public void addHashMapPutLogSpec(String name);

  public void addHashtablePutLogSpec(String name);

  public void addTHashMapPutLogSpec(String name);

  public void addTHashSetAddLogSpec(String name);

  public void addTHashRemoveAtLogSpec(String name);

  public void addTHashSetRemoveAtLogSpec(String name);

  public void addHashtableClearLogSpec(String name);

  public void addHashtableRemoveLogSpec(String name);

  public void addHashMapRemoveLogSpec(String name);

  public void addListRemoveLogSpec(String name);

  public void addArrayCopyMethodCodeSpec(String name);

  public void disableWaitNotifyCodeSpec(String name);

  public void addDateMethodLogSpec(String name);

  public void addDateMethodLogSpec(String name, int methodSpec);

  public void addMethodCodeSpec(String name, TransparencyCodeSpec codeSpec);

  public TransparencyClassSpec setHonorVolatile(boolean b);

  public boolean isHonorVolatileSet();

  public boolean isHonorVolatile();

  public TransparencyClassSpec setHonorTransient(boolean b);

  public TransparencyClassSpec setCallConstructorOnLoad(boolean b);

  public TransparencyClassSpec setExecuteScriptOnLoad(String script);

  public TransparencyClassSpec setCallMethodOnLoad(String method);

  public boolean isCallConstructorSet();

  public boolean isHonorJavaTransient();

  public boolean isCallConstructorOnLoad();

  public boolean isHonorTransientSet();

  public TransparencyCodeSpec getCodeSpec(String methodName, String description, boolean isAutolock);

  public boolean isExecuteScriptOnLoadSet();

  public boolean isCallMethodOnLoadSet();

  public String getOnLoadMethod();

  public String getOnLoadExecuteScript();

  public boolean isUseNonDefaultConstructor();

  public void setUseNonDefaultConstructor(boolean useNonDefaultConstructor);

  public void generateNonStaticTCFields(boolean b);

  public boolean generateNonStaticTCFields();

  public void setInstrumentationAction(byte action);

  public byte getInstrumentationAction();

  public boolean isHonorJDKSubVersionSpecific();

  public void setHonorJDKSubVersionSpecific(boolean honorJDKSubVersionSpecific);

  public String getPreCreateMethod();

  public String getPostCreateMethod();

  public void setPreCreateMethod(String preCreateMethod);

  public void setPostCreateMethod(String postCreateMethod);

  public void setCustomClassAdapter(ClassAdapterFactory customClassAdapter);

  public ClassAdapterFactory getCustomClassAdapter();

  public String getChangeApplicatorClassName();

  public TransparencyClassSpec getClassSpec(String superName);

}
