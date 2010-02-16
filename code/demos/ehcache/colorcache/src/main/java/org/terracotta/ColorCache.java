/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import java.awt.Color;
import java.util.Iterator;
import java.util.ArrayList;

public class ColorCache {
  private static final CacheManager  cacheManager  = new CacheManager();
  private static final ColorDatabase colorDatabase = new ColorDatabase();

  public ColorCache() {
    /**/
  }

  public Color getColor(String name) {
    Element elem = getCache().get(name);
    if(elem == null) {
      Color color = colorDatabase.getColor(name);
      if(color == null) {
        return null;
      }
      getCache().put(elem = new Element(name, color));
    }
    return (Color)elem.getValue();
  }

  public String[] getColorNames() {
    Iterator keys = getCache().getKeys().iterator();
    ArrayList list = new ArrayList();
    while(keys.hasNext()) {
      list.add(keys.next().toString());
    }
    return (String[])list.toArray(new String[0]);
  }

  public long getTTL() {
    return getCache().getCacheConfiguration().getTimeToLiveSeconds();
  }

  public long getTTI() {
    return getCache().getCacheConfiguration().getTimeToIdleSeconds();
  }

  public int getSize() {
    return getCache().getSize();
  }

  private Ehcache getCache() {
    return cacheManager.getEhcache("colors");
  }
}
