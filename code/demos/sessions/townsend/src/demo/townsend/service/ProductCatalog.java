/*
@COPYRIGHT@
*/
package demo.townsend.service;

import java.util.ArrayList;

public class ProductCatalog {

   private ArrayList catalog;

   public ProductCatalog() {       
      catalog = new ArrayList();
      if (!catalog.isEmpty()) {
         return;
      }
      catalog.add(new Product("0001", 10, "Canon PowerShot A620", "7.1 Megapixel Digital"));
      catalog.add(new Product("0002", 24, "Olympus EVOLT E-500", "8.0 Megapixel Digital SLR Camera w/2.5\" LCD & Two Lenses"));
      catalog.add(new Product("0003", 150, "Canon PowerShot SD450", "5.0 Megapixel Digital Camera w/3x Zoom & 2.5\" LCD"));
      catalog.add(new Product("0004", 165, "Fuji FinePix A345", "4.1 Megapixel Digital Camera"));
      catalog.add(new Product("0005", 205, "Olympus SP-310", "7.1 Megapixel Digital Camera"));
      catalog.add(new Product("0006", 90, "Canon PowerShot A520", "4.0 Megapixel Digital Camera w/4X Zoom"));
      catalog.add(new Product("0007", 4, "Canon PowerShot SD500", "7.1 Megapixel Digital Camera w/3x Optical Zoom"));
      catalog.add(new Product("0008", 14, "Casio EX-Z850", "8.0 MegaPixel Camera with 3x Optical Zoom and Super Bright 2.5\" LCD"));
   }

   public ArrayList getCatalog() {
      return catalog;
   }
}
