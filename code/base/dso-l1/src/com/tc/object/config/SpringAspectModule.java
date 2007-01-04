/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.definition.deployer.AspectDefinitionBuilder;
import com.tc.aspectwerkz.definition.deployer.AspectModule;
import com.tc.aspectwerkz.definition.deployer.AspectModuleDeployer;


/**
 * Manages deployment of all AW aspects used to implement the Clustered Spring Runtime Container.
 *
 * @author Jonas Bon&#233;r
 * @author Eugene Kuleshov
 */
public class SpringAspectModule implements AspectModule {

  public void deploy(final AspectModuleDeployer deployer) {
    buildDefinitionForBeanDefinitionProtocol(deployer);
    
    buildDefinitionForGetBeanProtocol(deployer);
    
    buildDefinitionForScopeProtocol(deployer);
    
    buildDefinitionForApplicationContextEventProtocol(deployer);

    buildDefinitionForAopProxyFactoryProtocol(deployer);

    // buildDefinitionForTransactionManagerProtocol(deployer);

    // buildDefinitionForConfigurableAnnotationProtocol(deployer);
  }

  /**
   * Handle ignored fields and other class metadata.
   */
  private void buildDefinitionForBeanDefinitionProtocol(AspectModuleDeployer deployer) {
    deployer.addMixin("com.tcspring.DistributableBeanFactoryMixin", DeploymentModel.PER_INSTANCE,
                      "within(org.springframework.beans.factory.support.AbstractBeanFactory)", true);

    AspectDefinitionBuilder builder = deployer.newAspectBuilder("com.tcspring.BeanDefinitionProtocol",
                                                                DeploymentModel.PER_TARGET, null);

    // capture context parameters when BeanFactory is loaded by the
    builder.addAdvice("before",
        "execution(int org.springframework.beans.factory.support.BeanDefinitionReader+.loadBeanDefinitions(..)) "
            + "AND args(resource) AND target(reader)",
        "captureIdentity(StaticJoinPoint jp, org.springframework.core.io.Resource resource, "
            + "org.springframework.beans.factory.support.BeanDefinitionReader reader)");
    
    builder.addAdvice("around",
      "execution(* org.springframework.context.support.AbstractRefreshableApplicationContext+.loadBeanDefinitions(..)) "
          + "AND args(beanFactory)",
      "collectDefinitions(StaticJoinPoint jp, org.springframework.beans.factory.support.DefaultListableBeanFactory beanFactory)");

    builder.addAdvice("after", 
        "execution(org.springframework.beans.factory.config.BeanDefinitionHolder.new(..)) AND target(holder)",
        "saveBeanDefinition(StaticJoinPoint jp, org.springframework.beans.factory.config.BeanDefinitionHolder holder)");

    builder.addAdvice("around",
      "cflow(execution(* org.springframework.context.support.AbstractRefreshableApplicationContext+.loadBeanDefinitions(..))) "
          + "AND withincode(* org.springframework.beans.factory.support.BeanDefinitionReaderUtils.createBeanDefinition(..)) "
          + "AND call(* org.springframework.util.ClassUtils.forName(..)) "
          + "AND args(className, loader)", 
      "disableClassForName(String className, java.lang.ClassLoader loader)");
  }

  private void buildDefinitionForGetBeanProtocol(AspectModuleDeployer deployer) {
    AspectDefinitionBuilder builder = deployer.newAspectBuilder("com.tcspring.GetBeanProtocol", DeploymentModel.PER_JVM, null);

//    This approach does not work because of Spring's handling of the circular dependencies     
//    builder.addAdvice("after", 
//        "execution(org.springframework.beans.factory.support.AbstractBeanFactory.new(..)) "
//            + "AND this(factory)",
//        "registerBeanPostProcessor(StaticJoinPoint jp, org.springframework.beans.factory.support.AbstractBeanFactory factory)");
    
    builder.addAdvice("around",
      "execution(* org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(String, ..)) "
          + "AND args(beanName, ..) AND target(beanFactory)",
      "beanNameCflow(StaticJoinPoint jp, String beanName,"
          + "org.springframework.beans.factory.config.AutowireCapableBeanFactory beanFactory)");

    builder.addAdvice("around",
      "withincode(* org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(String, ..)) "
          + "AND call(* org.springframework.beans.BeanWrapper+.getWrappedInstance()) "
          + "AND this(beanFactory)",
      "virtualizeSingletonBean(StaticJoinPoint jp, "
          + "org.springframework.beans.factory.config.AutowireCapableBeanFactory beanFactory)");

    builder.addAdvice("after", 
      "withincode(* org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(String, ..)) "
          + "AND call(* org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.populateBean(..)) " 
          + "AND this(beanFactory) AND args(beanName, mergedBeanDefinition, instanceWrapper)",
      "initializeSingletonBean(String beanName, " 
          + "org.springframework.beans.factory.support.RootBeanDefinition mergedBeanDefinition, "
          + "org.springframework.beans.BeanWrapper instanceWrapper, " 
          + "org.springframework.beans.factory.config.AutowireCapableBeanFactory beanFactory)");
  }
  
