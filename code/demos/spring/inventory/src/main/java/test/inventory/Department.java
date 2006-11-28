package test.inventory;

public class Department {
  private final String    m_code;
  private final String    m_name;
  private final Product[] m_products;

  public Department(String code, String name, Product[] products) {
    m_code = code;
    m_name = name;
    m_products = products;
  }
  
  public String getName() {
    return m_name;
  }

  public Product[] getProducts() {
    return m_products;
  }
}
