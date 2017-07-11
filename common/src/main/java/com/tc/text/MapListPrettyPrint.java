/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.text;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapListPrettyPrint implements PrettyPrinter {
  
  StringWriter base = new StringWriter();
  PrintWriter printer = new PrintWriter(base);

  @Override
  public PrettyPrinter println(Object o) {
    printIndented(o, 0);
    return this;
  }
  
  private void printIndented(Object o, int depth) {
    if (o instanceof Map.Entry) {
      printer.print(((Map.Entry)o).getKey());
      printer.print('=');
      printIndented(((Map.Entry)o).getValue(), depth);
    } else if (o instanceof Map) {
      Set<Map.Entry> set = ((Map)o).entrySet();
      printer.println('{');
      for (Map.Entry e : set) {
        tabOver(depth + 1);
        printIndented(e, depth + 1);
      }
      tabOver(depth);
      printer.println('}');
    } else if (o instanceof List) {
      printer.println('[');
      for (Object e : ((List)o)) {
        tabOver(depth + 1);
        printIndented(e, depth + 1);
      }
      tabOver(depth);
      printer.println(']');
    } else {
      printer.println(o);
    }
  }
  
  public static void main(String[] args) {
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    Map<String, Object> sub = new LinkedHashMap<String, Object>();
    map.put("map", sub);
    sub.put("test", "value");
    List<String> values = new ArrayList<String>();
    values.add("a1");
    map.put("array",values);
    System.out.println(new MapListPrettyPrint().println(map).toString());
  }
  
  private void tabOver(int num) {
      for (int x=0;x<num;x++) {
        printer.print("  ");
      }
  }

  @Override
  public void flush() {

  }

  @Override
  public String toString() {
    return base.toString();
  }
}
