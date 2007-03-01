/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.support.AbstractApplicationContext;

import com.tc.aspectwerkz.joinpoint.StaticJoinPoint;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.DSOSpringConfigHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Manages the event multicast of ApplicationContext events. Preserves the context's ID, e.g. a multicast is a multicast
 * only to the same "logical" contexts in the cluster (and its parents).
 * 
 * @author Jonas Bon&#233;r
 * @author Eugene Kuleshov
 */
public class ApplicationContextEventProtocol {

  private static transient Log       logger   = LogFactory.getLog(ApplicationContextEventProtocol.class);

  /**
   * Local list of <code>ConfigurableApplicationContext</code> TODO use weak set or register for context disposal,
   * e.g. close()
   */
  private static transient final Map contexts = new HashMap();

  private transient ThreadLocal      isInMulticastEventCflow;

  private String                     appName;
  private Collection                 configs;
  private Collection                 beanNames;
  private Collection                 eventTypes;

  /**
   * Puts all clustered ApplicationContext instances into a node local HashMap .
   * <p>
   * Puts itself (the aspect) into a shared map, this is needed for DMI to work.
   * <p>
   * Puts some additional information into the aspect to be displayed as a regular info in the terracotta console.
   * <p>
   * Advises: 
   * <tt>
   * before(
   *       withincode(* org.springframework.context.support.AbstractApplicationContext+.refresh())
   *        AND call(* org.springframework.context.support.AbstractApplicationContext+.publishEvent(..))
   *        AND target(ctx))
   *  </tt>
   */
  public void registerContext(StaticJoinPoint jp, AbstractApplicationContext ctx) {
    DistributableBeanFactory distributableBeanFactory = (DistributableBeanFactory) ctx.getBeanFactory();
    if (!distributableBeanFactory.isClustered()) { return; }

    String id = distributableBeanFactory.getId();
    List configHelpers = distributableBeanFactory.getSpringConfigHelpers();

    String lockName = "@spring_info_" + id;

    ManagerUtil.beginLock(lockName, Manager.LOCK_TYPE_WRITE);
    try {
      populateAspectWithApplicationContextInfo(distributableBeanFactory, configHelpers);
      List aspects = (List) ManagerUtil.lookupOrCreateRoot("tc:spring_info:" + id, new ArrayList());
      aspects.add(this); // needed for DMI
    } finally {
      ManagerUtil.commitLock(lockName);
    }

    synchronized (contexts) {
      if (!contexts.containsKey(id)) {
        contexts.put(id, ctx);
      }
    }
  }

  private void populateAspectWithApplicationContextInfo(DistributableBeanFactory distributableBeanFactory, List configHelpers) {
    this.appName = distributableBeanFactory.getAppName();
    this.configs = distributableBeanFactory.getLocations();
    this.beanNames = new ArrayList();
    this.eventTypes = new ArrayList();

    for (Iterator it = configHelpers.iterator(); it.hasNext();) {
      DSOSpringConfigHelper configHelper = (DSOSpringConfigHelper) it.next();
      this.beanNames.addAll(configHelper.getDistributedBeans().keySet());
      this.eventTypes.addAll(configHelper.getDistributedEvents());
    }
  }

  /**
   * Intercepts the sending of a spring ApplicationContext event, this advice captures it and 
   * tries to publish it as a distributed event.
   * Invoked around void org.springframework.context.support.AbstractApplicationContext.publishEvent(..)
   */
  public Object interceptEvent(StaticJoinPoint jp, ApplicationEvent event, AbstractApplicationContext ctx)
      throws Throwable {

    BeanFactory beanFactory = ctx.getBeanFactory();
    if (tryToPublishDistributedEvent(beanFactory, event)) {
      return null;      
    }
    
    return jp.proceed();
  }
  
  /**
   *Tries to publish the event as a distributed event, by invoking a distributed method invocation.
   */
  public boolean tryToPublishDistributedEvent(BeanFactory beanFactory, ApplicationEvent event) {
    if (isInMulticastEventCflow != null && isInMulticastEventCflow.get() != null) {
      // logger.info("Could not publish as distributed (nested) " + event);
      return false;
    }

    if (ignoredEventType(event)) {
      // logger.info("Could not publish as distributed (ignored) " + event);
      return false;
    }
    if (beanFactory instanceof DistributableBeanFactory) {
      DistributableBeanFactory distributableBeanFactory = (DistributableBeanFactory) beanFactory;
      if (distributableBeanFactory.isDistributedEvent(event.getClass().getName())) {
        String ctxId = distributableBeanFactory.getId();
        // logger.info("Publishing distributed  " + event);
        boolean requireCommit = false;
        try {
          requireCommit = ManagerUtil.distributedMethodCall(
                                     this,
                                     "multicastEvent(Ljava/lang/String;Lorg/springframework/context/ApplicationEvent;)V",
                                     new Object[] { ctxId, event });
          multicastEvent(ctxId, event); // local call

        } catch (Throwable e) {
          logger.error("Unable to send event " + event + "; " + e.getMessage(), e);
        } finally {
          if(requireCommit) {
            ManagerUtil.distributedMethodCallCommit();
          }
        }
        return true;
//      } else {
//        logger.info("Could not publish as distributed (not distributed context) " + event);
      }
    }
    return false;

  }

  // TODO make ignored classes configurable
  private boolean ignoredEventType(ApplicationEvent event) {
    String name = event.getClass().getName();
    return "org.springframework.context.event.ContextRefreshedEvent".equals(name)
           || "org.springframework.context.event.ContextClosedEvent".equals(name)
           || "org.springframework.web.context.support.RequestHandledEvent".equals(name)
           || "org.springframework.web.context.support.ServletRequestHandledEvent".equals(name); // since 2.0
  }

  /**
   * [Distributed Method Invocation] 
   * <p>
   * 1. Gets the context by ID - needed since the DMI is invoked on another node and
   * need to look up [it's context]. 
   * <p>
   * 2. Publish event in the context matching the ID
   */
  public void multicastEvent(final String ctxId, final ApplicationEvent event) {
    AbstractApplicationContext context;
    synchronized (contexts) {
      context = (AbstractApplicationContext) contexts.get(ctxId);
    }

    logger.info(ctxId + " Publishing event " + event + " to " + context + " " + Thread.currentThread());

    if (context != null) {
      publish(context, event);
    }
  }

  private void publish(AbstractApplicationContext context, final ApplicationEvent event) {
    if (isInMulticastEventCflow == null) {
      isInMulticastEventCflow = new ThreadLocal();
    }
    Integer n = (Integer) isInMulticastEventCflow.get();
    if (n == null) {
      isInMulticastEventCflow.set(new Integer(0));
    } else {
      isInMulticastEventCflow.set(new Integer(n.intValue() + 1));
    }
    try {
      context.publishEvent(event);
    } finally {
      n = (Integer) isInMulticastEventCflow.get();
      if (n == null || n.intValue() == 0) {
        isInMulticastEventCflow.set(null);
      } else {
        isInMulticastEventCflow.set(new Integer(n.intValue() - 1));
      }
    }
  }

  public String toString() {
    return appName + ":" + configs;
  }
}
