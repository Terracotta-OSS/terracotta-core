/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import com.tc.asm.ClassVisitor;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.bytecode.DateMethodAdapter;
import com.tc.object.bytecode.DistributedMethodCallAdapter;
import com.tc.object.bytecode.LogicalMethodAdapter;
import com.tc.object.bytecode.ManagerHelper;
import com.tc.object.bytecode.MethodAdapter;
import com.tc.object.bytecode.MethodCreator;
import com.tc.object.config.schema.IncludeOnLoad;
import com.tc.object.logging.InstrumentationLogger;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Describe the Custom adaption of a class
 */
public class TransparencyClassSpec implements ITransparencyClassSpec {
  
  private static final Object         HONOR_TRANSIENT_KEY        = "honor-transient";
  private static final Object         HONOR_VOLATILE_KEY         = "honor-volatile";

  public static final byte            NOT_SET                    = 0x00;
  public static final byte            NOT_ADAPTABLE              = 0x01;
  public static final byte            ADAPTABLE                  = 0x02;
  public static final byte            PORTABLE                   = 0x03;

  private final DSOClientConfigHelper configuration;
  private final String                className;
  private final List                  supportMethodCreators      = new LinkedList();
  private final Map                   methodAdapters             = new HashMap();
  private final Map                   flags                      = new HashMap();
  private final Map                   codeSpecs                  = new HashMap();
  private final Set                   nonInstrumentedMethods     = Collections.synchronizedSet(new HashSet());
  private String                      changeApplicatorClassName;
  private ChangeApplicatorSpec        changeApplicatorSpec;
  private boolean                     isLogical;
  private final IncludeOnLoad         onLoad                     = new IncludeOnLoad();
  private boolean                     preInstrumented;

  private boolean                     useNonDefaultConstructor   = false;
  private boolean                     generateNonStaticTCFields  = true;
  private boolean                     honorJDKSubVersionSpecific = false;

  private byte                        instrumentationAction      = NOT_SET;

  private String                      postCreateMethod           = null;
  private String                      preCreateMethod            = null;
  private String                      logicalExtendingClassName  = null;
  private ClassAdapterFactory         customClassAdapter         = null;

  public TransparencyClassSpec(String className, DSOClientConfigHelper configuration, String changeApplicatorClassName) {
    this.configuration = configuration;
    this.className = className;
    this.changeApplicatorClassName = changeApplicatorClassName;
    this.changeApplicatorSpec = new DSOChangeApplicatorSpec(changeApplicatorClassName);
    this.isLogical = true;
  }

  public TransparencyClassSpec(String className, DSOClientConfigHelper configuration) {
    this.className = className;
    this.configuration = configuration;
    this.isLogical = false;
    this.changeApplicatorClassName = null;
    this.changeApplicatorSpec = null;
    this.changeApplicatorSpec = null;
  }

  public ITransparencyClassSpec getClassSpec(String clazzName) {
    String name = clazzName.replace('/', '.');
    return configuration.getSpec(name);
  }

  public boolean hasPhysicallyPortableSpecs(ClassInfo classInfo) {
    String name = classInfo.getName();
    return configuration.shouldBeAdapted(classInfo) && !configuration.isLogical(name)
           && (configuration.getSpec(name) != null)
           && (configuration.getSpec(name).getInstrumentationAction() != ADAPTABLE);
  }

  public ITransparencyClassSpec addRoot(String variableName, String rootName) {
    configuration.addRoot(new Root(className, variableName, rootName), false);
    return this;
  }

  public ITransparencyClassSpec addRoot(String variableName, String rootName, boolean dsoFinal) {
    configuration.addRoot(new Root(className, variableName, rootName, dsoFinal), false);
    return this;
  }

  public void addDoNotInstrument(String methodName) {
    nonInstrumentedMethods.add(methodName);
  }

  public boolean doNotInstrument(String methodName) {
    return nonInstrumentedMethods.contains(methodName);
  }

  public ITransparencyClassSpec markPreInstrumented() {
    preInstrumented = true;
    return this;
  }

  public boolean isPreInstrumented() {
    return preInstrumented;
  }

  public synchronized ILockDefinition[] lockDefinitionsFor(MemberInfo memberInfo) {
    return configuration.lockDefinitionsFor(memberInfo);
  }

