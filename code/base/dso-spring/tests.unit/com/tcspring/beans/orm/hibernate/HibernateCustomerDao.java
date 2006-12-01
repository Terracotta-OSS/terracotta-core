/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring.beans.orm.hibernate;

import java.util.List;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import com.tcspring.beans.orm.data.CustomerDao;
import com.tcspring.beans.orm.domain.Customer;

public class HibernateCustomerDao 
  extends HibernateDaoSupport implements CustomerDao {

  public List getAll() {
    return getHibernateTemplate().find("from Customer");
  }

  public void save(Customer customer) {
    getHibernateTemplate().saveOrUpdate(customer);
  }

  public List getAllWithOnlyOnePermission() {
    return getHibernateTemplate().find(
        "from Customer as c where c.permissions.size = ?",
        new Object[] { new Integer(1) });
  }
}
