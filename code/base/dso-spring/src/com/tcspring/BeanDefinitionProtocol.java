/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.util.ClassUtils;

import com.tc.aspectwerkz.joinpoint.StaticJoinPoint;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Advice for collecting bean metadata.
 * 
 * @see com.tc.object.config.SpringAspectModule#buildDefinitionForBeanDefinitionProtocol()
 * 
 * @author Eugene Kuleshov
 */
public class BeanDefinitionProtocol {

  private final Map beanMap = new HashMap();
  private final Map classes = new HashMap();

  /**
   * Collects the different spring bean configuration files.
   * <tt>
   * Advices: Around(
   *        execution(* org.springframework.context.support.AbstractRefreshableApplicationContext+.loadBeanDefinitions(..)) 
   *        AND args(beanFactory))
   * </tt>
   */
  public Object collectDefinitions(StaticJoinPoint jp, DefaultListableBeanFactory beanFactory) throws Throwable {
    try {
      return jp.proceed();
    } finally {
      
      if (beanFactory instanceof DistributableBeanFactory) {
        ((DistributableBeanFactory) beanFactory).registerBeanDefinitions(beanMap);
      }

      for (Iterator it = beanMap.entrySet().iterator(); it.hasNext();) {
        Map.Entry entry = (Map.Entry) it.next();
        AbstractBeanDefinition definition = (AbstractBeanDefinition) entry.getValue();

        String beanClassName = definition.getBeanClassName();
        if (beanClassName == null) continue;
        
        ClassLoader loader = (ClassLoader) classes.get(beanClassName);
        if(loader==null) continue;
        
        try {
          Class c = ClassUtils.forName(beanClassName, loader);
          definition.setBeanClass(c);
        } catch (Exception e) {
          // ignore
        }
      }      
    }
  }

  /**
   * Since Spring 2.0m5 ClassUtils.forName() is not called when bean definitions are created 
   * 
   * @see org.springframework.beans.factory.support.BeanDefinitionReaderUtils#createBeanDefinition()
   */
  public Object disableClassForName(String className, ClassLoader loader) throws Exception {
    try {
      AsmClassInfo.getClassInfo(className, loader);
      // TODO this can be potential class loader clash
      classes.put(className, loader);
    } catch (Exception e) {
      throw new ClassNotFoundException(className);
    }    
    return null;
  }

  /**
   * Called after constructor initialization on <code>BeanDefinitionHolder</code> class
   * 
   * @see org.springframework.beans.factory.config.BeanDefinitionHolder
   */
  public void saveBeanDefinition(StaticJoinPoint jp, BeanDefinitionHolder holder) {
    beanMap.put(holder.getBeanName(), holder.getBeanDefinition());
  }  
}

