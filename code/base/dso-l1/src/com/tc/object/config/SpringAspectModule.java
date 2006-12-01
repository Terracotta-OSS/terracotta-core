/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.definition.deployer.AspectDefinitionBuilder;
import com.tc.aspectwerkz.definition.deployer.AspectModule;
import com.tc.aspectwerkz.definition.deployer.AspectModuleDeployer;

import java.net.URL;

/**
 * Manages deployment of all AW aspects used to implement the Clustered Spring Runtime Container.
 *
 * @author Jonas Bon&#233;r
 * @author Eugene Kuleshov
 */
public class SpringAspectModule implements AspectModule {

  public void deploy(final AspectModuleDeployer deployer) {
    buildDefinitionForGetBeanProtocol(deployer);
    buildDefinitionForApplicationContextEventProtocol(deployer);

    buildDefinitionForBeanDefinitionProtocol(deployer);

    buildDefinitionForAopProxyFactoryProtocol(deployer);
    
    buildDefinitionForBeanFactoryAwareProtocol(deployer);

    // buildDefinitionForTransactionManagerProtocol(deployer);

    // buildDefinitionForConfigurableAnnotationProtocol(deployer);
  }

  /**
   * Handle ignored fields and other class metadata.
   */
  private void buildDefinitionForBeanDefinitionProtocol(AspectModuleDeployer deployer) {
    AspectDefinitionBuilder builder = deployer.newAspectBuilder("com.tcspring.BeanDefinitionProtocol",
                                                                DeploymentModel.PER_TARGET, null);

    builder.addAdvice("around",
      "execution(* org.springframework.context.support.AbstractRefreshableApplicationContext+.loadBeanDefinitions(..)) "
          + "AND args(beanFactory)",
      "collectDefinitions(StaticJoinPoint jp, org.springframework.beans.factory.support.DefaultListableBeanFactory beanFactory)");

    builder.addAdvice("around",
      "cflow(execution(* org.springframework.context.support.AbstractRefreshableApplicationContext+.loadBeanDefinitions(..))) "
          + "AND withincode(* org.springframework.beans.factory.support.BeanDefinitionReaderUtils.createBeanDefinition(..)) "
          + "AND call(* org.springframework.util.ClassUtils.forName(..)) "
          + "AND args(className, loader)", 
      "disableClassForName(String className, java.lang.ClassLoader loader)");

    builder.addAdvice("after", 
        "execution(org.springframework.beans.factory.config.BeanDefinitionHolder.new(..)) AND target(holder)",
        "saveBeanDefinition(StaticJoinPoint jp, org.springframework.beans.factory.config.BeanDefinitionHolder holder)");
  }

  /**
   * Save a publicly accessible reference to BeanFactory for ProxyFactoryBean
   */
  private void buildDefinitionForBeanFactoryAwareProtocol(AspectModuleDeployer deployer) {
    deployer.addMixin("com.tcspring.BeanFactoryAwareProtocol$BeanFactoryAwareMixin", DeploymentModel.PER_INSTANCE,
                      "within(org.springframework.aop.framework.ProxyFactoryBean)", true);

    AspectDefinitionBuilder builder = deployer.newAspectBuilder("com.tcspring.BeanFactoryAwareProtocol",
                                                                DeploymentModel.PER_JVM, null);

    // save a accessible copy of the BeanFactory 
    builder.addAdvice("before",
                      "execution(void org.springframework.aop.framework.ProxyFactoryBean+.setBeanFactory(..)) "
                          + "AND args(beanFactory) AND this(bean)",
                      "saveBeanFactory(com.tcspring.BeanFactoryAware bean, "
                          + "org.springframework.beans.factory.BeanFactory beanFactory)");
  }

