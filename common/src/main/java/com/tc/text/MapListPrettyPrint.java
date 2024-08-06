/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.text;

import java.io.PrintWriter;
import java.io.StringWriter;
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
