/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.TransparentTestBase;
import com.tctest.runner.AbstractTransparentApp;
import com.tcspring.beans.orm.data.CustomerDao;
import com.tcspring.beans.orm.domain.Customer;
//import com.tcspring.beans.orm.domain.CustomerAddress;
//import com.tcspring.beans.orm.domain.Permission;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class Hibernate_Test extends TransparentTestBase {
  private static final int LOOP_ITERATIONS = 1;
  private static final int EXECUTION_COUNT = 1;
  private static final int NODE_COUNT      = 1;

  public Hibernate_Test() {
    disableAllUntil("2008-01-01");
  }

  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return Hibernate_TestApp.class;
  }

  public static class Hibernate_TestApp extends AbstractTransparentApp {

    private ApplicationContext context;

    private List shared = new ArrayList();

    public Hibernate_TestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public void run() {
      context = new ClassPathXmlApplicationContext("com/tctest/spring/hibernate-test-application-context.xml");
      CustomerDao customerDao = (CustomerDao) context.getBean("customerDao");
      assertNotNull(customerDao);
      AbstractPlatformTransactionManager txMgr = (AbstractPlatformTransactionManager)context.getBean("transactionManager");
      TransactionStatus status = txMgr.getTransaction(new DefaultTransactionDefinition());
      
      // FIXME how to test the AW and UoW code?
      // FIXME how to test the sharing code?
      
      List customers = customerDao.getAll();
      for (Iterator i = customers.iterator(); i.hasNext();) {
        Customer customer = (Customer) i.next();
        assertNotNull(customer);
      }

      customers = customerDao.getAllWithOnlyOnePermission();
      for (Iterator i = customers.iterator(); i.hasNext();) {
        Customer customer = (Customer) i.next();
        assertNotNull(customer);
      }

      Customer customer = new Customer();
      customer.setFirstName("Jack");
      customer.setLastName("Schitt");
      customer.setPermissions(new HashSet());
      customer.setAddresses(new HashSet());
      
      customerDao.save(customer);

      txMgr.commit(status);
      
//      status = txMgr.getTransaction(new DefaultTransactionDefinition());
//      
//      CustomerAddress ca = new CustomerAddress();
//      ca.setLine1("1");
//      ca.setLine2("2");
//      ca.setCity("Manchester");
//      ca.setPostCode("M1 xx");
//
//      customer.getAddresses().add(ca);
//      
//      customerDao.save(customer);
//      
//      customers = customerDao.getAll();
//      for (Iterator i = customers.iterator(); i.hasNext();) {
//        customer = (Customer) i.next();
//        assertNotNull(customer);
//      }      
//
//      
//      Permission p = new Permission();
//      p.setPermissionId(1);
//
//      customer.getPermissions().add(p);
//
//      customerDao.save(customer);
//
//      txMgr.commit(status);
    }

    protected Class getApplicationClass() {
      return Hibernate_TestApp.class;
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String name = Hibernate_TestApp.class.getName();
      config.addRoot(name, "shared", "shared", false);
      config.addAutolock("* " + name + ".run()", ConfigLockLevel.WRITE);
      config.addIncludePattern("com.tcspring.beans.orm.hibernate..*", false, false, false);
      config.addIncludePattern(TransparentTestBase.class.getName());
      config.addIncludePattern(Hibernate_TestApp.class.getName());
      
      // config.addIsolatedClass("org.springframework.");
      // config.addIsolatedClass("com.tctest.spring.orm.");
      // config.addIsolatedClass("com.tcspring.");
    }
  }
}
