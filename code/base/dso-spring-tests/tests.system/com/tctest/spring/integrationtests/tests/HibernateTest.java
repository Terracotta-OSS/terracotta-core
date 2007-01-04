/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;
import org.springframework.web.servlet.DispatcherServlet;

import com.tc.test.server.AbstractDBServer;
import com.tctest.spring.bean.IHibernateBean;
import com.tctest.spring.integrationtests.framework.AbstractTwoServerDeploymentTest;
import com.tctest.spring.integrationtests.framework.DeploymentBuilder;
import com.tctest.spring.integrationtests.framework.ProxyBuilder;
import com.tctest.spring.integrationtests.framework.ServerManagerUtil;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;


/**
 * 
 */
public class HibernateTest extends AbstractTwoServerDeploymentTest {
  private static IHibernateBean   hibernateBean1;
  private static IHibernateBean   hibernateBean2;
  
  public HibernateTest() {
    super();
    this.disableTestUntil("testLazyObj", "2010-03-01");
    this.disableTestUntil("testLazyChild", "2010-03-01");
  }
  
  public void testSharePersitentObj() throws Exception {
    hibernateBean1.sharePersistentObj();
    assertEquals("Failed to share persistent object", hibernateBean1.getSharedId(), hibernateBean2.getSharedId());
    assertEquals("Failed to share persistent object", hibernateBean1.getSharedFld(), hibernateBean2.getSharedFld());
  }
  
  public void testShareDetachedObj() throws Exception {
    hibernateBean1.shareDetachedObj();
    assertEquals("Failed to share detached object", hibernateBean1.getSharedId(), hibernateBean2.getSharedId());
    assertEquals("Failed to share detached object", hibernateBean1.getSharedFld(), hibernateBean2.getSharedFld());
  }
  
  public void testLazyObj() throws Exception {
    hibernateBean1.shareLazyObj();
    assertEquals("Failed to share lazy object", hibernateBean1.getSharedId(), hibernateBean2.getSharedId());
    assertEquals("Failed to share lazy object", hibernateBean1.getSharedFld(), hibernateBean2.getSharedFld());
  }
  
  public void testLazyChild() throws Exception {
    hibernateBean1.shareObjWithLazyChild();
    assertEquals("Failed to share lazy object", hibernateBean1.getSharedId(), hibernateBean2.getSharedId());
    assertEquals("Failed to share lazy object", hibernateBean1.getSharedFld(), hibernateBean2.getSharedFld());
  }
  
  public void testAssociateDetachedObject() throws Exception {
    hibernateBean1.sharePersistentObj();
    hibernateBean2.associateSharedObj();
    
    assertEquals("Failed to share lazy object", hibernateBean1.getSharedId(), hibernateBean2.getSharedId());
    assertEquals("Failed to share lazy object", hibernateBean1.getSharedFld(), hibernateBean2.getSharedFld());
    
    hibernateBean1.shareDetachedObj();
    hibernateBean2.associateSharedObj();
    
    assertEquals("Failed to share lazy object", hibernateBean1.getSharedId(), hibernateBean2.getSharedId());
    assertEquals("Failed to share lazy object", hibernateBean1.getSharedFld(), hibernateBean2.getSharedFld());
  }
  
  
  private static class HibernateTestSetup extends TwoSvrSetup {
    private static final int DB_PORT = 0; // will use the default port - 9001
    private static final String DB_NAME = "testdb";
    private static final String APP_NAME = "test-hibernate";
    private static final String REMOTE_SERVICE_NAME = "hibernateservice";
    

    private HibernateTestSetup() {
      super(HibernateTest.class, "/tc-config-files/hibernate-tc-config.xml", APP_NAME);
    }

    protected void setUp() throws Exception {
      try {
        sm = ServerManagerUtil.startAndBind(HibernateTest.class, isWithPersistentStore());       
        AbstractDBServer dbSvr = sm.makeDBServer("HSQL", DB_NAME, DB_PORT);
        dbSvr.start();
        setUpTwoWebAppServers();
        
        Map initCtx = new HashMap(); 
        initCtx.put(ProxyBuilder.EXPORTER_TYPE_KEY, HttpInvokerServiceExporter.class);
        hibernateBean1 = (IHibernateBean) server1.getProxy(IHibernateBean.class, APP_NAME + "/http/" + REMOTE_SERVICE_NAME, initCtx);
        hibernateBean2 = (IHibernateBean) server2.getProxy(IHibernateBean.class, APP_NAME + "/http/" + REMOTE_SERVICE_NAME, initCtx);
      } catch(Exception ex) {
        ex.printStackTrace(); throw ex;
      }
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addBeanDefinitionFile("classpath:/com/tctest/spring/beanfactory-hibernate.xml");
      builder.addRemoteService(HttpInvokerServiceExporter.class,REMOTE_SERVICE_NAME, "hibernate-bean", IHibernateBean.class);
      builder.setDispatcherServlet("httpinvoker", "/http/*", DispatcherServlet.class, null, true);
      builder.addDirectoryOrJARContainingClass(org.hibernate.Session.class);
      builder.addDirectoryOrJARContainingClass(org.dom4j.DocumentException.class);
      builder.addDirectoryOrJARContainingClass(org.apache.xerces.jaxp.SAXParserFactoryImpl.class);
      builder.addDirectoryOrJARContainingClass(org.hsqldb.jdbcDriver.class);
      builder.addDirectoryOrJARContainingClass(net.sf.ehcache.CacheException.class);
      builder.addDirectoryOrJARContainingClass(net.sf.cglib.beans.BulkBeanException.class);
      builder.addDirectoryOrJARContainingClass(org.apache.commons.dbcp.BasicDataSource.class);
      builder.addDirectoryOrJARContainingClass(org.apache.commons.pool.impl.GenericObjectPool.class);
      builder.addDirectoryOrJARContainingClass(org.apache.commons.collections.SequencedHashMap.class);
      // builder.addDirectoryOrJARContainingClass(com.tc.aspectwerkz.exception.WrappedRuntimeException.class);
      // builder.addDirectoryOrJARContainingClass(org.xml.sax.ext.Attributes2.class);
    }
  }

  public static Test suite() {
    return new HibernateTestSetup();
  }

}
