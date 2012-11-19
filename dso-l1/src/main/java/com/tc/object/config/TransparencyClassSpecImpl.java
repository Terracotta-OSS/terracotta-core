/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import com.tc.object.config.schema.IncludeOnLoad;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Describe the Custom adaption of a class
 */
public class TransparencyClassSpecImpl implements TransparencyClassSpec {

  private static final String                     HONOR_TRANSIENT_KEY       = "honor-transient";
  private static final String                     HONOR_VOLATILE_KEY        = "honor-volatile";
  private static final String                     IGNORE_REWRITE_KEY        = "ignore_rewrite";

  private final DSOClientConfigHelper             configuration;
  private final String                            className;
  private final Map<String, Boolean>              flags                     = new HashMap<String, Boolean>();
  private final Map<String, TransparencyCodeSpec> codeSpecs                 = new HashMap<String, TransparencyCodeSpec>();
  private final Set<String>                       nonInstrumentedMethods    = Collections
                                                                                .synchronizedSet(new HashSet<String>());
  private String                                  changeApplicatorClassName;
  private ChangeApplicatorSpec                    changeApplicatorSpec;
  private boolean                                 isLogical;
  private boolean                                 onLoadInjection;
  private final IncludeOnLoad                     onLoad                    = new IncludeOnLoad();
  private boolean                                 preInstrumented;
  private boolean                                 foreign;

  private boolean                                 useNonDefaultConstructor  = false;

  private byte                                    instrumentationAction     = NOT_SET;

  private String                                  postCreateMethod          = null;
  private String                                  preCreateMethod           = null;
  private String                                  logicalExtendingClassName = null;
  private TransparencyCodeSpec                    defaultCodeSpec           = null;

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

  @Override
  public TransparencyClassSpec getClassSpec(final String clazzName) {
    String name = clazzName.replace('/', '.');
    return configuration.getSpec(name);
  }

  @Override
  public void addDoNotInstrument(final String methodName) {
    nonInstrumentedMethods.add(methodName);
  }

  @Override
  public boolean doNotInstrument(final String methodName) {
    return nonInstrumentedMethods.contains(methodName);
  }

  @Override
  public TransparencyClassSpec markPreInstrumented() {
    preInstrumented = true;
    return this;
  }

  @Override
  public TransparencyClassSpec markForeign() {
    foreign = true;
    return this;
  }

  @Override
  public boolean isForeign() {
    return foreign;
  }

  @Override
  public boolean isPreInstrumented() {
    return preInstrumented;
  }

  /**
   * returns null if no LockDefinitions exists that makes the method autolocked.
   */
  @Override
  public LockDefinition getAutoLockDefinition(final LockDefinition lds[]) {
    if (lds == null) return null;
    for (LockDefinition ld : lds) {
      if (ld.isAutolock()) { return ld; }
    }
    return null;
  }

  @Override
  public LockDefinition getNonAutoLockDefinition(final LockDefinition lds[]) {
    if (lds == null) return null;
    for (int i = 0; i < lds.length; i++) {
      if (!lds[i].isAutolock()) { return lds[i]; }
    }
    return null;
  }

  @Override
  public String getClassName() {
    return className;
  }

  @Override
  public boolean isLogical() {
    return isLogical;
  }

  @Override
  public boolean isPhysical() {
    return !isLogical;
  }

  @Override
  public boolean ignoreChecks() {
    return TransparencyClassSpecUtil.ignoreChecks(className);
  }

  /**
   * returns null if no LockDefinitions exists that makes the method locked.
   */
  @Override
  public LockDefinition getLockMethodLockDefinition(final int access, final LockDefinition lds[]) {
    if (lds == null) return null;
    for (int i = 0; i < lds.length; i++) {
      if ((lds[i].isAutolock() && Modifier.isSynchronized(access) && !Modifier.isStatic(access))
          || !lds[i].isAutolock()) { return lds[i]; }
    }
    return null;
  }

  @Override
  public ChangeApplicatorSpec getChangeApplicatorSpec() {
    return changeApplicatorSpec;
  }

  @Override
  public String getLogicalExtendingClassName() {
    return this.logicalExtendingClassName;
  }

  @Override
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

  @Override
  public void setChangeApplicatorSpec(ChangeApplicatorSpec changeApplicatorSpec) {
    this.changeApplicatorSpec = changeApplicatorSpec;
  }

  @Override
  public void addArrayCopyMethodCodeSpec(final String name) {
    TransparencyCodeSpec codeSpec = new TransparencyCodeSpecImpl();
    codeSpec.setArraycopyInstrumentationReq(true);
    codeSpec.setArrayOperatorInstrumentationReq(true);
    codeSpecs.put(name, codeSpec);
  }

