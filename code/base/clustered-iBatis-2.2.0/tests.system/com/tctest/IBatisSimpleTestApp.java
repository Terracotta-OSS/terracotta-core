package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.ibatis.common.resources.Resources;
import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapClientBuilder;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.domain.Account;
import com.tctest.domain.Customer;
import com.tctest.runner.AbstractTransparentApp;

import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class IBatisSimpleTestApp extends AbstractTransparentApp {
  private CyclicBarrier barrier;

  /**
   * SqlMapClient instances are thread safe, so you only need one. In this case,
   * we'll use a static singleton. So sue me. ;-)
   */
  private SqlMapClient sqlMapper;

  private Customer cus;
  
  private static boolean pluginsLoaded = false;

  public static synchronized boolean pluginsLoaded() {
    return pluginsLoaded;
  }

  public IBatisSimpleTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int id = barrier.barrier();

      if (id == 0) {

        setupDatabaseResource();
        
        Account acc = new Account();
        acc.setNumber("ASI-001");
        insertAccount(acc);
        Customer cus = new Customer();
        cus.setEmailAddress("asi@yahoo.com");
        cus.setFirstName("Antonio");
        cus.setLastName("Si");
        cus.setAccount(acc);
        insertCustomer(cus);
        

      }

      barrier.barrier();

      if (id == 0) {
        cus = selectCustomerById(0);

        shutdownDatabase();
      }
      barrier.barrier();

      if (id == 1) {
        Account acc = cus.getAccount();
        Assert.assertEquals("ASI-001", acc.getNumber());
      }

      barrier.barrier();

    } catch (Throwable e) {
      e.printStackTrace(System.err);
    }

  }

  private void setupDatabaseResource() throws Exception {
    Reader reader = Resources.getResourceAsReader("com/tctest/SqlMapConfig.xml");
    sqlMapper = SqlMapClientBuilder.buildSqlMapClient(reader);
    reader.close();

    
    Connection conn = sqlMapper.getDataSource().getConnection();
    PreparedStatement stmt = conn
        .prepareStatement("create table ACCOUNT (acc_id int not null, acc_number varchar(80) null, constraint pk_acc_id primary key (acc_id))");
    stmt.execute();

    stmt = conn
        .prepareStatement("create table CUSTOMER (cus_id int not null, cus_first_name varchar(80) null, cus_last_name varchar(80) null, cus_email varchar(80) null, cus_account_id varchar(80) null, constraint pk_cus_id primary key (cus_id))");
    stmt.execute();
  }

  private void shutdownDatabase() throws Exception {
    Connection conn = sqlMapper.getDataSource().getConnection();
    PreparedStatement stmt = conn.prepareStatement("shutdown immediately");
    stmt.execute();
  }

  public Account selectAccountById(int id) throws SQLException {
    return (Account) sqlMapper.queryForObject("selectAccountById", new Integer(id));
  }

  public Customer selectCustomerById(int id) throws SQLException {
    return (Customer) sqlMapper.queryForObject("selectCustomerById", new Integer(id));
  }

  public void insertAccount(Account acc) throws SQLException {
    sqlMapper.insert("insertAccount", acc);
  }

  public void insertCustomer(Customer customer) throws SQLException {
    sqlMapper.insert("insertCustomer", customer);
  }

  public void updateAccount(Account account) throws SQLException {
    sqlMapper.update("updateAccount", account);
  }

  public void deleteAccount(int id) throws SQLException {
    sqlMapper.delete("deleteAccount", new Integer(id));
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    System.err.println("Visiting visitL1DSOConfig");
    String testClass = IBatisSimpleTestApp.class.getName();

    config.getOrCreateSpec(testClass) //
        .addRoot("barrier", "barrier") //
        .addRoot("cus", "cus");

    config.addWriteAutolock("* " + testClass + "*.*(..)");
    config.addIncludePattern("com.tctest.domain.Account");
    config.addIncludePattern("com.tctest.domain.Customer");
    new CyclicBarrierSpec().visit(visitor, config);
    
    config.addNewModule("clustered-cglib", "2.1.3");
    config.addNewModule("clustered-iBatis", "2.2.0");
    pluginsLoaded = true;
  }

}
