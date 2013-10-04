/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;


/**
 * Configure and describe the custom adaption of a class
 */
public interface TransparencyClassSpec {

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
   * Sets whether injection should happen on load
   * 
   * @param flag true or false
   * @return this
   */
  public TransparencyClassSpec setHasOnLoadInjection(boolean flag);

  /**
   * @return True if should honor transient
   */
  public boolean isHonorJavaTransient();

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
