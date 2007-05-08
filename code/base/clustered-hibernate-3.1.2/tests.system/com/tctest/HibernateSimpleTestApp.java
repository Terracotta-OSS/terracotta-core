package com.tctest;

import org.hibernate.Session;
import org.terracotta.modules.hibernate_3_1_2.util.HibernateUtil;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.test.HSqlDBServer;
import com.tc.util.Assert;
import com.tctest.domain.Account;
import com.tctest.domain.AccountIntf;
import com.tctest.domain.Customer;
import com.tctest.domain.Product;
import com.tctest.runner.AbstractTransparentApp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HibernateSimpleTestApp extends AbstractTransparentApp {
  private CyclicBarrier barrier;

  private Customer      cus;
  private Customer      cus2;
  private HSqlDBServer  dbServer = null;

  public HibernateSimpleTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.barrier();

      try {

        if (index == 0) {
          setupDatabaseResource();
        }

        barrier.barrier();
        
        insertData(index);

        singleNodeDeleteTest(index);

        insertData(index);

        singleNodeModifyTest(index);
        
        multiNodesShareTest(index);
        
        multiNodesLazyLoadingTest(index);
      } finally {
        if (index == 0) {
          shutdownDatabase();
        }
      }

    } catch (Throwable e) {
      notifyError(e);
    }

  }

  private void singleNodeDeleteTest(int index) throws Exception {
    Customer cus1 = (Customer) loadByIdentifier(Customer.class, 1);

    assertEqualCustomer(cus1);
    
    commitTransaction();

    barrier.barrier();

    if (index == 0) {
      cus1 = (Customer) loadByIdentifier(Customer.class, 1);
      delete(cus1);
    }
    barrier.barrier();

    cus1 = (Customer) getByIdentifier(Customer.class, 1);
    Assert.assertNull(cus1);

    barrier.barrier();
  }

  private void singleNodeModifyTest(int index) throws Exception {
    Customer cus1 = (Customer) loadByIdentifier(Customer.class, 2);

    assertEqualCustomer(cus1);
    
    commitTransaction();

    barrier.barrier();

    if (index == 0) {
      cus1 = (Customer) loadByIdentifier(Customer.class, 2);
      synchronized (cus1) {
        cus1.setEmailAddress("asi@gmail.com");
      }
      saveOrUpdate(cus1);
    }

    barrier.barrier();

    cus1 = (Customer) loadByIdentifier(Customer.class, 2);
    assertEqualNewEmailCustomer(cus1);

    barrier.barrier();
  }
  
  private void multiNodesShareTest(int index) throws Exception {
    Assert.assertNull(cus);
    
    barrier.barrier();
    
    if (index == 0) {
      cus = (Customer) loadByIdentifier(Customer.class, 2);
    }
    barrier.barrier();
    
    assertEqualNewEmailCustomer(cus);
    
    barrier.barrier();
    
    if (index == 1) {
      commitTransaction();
      synchronized(cus) {
        cus.setEmailAddress("asi@yahoo.com");
      }
      saveOrUpdate(cus);
    }
    
    barrier.barrier();
    
    assertEqualCustomer(cus);
    
    barrier.barrier();
  }
  
  private void multiNodesLazyLoadingTest(int index) throws Exception {
    if (index == 0) {
      cus2 = (Customer) loadByIdentifier(Customer.class, 2);
    }
    barrier.barrier();

    if (index == 1) {
      AccountIntf account = cus2.getAccount();
      startTransaction();
      Assert.assertEquals("ASI-001", account.getNumber());
    }
    
    barrier.barrier();
  }

  private Object loadByIdentifier(Class clazz, int id) throws Exception {
    Session session = HibernateUtil.getSessionFactory().getCurrentSession();
    session.beginTransaction();
    return session.load(clazz, new Integer(id));
  }

  private Object getByIdentifier(Class clazz, int id) {
    Session session = HibernateUtil.getSessionFactory().getCurrentSession();
    session.beginTransaction();
    return session.get(clazz, new Integer(id));
  }

  private void delete(Object o) throws Exception {
    Session session = HibernateUtil.getSessionFactory().getCurrentSession();
    session.beginTransaction();
    session.delete(o);

    session.getTransaction().commit();
  }

  private void save(Object o) throws Exception {
    Session session = HibernateUtil.getSessionFactory().getCurrentSession();
    session.beginTransaction();
    session.save(o);
    session.getTransaction().commit();
  }

  private void saveOrUpdate(Object o) throws Exception {
    Session session = HibernateUtil.getSessionFactory().getCurrentSession();
    session.beginTransaction();
    session.saveOrUpdate(o);
    session.getTransaction().commit();
  }

  private void startTransaction() throws Exception {
    Session session = HibernateUtil.getSessionFactory().getCurrentSession();
    session.beginTransaction();
  }

  private void commitTransaction() throws Exception {
    Session session = HibernateUtil.getSessionFactory().getCurrentSession();
    session.getTransaction().commit();
  }

  private void assertEqualCustomer(Customer customer) {
    Account acc = customer.getAccount();
    System.err.println("Client id: " + ManagerUtil.getClientID() + ", customer: " + customer);
    System.err.println("Client id: " + ManagerUtil.getClientID() + ", account: " + acc);
    Assert.assertEquals("ASI-001", acc.getNumber());

    Assert.assertEquals("asi@yahoo.com", customer.getEmailAddress());
    Assert.assertEquals("Antonio", customer.getFirstName());
    Assert.assertEquals("Si", customer.getLastName());
  }

  private void assertEqualNewEmailCustomer(Customer customer) {
    Account acc = customer.getAccount();
    System.err.println("Client id: " + ManagerUtil.getClientID() + ", customer: " + customer);
    System.err.println("Client id: " + ManagerUtil.getClientID() + ", account: " + acc);
    Assert.assertEquals("ASI-001", acc.getNumber());

    Assert.assertEquals("asi@gmail.com", customer.getEmailAddress());
    Assert.assertEquals("Antonio", customer.getFirstName());
    Assert.assertEquals("Si", customer.getLastName());
  }

  private void insertData(int index) throws Exception {
    if (index == 0) {
      Account acc = new Account();
      acc.setNumber("ASI-001");
      List accounts = new ArrayList();
      accounts.add(acc);

      Customer cus = new Customer();

      Set products = new HashSet();
      Product prod = new Product();
      prod.setNumber("prod-001");
      prod.setCustomer(cus);
      products.add(prod);
      prod = new Product();
      prod.setNumber("prod-002");
      prod.setCustomer(cus);
      products.add(prod);

      cus.setEmailAddress("asi@yahoo.com");
      cus.setFirstName("Antonio");
      cus.setLastName("Si");
      cus.setAccount(acc);
      cus.setProducts(products);
      save(cus);
    }
    barrier.barrier();
  }

  private void setupDatabaseResource() throws Exception {
    connectDatabase();

    Session session = HibernateUtil.getSessionFactory().getCurrentSession();

    session.beginTransaction();

    Connection conn = session.connection();
    PreparedStatement stmt = conn
        .prepareStatement("create table ACCOUNT (ACC_ID integer generated by default as identity (start with 1), ACC_NUMBER varchar(255), primary key (ACC_ID))");
    stmt.execute();

    stmt = conn
        .prepareStatement("create table CUSTOMER (CUS_ID integer generated by default as identity (start with 1), CUS_FIRST_NAME varchar(255), CUS_LAST_NAME varchar(255), CUS_EMAIL varchar(255), acc_id integer, primary key (CUS_ID), unique (acc_id))");
    stmt.execute();

    stmt = conn
        .prepareStatement("alter table CUSTOMER add constraint FK52C76FDE76EB25DC foreign key (acc_id) references ACCOUNT");
    stmt.execute();

    stmt = conn
        .prepareStatement("create table PRODUCT (PROD_ID integer generated by default as identity (start with 1), PROD_NUMBER varchar(255), primary key (PROD_ID))");
    stmt.execute();

    stmt = conn
        .prepareStatement("create table Customer_Products (PROD_ID integer not null, CUS_ID integer not null, primary key (PROD_ID, CUS_ID))");
    stmt.execute();

    stmt = conn
        .prepareStatement("alter table Customer_Products add constraint FKEFFD20A5FFC78061 foreign key (CUS_ID) references CUSTOMER");
    stmt.execute();

    stmt = conn
        .prepareStatement("alter table Customer_Products add constraint FKEFFD20A5ED0B4608 foreign key (PROD_ID) references PRODUCT");
    stmt.execute();

    session.getTransaction().commit();
  }

  private void connectDatabase() throws Exception {
    dbServer = new HSqlDBServer();
    dbServer.start();
  }

  private void shutdownDatabase() throws Exception {
    dbServer.stop();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = HibernateSimpleTestApp.class.getName();

    config.getOrCreateSpec(testClass).addRoot("barrier", "barrier").addRoot("cus", "cus").addRoot("cus2", "cus2");

    config.addWriteAutolock("* " + testClass + "*.*(..)");
    config.addIncludePattern("com.tctest.domain.Account");
    config.addIncludePattern("com.tctest.domain.Customer");
    config.addIncludePattern("com.tctest.domain.Product");
    new CyclicBarrierSpec().visit(visitor, config);

    config.addNewModule("clustered-hibernate-3.1.2", "1.0.0");
    config.addNewModule("clustered-cglib-2.1.3", "1.0.0");
    //config.addNewModule("clustered-ehcache-1.2.4", "1.0.0");
  }

}
