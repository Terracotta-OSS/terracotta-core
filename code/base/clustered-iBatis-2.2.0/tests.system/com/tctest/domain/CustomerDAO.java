package com.tctest.domain;

import com.ibatis.dao.client.Dao;
import com.ibatis.dao.client.DaoManager;

public interface CustomerDAO extends Dao {
  public int insertCustomer(Customer customer);

  public Customer selectCustomer(int customerId);

  public DaoManager getDaoManager();
}
