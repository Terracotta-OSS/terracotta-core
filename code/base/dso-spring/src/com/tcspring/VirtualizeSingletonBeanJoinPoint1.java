/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;

import com.tc.aspectwerkz.joinpoint.StaticJoinPoint;

/**
 * Version for Spring 1.x
 */
public interface VirtualizeSingletonBeanJoinPoint1 extends StaticJoinPoint {

  Object proceed(Object bean, String beanName, AbstractAutowireCapableBeanFactory beanFactory);

}
