package com.tc.admin;

public interface ConnectionListener {
  void handleConnection();
  void handleException();
}
