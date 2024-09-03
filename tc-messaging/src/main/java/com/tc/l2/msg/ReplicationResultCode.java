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
package com.tc.l2.msg;


/**
 * An enum defining the meaning of an acknowledgement from the passive.  These can be either RECEIVED, SUCCESS, or FAIL.
 */
public enum ReplicationResultCode {
  NONE {
    @Override
    public int code() {
      return 0;
    }
  }, RECEIVED {
    @Override
    public int code() {
      return 1;
    }
  }, SUCCESS {
    @Override
    public int code() {
      return 2;
    }
  }, FAIL {
    @Override
    public int code() {
      return 3;
    }
  };

  static ReplicationResultCode decode(int code) {
    switch (code) {
      case 0:
        return NONE;
      case 1:
        return RECEIVED;
      case 2:
        return SUCCESS;
      case 3:
        return FAIL;
      default:
        throw new RuntimeException("bad code");
    }
  }

  public abstract int code();
}
