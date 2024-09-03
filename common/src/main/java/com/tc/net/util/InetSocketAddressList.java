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

  private static final Pattern               ADDRESS_PATTERN = Pattern.compile("^([^:]+):(\\p{Digit}+)$");
  private final ArrayList<InetSocketAddress> addressList     = new ArrayList<InetSocketAddress>();

  public InetSocketAddressList(InetSocketAddress[] addressList) {
    Assert.assertNoNullElements(addressList);
    for (InetSocketAddress address : addressList) {
      this.addressList.add(address);
    }
  }

  @Override
  public String toString() {
    String[] addresses = new String[addressList.size()];
    int pos = 0;
    for (InetSocketAddress address : addressList) {
      addresses[pos] = address.getHostName() + ":" + address.getPort();
      pos++;
    }
    return StringUtil.toString(addresses, ",", null, null);
  }

  public static InetSocketAddress[] parseAddresses(String list) throws ParseException {
    Assert.assertNotNull(list);
    List<InetSocketAddress> addressList = new ArrayList<InetSocketAddress>();
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
