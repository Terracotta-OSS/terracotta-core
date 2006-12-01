/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package sysprops;

import java.util.Arrays;

/**
 * Little utility to dump the contents of virgin System properties
 * 
 * @author teck
 */
public class SysProps {

  public static void main(String[] args) {
    String[] props = (String[]) System.getProperties().keySet().toArray(new String[] {});

    Arrays.sort(props);

    for (int i = 0; i < props.length; i++) {
      System.out.println(props[i] + " --> " + System.getProperty(props[i], "null"));
    }
  }
}