package test.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Store {

  private final List m_departments = new ArrayList();
  private final Map  m_inventory   = new HashMap();

  public synchronized List getDepartments() {
    return m_departments;
  }

  public synchronized Map getInventory() {
    return m_inventory;
  }

  // FIXME need locking
  public synchronized void addDepartment(Department department) {
    m_departments.add(department);
  }

  // FIXME need locking
  public synchronized void addInventoryItem(String sku, Product product) {
    m_inventory.put(sku, product);
  }
}
