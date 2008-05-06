/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.util;

import com.tc.util.Assert;
import com.tc.util.StringUtil;

import java.net.InetSocketAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InetSocketAddressList {

  private static final Pattern      ADDRESS_PATTERN = Pattern.compile("^([^:]+):(\\p{Digit}+)$");

  private final InetSocketAddress[] addressList;

  public InetSocketAddressList(InetSocketAddress[] addressList) {
    Assert.assertNoNullElements(addressList);
    this.addressList = addressList;
  }

  public InetSocketAddress[] addresses() {
    return addressList;
  }

  public String toString() {
    String[] addresses = new String[addressList.length];
    for (int pos = 0; pos < addressList.length; pos++) {
      addresses[pos] = addressList[pos].getHostName() + ":" + addressList[pos].getPort();
    }
    return StringUtil.toString(addresses, ",", null, null);
  }

  public static InetSocketAddress[] parseAddresses(String list) throws ParseException {
    Assert.assertNotNull(list);
    List addressList = new ArrayList();
    String[] addresses = list.split(",");
    int currentPosition = 0;
    for (int pos = 0; pos < addresses.length; ++pos) {
      Matcher addressMatcher = ADDRESS_PATTERN.matcher(addresses[pos]);
      if (!addressMatcher.matches()) {
        throw new ParseException("Unable to parse address, expected a format of <host>:<port>", currentPosition);
      } else {
        addressList.add(new InetSocketAddress(addressMatcher.group(1), Integer.parseInt(addressMatcher.group(2))));
      }
      // Account for the comma separator
      if (pos > 0) ++currentPosition;
      currentPosition += addresses[pos].length();
    }
    InetSocketAddress[] rv = new InetSocketAddress[addressList.size()];
    addressList.toArray(rv);
    return rv;
  }

}
