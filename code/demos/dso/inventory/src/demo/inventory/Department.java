/*
@COPYRIGHT@
*/
package demo.inventory;

public class Department 
{
   private final String code;
   private final String name;
   private final Product[] products;

   public Department(String c, String n, Product[] p) 
   {
      code = c;
      name = n;
      products = p;
   }

   public String getName() 
   {
      return name; 
   }

   public Product[] getProducts() 
   { 
      return products; 
   }
}
