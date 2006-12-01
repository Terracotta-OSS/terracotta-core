/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring.beans.orm.data;

import com.tcspring.beans.orm.domain.Customer;

import java.util.List;

public interface CustomerDao {
  
  public List getAll();
  
  public List getAllWithOnlyOnePermission();

  public void save(Customer customer);
}
