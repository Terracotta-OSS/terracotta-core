/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.ChildBeanDefinition;

import com.tc.object.TCClass;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.NonDistributableObjectRegistry;
import com.tc.object.bytecode.hook.DSOContext;
import com.tc.object.config.DSOSpringConfigHelper;
import com.tc.object.field.TCField;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds a unique ID for all the instance the mixin is applied to. Usage: 1. Call addLocation() 1 or more times 2. call
 * registerBeanDefinitions() once 3. Call getters (is...() or get...()) as many times as you like
 * 
 * @author Eugene Kuleshov
 * @author Jonas Bon&#233;r 
 * 
 * TODO performance optimization of the access to <code>DSOSpringConfigHelper</code>
 */
public final class DistributableBeanFactoryMixin implements DistributableBeanFactory {

  private final transient Log logger              = LogFactory.getLog(getClass());

  private final String        appName;
  private final DSOContext    dsoContext;

  private List                springConfigHelpers = Collections.synchronizedList(new ArrayList());

  private Map                 beanDefinitions     = Collections.synchronizedMap(new HashMap());

  private String              id;
  private List                locations           = new ArrayList();

  private boolean             isClustered         = false;
  private Map                 clusteredBeans      = new HashMap();

  private ManagerUtilWrapper  managerUtilWrapper;

  private Set                 nonDistributables;

  public DistributableBeanFactoryMixin() {
    ApplicationHelper applicationHelper = new ApplicationHelper(getClass());
    if (applicationHelper.isDSOApplication()) {
      this.appName = applicationHelper.getAppName();
      this.dsoContext = applicationHelper.getDsoContext();
    } else {
      this.appName = null;
      this.dsoContext = null;
    }
    NonDistributableObjectRegistry nonDistributableObjectRegistry = NonDistributableObjectRegistry.getInstance();
    this.managerUtilWrapper = new ManagerUtilWrapperImpl(nonDistributableObjectRegistry);
    this.nonDistributables = nonDistributableObjectRegistry.getNondistributables();
  }

  protected DistributableBeanFactoryMixin(String appName, DSOContext dsoContext, ManagerUtilWrapper managerUtilWrapper, Set nonDistributables) {
    this.appName = appName;
    this.dsoContext = dsoContext;
    this.managerUtilWrapper = managerUtilWrapper;
    this.nonDistributables = nonDistributables;
  }

  public boolean isClustered() {
    return isClustered;
  }

  public String getAppName() {
    return appName;
  }

  public String getId() {
    return id;
  }

  public List getLocations() {
    return locations;
  }

  public List getSpringConfigHelpers() {
    return this.springConfigHelpers;
  }

  public boolean isDistributedEvent(String className) {
    for (Iterator it = this.springConfigHelpers.iterator(); it.hasNext();) {
      DSOSpringConfigHelper springConfigHelper = (DSOSpringConfigHelper) it.next();
      if (springConfigHelper.isDistributedEvent(className)) { return true; }
    }
    return false;
  }

  public boolean isDistributedScoped(String beanName) {
    AbstractBeanDefinition definition = (AbstractBeanDefinition) beanDefinitions.get(beanName);
    // method definition.isPrototype() is Spring 2.0+ 
    return definition!=null && !definition.isSingleton() && !definition.isPrototype();
  }
  
  public boolean isDistributedSingleton(String beanName) {
    AbstractBeanDefinition definition = (AbstractBeanDefinition) beanDefinitions.get(beanName);
    return definition!=null && definition.isSingleton();
  }

  public boolean isDistributedBean(String beanName) {
    for (Iterator it = this.springConfigHelpers.iterator(); it.hasNext();) {
      DSOSpringConfigHelper springConfigHelper = (DSOSpringConfigHelper) it.next();
      if (springConfigHelper.isDistributedBean(beanName)) {
        logger.debug(id + " bean " + beanName + " is distributed");
        return true;
      }
    }
    logger.debug(id + " bean " + beanName + " is NOT distributed");
    return false;
  }

  public boolean isDistributedField(String beanName, String fieldName) {
    for (Iterator it = this.springConfigHelpers.iterator(); it.hasNext();) {
      DSOSpringConfigHelper springConfigHelper = (DSOSpringConfigHelper) it.next();
      if (springConfigHelper.isDistributedField(beanName, fieldName)) {
        logger.debug(id + " field " + fieldName + " in bean " + beanName + " is distributed");
        return true;
      }
    }
    logger.debug(id + " field " + fieldName + " in bean " + beanName + " is NOT distributed");
    return false;
  }

  public void addLocation(String location) {
    this.locations.add(location);
  }

  private void calculateId() {
    if (this.appName == null) { return; }

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    pw.println("app " + this.appName);
    for (Iterator iter = locations.iterator(); iter.hasNext();) {
      String loc = (String) iter.next();
      pw.println("params " + loc);
    }
    // Should not do this
    new Throwable().printStackTrace(pw);
    this.id = getDigest(sw.toString());
  }

