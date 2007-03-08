package com.tctest.domain;

import com.ibatis.dao.client.Dao;
import com.ibatis.dao.client.DaoManager;

public interface AccountDAO extends Dao {
  public int insertAccount(Account account);

  public Account selectAccount(int accountId);

  public DaoManager getDaoManager();

}
