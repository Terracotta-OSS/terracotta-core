/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 */
package demo.townsend.service;

import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

public class ProductCatalog {
  private final Ehcache cache;

  public ProductCatalog() {
    cache = CacheManager.getInstance().getCache("catalog");
  }

  private List<Product> initCatalog() {
    List<Product> catalog = new ArrayList<Product>();
    catalog.add(new Product("0001", 10, "Canon PowerShot A620",
        "7.1 Megapixel Digital"));
    catalog.add(new Product("0002", 24, "Olympus EVOLT E-500",
        "8.0 Megapixel Digital SLR Camera w/2.5\" LCD & Two Lenses"));
    catalog.add(new Product("0003", 150, "Canon PowerShot SD450",
        "5.0 Megapixel Digital Camera w/3x Zoom & 2.5\" LCD"));
    catalog.add(new Product("0004", 165, "Fuji FinePix A345",
        "4.1 Megapixel Digital Camera"));
    catalog.add(new Product("0005", 205, "Olympus SP-310",
        "7.1 Megapixel Digital Camera"));
    catalog.add(new Product("0006", 90, "Canon PowerShot A520",
        "4.0 Megapixel Digital Camera w/4X Zoom"));
    catalog.add(new Product("0007", 4, "Canon PowerShot SD500",
        "7.1 Megapixel Digital Camera w/3x Optical Zoom"));
    catalog
        .add(new Product("0008", 14, "Casio EX-Z850",
            "8.0 MegaPixel Camera with 3x Optical Zoom and Super Bright 2.5\" LCD"));
    cache.put(new Element("cameras", catalog));
    return catalog;
  }

  @SuppressWarnings("unchecked")
  public List<Product> getCatalog() {
    Element cachedCameras = cache.get("cameras");
    if (cachedCameras == null) {
      return initCatalog();
    } else {
      return (List<Product>) cachedCameras.getValue();
    }
  }
}
