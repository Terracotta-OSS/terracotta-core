package test.inventory;

public class Product {
  private double       m_price;
  private final String m_name;
  private final String m_sku;

  public Product(String name, double price, String sku) {
    m_name = name;
    m_price = price;
    m_sku = sku;
  }

  public String getName() {
    return m_name;
  }

  public String getSku() {
    return m_sku;
  }

  public synchronized double getPrice() {
    return m_price;
  }

  // FIXME need locking
  public synchronized void setPrice(double price) {
    this.m_price = price;
  }
}
