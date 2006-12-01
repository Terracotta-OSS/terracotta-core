/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class InstanceWalker {

  public static void hierarchy(Object instance, Writer w) throws IOException {
    // StringWriter sw = new StringWriter();
    HashSet completed = new HashSet();
    hierarchy(instance, instance.getClass(), "", w, completed);
  }
  
  public static void hierarchy(Object instance, Class c, String off, Writer sw, Set completed) throws IOException {
    if(
        c.getName().startsWith("java.lang.Object") ||
        c.getName().startsWith("java.lang.String") || 
        c.getName().startsWith("java.lang.Class") || 
        c.getName().startsWith("java.lang.Integer") || 
        c.getName().startsWith("java.lang.Number") || 
        c.getName().startsWith("java.lang.Long") || 
        c.getName().startsWith("java.lang.Short") || 
        c.getName().startsWith("java.util.Date") || 
        c.getName().startsWith("java.util.Locale") || 
        c.getName().startsWith("java.lang.Object") || 
        c.getName().startsWith("java.lang.reflect.") || 
        c.getName().startsWith("com.tc.") || 
        c.getName().startsWith("com.tcspring.") || 
        c.getName().startsWith("org.apache.") ||
        (c.isArray() && c.getComponentType().isPrimitive())) { 
      return;
    }
    completed.add(instance);

    if(c.isArray()) {
      int n = Array.getLength(instance);
      sw.write(off + "  ARRAY " + " ("+n+") " + c.getName() + "\n");
      for(int j = 0; j < n; j++) {
        Object e = Array.get(instance, j);
        if(e!=null) {
          Class a = e.getClass();
          sw.write(off + "  [" + j +"] " + a.getName() + "\n");
          if(!c.getComponentType().isPrimitive() && !completed.contains(e)) {
            hierarchy(e, a, off + "    ", sw, completed);
          }
        }
      }
      return;
      
    } else if(instance instanceof Map) {
      Map map = (Map) instance;
      sw.write(off + "  MAP ("+map.size()+") " + c.getName() + "\n");
      for(Iterator it = map.entrySet().iterator(); it.hasNext();) {
        Map.Entry entry = (Map.Entry) it.next();
        Object key = entry.getKey();
        Object value = entry.getValue();
        sw.write(off + "    [" + key +"] " + ( value==null ? "null" : value.getClass().getName() ) + "\n");
        if(value!=null && !completed.contains(value)) {
          hierarchy(value, value.getClass(), off+"    ", sw, completed);
        }
      }
      return;
      
    } else if(instance instanceof Collection) {
      Collection collection = (Collection) instance;
      sw.write(off + "  COLLECTION ("+collection.size()+") " + c.getName() + "\n");
      for(Iterator it = collection.iterator(); it.hasNext();) {
        Object value = it.next();
        sw.write(off + "  -- " + ( value==null ? "null" : value.getClass().getName() ) + "\n");
        if(value!=null && !completed.contains(value)) {
          hierarchy(value, value.getClass(), off+"    ", sw, completed);
        }
      }
      return;

    }    
    
    Class s = c.getSuperclass();
    if(s!=null && !s.getName().equals("java.lang.Object")) {
      hierarchy(instance, s, off, sw, completed);
    }

    sw.write(off + "  "+c.getName()+"\n");
    Field[] declaredFields = c.getDeclaredFields();
    for(int i = 0; i < declaredFields.length; i++) {
      Field f = declaredFields[i];

      try {
        f.setAccessible(true);
        // Class type = f.getType();
        Object o = f.get(instance);
        
        if(o!=null) {
          Class actualType = o.getClass();  
          Class declaredType = f.getType();
          
          sw.write(off + "  ## " + f.getName()+" : " + declaredType.getName() + " : " + actualType.getName() + " : " + completed.contains(o) + "\n");
          if(!declaredType.isPrimitive() && !Modifier.isStatic(f.getModifiers()) && !completed.contains(o)) {
            hierarchy(o, actualType, off + "  ", sw, completed);
          }
        }
      } catch(Exception ex) {
        sw.write("!!! " + ex.toString()+"\n");
      }
    }
  }
  
}