  @Override
  public void disableWaitNotifyCodeSpec(final String name) {
    TransparencyCodeSpec codeSpec = TransparencyCodeSpecImpl.getDefaultPhysicalCodeSpec();
    codeSpec.setWaitNotifyInstrumentationReq(false);
    codeSpecs.put(name, codeSpec);
  }

  @Override
  public void addMethodCodeSpec(final String name, final TransparencyCodeSpec codeSpec) {
    codeSpecs.put(name, codeSpec);
  }

  @Override
  public TransparencyClassSpec setHonorVolatile(final boolean b) {
    flags.put(HONOR_VOLATILE_KEY, b);
    return this;
  }

  @Override
  public boolean isHonorVolatileSet() {
    return flags.containsKey(HONOR_VOLATILE_KEY);
  }

  @Override
  public boolean isHonorVolatile() {
    Boolean flag = flags.get(HONOR_VOLATILE_KEY);
    if (flag == null) return false;
    return flag;
  }

  @Override
  public TransparencyClassSpec setHonorTransient(final boolean b) {
    flags.put(HONOR_TRANSIENT_KEY, b);
    return this;
  }

  @Override
  public boolean isIgnoreRewrite() {
    Boolean flag = flags.get(IGNORE_REWRITE_KEY);
    if (flag == null) return false;
    return flag;
  }

  @Override
  public TransparencyClassSpec setIgnoreRewrite(final boolean b) {
    flags.put(IGNORE_REWRITE_KEY, b);
    return this;
  }

  @Override
  public TransparencyClassSpec setCallConstructorOnLoad(final boolean b) {
    onLoad.setToCallConstructorOnLoad(b);
    return this;
  }

  @Override
  public TransparencyClassSpec setExecuteScriptOnLoad(final String script) {
    onLoad.setExecuteScriptOnLoad(script);
    return this;
  }

  @Override
  public TransparencyClassSpec setCallMethodOnLoad(final String method) {
    onLoad.setMethodCallOnLoad(method);
    return this;
  }

  private boolean basicIsHonorJavaTransient() {
    return flags.get(HONOR_TRANSIENT_KEY);
  }

  @Override
  public boolean isCallConstructorSet() {
    return onLoad.isCallConstructorOnLoadType();
  }

  @Override
  public boolean isHonorJavaTransient() {
    return basicIsHonorJavaTransient();
  }

  @Override
  public boolean isCallConstructorOnLoad() {
    return onLoad.isCallConstructorOnLoad();
  }

  @Override
  public boolean isHonorTransientSet() {
    return flags.containsKey(HONOR_TRANSIENT_KEY);
  }

  @Override
  public TransparencyCodeSpec getCodeSpec(final String methodName, final String description, final boolean isAutolock) {
    TransparencyCodeSpec spec = codeSpecs.get(methodName + description);
    if (spec != null) { return spec; }
    if (defaultCodeSpec != null) { return defaultCodeSpec; }
    return TransparencyCodeSpecImpl.getDefaultCodeSpec(className, isLogical, isAutolock);
  }

  @Override
  public boolean isExecuteScriptOnLoadSet() {
    return onLoad.isExecuteScriptOnLoadType();
  }

  @Override
  public boolean isCallMethodOnLoadSet() {
    return onLoad.isCallMethodOnLoadType();
  }

  @Override
  public String getOnLoadMethod() {
    return onLoad.getMethod();
  }

  @Override
  public String getOnLoadExecuteScript() {
    return onLoad.getExecuteScript();
  }

  @Override
  public boolean isUseNonDefaultConstructor() {
    return this.useNonDefaultConstructor;
  }

  @Override
  public void setUseNonDefaultConstructor(final boolean useNonDefaultConstructor) {
    this.useNonDefaultConstructor = useNonDefaultConstructor;
  }

  @Override
  public void setInstrumentationAction(final byte action) {
    this.instrumentationAction = action;
  }

  @Override
  public byte getInstrumentationAction() {
    return this.instrumentationAction;
  }

  @Override
  public String getPreCreateMethod() {
    return preCreateMethod;
  }

  @Override
  public String getPostCreateMethod() {
    return postCreateMethod;
  }

  @Override
  public void setPreCreateMethod(final String preCreateMethod) {
    this.preCreateMethod = preCreateMethod;
  }

  @Override
  public void setPostCreateMethod(final String postCreateMethod) {
    this.postCreateMethod = postCreateMethod;
  }

  @Override
  public String getChangeApplicatorClassName() {
    return this.changeApplicatorClassName;
  }

  @Override
  public void setDefaultCodeSpec(final TransparencyCodeSpec codeSpec) {
    this.defaultCodeSpec = codeSpec;
  }

  @Override
  public boolean hasOnLoadInjection() {
    return onLoadInjection;
  }

  @Override
  public TransparencyClassSpec setHasOnLoadInjection(final boolean flag) {
    this.onLoadInjection = flag;
    return this;
  }
}
