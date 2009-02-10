package eclipsegen;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

/**
 * This is just like a regular properties object except that the keys will be written in a predictable (sorted) order
 * when store() is called. Also the store() method here does not use the platform specific newline sequence (this is to
 * prevent needless deltas in source control)
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

  @Override
  public void store(OutputStream out, String comments) throws IOException {
    // remove the timestamp -- this code is ugly for sure, but will make it so
    // there isn't needless change when props are rewritten

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    super.store(baos, comments);

    BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));

    PrintWriter pw = new PrintWriter(new OutputStreamWriter(out));

    int lineNum = 0;
    String line;
    while ((line = br.readLine()) != null) {
      lineNum++;
      if (lineNum != 2) {
        pw.print(line);
        pw.print((char) 13);
        pw.print((char) 10);
      }
    }

    pw.flush();
  }

}