  public synchronized ILockDefinition autoLockDefinitionFor(MethodInfo methodInfo) {
    ILockDefinition[] lds = lockDefinitionsFor(methodInfo);
    for (int i = 0; i < lds.length; i++) {
      if (lds[i].isAutolock()) { return lds[i]; }
    }
    throw new AssertionError("Can't be an autolock and not have an autolock def:" //
                             + methodInfo.getName() + methodInfo.getSignature() + " className:" + className);
  }

  /**
   * returns null if no ILockDefinitions exists that makes the method autolocked.
   */
  public ILockDefinition getAutoLockDefinition(ILockDefinition lds[]) {
    if (lds == null) return null;
    for (int i = 0; i < lds.length; i++) {
      if (lds[i].isAutolock()) { return lds[i]; }
    }
    return null;
  }

  public ILockDefinition getNonAutoLockDefinition(ILockDefinition lds[]) {
    if (lds == null) return null;
    for (int i = 0; i < lds.length; i++) {
      if (!lds[i].isAutolock()) { return lds[i]; }
    }
    return null;
  }

  public ITransparencyClassSpec addSupportMethodCreator(MethodCreator creator) {
    supportMethodCreators.add(creator);
    return this;
  }

  public ITransparencyClassSpec addDistributedMethodCall(String methodName, String description, boolean runOnAllNodes) {
    if ("<init>".equals(methodName) || "<clinit>".equals(methodName)) { throw new AssertionError(
                                                                                                 "Initializers of class "
                                                                                                     + className
                                                                                                     + " cannot be participated in distrbuted method call and are ignored."); }
    StringBuffer sb = new StringBuffer("* ");
    sb.append(className);
    sb.append(".");
    sb.append(methodName);
    String arguments = ByteCodeUtil.methodDescriptionToMethodArgument(description);
    sb.append(arguments);
    final DistributedMethodSpec dms = new DistributedMethodSpec(sb.toString(), runOnAllNodes);
    configuration.addDistributedMethodCall(dms);
    return this;
  }

  public ITransparencyClassSpec addTransient(String variableName) {
    configuration.addTransient(className, variableName);
    return this;
  }

  public ITransparencyClassSpec addMethodAdapter(String method, MethodAdapter adapter) {
    methodAdapters.put(method, adapter);
    return this;
  }

  public String getClassName() {
    return className;
  }

  public void createClassSupportMethods(ClassVisitor classVisitor) {
    for (Iterator i = supportMethodCreators.iterator(); i.hasNext();) {
      MethodCreator mc = (MethodCreator) i.next();
      mc.createMethods(classVisitor);
    }
  }

  public boolean isLogical() {
    return isLogical;
  }

  public boolean isPhysical() {
    return !isLogical;
  }

  public boolean ignoreChecks() {
    return TransparencyClassSpecUtil.ignoreChecks(className);
  }

  public boolean isRootInThisClass(FieldInfo fieldInfo) {
    return configuration.isRoot(fieldInfo);
  }

  public boolean isRoot(FieldInfo fieldInfo) {
    return configuration.isRoot(fieldInfo);
  }

  public boolean isRootDSOFinal(FieldInfo fieldInfo) {
    return configuration.isRootDSOFinal(fieldInfo);
  }

  public boolean isTransient(int access, ClassInfo classInfo, String fieldName) {
    return configuration.isTransient(access, classInfo, fieldName);
  }

  public boolean isVolatile(int access, ClassInfo classInfo, String fieldName) {
    return configuration.isVolatile(access, classInfo, fieldName);
  }

  public String rootNameFor(FieldInfo fieldInfo) {
    return configuration.rootNameFor(fieldInfo);
  }

  public boolean isLockMethod(MemberInfo memberInfo) {
    return configuration.isLockMethod(memberInfo);
  }

  /**
   * returns null if no ILockDefinitions exists that makes the method locked.
   */
  public ILockDefinition getLockMethodILockDefinition(int access, ILockDefinition lds[]) {
    if (lds == null) return null;
    for (int i = 0; i < lds.length; i++) {
      if ((lds[i].isAutolock() && Modifier.isSynchronized(access) && !Modifier.isStatic(access))
          || !lds[i].isAutolock()) { return lds[i]; }
    }
    return null;
  }

  public boolean hasCustomMethodAdapter(MemberInfo memberInfo) {
    return memberInfo != null && getMethodAdapter(memberInfo) != null;
  }

