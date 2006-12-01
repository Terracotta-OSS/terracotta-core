/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import org.springframework.orm.hibernate3.HibernateTemplate;

public class HibernatePersister {

  private PersistentSubobject         root;

  private transient HibernateTemplate template;

  public HibernatePersister(HibernateTemplate template) {
    this.template = template;
  }

  public PersistentObject make() {
    synchronized (this) {
      root = new PersistentSubobject(PersistentSubobject.DELIVERED);
    }
    
    PersistentObject po = new PersistentObject();
    template.save(po);
    return po;

  }

  public void changeStatus(int id) {
    PersistentObject po = (PersistentObject) template.load(PersistentObject.class, new Integer(id));
    po.noteMessageStatus(root);
    template.saveOrUpdate(po);
  }

}
