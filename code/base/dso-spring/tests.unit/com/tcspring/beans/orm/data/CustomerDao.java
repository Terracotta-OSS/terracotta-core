/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tcspring.beans.orm.data;

import com.tcspring.beans.orm.domain.Customer;

import java.util.List;

public interface CustomerDao {
  
  public List getAll();
  
  public List getAllWithOnlyOnePermission();

  public void save(Customer customer);
}