  private void buildDefinitionForGetBeanProtocol(AspectModuleDeployer deployer) {
    deployer.addMixin("com.tcspring.DistributableBeanFactoryMixin", DeploymentModel.PER_INSTANCE,
                      "within(org.springframework.beans.factory.support.AbstractBeanFactory)", true);

    String protocolClassName = "com.tcspring.GetBeanProtocol";
    if (deployer.getClassLoader().getResource("org/springframework/beans/factory/config/Scope.class") != null) {
      protocolClassName = "com.tcspring.GetBeanProtocolWithScope";
    }
    
    AspectDefinitionBuilder builder = deployer.newAspectBuilder(protocolClassName,
                                                                DeploymentModel.PER_JVM, null);

    // capture context parameters when BeanFactory is loaded by the
    builder.addAdvice("before",
                      "execution(int org.springframework.beans.factory.support.BeanDefinitionReader+.loadBeanDefinitions(..)) "
                          + "AND args(resource) AND target(reader)",
                      "captureIdentity(StaticJoinPoint jp, org.springframework.core.io.Resource resource, "
                          + "org.springframework.beans.factory.support.BeanDefinitionReader reader)");

    builder.addAdvice("around",
                    "execution(* org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(String, ..)) "
                       + "AND args(beanName, ..) AND target(beanFactory)",
                   "beanNameCflow(StaticJoinPoint jp, String beanName,"
                       + "org.springframework.beans.factory.config.AutowireCapableBeanFactory beanFactory)");

    builder.addAdvice("around",
                   "withincode(* org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(String, ..)) "
                       + "AND call(* org.springframework.beans.BeanWrapper+.getWrappedInstance()) "
                       // + "AND args(beanName, bean, mergedBeanDefinition) "
                       + "AND this(beanFactory)",
                   "virtualizeSingletonBean(StaticJoinPoint jp, "
                       + "org.springframework.beans.factory.config.AutowireCapableBeanFactory beanFactory)");

    builder.addAdvice("after", 
        "withincode(* org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(String, ..)) "
            + "AND call(* org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.populateBean(..)) " 
            + "AND this(beanFactory) AND args(beanName, mergedBeanDefinition, instanceWrapper)",
        "copyTransientFields(String beanName, " 
            + "org.springframework.beans.factory.support.RootBeanDefinition mergedBeanDefinition, "
            + "org.springframework.beans.BeanWrapper instanceWrapper, " 
            + "org.springframework.beans.factory.config.AutowireCapableBeanFactory beanFactory)");
    
    if (protocolClassName.equals("com.tcspring.GetBeanProtocolWithScope")) {     
      builder.addAdvice("around",
         "withincode(* org.springframework.beans.factory.support.AbstractBeanFactory.getBean(String, ..)) "
           + "AND call(* org.springframework.beans.factory.config.Scope+.get(String, ..)) "
           + "AND target(scope) AND this(beanFactory) AND args(name, ..)",
         "scopeIdCflow(StaticJoinPoint jp, "
           + "org.springframework.beans.factory.config.Scope scope, java.lang.String name, org.springframework.beans.factory.support.AbstractBeanFactory beanFactory)");
      builder.addAdvice("around",
         "cflow(execution(* org.springframework.web.context.request.AbstractRequestAttributesScope+.get(String, ..))) "
             + "AND withincode(* org.springframework.web.context.request.ServletRequestAttributes+.getAttribute(..)) "
             + "AND call(* javax.servlet.http.HttpSession.getAttribute(..)) AND args(name)",
         "interceptSessionGet(StaticJoinPoint jp, java.lang.String name)"); 
      builder.addAdvice("around",
                        "cflow(execution(* org.springframework.web.context.request.AbstractRequestAttributesScope+.get(String, ..))) "
                          + "AND withincode(void org.springframework.web.context.request.ServletRequestAttributes.registerSessionDestructionCallback(..)) "
                          + "AND call(* javax.servlet.http.HttpSession+.setAttribute(..)) AND target(session) AND args(name, listener)",
                        "interceptRegisterCallback(StaticJoinPoint jp, javax.servlet.http.HttpSession session, java.lang.String name, java.lang.Object listener)");      
      builder.addAdvice("around",
                        "cflow(execution(* org.springframework.web.context.request.SessionScope.getConversationId())) "
                          + "AND withincode(* org.springframework.web.context.request.ServletRequestAttributes.getSessionId()) "
                          + "AND call(* javax.servlet.http.HttpSession+.getId()) AND target(session)",
                        "interceptGetSessionId(StaticJoinPoint jp, javax.servlet.http.HttpSession session)");      
    }    
    
    // org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsBeforeInstantiation(Class
    // beanClass, String beanName)
    // org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation(Class
    // beanClass, String beanName)
    // org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsAfterInitialization(Object
    // existingBean, String beanName)
    // org.springframework.beans.factory.config.BeanPostProcessor.postProcessAfterInitialization(Object bean, String
    // beanName)

    // instanceWrapper.getWrappedInstance(); // bean instantiated

    // REQUIRE LOCK from this point
    // org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation(Object
    // bean, String beanName)

    // org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.populateBean(String beanName,
    // RootBeanDefinition mergedBeanDefinition, BeanWrapper bw)

    // org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(String beanName,
    // Object bean, RootBeanDefinition mergedBeanDefinition)
    // org.springframework.beans.factory.BeanNameAware.setBeanName(String name)
    // org.springframework.beans.factory.BeanFactoryAware.setBeanFactory(BeanFactory beanFactory)
    // org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsBeforeInitialization(Object
    // existingBean, String beanName)
    // org.springframework.beans.factory.config.BeanPostProcessor.postProcessBeforeInitialization(Object bean, String
    // beanName)
    // org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.invokeInitMethods(String beanName,
    // Object bean, RootBeanDefinition mergedBeanDefinition)
    // org.springframework.beans.factory.InitializingBean.afterPropertiesSet()
    // org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.invokeCustomInitMethod(String
    // beanName, Object bean, String initMethodName, boolean enforceInitMethod)
    // org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsAfterInitialization(Object
    // existingBean, String beanName)
    // org.springframework.beans.factory.config.BeanPostProcessor.postProcessAfterInitialization(Object bean, String
    // beanName)

    // org.springframework.beans.factory.support.AbstractBeanFactory.registerDisposableBeanIfNecessary(String beanName,
    // Object bean, RootBeanDefinition mergedBeanDefinition)

  }

