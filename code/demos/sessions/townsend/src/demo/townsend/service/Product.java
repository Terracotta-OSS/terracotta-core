/*
@COPYRIGHT@
*/
package demo.townsend.service;

public class Product {

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
}