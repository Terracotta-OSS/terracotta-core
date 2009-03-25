/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.util.Arrays;

public class ActiveCoordinatorHelper {
  public static String getGroupNameFrom(String[] members) {
    String[] temp = new String[members.length];
    for (int i = 0; i < temp.length; i++) {
      temp[i] = members[i];
    }
    Arrays.sort(temp);

    StringBuffer grpName = new StringBuffer();
    for (int i = 0; i < temp.length; i++) {
      grpName.append(temp[i]);
    }
    return grpName.toString();
  }
}
