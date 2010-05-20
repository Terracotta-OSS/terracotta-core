/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
import com.tc.object.bytecode.MethodAdapter;
import com.tc.object.bytecode.MethodCreator;
import com.tc.object.config.schema.IncludeOnLoad;
import com.tc.object.logging.InstrumentationLogger;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
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
public class TransparencyClassSpecImpl implements TransparencyClassSpec {

  private static final String                     HONOR_TRANSIENT_KEY        = "honor-transient";
  private static final String                     HONOR_VOLATILE_KEY         = "honor-volatile";
  private static final String                     IGNORE_REWRITE_KEY         = "ignore_rewrite";

  private final DSOClientConfigHelper             configuration;
  private final String                            className;
  private final List<MethodCreator>               supportMethodCreators      = new LinkedList<MethodCreator>();
  private final Map<String, MethodAdapter>        methodAdapters             = new HashMap<String, MethodAdapter>();
  private final List<ClassAdapterFactory>         customClassAdapters        = new ArrayList<ClassAdapterFactory>();
  private final Map<String, Boolean>              flags                      = new HashMap<String, Boolean>();
  private final Map<String, TransparencyCodeSpec> codeSpecs                  = new HashMap<String, TransparencyCodeSpec>();
  private final Set<String>                       nonInstrumentedMethods     = Collections.synchronizedSet(new HashSet<String>());
  private String                                  changeApplicatorClassName;
  private ChangeApplicatorSpec                    changeApplicatorSpec;
  private boolean                                 isLogical;
  private boolean                                 onLoadInjection;
  private final IncludeOnLoad                     onLoad                     = new IncludeOnLoad();
  private boolean                                 preInstrumented;
  private boolean                                 foreign;

  private boolean                                 useNonDefaultConstructor   = false;
  private boolean                                 honorJDKSubVersionSpecific = false;

  private byte                                    instrumentationAction      = NOT_SET;

  private String                                  postCreateMethod           = null;
  private String                                  preCreateMethod            = null;
  private String                                  logicalExtendingClassName  = null;
  private TransparencyCodeSpec                    defaultCodeSpec            = null;

  public TransparencyClassSpecImpl(final String className, final DSOClientConfigHelper configuration,
                                   final String changeApplicatorClassName) {
    this.configuration = configuration;
    this.className = className;
    this.changeApplicatorClassName = changeApplicatorClassName;
    this.changeApplicatorSpec = new DSOChangeApplicatorSpec(changeApplicatorClassName);
    this.isLogical = true;
  }

  public TransparencyClassSpecImpl(final String className, final DSOClientConfigHelper configuration) {
    this.className = className;
    this.configuration = configuration;
    this.isLogical = false;
    this.changeApplicatorClassName = null;
    this.changeApplicatorSpec = null;
    this.changeApplicatorSpec = null;
  }

  public TransparencyClassSpec getClassSpec(final String clazzName) {
    String name = clazzName.replace('/', '.');
    return configuration.getSpec(name);
  }

  public boolean hasPhysicallyPortableSpecs(final ClassInfo classInfo) {
    String name = classInfo.getName();
    return configuration.shouldBeAdapted(classInfo) && !configuration.isLogical(name)
           && (configuration.getSpec(name) != null)
           && (configuration.getSpec(name).getInstrumentationAction() != ADAPTABLE);
  }

  public TransparencyClassSpec addRoot(final String variableName, final String rootName) {
    configuration.addRoot(new Root(className, variableName, rootName), false);
    return this;
  }

  public TransparencyClassSpec addRoot(final String variableName, final String rootName, final boolean dsoFinal) {
    configuration.addRoot(new Root(className, variableName, rootName, dsoFinal), false);
    return this;
  }

  public void addDoNotInstrument(final String methodName) {
    nonInstrumentedMethods.add(methodName);
  }

  public boolean doNotInstrument(final String methodName) {
    return nonInstrumentedMethods.contains(methodName);
  }

  public TransparencyClassSpec markPreInstrumented() {
    preInstrumented = true;
    return this;
  }

