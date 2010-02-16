/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta;

import java.awt.Color;
import java.util.Map;
import java.util.HashMap;

/**
 * This class simulates an external color database that is used to populate the
 * ColorCache. The primary, named AWT colors are stored in a map. Anytime a
 * color is requested and found, the calling thread is put to sleep for 3 seconds
 * to simulate a slow or overloaded database.
 */

public class ColorDatabase {
  private static final Map<String, Color> colorMap = new HashMap<String, Color>();

  static {
    colorMap.put("red", Color.red);
    colorMap.put("blue", Color.blue);
    colorMap.put("green", Color.green);
    colorMap.put("white", Color.white);
    colorMap.put("black", Color.black);
    colorMap.put("lightGray", Color.lightGray);
    colorMap.put("gray", Color.gray);
    colorMap.put("darkGray", Color.darkGray);
    colorMap.put("pink", Color.pink);
    colorMap.put("orange", Color.orange);
    colorMap.put("yellow", Color.yellow);
    colorMap.put("magenta", Color.magenta);
    colorMap.put("cyan", Color.cyan);
  }

  public ColorDatabase() {
    /**/
  }

  /**
   * Simulates retrieving expensive object from SOR.
   */
  public Color getColor(String name) {
    Color color = colorMap.get(name);
    if(color == null) {
      return null;
    }
    try {
      Thread.sleep(3000);
    } catch(Exception e) {/**/}
    return color;
  }
}
