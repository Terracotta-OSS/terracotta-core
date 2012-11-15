/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;


/**
 * Configure and describe the custom adaption of a class
 */
public interface TransparencyClassSpec {

  public static final byte NOT_SET       = 0x00;

  public static final byte NOT_ADAPTABLE = 0x01;

  // For classes are not PORTABLE by themselves, but logical classes subclasses them.
  // We dont want them to get tc fields, TransparentAccess interfaces etc. but we do want them
  // to be instrumented for Array manipulations, clone(), wait(), notify() calls etc.
  public static final byte ADAPTABLE     = 0x02;

  public static final byte PORTABLE      = 0x03;

  /**
   * Mark method as not instrumented
   * 
   * @param methodName
   */
  public void addDoNotInstrument(String methodName);

  /**
   * Check whether method is marked as do not instrument
   * 
   * @param methodName Method name
   * @return True if do not instrument
   */
  public boolean doNotInstrument(String methodName);

  /**
   * Mark this class spec as being preinstrumented.
   * 
   * @return this
   */
  public TransparencyClassSpec markPreInstrumented();

  /**
   * Check whether this class is preinstrumented
   * 
   * @return True if preinstrumented
   */
  public boolean isPreInstrumented();

  /**
   * Mark this class spec as foreign.
   * 
   * @return this
   */
  public TransparencyClassSpec markForeign();

  /**
   * Check whether this class is not included in the bootjar by default. When a class is declared in the
   * <additional-boot-jar-classes/> section of the tc-config, then it is marked as foreign.
   */
  public boolean isForeign();

  /**
   * Examine lock definitions to find the the one that makes the method autolocked
   * 
   * @param lds Lock defs
   * @return null if no LockDefinitions exists that makes the method autolocked.
   */
  public LockDefinition getAutoLockDefinition(LockDefinition lds[]);

  /**
   * Find lock definition that makes method not autolocked
   * 
   * @param lds Lock defs
   * @return Lock def or null if none
   */
  public LockDefinition getNonAutoLockDefinition(LockDefinition lds[]);

  /**
   * Get the class name for this spec
   * 
   * @return Name
   */
  public String getClassName();

  /**
   * @return True if logical
   */
  public boolean isLogical();

  /**
   * @return True if physical
   */
  public boolean isPhysical();

  /**
   * @return True if checks should be ignored (only for special classes)
   */
  public boolean ignoreChecks();

  /**
   * Check whether a ignore rewrite of instrumented methods
   * 
   * @return True if ignoreRewrite
   */
  public boolean isIgnoreRewrite();

  /**
   * Get lock definition for locked method
   * 
   * @param access Access modifiers
   * @param lds Lock defs
   * @return Lock definition
   */
  public LockDefinition getLockMethodLockDefinition(int access, LockDefinition lds[]);

  /**
   * @return Change applicator specification
   */
  public ChangeApplicatorSpec getChangeApplicatorSpec();

  /**
   * @return Logical class being extended
   */
  public String getLogicalExtendingClassName();

  /**
   * Make this class extend a logically managed class
   * 
   * @param superClassSpec The logically managed super class
   */
  public void moveToLogical(TransparencyClassSpec superClassSpec);

  /**
   * Add logical method adapter to log calls to System.arraycopy()
   * 
   * @param name Method signature
   */
  public void addArrayCopyMethodCodeSpec(String name);

  /**
   * Add logical method adapter to disable wait/notify code
   * 
   * @param name Method signature
   */
  public void disableWaitNotifyCodeSpec(String name);

  /**
   * Add method code specification
   * 
   * @param name Method name
   * @param codeSpec Transparency spec
   */
  public void addMethodCodeSpec(String name, TransparencyCodeSpec codeSpec);

  /**
   * Set honor volatile flag
   * 
   * @param b New flag value
   * @return this
   */
  public TransparencyClassSpec setHonorVolatile(boolean b);

  /**
   * Set ignore rewrite flag
   * 
   * @param b New flag value
   * @return this
   */
  public TransparencyClassSpec setIgnoreRewrite(final boolean b);

  /**
   * @return True if is honor flag set
   */
  public boolean isHonorVolatileSet();

  /**
   * @return Value of is honor flag
   */
  public boolean isHonorVolatile();

