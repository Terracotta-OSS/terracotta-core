/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring.bean;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.tctest.spring.bean.domain.PersistentObj;
import com.tctest.spring.bean.orm.hibernate.HibernateDAO;

public class HibernateBean implements IHibernateBean {
  private Object                 sharedOne = null;
  private transient HibernateDAO dao       = null;

  public void associateSharedObj() {
    dao.getHibernateTemplate().update(this.getSharedOne());
  }

  public void sharePersistentObj() {
    TransactionStatus status = dao.getTransactionManager().getTransaction(new DefaultTransactionDefinition());
    PersistentObj po = new PersistentObj();
    po.setStrFld("persistent");
    dao.getHibernateTemplate().save(po);
    this.setSharedOne(po); // share persistent object
    dao.getTransactionManager().commit(status);
  }

  public void shareDetachedObj() {
    TransactionStatus status = dao.getTransactionManager().getTransaction(new DefaultTransactionDefinition());
    PersistentObj po = new PersistentObj();
    po.setStrFld("detached");
    dao.getHibernateTemplate().save(po);
    dao.getTransactionManager().commit(status);
    this.setSharedOne(po); // share detched object
  }

  public void shareLazyObj() {
    Integer id = null;
    PersistentObj po = new PersistentObj();
    po.setStrFld("parent");
    PersistentObj child = new PersistentObj();
    child.setStrFld("lazy");
    po.setChild(child);
    dao.getHibernateTemplate().save(po);
    id = po.getId();

    TransactionStatus status = dao.getTransactionManager().getTransaction(new DefaultTransactionDefinition());
    po = (PersistentObj) dao.getHibernateTemplate().get(PersistentObj.class, id);
    this.setSharedOne(po.getChild()); // share proxied object
    dao.getTransactionManager().commit(status);
  }

  public void shareObjWithLazyChild() {
    Integer id = null;
    PersistentObj po = new PersistentObj();
    po.setStrFld("parent");
    PersistentObj child = new PersistentObj();
    child.setStrFld("lazy");
    po.setChild(child);
    dao.getHibernateTemplate().save(po);
    id = po.getId();

    TransactionStatus status = dao.getTransactionManager().getTransaction(new DefaultTransactionDefinition());
    po = (PersistentObj) dao.getHibernateTemplate().get(PersistentObj.class, id);
    this.setSharedOne(po); // share proxied object
    dao.getTransactionManager().commit(status);
  }

  public Integer getSharedId() {
    PersistentObj persistentObj = (PersistentObj) getSharedOne();
    return persistentObj==null ? null : persistentObj.getId();
  }

  public String getSharedFld() {
    PersistentObj persistentObj = (PersistentObj) getSharedOne();
    return persistentObj==null ? null : persistentObj.getStrFld();
  }

  public HibernateDAO getDao() {
    return dao;
  }

  public void setDao(HibernateDAO dao) {
    this.dao = dao;
  }

  synchronized public Object getSharedOne() {
    // System.err.println("----" + this.sharedOne.getClass().getName());
    return sharedOne;
  }

  synchronized public void setSharedOne(Object sharedOne) {
    this.sharedOne = sharedOne;
  }

}
