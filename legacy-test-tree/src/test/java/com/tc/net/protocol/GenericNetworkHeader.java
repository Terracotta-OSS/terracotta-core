/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.protocol;

public class GenericNetworkHeader extends AbstractTCNetworkHeader {

  private static final int LENGTH = 12;

  public GenericNetworkHeader() {
    super(LENGTH, LENGTH);
  }

  public void setSequence(int sequence) {
    data.putInt(4, sequence);
  }

  public int getSequence() {
    return data.getInt(4);
  }

  public void setClientNum(int num) {
    data.putInt(8, num);
  }

  public int getClientNum() {
    return data.getInt(8);
  }

  @Override
  public int getHeaderByteLength() {
    return LENGTH;
  }

  @Override
  protected void setHeaderLength(short length) {
    if (length != LENGTH) { throw new IllegalArgumentException("Header length must be " + LENGTH); }

    return;
  }

  public int getMessageDataLength() {
    return data.getInt(0);
  }

  void setMessageDataLength(int length) {
    data.putInt(0, length);
  }

  @Override
  public void validate() {
    return;
  }

}