  private void buildDefinitionForApplicationContextEventProtocol(final AspectModuleDeployer deployer) {
    AspectDefinitionBuilder builder = deployer.newAspectBuilder("com.tcspring.ApplicationContextEventProtocol",
                                                                DeploymentModel.PER_JVM, null);

    builder.addAdvice("before",
        "withincode(* org.springframework.context.support.AbstractApplicationContext+.refresh()) "
            + "AND call(* org.springframework.context.support.AbstractApplicationContext+.publishEvent(..)) "
            + "AND target(ctx)",
        "registerContext(StaticJoinPoint jp, org.springframework.context.support.AbstractApplicationContext ctx)");

    builder.addAdvice("around",
        "execution(void org.springframework.context.support.AbstractApplicationContext.publishEvent(..)) "
            + "AND args(event) AND target(ctx)",
        "interceptEvent(StaticJoinPoint jp, " 
            + "org.springframework.context.ApplicationEvent event, "
            + "org.springframework.context.support.AbstractApplicationContext ctx)");
  }

  // private void buildDefinitionForTransactionManagerProtocol(final AspectModuleDeployer deployer) {
  // AspectDefinitionBuilder builder = deployer.newAspectBuilder("com.tcspring.TransactionManagerProtocol",
  // DeploymentModel.PER_JVM, null);
  //
  // // hijack anything but NOT derivatives of the AbstractPlatformTransactionManager
  //
  // builder
  // .addAdvice(
  // "around",
  // "execution(* org.springframework.transaction.PlatformTransactionManager+.getTransaction(..)) "
  // + "AND !execution(* org.springframework.transaction.support.AbstractPlatformTransactionManager+.getTransaction(..))
  // "
  // + "AND target(manager)", "startTransaction(StaticJoinPoint jp, java.lang.Object manager)");
  //
  // builder
  // .addAdvice(
  // "around",
  // "execution(* org.springframework.transaction.PlatformTransactionManager+.commit(..)) "
  // + "AND !execution(* org.springframework.transaction.support.AbstractPlatformTransactionManager+.commit(..)) "
  // + "AND target(manager)", "commitTransaction(StaticJoinPoint jp, java.lang.Object manager)");
  //
  // builder
  // .addAdvice(
  // "around",
  // "execution(void org.springframework.transaction.PlatformTransactionManager+.rollback(..)) "
  // + "AND !execution(* org.springframework.transaction.support.AbstractPlatformTransactionManager+.rollback(..)) "
  // + "AND target(manager)", "rollbackTransaction(StaticJoinPoint jp, java.lang.Object manager)");
  // }

