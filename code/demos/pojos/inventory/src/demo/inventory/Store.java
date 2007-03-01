/*
@COPYRIGHT@
*/
package demo.inventory;

import demo.inventory.Department;
import demo.inventory.Product;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Store 
{
   public List departments = new ArrayList();
   public Map inventory    = new HashMap();  

   public Store() 
   {
      Product warandpeace    = new Product("War and Peace",      7.99, "WRPC");
      Product tripod         = new Product("Camera Tripod",     78.99, "TRPD");
      Product usbmouse       = new Product("USB Mouse",         19.99, "USBM");
      Product flashram       = new Product("1GB FlashRAM card", 47.99, "1GFR");  

      Department housewares  = new Department("B", "Books",       new Product[] { warandpeace } );
      Department photography = new Department("P", "Photography", new Product[] { tripod,  flashram } );
      Department computers   = new Department("C", "Computers",   new Product[] { usbmouse, flashram, } );

      departments.add(housewares);
      departments.add(photography);
      departments.add(computers);

      inventory.put(warandpeace.sku, warandpeace);
      inventory.put(tripod.sku,      tripod);
      inventory.put(usbmouse.sku,    usbmouse);
      inventory.put(flashram.sku,    flashram);
   }
}
