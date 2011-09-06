/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

import org.mortbay.jetty.bio.SocketConnector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

/**
 * A Jettu connector that is handed sockets from the DSO listen port once they are identified as HTTP requests
 */
public class TerracottaConnector extends SocketConnector {
  private boolean shutdown = false;

  public void shutdown() {
    synchronized (this) {
      shutdown = true;
      notifyAll();
    }
  }

  public void handleSocketFromDSO(Socket s, byte[] data) throws InterruptedException, IOException {
    Connection connection = new Connection(new SocketWrapper(s, data));
    connection.dispatch();
  }

  public void open() {
    // don't call supper since it would open another server socket here
  }

  public void accept(int acceptorID) throws InterruptedException {
    // Jetty's accept thread will call here continuously , so we need to block it
    synchronized (this) {
      while (!shutdown) {
        wait();
      }
    }
  }

  /**
   * Wraps an existing socket such that we can wrap the input stream and provide the bytes already read
   */
  private static class SocketWrapper extends Socket {

    private final byte[] data;
    private final Socket s;

    public SocketWrapper(Socket s, byte[] data) {
      this.s = s;
      this.data = data;
    }

    public void bind(SocketAddress bindpoint) throws IOException {
      s.bind(bindpoint);
    }

    public void close() throws IOException {
      s.close();
    }

    public void connect(SocketAddress endpoint, int timeout) throws IOException {
      s.connect(endpoint, timeout);
    }

    public void connect(SocketAddress endpoint) throws IOException {
      s.connect(endpoint);
    }

    public boolean equals(Object obj) {
      return s.equals(obj);
    }

    public SocketChannel getChannel() {
      return s.getChannel();
    }

    public InetAddress getInetAddress() {
      return s.getInetAddress();
    }

    public InputStream getInputStream() throws IOException {
      PushbackInputStream pis = new PushbackInputStream(s.getInputStream(), data.length);
      pis.unread(data);
      return pis;
    }

    public boolean getKeepAlive() throws SocketException {
      return s.getKeepAlive();
    }

    public InetAddress getLocalAddress() {
      return s.getLocalAddress();
    }

    public int getLocalPort() {
      return s.getLocalPort();
    }

    public SocketAddress getLocalSocketAddress() {
      return s.getLocalSocketAddress();
    }

    public boolean getOOBInline() throws SocketException {
      return s.getOOBInline();
    }

    public OutputStream getOutputStream() throws IOException {
      return s.getOutputStream();
    }

    public int getPort() {
      return s.getPort();
    }

    public int getReceiveBufferSize() throws SocketException {
      return s.getReceiveBufferSize();
    }

    public SocketAddress getRemoteSocketAddress() {
      return s.getRemoteSocketAddress();
    }

    public boolean getReuseAddress() throws SocketException {
      return s.getReuseAddress();
    }

    public int getSendBufferSize() throws SocketException {
      return s.getSendBufferSize();
    }

    public int getSoLinger() throws SocketException {
      return s.getSoLinger();
    }

    public int getSoTimeout() throws SocketException {
      return s.getSoTimeout();
    }

    public boolean getTcpNoDelay() throws SocketException {
      return s.getTcpNoDelay();
    }

    public int getTrafficClass() throws SocketException {
      return s.getTrafficClass();
    }

    public int hashCode() {
      return s.hashCode();
    }

    public boolean isBound() {
      return s.isBound();
    }

    public boolean isClosed() {
      return s.isClosed();
    }

    public boolean isConnected() {
      return s.isConnected();
    }

    public boolean isInputShutdown() {
      return s.isInputShutdown();
    }

    public boolean isOutputShutdown() {
      return s.isOutputShutdown();
    }

    public void sendUrgentData(int d) throws IOException {
      s.sendUrgentData(d);
    }

    public void setKeepAlive(boolean on) throws SocketException {
      s.setKeepAlive(on);
    }

    public void setOOBInline(boolean on) throws SocketException {
      s.setOOBInline(on);
    }

    // public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
    // s.setPerformancePreferences(connectionTime, latency, bandwidth);
    // }

    public void setReceiveBufferSize(int size) throws SocketException {
      s.setReceiveBufferSize(size);
    }

    public void setReuseAddress(boolean on) throws SocketException {
      s.setReuseAddress(on);
    }

    public void setSendBufferSize(int size) throws SocketException {
      s.setSendBufferSize(size);
    }

    public void setSoLinger(boolean on, int linger) throws SocketException {
      s.setSoLinger(on, linger);
    }

    public void setSoTimeout(int timeout) throws SocketException {
      s.setSoTimeout(timeout);
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
      s.setTcpNoDelay(on);
    }

    public void setTrafficClass(int tc) throws SocketException {
      s.setTrafficClass(tc);
    }

    public void shutdownInput() throws IOException {
      s.shutdownInput();
    }

    public void shutdownOutput() throws IOException {
      s.shutdownOutput();
    }

    public String toString() {
      return s.toString();
    }

  }

}
