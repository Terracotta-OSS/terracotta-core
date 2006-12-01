/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

/**
 * Implements support for Spring's
 * 
 * @Configurable annotation, using AW.
 * @author Jonas Bon&#233;r
 * @author Eugene Kuleshov
 */
public class ConfigurableAnnotationProtocol { // extends BeanConfigurerSupport {

  // public static interface RegisterBeanDefinitionParserJoinPoint extends StaticJoinPoint {
  // Object proceed(String elementName, BeanDefinitionParser parser);
  // }

  // public ConfigurableAnnotationProtocol() {
  // setBeanWiringInfoResolver(new AnnotationBeanWiringInfoResolver());
  // }

  /**
   * After Advice. Performs configuration of the bean after bean construction.
   */
  // public void afterBeanConstruction(Object beanInstance) {
  // configureBean(beanInstance);
  // }
  /**
   * org.springframework.aop.config.SpringConfiguredBeanDefinitionParser.getBeanConfigurerClass()
   */
  // public Object hijackBeanConfigurerClass(StaticJoinPoint jp) {
  // return getClass();
  // }
}
