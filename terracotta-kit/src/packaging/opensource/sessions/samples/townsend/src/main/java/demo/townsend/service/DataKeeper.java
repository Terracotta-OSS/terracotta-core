/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 */
package demo.townsend.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * DataKeeper keeps track of the current state of the user's list. All
 * modifications to the user's list are made by calling DataKeeper's methods.
 */
public class DataKeeper implements java.io.Serializable {
  private static final long serialVersionUID = 5262292781659376986L;
  private final int MAX_NUM = 5;
  private final ArrayList<Product> userList;

  public DataKeeper() {
    userList = new ArrayList<Product>();
  }

  public void addListItem(Product newProd) {
    for (Iterator<Product> iter = userList.iterator(); iter.hasNext();) {
      if (iter.next().getId().equals(newProd.getId())) {
        iter.remove();
      }
    }

    userList.add(0, newProd);

    if (userList.size() > MAX_NUM) {
      userList.remove(MAX_NUM);
    }
  }

  public int getListSize() {
    return userList.size();
  }

  public List<Product> getList() {
    return userList;
  }

  public Product getCurrent() {
    if (getListSize() > 0) {
      return userList.get(0);
    }
    return null;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    boolean startedSeps = false;
    for (Iterator<Product> iter = getList().iterator(); iter.hasNext();) {
      if (startedSeps) {
        sb.append(", ");
      } else {
        startedSeps = true;
      }
      sb.append(iter.next().toString());
    }
    return sb.toString();
  }
}