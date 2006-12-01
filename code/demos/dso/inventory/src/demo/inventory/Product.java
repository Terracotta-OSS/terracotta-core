/*
@COPYRIGHT@
*/
package demo.inventory;

public class Product 
{
   public double price;
   public final String name;
   public final String sku;

   public Product(String n, double p, String s) 
   {
      name = n;
      price = p;
      sku = s;
   }

   public int hashCode() 
   { 
      return sku.hashCode();
   }

   public void setPrice(double p) 
   { 
      synchronized(this) { price = p; }
   }
}
