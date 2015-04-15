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
package com.tc.server;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.bio.SocketConnector;

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
 * A Jetty connector that is handed sockets from the DSO listen port once they are identified as HTTP requests
 */
public class TerracottaConnector extends SocketConnector {
  private final boolean secure;
  private boolean shutdown = false;

  public TerracottaConnector(boolean secure) {
    this.secure = secure;
  }

  public void shutdown() {
    synchronized (this) {
      shutdown = true;
      notifyAll();
    }
  }

  public void handleSocketFromDSO(Socket s, byte[] data) throws IOException {
    ConnectorEndPoint connection = new ConnectorEndPoint(new SocketWrapper(s, data));
    connection.dispatch();
  }

  @Override
  public void open() {
    // don't call supper since it would open another server socket here
  }

  @Override
  public void accept(int acceptorID) throws InterruptedException {
    // Jetty's accept thread will call here continuously , so we need to block it
    synchronized (this) {
      while (!shutdown) {
        wait();
      }
    }
  }

  @Override
  public boolean isConfidential(Request request) {
    return secure;
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

    @Override
    public void bind(SocketAddress bindpoint) throws IOException {
      s.bind(bindpoint);
    }

    @Override
    public void close() throws IOException {
      s.close();
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
      s.connect(endpoint, timeout);
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
      s.connect(endpoint);
    }

    @Override
    public boolean equals(Object obj) {
      return s.equals(obj);
    }

    @Override
    public SocketChannel getChannel() {
      return s.getChannel();
    }

    @Override
    public InetAddress getInetAddress() {
      return s.getInetAddress();
    }

    @Override
    public InputStream getInputStream() throws IOException {
      PushbackInputStream pis = new PushbackInputStream(s.getInputStream(), data.length);
      pis.unread(data);
      return pis;
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
      return s.getKeepAlive();
    }

    @Override
    public InetAddress getLocalAddress() {
      return s.getLocalAddress();
    }

    @Override
    public int getLocalPort() {
      return s.getLocalPort();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
      return s.getLocalSocketAddress();
    }

    @Override
    public boolean getOOBInline() throws SocketException {
      return s.getOOBInline();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
      return s.getOutputStream();
    }

    @Override
    public int getPort() {
      return s.getPort();
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
      return s.getReceiveBufferSize();
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
      return s.getRemoteSocketAddress();
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
      return s.getReuseAddress();
    }

    @Override
    public int getSendBufferSize() throws SocketException {
      return s.getSendBufferSize();
    }

    @Override
    public int getSoLinger() throws SocketException {
      return s.getSoLinger();
    }

    @Override
    public int getSoTimeout() throws SocketException {
      return s.getSoTimeout();
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
      return s.getTcpNoDelay();
    }

    @Override
    public int getTrafficClass() throws SocketException {
      return s.getTrafficClass();
    }

    @Override
    public int hashCode() {
      return s.hashCode();
    }

    @Override
    public boolean isBound() {
      return s.isBound();
    }

    @Override
    public boolean isClosed() {
      return s.isClosed();
    }

    @Override
    public boolean isConnected() {
      return s.isConnected();
    }

    @Override
    public boolean isInputShutdown() {
      return s.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
      return s.isOutputShutdown();
    }

    @Override
    public void sendUrgentData(int d) throws IOException {
      s.sendUrgentData(d);
    }

    @Override
    public void setKeepAlive(boolean on) throws SocketException {
      s.setKeepAlive(on);
    }

    @Override
    public void setOOBInline(boolean on) throws SocketException {
      s.setOOBInline(on);
    }

    // public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
    // s.setPerformancePreferences(connectionTime, latency, bandwidth);
    // }

    @Override
    public void setReceiveBufferSize(int size) throws SocketException {
      s.setReceiveBufferSize(size);
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
      s.setReuseAddress(on);
    }

    @Override
    public void setSendBufferSize(int size) throws SocketException {
      s.setSendBufferSize(size);
    }

    @Override
    public void setSoLinger(boolean on, int linger) throws SocketException {
      s.setSoLinger(on, linger);
    }

    @Override
    public void setSoTimeout(int timeout) throws SocketException {
      s.setSoTimeout(timeout);
    }

    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
      s.setTcpNoDelay(on);
    }

    @Override
    public void setTrafficClass(int tc) throws SocketException {
      s.setTrafficClass(tc);
    }

    @Override
    public void shutdownInput() throws IOException {
      s.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
      s.shutdownOutput();
    }

    @Override
    public String toString() {
      return s.toString();
    }

  }

}
