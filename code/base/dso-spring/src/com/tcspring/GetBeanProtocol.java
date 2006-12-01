/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.ServletContextResource;

import com.tc.aspectwerkz.joinpoint.StaticJoinPoint;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;

import java.util.LinkedList;


/**
 * Virtualize <code>AbstractBeanFactory.getBean()</code>.
 * 
 * @author Eugene Kuleshov
 * @author Jonas Bon&#233;r
 */
public class GetBeanProtocol {
  private final transient Log logger = LogFactory.getLog(getClass());
    
  protected ThreadLocal cflowStack = new ThreadLocal() {
    protected Object initialValue() {
      return new LinkedList();
    }
  };
  
  /**
   * Invoked after loadBeanDefinitions method in BeanDefinitionReader. Adds resource location to the DistributableBeanFactory mixin.
   * 
   * @see org.springframework.beans.factory.support.BeanDefinitionReader#loadBeanDefinitions(org.springframework.core.io.Resource)
   */
  public void captureIdentity(StaticJoinPoint jp, Resource resource, BeanDefinitionReader reader) throws Throwable {
    Object beanFactory = reader.getBeanFactory();
    if (beanFactory instanceof DistributableBeanFactory) {
      String location;
      if (resource instanceof ClassPathResource) {
        location = ((ClassPathResource) resource).getPath();
      } else if (resource instanceof FileSystemResource) {
        location = ((FileSystemResource) resource).getPath();
      } else if (resource instanceof ServletContextResource) {
        location = ((ServletContextResource) resource).getPath();
      } else {
        location = resource.getDescription();
      }

      DistributableBeanFactory distributableBeanFactory = (DistributableBeanFactory) beanFactory;
      distributableBeanFactory.addLocation(location);
    }
  }

  /**
   * Captures the name of the bean being created and makes it accessible to virtualizeSingletonBean() It also exits the
   * monitor entered while Spring finishes initialization of the Bean
   */
  public Object beanNameCflow(StaticJoinPoint jp, String beanName, AutowireCapableBeanFactory beanFactory)
      throws Throwable {
    
    if (beanFactory instanceof DistributableBeanFactory) {
      Object beanId = beanName;
      LinkedList stack = (LinkedList)cflowStack.get();
      if (!stack.isEmpty()) {
        Object param = stack.getFirst();
        if (param instanceof Object[] && ((Object[])param)[0] instanceof ComplexBeanId && ((ComplexBeanId)((Object[])param)[0]).getBeanName().equals(beanName)) {
          beanId = ((Object[])param)[0];
        }
      } 
            
      try {
        stack.addFirst(beanId);
        DistributableBeanFactory distributableBeanFactory = (DistributableBeanFactory) beanFactory;
        if (!distributableBeanFactory.isDistributedBean(beanName)) {
          return jp.proceed();
        }
        
        try {
          return jp.proceed();
        } finally {
          Object distributed = distributableBeanFactory.getBeanFromSingletonCache(beanId);
          if (distributed != null) {
            // This exits the monitor entered in virtualizeSingletonBean
            ManagerUtil.monitorExit(distributed);
          }
        }
      } finally {
        stack.removeFirst();
      }
    } else {
      return jp.proceed();
    }
  }

  /**
   * Called on...
   */
  public Object virtualizeSingletonBean(StaticJoinPoint jp, AutowireCapableBeanFactory beanFactory) throws Throwable {
    Object localBean = jp.proceed();

    if (beanFactory instanceof DistributableBeanFactory) {
      DistributableBeanFactory distributableBeanFactory = (DistributableBeanFactory) beanFactory;
      Object beanId = ((LinkedList)cflowStack.get()).getFirst();
      Object distributed = distributableBeanFactory.virtualizeSingletonBean(beanId, localBean);
      if (distributed != null) {
        ManagerUtil.monitorEnter(distributed, Manager.LOCK_TYPE_WRITE);
        // This monitor is exited in GetBeanProtocol.beanNameCflow()
        return distributed;
      }
    }
    return localBean;
  }
  
  /**
   * Called after call(* org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.populateBean(..))
   *  AND withincode(* org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(String, ..))
   */
  public void copyTransientFields(String beanName, RootBeanDefinition mergedBeanDefinition,
      BeanWrapper instanceWrapper, AutowireCapableBeanFactory beanFactory) {
    if (beanFactory instanceof DistributableBeanFactory) {
      Object beanId = ((LinkedList)cflowStack.get()).getFirst();
      
      DistributableBeanFactory distributableBeanFactory = (DistributableBeanFactory) beanFactory;
      Object distributed = distributableBeanFactory.getBeanFromSingletonCache(beanId);
      if(distributed!=null) {
        Object localInstance = instanceWrapper.getWrappedInstance();
        if(localInstance==distributed) {
          return;
        }

        logger.info(distributableBeanFactory.getId() + " Initializing distributed bean " + beanName);
        try {
          distributableBeanFactory.copyTransientFields(
              beanName, 
              localInstance, 
              distributed, 
              distributed.getClass(), 
              ((Manageable) distributed).__tc_managed().getTCClass()
          );
        } catch (Throwable e) {
          // TODO should we fail here?
          logger.warn(distributableBeanFactory.getId() + " Error when copying transient fields to " + beanName, e);
        }        
      }
    }
  }
  
}