  private void determineIfClustered() {
    for (Iterator iter = this.dsoContext.getDSOSpringConfigHelpers().iterator(); iter.hasNext();) {
      DSOSpringConfigHelper config = (DSOSpringConfigHelper) iter.next();
      if (config.isMatchingApplication(this.appName) && isMatchingLocations(config)) {
        this.springConfigHelpers.add(config);
        this.isClustered = true;
        logger.info(id + " found matching configuration for " + locations);
      }
    }

    if (this.isClustered) {
      logger.info(id + " Context is distributed");
    } else {
      logger.info(id + " Context is NOT distributed");
    }
  }

  private boolean isMatchingLocations(DSOSpringConfigHelper config) {
    for (Iterator iter = locations.iterator(); iter.hasNext();) {
      String location = (String) iter.next();
      if (config.isMatchingConfig(location)) {
        return true;
      }
    }
    return false;
  }
   
  public void registerBeanDefinitions(Map beanMap) {
    calculateId();
    determineIfClustered();

    if (!this.isClustered) { return; }

    for (Iterator it = springConfigHelpers.iterator(); it.hasNext();) {
      DSOSpringConfigHelper configHelper = (DSOSpringConfigHelper) it.next();
      if (configHelper.isMatchingApplication(this.appName)) {
        registerDistributedEvents(configHelper.getDistributedEvents());
        registerDistributedBeans(configHelper.getDistributedBeans(), beanMap);
      }
    }

    String lockName = "@spring_context_" + this.id;

    managerUtilWrapper.beginLock(lockName, Manager.LOCK_TYPE_WRITE);
    try {
      this.clusteredBeans = (Map) managerUtilWrapper.lookupOrCreateRoot("tc:spring_context:" + this.id, this.clusteredBeans);
    } finally {
      managerUtilWrapper.commitLock(lockName);
    }
  }

  protected void registerDistributedEvents(final List distributedEvents) {
    ClassHierarchyWalker walker = new ClassHierarchyWalker(id, dsoContext);

    for (Iterator eventIterator = distributedEvents.iterator(); eventIterator.hasNext();) {
      String event = (String) eventIterator.next();
      // instrument only exact classes to avoid conflicts
      if (event.indexOf('*') == -1) {
        walker.walkClass(event, getClass().getClassLoader());
      }
    }
  }

  protected void registerDistributedBeans(Map distributedBeans, Map beanMap) {
    ClassHierarchyWalker walker = new ClassHierarchyWalker(id, dsoContext);

    for (Iterator beanMapIterator = beanMap.entrySet().iterator(); beanMapIterator.hasNext();) {
      Map.Entry entry = (Map.Entry) beanMapIterator.next();
      String beanName = (String) entry.getKey();
      AbstractBeanDefinition definition = (AbstractBeanDefinition) entry.getValue();

      Set excludedFields = (Set) distributedBeans.get(beanName);
      if (excludedFields != null) {
        beanDefinitions.put(beanName, definition);  // need to unregister on reload/destroy
        
        String beanClassName = getBeanClassName(definition, beanMap);

        walker.walkClass(beanClassName, getClass().getClassLoader());

        logger.info(this.id + " registering transient fields for " + beanName + " " + beanClassName);
        for (Iterator fieldIterator = excludedFields.iterator(); fieldIterator.hasNext();) {
          String fieldName = (String) fieldIterator.next();
          logger.info(this.id + " adding transient field " + beanClassName + "." + fieldName);
          dsoContext.addTransient(beanClassName, fieldName);
        }
      }

      // process bean metadata
      // String[] names = definition.attributeNames();
      // for (int i = 0; i < names.length; i++) {
      // String name = names[i];
      // Object value = definition.getAttribute(name);
      // if ("com.terracotta.Distributed".equals(name)) {
      // // TODO handle singleton attribute
      //
      // } else if ("com.terracotta.Transient".equals(name)) {
      // // TODO handle transient attribute
      //
      // } else if ("com.terracotta.AutoLock".equals(name)) {
      // // TODO handle autolock attribute
      //
      // } else {
      // // etc...
      //
      // }
      // }
    }
  }

  /**
   * Get bean's class name. If necessary, walk to the parent beans.
   */
  static String getBeanClassName(AbstractBeanDefinition definition, Map beanMap) {
    String beanClassName = definition.getBeanClassName();
    if (beanClassName == null && definition instanceof ChildBeanDefinition) {
      String parent = ((ChildBeanDefinition) definition).getParentName();
      definition = (AbstractBeanDefinition) beanMap.get(parent);
      if (definition != null) { return getBeanClassName(definition, beanMap); }
    }
    return beanClassName;
  }

