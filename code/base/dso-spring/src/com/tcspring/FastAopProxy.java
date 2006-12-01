/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.BeanFactory;

import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.aspect.container.AspectFactoryManager;
import com.tc.aspectwerkz.definition.AspectDefinition;
import com.tc.aspectwerkz.definition.DocumentParser;
import com.tc.aspectwerkz.definition.SystemDefinition;
import com.tc.aspectwerkz.definition.SystemDefinitionContainer;
import com.tc.aspectwerkz.definition.deployer.AspectDefinitionBuilder;
import com.tc.aspectwerkz.exception.WrappedRuntimeException;
import com.tc.aspectwerkz.proxy.Proxy;
import com.tc.aspectwerkz.proxy.ProxyDelegationStrategy;
import com.tc.aspectwerkz.proxy.ProxySubclassingStrategy;
import com.tc.aspectwerkz.proxy.Uuid;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.reflect.impl.java.JavaClassInfo;
import com.tc.aspectwerkz.transform.TransformationConstants;
import com.tc.aspectwerkz.util.UuidGenerator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of the Spring AOP proxy that is order of magnitude faster. Based on the AspectWerkz AWProxy
 * architecture.
 * 
 * @author Jonas Bon&#233;r
 */
public class FastAopProxy implements AopProxy {
  private final transient Log     logger                           = LogFactory.getLog(getClass());

  private static final boolean    USE_CACHE                        = false;
  private static final boolean    MAKE_ADVISABLE                   = false;

  private static final String     SPRING_ASPECT_CONTAINER          = SpringAspectContainer.class.getName();

  private static final MethodInfo s_methodBeforeAdviceMethodInfo   = JavaClassInfo
                                                                       .getClassInfo(MethodBeforeAdvice.class)
                                                                       .getMethods()[0];
  private static final MethodInfo s_methodInterceptorMethodInfo    = JavaClassInfo
                                                                       .getClassInfo(MethodInterceptor.class)
                                                                       .getMethods()[0];
  private static final MethodInfo s_afterReturningAdviceMethodInfo = JavaClassInfo
                                                                       .getClassInfo(AfterReturningAdvice.class)
                                                                       .getMethods()[0];

  private static final String     s_throwsAdviceMethodName         = "afterThrowing";

  private final ProxyFactoryBean  m_proxyFactory;
  private final SystemDefinition  m_systemDef;
  private final ClassLoader       m_loader;
  private final Class             m_targetClass;
  private final String            m_proxyName;
  private final boolean           m_isSubclassingProxy;
  private final BeanFactory       m_beanFactory;

  // private final String[] m_interceptorNames;

  public FastAopProxy(final ProxyFactoryBean proxyFactory) throws AopConfigException {
    if (proxyFactory == null) { throw new AopConfigException("Cannot create AopProxy with null ProxyConfig"); }
    if (proxyFactory.getAdvisors().length == 0 && proxyFactory.getTargetSource() == AdvisedSupport.EMPTY_TARGET_SOURCE) { throw new AopConfigException(
                                                                                                                                                       "Cannot create AopProxy with no advisors and no target source"); }
    if (proxyFactory.getTargetSource().getTargetClass() == null) { throw new AopConfigException(
                                                                                                "Either an interface or a target is required for proxy creation"); }

    // System.out.println("### creating Fast AOP proxy for " +
    // proxyFactory.getTargetSource().getTargetClass().getName());
    logger.info("Creating FastProxy for " + proxyFactory.getTargetSource().getTargetClass().getName());

    m_proxyFactory = proxyFactory;
    m_targetClass = m_proxyFactory.getTargetSource().getTargetClass();
    m_loader = m_targetClass.getClassLoader();
    m_isSubclassingProxy = m_proxyFactory.isProxyTargetClass() || m_proxyFactory.getProxiedInterfaces().length == 0;
    m_proxyName = getProxyName();
    m_systemDef = new SystemDefinition(m_proxyName + UuidGenerator.generate(m_proxyName));

    try {
      m_beanFactory = ((BeanFactoryAware)proxyFactory).tc$getBeanFactory();
    } catch (Exception e) {
      throw new WrappedRuntimeException(e);
    }

    prepareSystemDefinition();
    parseSpringDefinition();
  }

  private String getProxyName() {
    return m_targetClass.getName().replace('.', '/')
        + (m_isSubclassingProxy ? ProxySubclassingStrategy.PROXY_SUFFIX : ProxyDelegationStrategy.PROXY_SUFFIX)
        + Long.toString(Uuid.newUuid());
  }

  public Object getProxy() {
    return getProxy(null);
  }

  public Object getProxy(final ClassLoader classLoader) {
    final Object target;
    try {
      target = m_proxyFactory.getTargetSource().getTarget();
    } catch (Exception e) {
      throw new AopConfigException("ProxyFactory is not correctly initialized - target is NULL", e);
    }

    if (m_isSubclassingProxy) {
      // create subclassing proxy (cglib style)
      return Proxy.newInstance(target.getClass(), USE_CACHE, MAKE_ADVISABLE, m_systemDef);
    } else {
      // create delegating proxy (DP style)
      final Class[] interfaces = m_proxyFactory.getProxiedInterfaces();
      final Object[] implementations = new Object[interfaces.length];
      for (int i = 0; i < implementations.length; i++) {
        implementations[i] = target;
      }
      return Proxy.newInstance(interfaces, implementations, USE_CACHE, MAKE_ADVISABLE, m_systemDef);
    }
  }

