/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.jmock.core.constraint.IsAnything;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.io.Resource;

import com.tc.aspectwerkz.joinpoint.StaticJoinPoint;

import java.util.LinkedList;

public class GetBeanProtocolTest extends MockObjectTestCase {

  private GetBeanProtocol            getBeanProtocol;

  private Mock                       mockStaticJoinPoint;

  private Mock                       mockBeanFactory;

  private StaticJoinPoint            jp;

  private AutowireCapableBeanFactory beanFactory;

  private Object                     distributedBean;

  private Object                     localBean;

  private Mock mockResource;

  private Resource resource;

  private Mock mockReader;

  private BeanDefinitionReader reader;

  interface WovenApplicationContext extends AutowireCapableBeanFactory, DistributableBeanFactory, BeanDefinitionRegistry {
    // 
  }

  protected void setUp() throws Exception {
    getBeanProtocol = new GetBeanProtocol();

    mockStaticJoinPoint = new Mock(StaticJoinPoint.class, "jp");
    jp = (StaticJoinPoint) mockStaticJoinPoint.proxy();

    mockBeanFactory = new Mock(WovenApplicationContext.class);
    beanFactory = (AutowireCapableBeanFactory) mockBeanFactory.proxy();
    distributedBean = new Object();
    localBean = new Object();
    mockReader = new Mock(BeanDefinitionReader.class);
    reader = (BeanDefinitionReader)mockReader.proxy();
  }
  
  public void testCaptureIdentity_default() throws Throwable {
    mockResource = new Mock(Resource.class);
    resource  = (Resource) mockResource.proxy();
    mockReader.expects(once()).method("getBeanFactory").will(returnValue(beanFactory));
    mockResource.expects(once()).method("getDescription").will(returnValue("classpath:tosomewhere"));
    mockBeanFactory.expects(once()).method("addLocation").with(eq("classpath:tosomewhere"));
    getBeanProtocol.captureIdentity(jp, resource, reader);
  }

// TODO - needs cglib.Mock
//  public void testCaptureIdentity_ClassPathResource() throws Throwable {
//    mockResource = new Mock(ClassPathResource.class);
//    resource  = (Resource) mockResource.proxy();
//    mockReader.expects(once()).method("getBeanFactory").will(returnValue(beanFactory));
//    mockResource.expects(once()).method("getPath").will(returnValue("classpath:tosomewhere"));
//    mockBeanFactory.expects(once()).method("addLocation").with(eq("classpath:tosomewhere"));
//    getBeanProtocol.captureIdentity(jp, resource, reader);
//  }

  public void testBeanNameCflow() throws Throwable {
    // mockBeanFactory.expects(once()).method("noteCreatingBean").with(eq("beanName"));
    mockBeanFactory.expects(once()).method("isDistributedBean").with(eq("beanName")).will(returnValue(true));
    mockBeanFactory.expects(once()).method("getBeanFromSingletonCache").with(eq("beanName")).will(returnValue(distributedBean));
    // mockBeanFactory.expects(once()).method("noteCreatedBean");
    mockStaticJoinPoint.expects(once()).method("proceed").will(returnValue(distributedBean));

    Object result = getBeanProtocol.beanNameCflow(jp, "beanName", beanFactory);
    assertSame(distributedBean, result);
  }

  public void testVirtualizeSingletonBean() throws Throwable {
    mockStaticJoinPoint.expects(once()).method("proceed").will(returnValue(localBean));
    mockBeanFactory.expects(once()).method("virtualizeSingletonBean").with(new IsAnything(), eq(localBean)).will(returnValue(distributedBean));
    ((LinkedList)getBeanProtocol.cflowStack.get()).addFirst("bean");
    Object result = getBeanProtocol.virtualizeSingletonBean(jp, beanFactory);
    assertSame(distributedBean, result);
  }
}