  private String getDigest(String s) {
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      digest.update(s.getBytes("ASCII"));
      byte[] b = digest.digest();

      StringBuffer sb = new StringBuffer();
      String hex = "0123456789ABCDEF";
      for (int i = 0; i < b.length; i++) {
        int n = b[i];
        sb.append(hex.charAt((n & 0xF) >> 4)).append(hex.charAt(n & 0xF));
      }
      return sb.toString();
      
    } catch (NoSuchAlgorithmException e) {
      // should never happens
      throw new RuntimeException(e.getMessage());
    } catch (UnsupportedEncodingException e) {
      // should never happens
      throw new RuntimeException(e.getMessage());
    }
  }

  
  public BeanContainer getBeanContainer(ComplexBeanId beanId) {
    ManagerUtil.monitorEnter(this.clusteredBeans, Manager.LOCK_TYPE_READ);
    try {
      return (BeanContainer) clusteredBeans.get(beanId);
    } finally {
      ManagerUtil.monitorExit(this.clusteredBeans);
    }
  }
  
  public BeanContainer putBeanContainer(ComplexBeanId beanId, BeanContainer container) {
    ManagerUtil.monitorEnter(this.clusteredBeans, Manager.LOCK_TYPE_WRITE);
    try {
      return (BeanContainer) clusteredBeans.put(beanId, container);
    } finally {
      ManagerUtil.monitorExit(this.clusteredBeans);
    }
  }

  public BeanContainer removeBeanContainer(ComplexBeanId beanId) {
    ManagerUtil.monitorEnter(this.clusteredBeans, Manager.LOCK_TYPE_WRITE);
    try {
      return (BeanContainer) clusteredBeans.remove(beanId);
    } finally {
      ManagerUtil.monitorExit(this.clusteredBeans);
    }
  }
  
  public void initializeBean(ComplexBeanId beanId, Object bean, BeanContainer container) {
    logger.info(getId() + " Initializing distributed bean " + beanId);

    // TODO make initialization from shadow local copy optional

    Object distributed = container.getBean();
    try {
      copyTransientFields(
          beanId.getBeanName(), 
          bean, 
          distributed,
          distributed.getClass(), 
          ((Manageable) distributed).__tc_managed().getTCClass()
      );
    } catch (Throwable e) {
      // TODO should we fail here?
      logger.warn(getId() + " Error when copying transient fields to " + beanId, e);
    }        
  }
  
  private void copyTransientFields(String beanName, Object sourceBean, Object targetBean, 
        Class targetClass, TCClass tcClass) throws IllegalAccessException {
    if(tcClass.isLogical()) {
      return;
    }
    
    Field[] declaredFields = targetClass.getDeclaredFields();
    for (int i = 0; i < declaredFields.length; i++) {
      Field f = declaredFields[i];

      if ((f.getModifiers() & (Modifier.FINAL | Modifier.STATIC | Modifier.NATIVE)) != 0) {
        continue;
      }

      String fieldName = f.getName();
      if (fieldName.startsWith(ByteCodeUtil.TC_FIELD_PREFIX)) {
        continue;
      }

      TCField tcf = tcClass.getField(targetClass.getName() + "." + fieldName);
      f.setAccessible(true);
      Object value = f.get(sourceBean);

      if (tcf == null || !tcf.isPortable() || !this.isDistributedField(beanName, fieldName) || nonDistributables.contains(value)) {
        logger.info(this.getId() + " Initializing field " + fieldName + " in bean " + beanName);
        f.set(targetBean, value);
      }
    }

    Class superclass = targetClass.getSuperclass();
    TCClass tcsuperclass = tcClass.getSuperclass();
    if (superclass != null && tcsuperclass != null) {
      copyTransientFields(beanName, sourceBean, targetBean, superclass, tcsuperclass);
    }
  }
  
  
  
  interface ManagerUtilWrapper {
    void beginLock(String lockId, int type);

    Object lookupOrCreateRoot(String name, Object object);

    void commitLock(String lockId);
  }

  
  private static class ManagerUtilWrapperImpl implements ManagerUtilWrapper {

    public ManagerUtilWrapperImpl(NonDistributableObjectRegistry nonDistributableObjectRegistry) {
//      if (nonDistributableObjectRegistry.isAdded() == false) {
//        ManagerUtil.addTraverseTest(nonDistributableObjectRegistry);
//        nonDistributableObjectRegistry.setAdded();
//      }
    }

    public void beginLock(String lockId, int type) {
      ManagerUtil.beginLock(lockId, type);
    }

    public Object lookupOrCreateRoot(String name, Object object) {
      return ManagerUtil.lookupOrCreateRoot(name, object);
    }

    public void commitLock(String lockId) {
      ManagerUtil.commitLock(lockId);
    }

  }

}