  private void buildDefinitionForAopProxyFactoryProtocol(final AspectModuleDeployer deployer) {
    URL url = deployer.getClassLoader().getResource("org/springframework/aop/framework/AopProxyFactory.class");
    if (url != null) {  
      AspectDefinitionBuilder builder = deployer.newAspectBuilder("com.tcspring.AopProxyFactoryProtocol", DeploymentModel.PER_JVM, null);
  
      builder.addAdvice("around", 
          "execution(* org.springframework.aop.framework.AopProxyFactory+.createAopProxy(..)) "
                    + "AND args(proxyFactory)",
          "createAopProxy(StaticJoinPoint, org.springframework.aop.framework.AdvisedSupport proxyFactory)");
    }
  }

  // private void buildDefinitionForConfigurableAnnotationProtocol(AspectModuleDeployer deployer) {
  // try {
  // AspectDefinitionBuilder builder =
  // deployer.newAspectBuilder(
  // "com.tcspring.ConfigurableAnnotationProtocol",
  // DeploymentModel.PER_JVM, null);
  //
  // builder.addAdvice("afterReturning",
  // "execution(*.new(..)) AND within(@Configurable *) AND this(beanInstance)",
  // "afterBeanConstruction(Object beanInstance)");
  //
  // builder.addAdvice("around",
  // "execution(* org.springframework.aop.config.SpringConfiguredBeanDefinitionParser.getBeanConfigurerClass())",
  // "hijackBeanConfigurerClass(StaticJoinPoint jp)");
  //
  // } catch(Throwable t) {
  // System.err.println("[AW::WARNING] unable to build aspect for @Configurable; " + t.toString()+"
  // "+Thread.currentThread());
  // }
  // }

  /*
  private void buildDefinitionForWebFlow10rc3Protocol(AspectModuleDeployer deployer) {
    URL url = deployer.getClassLoader().getResource("org/springframework/webflow/execution/impl/FlowExecutionImpl.class");
    if (url != null) { 
      AspectDefinitionBuilder builder = deployer.newAspectBuilder("com.tcspring.WebFlowProtocol", DeploymentModel.PER_JVM, null);

      // for some reason couldn't use java.lang.Object type for "instance", so had to create 3 separate methods
      builder.addAdvice("after", 
          "execution(org.springframework.webflow.FlowSession+.new(org.springframework.webflow.Flow, ..))"
              + " AND args(flow, ..) AND target(instance)",
          "setFlowId(StaticJoinPoint, " 
              + "org.springframework.webflow.Flow flow, "
              + "org.springframework.webflow.FlowSession instance)");

      builder.addAdvice("after", 
          "execution(org.springframework.webflow.execution.FlowExecution+.new(org.springframework.webflow.Flow, ..))"
              + " AND args(flow, ..) AND target(instance)",
          "setFlowId(StaticJoinPoint, " 
              + "org.springframework.webflow.Flow flow, "
              + "org.springframework.webflow.execution.FlowExecution instance)");
      
      builder.addAdvice("after", 
          "execution(* org.springframework.webflow.FlowSession+.setState(org.springframework.webflow.State))"
              + " AND args(state) AND target(instance)",
          "setStateId(StaticJoinPoint, " 
              + "org.springframework.webflow.State state, "
              + "org.springframework.webflow.FlowSession instance)");

      builder.addAdvice("around",
           "(set(* org.springframework.webflow.FlowSession+.flowId)"
              + " OR set(* org.springframework.webflow.FlowSession+.stateId)"
              + " OR set(* org.springframework.webflow.execution.impl.FlowExecutionImpl.flowId))"
              + " AND args(value)",
          "disableIdCleanup(StaticJoinPoint, String value)");

      builder.addAdvice("around",
          "withincode(* org.springframework.webflow.execution.impl.FlowSessionImpl.rehydrate(..))"
              +" AND call(* org.springframework.util.Assert.state(..))",
          "disableAssertState(StaticJoinPoint)");

      builder.addAdvice("around",
          "withincode(* org.springframework.webflow.execution.impl.FlowExecutionImpl.rehydrate(..))"
              +" AND call(* org.springframework.webflow.execution.impl.FlowExecutionImpl.isHydrated())",
          "disableIsHydrated(StaticJoinPoint)");

      // fix for LKC-2369
      builder.addAdvice("before",
          "execution(* org.springframework.webflow.execution.repository.conversation.impl.LocalConversationService$ConversationProxy.end())"
               + " AND this(conversation)",
          "unlockConversationOnEnd(org.springframework.webflow.execution.repository.conversation.Conversation conversation)");
    }
  }
  */

}