  /**
   * Set honor transient flag
   * 
   * @param b New flag value
   * @return this
   */
  public TransparencyClassSpec setHonorTransient(boolean b);

  /**
   * Set call constructor on load flag
   * 
   * @param b New value
   * @return this
   */
  public TransparencyClassSpec setCallConstructorOnLoad(boolean b);

  /**
   * Set execute script on load flag
   * 
   * @param script Script to load
   * @return this
   */
  public TransparencyClassSpec setExecuteScriptOnLoad(String script);

  /**
   * Set method to call on load
   * 
   * @param method Method name
   * @return this
   */
  public TransparencyClassSpec setCallMethodOnLoad(String method);

  /**
   * Sets whether injection should happen on load
   * 
   * @param flag true or false
   * @return this
   */
  public TransparencyClassSpec setHasOnLoadInjection(boolean flag);

  /**
   * @return True if call constructor flag is set
   */
  public boolean isCallConstructorSet();

  /**
   * @return True if should honor transient
   */
  public boolean isHonorJavaTransient();

  /**
   * @return Get value of call constructor on load flag
   */
  public boolean isCallConstructorOnLoad();

  /**
   * @return True if is honor transient flag is set
   */
  public boolean isHonorTransientSet();

  /**
   * Find code spec for method
   * 
   * @param methodName Method name
   * @param description Method signature
   * @param isAutolock True if autolocked
   * @return Transparency spec
   */
  public TransparencyCodeSpec getCodeSpec(String methodName, String description, boolean isAutolock);

  /**
   * @return True if execute script on load flag is set
   */
  public boolean isExecuteScriptOnLoadSet();

  /**
   * @return True if call method on load flag is set
   */
  public boolean isCallMethodOnLoadSet();

  /**
   * @return Get on load method to call
   */
  public String getOnLoadMethod();

  /**
   * @return Get on load execute script to execute
   */
  public String getOnLoadExecuteScript();

  /**
   * @return True of injection should occur on class load
   */
  public boolean hasOnLoadInjection();

  /**
   * @return True if should use non-default constrcutor
   */
  public boolean isUseNonDefaultConstructor();

  /**
   * Set to use non default constructor
   * 
   * @param useNonDefaultConstructor True to use non-default
   */
  public void setUseNonDefaultConstructor(boolean useNonDefaultConstructor);

  /**
   * Set instrumentation action
   * 
   * @param action Action constants defined in TransparencyClassSpecImpl
   */
  public void setInstrumentationAction(byte action);

  /**
   * Get instrumentation action
   * 
   * @return Action code ADAPTABLE, etc
   */
  public byte getInstrumentationAction();

  /**
   * Get method to call prior to create
   * 
   * @return Method name
   */
  public String getPreCreateMethod();

  /**
   * Get method to call post-create
   * <p>
   * This facility is for situations in which a method needs to be invoked when an object moved from non-shared to
   * shared (but not until the end of the current traverser that caused the change). The method could be an instrumented
   * method. For instance, for ConcurrentHashMap, we need to re-hash the objects already in the map because the hashing
   * algorithm is different when a ConcurrentHashMap is shared. The rehash method is an instrumented method. This should
   * be executed only once.
   * 
   * @return Method name
   */
  public String getPostCreateMethod();

  /**
   * Set method to call pre-create
   * 
   * @param preCreateMethod Method name
   */
  public void setPreCreateMethod(String preCreateMethod);

  /**
   * Set method to call post-create
   * 
   * @param postCreateMethod Method name
   */
  public void setPostCreateMethod(String postCreateMethod);

  /**
   * @return Get name of change applicator class
   */
  public String getChangeApplicatorClassName();

  /**
   * Get spec for super class
   * 
   * @param superName Super class name
   * @return Class spec for super class
   */
  public TransparencyClassSpec getClassSpec(String superName);

  /**
   * The supplied spec will be returned if there exists no specific code spec for particular methods
   */
  public void setDefaultCodeSpec(TransparencyCodeSpec codeSpec);

  /**
   * Set the change applicator spec for this class spec
   */
  public void setChangeApplicatorSpec(ChangeApplicatorSpec changeApplicatorSpec);


}
