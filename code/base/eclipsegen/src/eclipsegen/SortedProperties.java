package eclipsegen;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

/**
 * This is just like a regular properties object except that the keys will be written in a predicaable (sorted) order
 * when store() is called
 */
public class SortedProperties extends Properties {

  @Override
  public synchronized Enumeration<Object> keys() {
    Vector<String> keysList = new Vector<String>();
    Enumeration<Object> keys = super.keys();
    while (keys.hasMoreElements()) {
      keysList.add((String) keys.nextElement());
    }
    Collections.sort(keysList);
    return new TypeConvertEnumeration(keysList);
  }

  private static class TypeConvertEnumeration implements Enumeration<Object> {

    private final Enumeration<String> elements;

    TypeConvertEnumeration(Vector<String> keysList) {
      elements = keysList.elements();
    }

    public boolean hasMoreElements() {
      return elements.hasMoreElements();
    }

    public Object nextElement() {
      return elements.nextElement();
    }
  }

}