  public TransparencyClassSpec markForeign() {
    foreign = true;
    return this;
  }

  public boolean isForeign() {
    return foreign;
  }

  public boolean isPreInstrumented() {
    return preInstrumented;
  }

  public synchronized LockDefinition[] lockDefinitionsFor(final MemberInfo memberInfo) {
    return configuration.lockDefinitionsFor(memberInfo);
  }

  public synchronized LockDefinition autoLockDefinitionFor(final MethodInfo methodInfo) {
    LockDefinition[] lds = lockDefinitionsFor(methodInfo);
    for (LockDefinition ld : lds) {
      if (ld.isAutolock()) { return ld; }
    }
    throw new AssertionError("Can't be an autolock and not have an autolock def:" //
                             + methodInfo.getName() + methodInfo.getSignature() + " className:" + className);
  }

  /**
   * returns null if no LockDefinitions exists that makes the method autolocked.
   */
  public LockDefinition getAutoLockDefinition(final LockDefinition lds[]) {
    if (lds == null) return null;
    for (LockDefinition ld : lds) {
      if (ld.isAutolock()) { return ld; }
    }
    return null;
  }

  public LockDefinition getNonAutoLockDefinition(final LockDefinition lds[]) {
    if (lds == null) return null;
    for (int i = 0; i < lds.length; i++) {
      if (!lds[i].isAutolock()) { return lds[i]; }
    }
    return null;
  }

  public TransparencyClassSpec addSupportMethodCreator(final MethodCreator creator) {
    supportMethodCreators.add(creator);
    return this;
  }

  public TransparencyClassSpec addDistributedMethodCall(final String methodName, final String description,
                                                        final boolean runOnAllNodes) {
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

  public TransparencyClassSpec addTransient(final String variableName) {
    configuration.addTransient(className, variableName);
    return this;
  }

  public TransparencyClassSpec addMethodAdapter(final String method, final MethodAdapter adapter) {
    methodAdapters.put(method, adapter);
    return this;
  }

  public String getClassName() {
    return className;
  }

  public void createClassSupportMethods(final ClassVisitor classVisitor) {
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

  public boolean isRootInThisClass(final FieldInfo fieldInfo) {
    return configuration.isRoot(fieldInfo);
  }

  public boolean isRoot(final FieldInfo fieldInfo) {
    return configuration.isRoot(fieldInfo);
  }

  public boolean isRootDSOFinal(final FieldInfo fieldInfo) {
    return configuration.isRootDSOFinal(fieldInfo);
  }

  public boolean isInjectedField(final String fieldName) {
    return configuration.isInjectedField(className, fieldName);
  }

  public boolean isTransient(final int access, final ClassInfo classInfo, final String fieldName) {
    return configuration.isTransient(access, classInfo, fieldName);
  }

  public boolean isVolatile(final int access, final ClassInfo classInfo, final String fieldName) {
    return configuration.isVolatile(access, classInfo, fieldName);
  }

  public String rootNameFor(final FieldInfo fieldInfo) {
    return configuration.rootNameFor(fieldInfo);
  }

  public boolean isLockMethod(final MemberInfo memberInfo) {
    return configuration.isLockMethod(memberInfo);
  }

  /**
   * returns null if no LockDefinitions exists that makes the method locked.
   */
  public LockDefinition getLockMethodLockDefinition(final int access, final LockDefinition lds[]) {
    if (lds == null) return null;
    for (int i = 0; i < lds.length; i++) {
      if ((lds[i].isAutolock() && Modifier.isSynchronized(access) && !Modifier.isStatic(access))
          || !lds[i].isAutolock()) { return lds[i]; }
    }
    return null;
  }

  public boolean hasCustomMethodAdapter(final MemberInfo memberInfo) {
    return memberInfo != null && getMethodAdapter(memberInfo) != null;
  }

  public MethodAdapter customMethodAdapterFor(final int access, final String methodName, final String origMethodName,
                                              final String description, final String signature,
                                              final String[] exceptions, final InstrumentationLogger logger,
                                              final MemberInfo memberInfo) {
    MethodAdapter ma = getMethodAdapter(memberInfo);
    ma
        .initialize(access, className, methodName, origMethodName, description, signature, exceptions, logger,
                    memberInfo);
    return ma;
  }

  private MethodAdapter getMethodAdapter(final MemberInfo memberInfo) {
    if (memberInfo == null) { return null; }
    DistributedMethodSpec dms = configuration.getDmiSpec(memberInfo);
    if (dms != null) { return new DistributedMethodCallAdapter(dms.runOnAllNodes()); }
    return methodAdapters.get(memberInfo.getName() + memberInfo.getSignature());
  }

  public ChangeApplicatorSpec getChangeApplicatorSpec() {
    return changeApplicatorSpec;
  }

  public String getLogicalExtendingClassName() {
    return this.logicalExtendingClassName;
  }

  public void moveToLogical(final TransparencyClassSpec superClassSpec) {
    this.isLogical = true;
    String superClassLogicalExtendingClassName = superClassSpec.getLogicalExtendingClassName();
    if (superClassLogicalExtendingClassName == null) {
      superClassLogicalExtendingClassName = superClassSpec.getClassName();
    }
    this.changeApplicatorClassName = superClassSpec.getChangeApplicatorClassName();
    this.changeApplicatorSpec = new DSOChangeApplicatorSpec(superClassSpec.getChangeApplicatorClassName());
    this.logicalExtendingClassName = superClassLogicalExtendingClassName;
  }

  public void setChangeApplicatorSpec(ChangeApplicatorSpec changeApplicatorSpec) {
    this.changeApplicatorSpec = changeApplicatorSpec;
  }

  public void addAlwaysLogSpec(final String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.ALWAYS_LOG));
  }

