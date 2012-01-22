/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 */
package demo.townsend.service;

public class Product implements java.io.Serializable {
  private static final long serialVersionUID = -3859001963310355634L;
  private final String id;
  private final int quantity;
  private final String name;
  private final String details;

  public Product(String id, int quantity, String name, String details) {
    this.id = id;
    this.quantity = quantity;
    this.name = name;
    this.details = details;
  }

  public String getId() {
    return id;
  }

  public int getQuantity() {
    return quantity;
  }

  public String getName() {
    return name;
  }

  public String getDetails() {
    return details;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(name);
    sb.append("[id=");
    sb.append(id);
    sb.append(", quantity=");
    sb.append(quantity);
    sb.append(", details='");
    sb.append(details);
    sb.append("']");
    return sb.toString();
  }
}