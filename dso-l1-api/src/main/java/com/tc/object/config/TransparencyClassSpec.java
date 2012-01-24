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
import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.bytecode.MethodAdapter;
import com.tc.object.bytecode.MethodCreator;
import com.tc.object.logging.InstrumentationLogger;

import java.util.Collection;
import java.util.List;

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
   * @param classInfo Class information
   * @return True if physically instrumented and portable
   */
  public boolean hasPhysicallyPortableSpecs(ClassInfo classInfo);

  /**
   * Add root field
   * 
   * @param variableName Field name
   * @param rootName Root name
   * @return this
   */
  public TransparencyClassSpec addRoot(String variableName, String rootName);

  /**
   * Add root field
   * 
   * @param variableName Field name
   * @param rootName Root name
   * @param dsoFinal True if final
   * @return this
   */
  public TransparencyClassSpec addRoot(String variableName, String rootName, boolean dsoFinal);

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
   * Get lock definitions for member
   * 
   * @param memberInfo Member
   * @return Locks
   */
  public LockDefinition[] lockDefinitionsFor(MemberInfo memberInfo);

  /**
   * Get auto lock definition for member
   * 
   * @param memberInfo Member
   * @return Auto lock
   */
  public LockDefinition autoLockDefinitionFor(MethodInfo methodInfo);

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
   * Add support method creator
   * 
   * @param creator Creator
   * @return this
   */
  public TransparencyClassSpec addSupportMethodCreator(MethodCreator creator);

  /**
   * Add distributed method call
   * 
   * @param methodName Method
   * @param description Method signature
   * @param runOnAllNodes True to run on all nodes, false for local
   * @return this
   */
  public TransparencyClassSpec addDistributedMethodCall(String methodName, String description, boolean runOnAllNodes);

  /**
   * Add a transient field
   * 
   * @param variableName Field name
   * @return this
   */
  public TransparencyClassSpec addTransient(String variableName);

  /**
   * Add method adapter
   * 
   * @param method Method name
   * @param adapter The adapter
   * @return this
   */
  public TransparencyClassSpec addMethodAdapter(String method, MethodAdapter adapter);

  /**
   * Get the class name for this spec
   * 
   * @return Name
   */
  public String getClassName();

  /**
   * Call support method creators and add to the class via the visitor
   * 
   * @param classVisitor Class visitor
   */
  public void createClassSupportMethods(ClassVisitor classVisitor);

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
   * Check whether a field is a root in this class
   * 
   * @param fieldInfo Field
   * @return True if root
   */
  public boolean isRootInThisClass(FieldInfo fieldInfo);

  /**
   * Check whether a field is a root in this class
   * 
   * @param fieldInfo Field
   * @return True if root
   */
  public boolean isRoot(FieldInfo fieldInfo);

  /**
   * Check whether a field is a DSO final root
   * 
   * @param fieldInfo Field
   * @return True if DSO final root
   */
  public boolean isRootDSOFinal(FieldInfo fieldInfo);

  /**
   * Check whether a field is injected by DSO.
   * 
   * @param fieldName Field name
   * @return {@code true} when the field is injected; or {@code false} otherwise
   */
  public boolean isInjectedField(String fieldName);

  /**
   * Check whether a field is transient
   * 
   * @param access Access modifiers
   * @param classInfo Class info
   * @param fieldName Field name
   * @return True if transient
   */
  public boolean isTransient(int access, ClassInfo classInfo, String fieldName);

  /**
   * Check whether a ignore rewrite of instrumented methods
   * 
   * @return True if ignoreRewrite
   */
  public boolean isIgnoreRewrite();

  /**
   * Check whether a field is volatile
   * 
   * @param access Access modifiers
   * @param classInfo Class info
   * @param fieldName Field name
   * @return True if volatile
   */
  public boolean isVolatile(int access, ClassInfo classInfo, String fieldName);

  /**
   * @param fieldInfo Field
   * @return Root name for field
   */
  public String rootNameFor(FieldInfo fieldInfo);

  /**
   * Check whether this method is a locked method
   * 
   * @param memberInfo Method
   * @return True if locked
   */
  public boolean isLockMethod(MemberInfo memberInfo);

  /**
   * Get lock definition for locked method
   * 
   * @param access Access modifiers
   * @param lds Lock defs
   * @return Lock definition
   */
  public LockDefinition getLockMethodLockDefinition(int access, LockDefinition lds[]);

  /**
   * Check if has custom method adapter
   * 
   * @param memberInfo Method
   * @return True if has custom adapter
   */
  public boolean hasCustomMethodAdapter(MemberInfo memberInfo);

  /**
   * Get custom method adapter
   * 
   * @param access Access modifiers
   * @param methodName Method name
   * @param origMethodName Original method name
   * @param description Method description
   * @param signature Method signature
   * @param exceptions Exceptions thrown
   * @param logger Logger
   * @param memberInfo Method
   * @return Custom adapter
   */
  public MethodAdapter customMethodAdapterFor(int access, String methodName, String origMethodName, String description,
                                              String signature, String[] exceptions, InstrumentationLogger logger,
                                              MemberInfo memberInfo);

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
   * Add logical method adapter to always log access to method
   * 
   * @param name Method signature
   */
  public void addAlwaysLogSpec(String name);

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
   * Add a custom class adapter factory
   * 
   * @param customClassAdapter Custom factory
   * @deprecated see {@link #addCustomClassAdapter(ClassAdapterFactory)}
   * @see #addCustomClassAdapter(ClassAdapterFactory)
   */
  @Deprecated
  public void setCustomClassAdapter(ClassAdapterFactory customClassAdapter);

  /**
   * Add a custom class adapter factory. They will later be processed according to their order of registration and
   * delegate control to the earlier ones through the standard ASM visitor pattern. This means that any instrumentation
   * that's done in the first class adapter will be seen by the second class adapter, and so on.
   * 
   * @param customClassAdapter Custom factory
   */
  public void addCustomClassAdapter(ClassAdapterFactory customClassAdapter);

  /**
   * Get the custom class adapter factories in the reverse order of addition. The first class adapter factory that was
   * added will be the last one in the list.
   * 
   * @return Adapter factories
   */
  public List<ClassAdapterFactory> getCustomClassAdapters();

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

  /**
   * Add a custom class adapter factory to be executed after DSO adapters
   * <p/>
   * They will later be processed according to their order of registration and delegate control to the earlier ones
   * through the standard ASM visitor pattern. This means that any instrumentation that's done in the first class
   * adapter will be seen by the second class adapter, and so on.
   * 
   * @param customClassAdapter Custom factory
   */
  public void addAfterDSOClassAdapter(ClassAdapterFactory customClassAdapter);

  /**
   * Get the custom class adapter factories to be executed after DSO adapters. The returned list is in the reverse order
   * of addition. The first class adapter factory that was added will be the last one in the list.
   * 
   * @return Adapter factories
   */
  public Collection<ClassAdapterFactory> getAfterDSOClassAdapters();

}