  public void addIfTrueLogSpec(final String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.IF_TRUE_LOG));
  }

  public void addSetIteratorWrapperSpec(final String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.SET_ITERATOR_WRAPPER_LOG));
  }

  public void addViewSetWrapperSpec(final String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.SORTED_SET_VIEW_WRAPPER_LOG));
  }

  public void addEntrySetWrapperSpec(final String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.ENTRY_SET_WRAPPER_LOG));
  }

  public void addKeySetWrapperSpec(final String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.KEY_SET_WRAPPER_LOG));
  }

  public void addValuesWrapperSpec(final String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.VALUES_WRAPPER_LOG));
  }

  public void addHashMapPutLogSpec(final String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.HASHMAP_PUT_LOG));
  }

  public void addHashtablePutLogSpec(final String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.HASHTABLE_PUT_LOG));
  }

  public void addTHashMapPutLogSpec(final String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.THASHMAP_PUT_LOG));
  }

  public void addTObjectHashRemoveAtLogSpec(final String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.TOBJECTHASH_REMOVE_AT_LOG));
  }

  public void addHashtableClearLogSpec(final String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.HASHTABLE_CLEAR_LOG));
  }

  public void addHashtableRemoveLogSpec(final String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.HASHTABLE_REMOVE_LOG));
  }

  public void addHashMapRemoveLogSpec(final String name) {
    methodAdapters.put(name, new LogicalMethodAdapter(name, MethodSpec.HASHMAP_REMOVE_LOG));
  }

  public void addArrayCopyMethodCodeSpec(final String name) {
    TransparencyCodeSpec codeSpec = new TransparencyCodeSpecImpl();
    codeSpec.setArraycopyInstrumentationReq(true);
    codeSpec.setArrayOperatorInstrumentationReq(true);
    codeSpecs.put(name, codeSpec);
  }

  public void disableWaitNotifyCodeSpec(final String name) {
    TransparencyCodeSpec codeSpec = TransparencyCodeSpecImpl.getDefaultPhysicalCodeSpec();
    codeSpec.setWaitNotifyInstrumentationReq(false);
    codeSpecs.put(name, codeSpec);
  }

  public void addDateMethodLogSpec(final String name) {
    methodAdapters.put(name, new DateMethodAdapter(name, MethodSpec.DATE_ADD_SET_TIME_WRAPPER_LOG));
  }

  public void addDateMethodLogSpec(final String name, final int methodSpec) {
    methodAdapters.put(name, new DateMethodAdapter(name, methodSpec));
  }

  public void addMethodCodeSpec(final String name, final TransparencyCodeSpec codeSpec) {
    codeSpecs.put(name, codeSpec);
  }

  public TransparencyClassSpec setHonorVolatile(final boolean b) {
    flags.put(HONOR_VOLATILE_KEY, b);
    return this;
  }

  public boolean isHonorVolatileSet() {
    return flags.containsKey(HONOR_VOLATILE_KEY);
  }

  public boolean isHonorVolatile() {
    Boolean flag = flags.get(HONOR_VOLATILE_KEY);
    if (flag == null) return false;
    return flag;
  }

  public TransparencyClassSpec setHonorTransient(final boolean b) {
    flags.put(HONOR_TRANSIENT_KEY, b);
    return this;
  }

  public boolean isIgnoreRewrite() {
    Boolean flag = flags.get(IGNORE_REWRITE_KEY);
    if (flag == null) return false;
    return flag;
  }

  public TransparencyClassSpec setIgnoreRewrite(final boolean b) {
    flags.put(IGNORE_REWRITE_KEY, b);
    return this;
  }
  
  public TransparencyClassSpec setCallConstructorOnLoad(final boolean b) {
    onLoad.setToCallConstructorOnLoad(b);
    return this;
  }

  public TransparencyClassSpec setExecuteScriptOnLoad(final String script) {
    onLoad.setExecuteScriptOnLoad(script);
    return this;
  }

  public TransparencyClassSpec setCallMethodOnLoad(final String method) {
    onLoad.setMethodCallOnLoad(method);
    return this;
  }

  private boolean basicIsHonorJavaTransient() {
    return flags.get(HONOR_TRANSIENT_KEY);
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

  public TransparencyCodeSpec getCodeSpec(final String methodName, final String description, final boolean isAutolock) {
    TransparencyCodeSpec spec = codeSpecs.get(methodName + description);
    if (spec != null) { return spec; }
    if (defaultCodeSpec != null) { return defaultCodeSpec; }
    return TransparencyCodeSpecImpl.getDefaultCodeSpec(className, isLogical, isAutolock);
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

  public void setUseNonDefaultConstructor(final boolean useNonDefaultConstructor) {
    this.useNonDefaultConstructor = useNonDefaultConstructor;
  }

  public void setInstrumentationAction(final byte action) {
    this.instrumentationAction = action;
  }

  public byte getInstrumentationAction() {
    return this.instrumentationAction;
  }

  public boolean isHonorJDKSubVersionSpecific() {
    return honorJDKSubVersionSpecific;
  }

  public void setHonorJDKSubVersionSpecific(final boolean honorJDKSubVersionSpecific) {
    this.honorJDKSubVersionSpecific = honorJDKSubVersionSpecific;
  }

  public String getPreCreateMethod() {
    return preCreateMethod;
  }

  public String getPostCreateMethod() {
    return postCreateMethod;
  }

  public void setPreCreateMethod(final String preCreateMethod) {
    this.preCreateMethod = preCreateMethod;
  }

  public void setPostCreateMethod(final String postCreateMethod) {
    this.postCreateMethod = postCreateMethod;
  }

  public void setCustomClassAdapter(final ClassAdapterFactory customClassAdapter) {
    addCustomClassAdapter(customClassAdapter);
  }

  public void addCustomClassAdapter(final ClassAdapterFactory adapter) {
    this.customClassAdapters.add(0, adapter);
  }

  public List<ClassAdapterFactory> getCustomClassAdapters() {
    return customClassAdapters;
  }

  public String getChangeApplicatorClassName() {
    return this.changeApplicatorClassName;
  }

  public void setDefaultCodeSpec(final TransparencyCodeSpec codeSpec) {
    this.defaultCodeSpec = codeSpec;
  }

  public boolean hasOnLoadInjection() {
    return onLoadInjection;
  }

  public TransparencyClassSpec setHasOnLoadInjection(final boolean flag) {
    this.onLoadInjection = flag;
    return this;
  }
}