  public MethodAdapter customMethodAdapterFor(ManagerHelper managerHelper, int access, String methodName,
                                              String origMethodName, String description, String signature,
                                              String[] exceptions, InstrumentationLogger logger, MemberInfo memberInfo) {
    MethodAdapter ma = getMethodAdapter(memberInfo);
    ma.initialize(managerHelper, access, className, methodName, origMethodName, description, signature, exceptions,
                  logger, memberInfo);
    return ma;
  }

  private MethodAdapter getMethodAdapter(MemberInfo memberInfo) {
    if (memberInfo == null) { return null; }
    DistributedMethodSpec dms = configuration.getDmiSpec(memberInfo);
    if (dms != null) { return new DistributedMethodCallAdapter(dms.runOnAllNodes()); }
    return (MethodAdapter) methodAdapters.get(memberInfo.getName() + memberInfo.getSignature());
  }

  public ChangeApplicatorSpec getChangeApplicatorSpec() {
    return changeApplicatorSpec;
  }

  public String getLogicalExtendingClassName() {
    return this.logicalExtendingClassName;
  }

  public void moveToLogical(ITransparencyClassSpec superClassSpec) {
    this.isLogical = true;
    String superClassLogicalExtendingClassName = superClassSpec.getLogicalExtendingClassName();
    if (superClassLogicalExtendingClassName == null) {
      superClassLogicalExtendingClassName = superClassSpec.getClassName();
    }
    this.changeApplicatorClassName = superClassSpec.getChangeApplicatorClassName();
    this.changeApplicatorSpec = new DSOChangeApplicatorSpec(superClassSpec.getChangeApplicatorClassName());
    this.logicalExtendingClassName = superClassLogicalExtendingClassName;
  }

