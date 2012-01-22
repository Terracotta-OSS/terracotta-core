/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 */
package demo.inventory;

public class Product {
  private double price;
  private final String name;
  private final String sku;

  public Product(String n, double p, String s) {
    name = n;
    price = p;
    sku = s;
  }

  public int hashCode() {
    return sku.hashCode();
  }

  public final String getName() {
    return name;
  }

  public final String getSKU() {
    return sku;
  }
  
  public synchronized void setPrice(double p) {
    price = p;
  }

  public synchronized double getPrice() {
    return price;
  }
}