  private void prepareSystemDefinition() {
    DocumentParser.addVirtualAspect(m_systemDef);
    if (!SystemDefinitionContainer.s_classLoaderSystemDefinitions.containsKey(m_loader)) {
      SystemDefinitionContainer.s_classLoaderSystemDefinitions.put(m_loader, new HashSet());
    }
    ((Set) SystemDefinitionContainer.s_classLoaderSystemDefinitions.get(m_loader)).add(m_systemDef);
  }

  private void parseSpringDefinition() {
    Advisor[] advisors = m_proxyFactory.getAdvisors();

    // loop over all aspects (advice/interceptors)
    for (int i = 0; i < advisors.length; i++) {
      Advisor advisor = advisors[i];
      if (advisor instanceof PointcutAdvisor) {
        PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;

        final Advice aspect = pointcutAdvisor.getAdvice();
        // FIXME BAAAAAAAD!!! support IntroductionInterceptor
        if (aspect instanceof IntroductionInterceptor) { throw new IllegalStateException(
                                                                                         "IntroductionInterceptor is currently not supported by FastProxy"); }
        // FIXME BAAAAAAAD!!! support ThrowsAdvice
        if (aspect instanceof ThrowsAdvice) { throw new IllegalStateException(
                                                                              "ThrowsAdvice is currently not supported by FastProxy"); }

        final Class aspectClass = aspect.getClass();
        final Pointcut pointcut = pointcutAdvisor.getPointcut();
        if (!pointcut.getClassFilter().matches(m_targetClass)) {
          continue;
        }

        // create AW version of the Spring pointcut
        final StringBuffer pcd = new StringBuffer();
        MethodMatcher methodMatcher = pointcut.getMethodMatcher();
        Method[] methods = m_targetClass.getDeclaredMethods();
        boolean hasAtLeastOneMatch = false;
        for (int j = 0; j < methods.length; j++) {
          Method method = methods[j];
          if (methodMatcher.matches(method, m_targetClass)) {
            buildPointcutForMethod(pcd, method);
            hasAtLeastOneMatch = true;
          }
        }
        if (hasAtLeastOneMatch) {
          int length = pcd.length();
          pcd.delete(length - 3, length);
        } else {
          continue;
        }

        // final AspectDefinition aspectDef = createAspectDefUsingXml(aspect, aspectClass, pcd);
        final AspectDefinition aspectDef = buildAspectDefinition(aspect, aspectClass, pcd.toString());

        initializeAspectFactory(aspect, aspectDef);
      } else {
        throw new IllegalStateException("introductions (mixins) are currently not supported");
      }
    }
  }

  private void buildPointcutForMethod(final StringBuffer pcd, final Method method) {
    pcd.append("execution(");
    pcd.append(method.getReturnType().getName());
    pcd.append(" ");
    pcd.append(m_targetClass.getName());
    pcd.append(".");
    pcd.append(method.getName());
    pcd.append("(");
    pcd.append("..");
    pcd.append(")) || ");
  }

  private AspectDefinition buildAspectDefinition(final Advice aspect, final Class aspectClass, final String pcd) {
    AspectDefinitionBuilder builder = new AspectDefinitionBuilder(aspectClass.getName(), DeploymentModel.PER_JVM,
                                                                  SPRING_ASPECT_CONTAINER, m_systemDef, m_loader);

    if (aspect instanceof MethodBeforeAdvice) {
      builder.addAdvice("before", pcd.toString(), s_methodBeforeAdviceMethodInfo.getName());
    } else if (aspect instanceof MethodInterceptor) {
      builder.addAdvice("around", pcd.toString(), s_methodInterceptorMethodInfo.getName());
    } else if (aspect instanceof AfterReturningAdvice) {
      builder
          .addAdvice("after returning(java.lang.Object)", pcd.toString(), s_afterReturningAdviceMethodInfo.getName());
    } else if (aspect instanceof ThrowsAdvice) {
      builder.addAdvice("after throwing(java.lang.Throwable+)", pcd.toString(), s_throwsAdviceMethodName);
    }
    builder.build();

    final AspectDefinition aspectDef = builder.getAspectDefinition();
    aspectDef.addParameter(SpringAspectContainer.BEAN_FACTORY_KEY, m_beanFactory);
    m_systemDef.addAspect(aspectDef);
    return aspectDef;
  }

  /**
   * @param aspectDef
   * @return the factory class name
   */
  private String createAspectFactory(final AspectDefinition aspectDef) {
    String aspectFactoryClassName = AspectFactoryManager.getAspectFactoryClassName(aspectDef.getClassName(), aspectDef
        .getQualifiedName());
    String aspectFactoryJavaName = aspectFactoryClassName.replace('/', '.');
    AspectFactoryManager.loadAspectFactory(aspectFactoryClassName, m_proxyName, aspectDef.getClassName().replace('.',
                                                                                                                 '/'),
                                           aspectDef.getQualifiedName(), null, null, m_loader, DeploymentModel.PER_JVM
                                               .toString());
    return aspectFactoryJavaName;
  }

  /**
   * @param aspect
   * @param aspectDef
   */
  private void initializeAspectFactory(final Advice aspect, final AspectDefinition aspectDef) {
    // create and load the factory for the aspect
    final String factoryName = createAspectFactory(aspectDef);
    try {
      Class factory = m_loader.loadClass(factoryName);
      Field aspectField = factory.getDeclaredField(TransformationConstants.FACTORY_SINGLE_ASPECT_FIELD_NAME);
      aspectField.set(null, aspect);
    } catch (Exception e) {
      throw new WrappedRuntimeException(e);
    }
  }
}