  public void addAlwaysLogSpec(String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.ALWAYS_LOG));
  }

  public void addIfTrueLogSpec(String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.IF_TRUE_LOG));
  }

  public void addSetIteratorWrapperSpec(String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.SET_ITERATOR_WRAPPER_LOG));
  }

  public void addViewSetWrapperSpec(String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.SORTED_SET_VIEW_WRAPPER_LOG));
  }

  public void addEntrySetWrapperSpec(String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.ENTRY_SET_WRAPPER_LOG));
  }

  public void addKeySetWrapperSpec(String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.KEY_SET_WRAPPER_LOG));
  }

  public void addValuesWrapperSpec(String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.VALUES_WRAPPER_LOG));
  }

  public void addHashMapPutLogSpec(String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.HASHMAP_PUT_LOG));
  }

  public void addHashtablePutLogSpec(String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.HASHTABLE_PUT_LOG));
  }

  public void addTHashMapPutLogSpec(String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.THASHMAP_PUT_LOG));
  }

  public void addTHashSetAddLogSpec(String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.THASHSET_ADD_LOG));
  }

  public void addTHashRemoveAtLogSpec(String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.THASH_REMOVE_AT_LOG));
  }

  public void addTHashSetRemoveAtLogSpec(String name) {
    // Do nothing it's taken care of in the add method
  }

  public void addHashtableClearLogSpec(String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.HASHTABLE_CLEAR_LOG));
  }

  public void addHashtableRemoveLogSpec(String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.HASHTABLE_REMOVE_LOG));
  }

  public void addHashMapRemoveLogSpec(String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.HASHMAP_REMOVE_LOG));
  }

  public void addListRemoveLogSpec(String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.LIST_REMOVE_LOG));
  }

  public void addArrayCopyMethodCodeSpec(String name) {
    TransparencyCodeSpec codeSpec = new TransparencyCodeSpec();
    codeSpec.setArraycopyInstrumentationReq(true);
    codeSpec.setArrayOperatorInstrumentationReq(true);
    codeSpecs.put(name, codeSpec);
  }

  public void disableWaitNotifyCodeSpec(String name) {
    TransparencyCodeSpec codeSpec = TransparencyCodeSpec.getDefaultPhysicalCodeSpec();
    codeSpec.setWaitNotifyInstrumentationReq(false);
    codeSpecs.put(name, codeSpec);
  }

  public void addDateMethodLogSpec(String name) {
    methodAdapters.put(name, new DateMethodAdapter(name, MethodSpec.DATE_ADD_SET_TIME_WRAPPER_LOG));
  }

  public void addDateMethodLogSpec(String name, int methodSpec) {
    methodAdapters.put(name, new DateMethodAdapter(name, methodSpec));
  }

  public void addMethodCodeSpec(String name, ITransparencyCodeSpec codeSpec) {
    codeSpecs.put(name, codeSpec);
  }

  public ITransparencyClassSpec setHonorVolatile(boolean b) {
    flags.put(HONOR_VOLATILE_KEY, new Boolean(b));
    return this;
  }

  public boolean isHonorVolatileSet() {
    return flags.containsKey(HONOR_VOLATILE_KEY);
  }

  public boolean isHonorVolatile() {
    Object flag = flags.get(HONOR_VOLATILE_KEY);
    if (flag == null) return false;
    return ((Boolean) flag).booleanValue();
  }

  public ITransparencyClassSpec setHonorTransient(boolean b) {
    flags.put(HONOR_TRANSIENT_KEY, new Boolean(b));
    return this;
  }

  public ITransparencyClassSpec setCallConstructorOnLoad(boolean b) {
    onLoad.setToCallConstructorOnLoad(b);
    return this;
  }

  public ITransparencyClassSpec setExecuteScriptOnLoad(String script) {
    onLoad.setExecuteScriptOnLoad(script);
    return this;
  }

  public ITransparencyClassSpec setCallMethodOnLoad(String method) {
    onLoad.setMethodCallOnLoad(method);
    return this;
  }

  private boolean basicIsHonorJavaTransient() {
    return ((Boolean) flags.get(HONOR_TRANSIENT_KEY)).booleanValue();
  }

  public boolean isCallConstructorSet() {
    return onLoad.isCallConstructorOnLoadType();
  }

  public boolean isHonorJavaTransient() {
    return basicIsHonorJavaTransient();
  }

  public boolean isCallConstructorOnLoad() {
    return onLoad.isCallConstructorOnLoad();
  }

  public boolean isHonorTransientSet() {
    return flags.containsKey(HONOR_TRANSIENT_KEY);
  }

  public ITransparencyCodeSpec getCodeSpec(String methodName, String description, boolean isAutolock) {
    Object o = codeSpecs.get(methodName + description);
    if (o == null) { return (ITransparencyCodeSpec) TransparencyCodeSpec.getDefaultCodeSpec(className, isLogical,
                                                                                            isAutolock); }
    return (ITransparencyCodeSpec) o;
  }

  public boolean isExecuteScriptOnLoadSet() {
    return onLoad.isExecuteScriptOnLoadType();
  }

  public boolean isCallMethodOnLoadSet() {
    return onLoad.isCallMethodOnLoadType();
  }

  public String getOnLoadMethod() {
    return onLoad.getMethod();
  }

  public String getOnLoadExecuteScript() {
    return onLoad.getExecuteScript();
  }

  public boolean isUseNonDefaultConstructor() {
    return this.useNonDefaultConstructor;
  }

  public void setUseNonDefaultConstructor(boolean useNonDefaultConstructor) {
    this.useNonDefaultConstructor = useNonDefaultConstructor;
  }

  public void generateNonStaticTCFields(boolean b) {
    this.generateNonStaticTCFields = b;
  }

  public boolean generateNonStaticTCFields() {
    return this.generateNonStaticTCFields;
  }

  public void setInstrumentationAction(byte action) {
    this.instrumentationAction = action;
  }

  public byte getInstrumentationAction() {
    return this.instrumentationAction;
  }

  public boolean isHonorJDKSubVersionSpecific() {
    return honorJDKSubVersionSpecific;
  }

  public void setHonorJDKSubVersionSpecific(boolean honorJDKSubVersionSpecific) {
    this.honorJDKSubVersionSpecific = honorJDKSubVersionSpecific;
  }

  public String getPreCreateMethod() {
    return preCreateMethod;
  }

  public String getPostCreateMethod() {
    return postCreateMethod;
  }

  public void setPreCreateMethod(String preCreateMethod) {
    this.preCreateMethod = preCreateMethod;
  }

  public void setPostCreateMethod(String postCreateMethod) {
    this.postCreateMethod = postCreateMethod;
  }

  public void setCustomClassAdapter(ClassAdapterFactory customClassAdapter) {
    this.customClassAdapter = customClassAdapter;
  }

  public ClassAdapterFactory getCustomClassAdapter() {
    return customClassAdapter;
  }

  public String getChangeApplicatorClassName() {
    return this.changeApplicatorClassName;
  }
  
}