  private void buildDefinitionForScopeProtocol(AspectModuleDeployer deployer) {
    if (hasClass("org.springframework.beans.factory.config.Scope", deployer)) {
      deployer.addMixin("com.tcspring.ScopeProtocol$DistributableBeanFactoryAwareMixin", DeploymentModel.PER_INSTANCE,
          "within(org.springframework.beans.factory.config.Scope+)", true);

      AspectDefinitionBuilder builder = deployer.newAspectBuilder("com.tcspring.ScopeProtocol", DeploymentModel.PER_JVM, null);

      builder.addAdvice("after", 
          "execution(* org.springframework.beans.factory.support.AbstractBeanFactory+.registerScope(String, org.springframework.beans.factory.config.Scope+)) " 
              + "AND target(beanFactory) AND args(scopeName, scope)", 
          "setDistributableBeanFactory(String scopeName, " 
              + "org.springframework.beans.factory.config.Scope scope, " 
              + "org.springframework.beans.factory.support.AbstractBeanFactory beanFactory)");

      builder.addAdvice("around",
          "withincode(* org.springframework.beans.factory.support.AbstractBeanFactory.getBean(String, ..)) "                        
              + "AND call(* org.springframework.beans.factory.config.Scope+.get(String, ..)) " 
              + "AND target(s) AND args(beanName, ..)", 
          "virtualizeScopedBean(StaticJoinPoint jp, org.springframework.beans.factory.config.Scope s, String beanName)");
      
      builder.addAdvice("around",
          "withincode(* org.springframework.beans.factory.ObjectFactory+.getObject()) "
              + "AND call(* org.springframework.beans.factory.config.Scope+.registerDestructionCallback(..)) "
              + "AND target(scope) AND args(beanName, callback)", 
          "wrapDestructionCallback(StaticJoinPoint jp, String beanName, java.lang.Runnable callback, "
              + "org.springframework.beans.factory.config.Scope scope)");
      
      builder.addAdvice("around", 
          "withincode(* org.springframework.beans.factory.config.Scope+.get(..)) " 
              + "AND call(* org.springframework.web.context.request.RequestAttributes+.getAttribute(String, ..)) " 
              + "AND this(s) AND args(beanName, ..)", 
          "suspendRequestAttributeGet(StaticJoinPoint jp, org.springframework.beans.factory.config.Scope s, String beanName)");
      builder.addAdvice("around", 
          "withincode(* org.springframework.beans.factory.config.Scope+.get(..)) " 
              + "AND call(* org.springframework.web.context.request.RequestAttributes+.setAttribute(String, ..)) " 
              + "AND this(s) AND args(beanName, ..)", 
          "suspendRequestAttributeSet(StaticJoinPoint jp, org.springframework.beans.factory.config.Scope s, String beanName)");
    }

    if(hasClass("javax.servlet.http.HttpSession", deployer)) {
      AspectDefinitionBuilder sessionBuilder = deployer.newAspectBuilder("com.tcspring.SessionProtocol", 
          DeploymentModel.PER_JVM, null);

      // XXX change to wrap call org.springframework.web.context.request.RequestAttributes+.getSessionId()
      sessionBuilder.addAdvice("around",
        "cflow(execution(* org.springframework.web.context.request.SessionScope.getConversationId())) "
            + "AND withincode(* org.springframework.web.context.request.ServletRequestAttributes.getSessionId()) "
            + "AND call(* javax.servlet.http.HttpSession+.getId()) AND target(session)",
        "clusterSessionId(StaticJoinPoint jp, javax.servlet.http.HttpSession session)");

      if(hasClass("javax.servlet.http.HttpSessionBindingListener", deployer)) {
        sessionBuilder.addAdvice("around",
            "execution(* org.springframework.web.context.request.ServletRequestAttributes.registerSessionDestructionCallback(..)) "
                + "AND args(name, callback)",
            "captureDestructionCallback(StaticJoinPoint jp, String name, java.lang.Runnable callback)");

        sessionBuilder.addAdvice("around",
            "withincode(* org.springframework.web.context.request.ServletRequestAttributes.registerSessionDestructionCallback(..)) "
                + "AND call(* javax.servlet.http.HttpSession.setAttribute(..)) " 
                + "AND target(session) AND args(name, ..)",
            "virtualizeSessionDestructionListener(StaticJoinPoint jp, String name, javax.servlet.http.HttpSession session)");
      }
    }
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

  private void buildDefinitionForAopProxyFactoryProtocol(final AspectModuleDeployer deployer) {
    if (hasClass("org.springframework.aop.framework.AopProxyFactory", deployer)) {  

      deployer.addMixin("com.tcspring.AopProxyFactoryProtocol$BeanFactoryAwareMixin", DeploymentModel.PER_INSTANCE,
                        "within(org.springframework.aop.framework.ProxyFactoryBean)", true);

      AspectDefinitionBuilder builder = deployer.newAspectBuilder("com.tcspring.AopProxyFactoryProtocol", DeploymentModel.PER_JVM, null);
      
      // save a accessible copy of the BeanFactory 
      builder.addAdvice("before",
          "execution(void org.springframework.aop.framework.ProxyFactoryBean+.setBeanFactory(..)) "
              + "AND args(beanFactory) AND this(bean)",
          "saveBeanFactory(com.tcspring.BeanFactoryAware bean, "
              + "org.springframework.beans.factory.BeanFactory beanFactory)");
      
      builder.addAdvice("around", 
          "execution(* org.springframework.aop.framework.AopProxyFactory+.createAopProxy(..)) "
                    + "AND args(proxyFactory)",
          "createAopProxy(StaticJoinPoint, org.springframework.aop.framework.AdvisedSupport proxyFactory)");
    }
  }
  
  private boolean hasClass(String name, AspectModuleDeployer deployer) {
    return deployer.getClassLoader().getResource(name.replace('.', '/')+".class") != null;
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

}

