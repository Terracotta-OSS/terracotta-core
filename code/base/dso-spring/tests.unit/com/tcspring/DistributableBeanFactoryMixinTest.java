/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.ChildBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.hook.DSOContext;
import com.tc.object.config.DSOSpringConfigHelper;
import com.tcspring.DistributableBeanFactoryMixin.ManagerUtilWrapper;
import com.tcspring.beans.SimpleBean;
import com.tcspring.beans.SimpleBean1;
import com.tcspring.beans.SimpleBean2;
import com.tcspring.beans.SimpleParentBean;
import com.tcspring.beans.SimplePropertyBean;
import com.tcspring.events.BaseEvent;
import com.tcspring.events.ChildEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DistributableBeanFactoryMixinTest extends MockObjectTestCase {
  private String                        appName           = "testApp";
  private DSOContext                    dsoContext;
  private Mock                          dsoContextMock;

  private DistributableBeanFactoryMixin distributableBeanFactoryMixin;
  private Mock                          mockSpringConfigHelper;
  private DSOSpringConfigHelper         springConfigHelper;
  private Mock                          mockManagerUtilWrapper;
  private Set                           nonDistributables = new HashSet();

  private HashMap                       singletonCache;

  private Object                        localBean;
  private Object                        distributedBean;

  protected void setUp() {
    dsoContextMock = mock(DSOContext.class);
    dsoContext = (DSOContext) dsoContextMock.proxy();

    mockManagerUtilWrapper = new Mock(DistributableBeanFactoryMixin.ManagerUtilWrapper.class);
    ManagerUtilWrapper managerUtilWrapper = (ManagerUtilWrapper) mockManagerUtilWrapper.proxy();
    distributableBeanFactoryMixin = new DistributableBeanFactoryMixin(appName, dsoContext, managerUtilWrapper, nonDistributables);

    mockSpringConfigHelper = new Mock(DSOSpringConfigHelper.class);
    springConfigHelper = (DSOSpringConfigHelper) mockSpringConfigHelper.proxy();

    singletonCache = new HashMap();

    // tcClassMock = new Mock(TCClass.class);
    // tcClass = (TCClass) tcClassMock.proxy();
    //
    // tcSuperClassMock = new Mock(TCClass.class);
    //
    // tcObjectMock = new Mock(TCObject.class);
    //
    // tcFieldMock = new Mock(TCField.class);
    // tcField = (TCField) tcFieldMock.proxy();
    //
    // tcObject = (TCObject) tcObjectMock.proxy();
    //
    // localBean = new TestObject();
    // distributedBean = new TestObject();

  }

  public void testGetBeanClassName() {
    AbstractBeanDefinition rootDefinition = new RootBeanDefinition();
    Map beanMap = new HashMap();

    assertNull(DistributableBeanFactoryMixin.getBeanClassName(rootDefinition, beanMap));

    beanMap.put("parent1", rootDefinition);

    AbstractBeanDefinition childDefinition = new ChildBeanDefinition("parent1");
    assertNull(DistributableBeanFactoryMixin.getBeanClassName(childDefinition, beanMap));

    rootDefinition.setBeanClassName("class1");
    assertEquals("class1", DistributableBeanFactoryMixin.getBeanClassName(rootDefinition, beanMap));
    assertEquals("class1", DistributableBeanFactoryMixin.getBeanClassName(childDefinition, beanMap));
  }

  public void testAddDistributedEvents() {
    String eventClass = "com.tcspring.events.ChildEvent";
    String baseEventClass = "com.tcspring.events.BaseEvent";

    dsoContextMock.expects(once()).method("addInclude") //
        .with(eq(eventClass), ANYTHING, eq("* " + eventClass + ".*(..)"), ANYTHING);
    dsoContextMock.expects(once()).method("addInclude") //
        .with(eq(baseEventClass), ANYTHING, eq("* " + baseEventClass + ".*(..)"), ANYTHING);
    distributableBeanFactoryMixin.registerDistributedEvents(Collections.singletonList(eventClass));
  }

  public void testAddDistributedEventsWithWildcard() {
    String eventClass = "com.tcspring.events.*Event"; // XXX we don't support this kind of wildcards!
    distributableBeanFactoryMixin.registerDistributedEvents(Collections.singletonList(eventClass));
  }

  public void testRegisterBeanDefinitions() {
    distributableBeanFactoryMixin.addLocation("config/foo.xml");

    dsoContextMock.expects(once()).method("getDSOSpringConfigHelpers").withNoArguments()
        .will(returnValue(Collections.singletonList(springConfigHelper)));
    mockSpringConfigHelper.expects(atLeastOnce()).method("isMatchingApplication").with(eq("testApp"))
        .will(returnValue(true));
    mockSpringConfigHelper.expects(atLeastOnce()).method("isMatchingConfig").with(eq("config/foo.xml"))
        .will(returnValue(true));
    mockSpringConfigHelper.expects(atLeastOnce()).method("getDistributedEvents").withNoArguments()
        .will(returnValue(Collections.singletonList(ChildEvent.class.getName())));
    mockSpringConfigHelper.expects(atLeastOnce()).method("getDistributedBeans").withNoArguments()
        .will(returnValue(Collections.singletonMap("bean", Collections.singleton("transientX"))));

    expectsAddInclude(SimplePropertyBean.class.getName());
    expectsAddInclude(SimpleBean.class.getName());
    expectsAddInclude(SimpleParentBean.class.getName());
    expectsAddInclude(ChildEvent.class.getName());
    expectsAddInclude(BaseEvent.class.getName());
    expectsAddInclude(SimpleBean1.class.getName());
    expectsAddInclude(SimpleBean2.class.getName());

    dsoContextMock.expects(once()).method("addTransient").with(eq(SimplePropertyBean.class.getName()), eq("transientX"));

    mockManagerUtilWrapper.expects(once()).method("beginLock").with(ANYTHING, eq(Manager.LOCK_TYPE_WRITE));
    mockManagerUtilWrapper.expects(once()).method("lookupOrCreateRoot").with(ANYTHING, ANYTHING)
        .will(returnValue(singletonCache));
    mockManagerUtilWrapper.expects(once()).method("commitLock").with(ANYTHING);

    mockSpringConfigHelper.expects(atLeastOnce()).method("getRootName").will(returnValue(null));
    mockSpringConfigHelper.expects(atLeastOnce()).method("isLocationInfoEnabled").will(returnValue(false));
    
    distributableBeanFactoryMixin.registerBeanDefinitions(Collections.singletonMap("bean",
                                                                  new RootBeanDefinition(SimplePropertyBean.class)));
  }

  private void expectsAddInclude(String name) {
    dsoContextMock.expects(once()).method("addInclude").with(eq(name), ANYTHING, eq(("* " + name + ".*(..)")), ANYTHING);
  }

  public void testIsDistibutedBean_yes() {
    testRegisterBeanDefinitions();
    mockSpringConfigHelper.expects(once()).method("isDistributedBean").with(eq("bean")).will(returnValue(true));
    assertTrue(distributableBeanFactoryMixin.isDistributedBean("bean"));
  }

  public void testIsDistibutedBean_no() {
    testRegisterBeanDefinitions();
    mockSpringConfigHelper.expects(once()).method("isDistributedBean").with(eq("someOtherBean")).will(returnValue(false));
    assertFalse(distributableBeanFactoryMixin.isDistributedBean("someOtherBean"));
  }

  public void testIsDistributedField_yes() {
    testRegisterBeanDefinitions();
    mockSpringConfigHelper.expects(once()).method("isDistributedField").with(eq("bean"), eq("fieldA"))
        .will(returnValue(true));
    assertTrue(distributableBeanFactoryMixin.isDistributedField("bean", "fieldA"));
  }

  public void testIsDistributedField_no() {
    testRegisterBeanDefinitions();
    mockSpringConfigHelper.expects(once()).method("isDistributedField").with(eq("bean"), eq("fieldB"))
        .will(returnValue(false));
    assertFalse(distributableBeanFactoryMixin.isDistributedField("bean", "fieldB"));
  }

  public void testIsDistributedEvent_yes() {
    testRegisterBeanDefinitions();
    mockSpringConfigHelper.expects(once()).method("isDistributedEvent").with(eq("event1")).will(returnValue(true));
    assertTrue(distributableBeanFactoryMixin.isDistributedEvent("event1"));
  }

  public void testIsDistributedEvent_no() {
    testRegisterBeanDefinitions();
    mockSpringConfigHelper.expects(once()).method("isDistributedEvent").with(eq("event2")).will(returnValue(false));
    assertFalse(distributableBeanFactoryMixin.isDistributedEvent("event2"));

  }

  // public void testCopyTransientField_NotPortableNotDistributed() throws IllegalAccessException {
  // tcClassMock.expects(once()).method("getSuperclass").will(returnValue(null));
  // tcClassMock.expects(once()).method("getField").with(eq(ClassWithIntField.class.getName() + ".foo"))
  // .will(returnValue(tcField));
  // tcFieldMock.expects(once()).method("isPortable").will(returnValue(false));
  // // beanFactoryMock.expects(once()).method("isDistributedField").with(eq(beanName),
  // // eq("foo")).will(returnValue(false));
  //
  // ClassWithIntField sourceBean = new ClassWithIntField(99);
  // ClassWithIntField targetBean = new ClassWithIntField(-1);
  //
  // identifiable.copyTransientFields(beanName, sourceBean, targetBean, ClassWithIntField.class, tcClass);
  //
  // assertEquals(99, targetBean.foo);
  // }
  //
  // public void testCopyTransientField_PortableNotDistributed() throws IllegalAccessException {
  // tcClassMock.expects(once()).method("getSuperclass").will(returnValue(null));
  // tcClassMock.expects(once()).method("getField").with(eq(ClassWithIntField.class.getName() + ".foo"))
  // .will(returnValue(tcField));
  // tcFieldMock.expects(once()).method("isPortable").will(returnValue(true));
  // // identifiableMock.expects(once()).method("isDistributedField").with(eq(beanName), eq("foo"))
  // // .will(returnValue(false));
  //
  // ClassWithIntField sourceBean = new ClassWithIntField(99);
  // ClassWithIntField targetBean = new ClassWithIntField(-1);
  //
  // identifiable.copyTransientFields(beanName, sourceBean, targetBean, ClassWithIntField.class, tcClass);
  //
  // assertEquals(99, targetBean.foo);
  // }
  //
  // public void testCopyTransientField_dsoTransientDistributed() throws IllegalAccessException {
  // tcClassMock.expects(once()).method("getSuperclass").will(returnValue(null));
  // tcClassMock.expects(once()).method("getField").with(eq(ClassWithIntField.class.getName() + ".foo"))
  // .will(returnValue(null));
  // // beanFactoryMock.expects(once()).method("isDistributedField").with(eq(beanName),
  // // eq("foo")).will(returnValue(true));
  //
  // ClassWithIntField sourceBean = new ClassWithIntField(98);
  // ClassWithIntField targetBean = new ClassWithIntField(-1);
  //
  // identifiable.copyTransientFields(beanName, sourceBean, targetBean, ClassWithIntField.class, tcClass);
  //
  // assertEquals(98, targetBean.foo);
  // }
  //
  // public void testCopyTransientField_dsoTransientNonDistributed() throws IllegalAccessException {
  // tcClassMock.expects(once()).method("getSuperclass").will(returnValue(null));
  // tcClassMock.expects(once()).method("getField").with(eq(ClassWithIntField.class.getName() + ".foo"))
  // .will(returnValue(null));
  // // beanFactoryMock.expects(once()).method("isDistributedField").with(eq(beanName),
  // // eq("foo")).will(returnValue(false));
  //
  // ClassWithIntField sourceBean = new ClassWithIntField(97);
  // ClassWithIntField targetBean = new ClassWithIntField(-1);
  //
  // identifiable.copyTransientFields(beanName, sourceBean, targetBean, ClassWithIntField.class, tcClass);
  //
  // assertEquals(97, targetBean.foo);
  // }
  //
  // public void testCopyTransientField_PortableNonDistributed() throws IllegalAccessException {
  // tcClassMock.expects(once()).method("getSuperclass").will(returnValue(null));
  // tcClassMock.expects(once()).method("getField").with(eq(ClassWithIntField.class.getName() + ".foo"))
  // .will(returnValue(tcField));
  // tcFieldMock.expects(once()).method("isPortable").will(returnValue(true));
  //
  // ClassWithIntField sourceBean = new ClassWithIntField(96);
  // ClassWithIntField targetBean = new ClassWithIntField(-1);
  //
  // identifiable.copyTransientFields(beanName, sourceBean, targetBean, ClassWithIntField.class, tcClass);
  //
  // assertEquals(96, targetBean.foo);
  // }
  //
  // public void testCopyTransientField_final() throws IllegalAccessException {
  // tcClassMock.expects(once()).method("getSuperclass").will(returnValue(null));
  // identifiable.copyTransientFields(beanName, new Object(), new Object(), ClassWithFinalField.class, tcClass);
  // }
  //
  // public void testCopyTransientField_static() throws IllegalAccessException {
  // tcClassMock.expects(once()).method("getSuperclass").will(returnValue(null));
  // identifiable.copyTransientFields(beanName, new Object(), new Object(), ClassWithStaticField.class, tcClass);
  // }
  //
  // public void testCopyTransientField_tcField() throws IllegalAccessException {
  // tcClassMock.expects(once()).method("getSuperclass").will(returnValue(null));
  // identifiable.copyTransientFields(beanName, new Object(), new Object(), ClassWithTCField.class, tcClass);
  // }
  //
  // public void testCopyTransientField_distributed() throws IllegalAccessException {
  // testRegisterBeanDefinitions();
  //
  // tcClassMock.expects(once()).method("getSuperclass").will(returnValue(null));
  // tcClassMock.expects(once()).method("getField").with(eq(ClassWithIntField.class.getName() + ".foo"))
  // .will(returnValue(tcField));
  // tcFieldMock.expects(once()).method("isPortable").will(returnValue(true));
  // mockSpringConfigHelper.expects(once()).method("isDistributedField").with(eq(beanName), eq("foo"))
  // .will(returnValue(true));
  //
  // identifiable.copyTransientFields(beanName, new Object(), new Object(), ClassWithIntField.class, tcClass);
  // }

  static class ClassWithTCField {
    static int _tc_foo = 1;
  }

  class ClassWithFinalField {
    final int foo = 1;
  }

  static class ClassWithStaticField {
    static int foo = 1;
  }

  class ClassWithIntField {
    int foo;

    public ClassWithIntField(int foo) {
      this.foo = foo;
    }
  }

  // class TestObject implements Manageable {
  //
  // public void __tc_managed(TCObject t) {
  // throw new RuntimeException();
  //
  // }
  //
  // public TCObject __tc_managed() {
  // return tcObject;
  // }
  //
  // }

//  public void testVirtualizeSingletonBeanWhenNotYetShared() {
//    testRegisterBeanDefinitions();
//
//    // mockSpringConfigHelper.expects(once()).method("isDistributedBean").with(eq("beanName")).will(returnValue(true));
//
//    ComplexBeanId beanId = new ComplexBeanId("beanName");
//    
//    distributableBeanFactoryMixin.virtualizeBean(beanId, localBean, container);
//    assertSame(localBean, result);
//    assertSame(localBean, distributableBeanFactoryMixin.getBeanContainer(beanId).getBean());
//  }

//  public void testVirtualizeSingletonBeanWhenAlreadyShared() {
//    testRegisterBeanDefinitions();
//
//    ComplexBeanId beanId = new ComplexBeanId("beanName");
//
//    singletonCache.put(beanId, new BeanContainer(distributedBean, true));
//    
//    // mockSpringConfigHelper.expects(once()).method("isDistributedBean").with(eq("beanName")).will(returnValue(true));
//
//    distributableBeanFactoryMixin.virtualizeBean(beanId, localBean, container);
//    assertSame(distributedBean, result);
//    assertSame(distributedBean, distributableBeanFactoryMixin.getBeanContainer(beanId).getBean());
//  }

//  public void testVirtualizeSingletonBeanWhenNotDistributed() {
//    testRegisterBeanDefinitions();
//
//    ComplexBeanId beanId = new ComplexBeanId("beanName");
//
//    // mockSpringConfigHelper.expects(once()).method("isDistributedBean").with(eq("beanName")).will(returnValue(false));
//
//    Object result = distributableBeanFactoryMixin.virtualizeBean(beanId, localBean);
//    assertSame(localBean, result);
//    assertNull(distributableBeanFactoryMixin.getBeanContainer(beanId));
//  }

  // TODO - mock the monitor code
}
