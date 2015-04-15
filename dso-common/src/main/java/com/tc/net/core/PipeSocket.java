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
package com.tc.net.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.channels.Pipe.SinkChannel;
import java.nio.channels.Pipe.SourceChannel;

/**
 * @author Ludovic Orban
 */
public class PipeSocket extends Socket {

  private final Pipe       inputPipe;
  private final Pipe       outputPipe;
  private final Socket     socket;
  private volatile boolean closed = false;

  public PipeSocket(Socket socket) throws IOException {
    this.socket = socket;
    this.inputPipe = Pipe.open();
    this.outputPipe = Pipe.open();
    this.outputPipe.source().configureBlocking(false);
  }

  public SourceChannel getOutputPipeSourceChannel() {
    return outputPipe.source();
  }

  public SinkChannel getInputPipeSinkChannel() {
    return inputPipe.sink();
  }

  @Override
  public InputStream getInputStream() {
    return Channels.newInputStream(inputPipe.source());
  }

  @Override
  public OutputStream getOutputStream() {
    return new PipeSocketOutputStream(Channels.newOutputStream(outputPipe.sink()));
  }

  @Override
  public SocketAddress getRemoteSocketAddress() {
    if (closed) { return null; }
    return socket.getRemoteSocketAddress();
  }

  @Override
  public SocketAddress getLocalSocketAddress() {
    if (closed) { return null; }
    return socket.getLocalSocketAddress();
  }

  @Override
  public InetAddress getLocalAddress() {
    if (closed) { return null; }
    return socket.getLocalAddress();
  }

  @Override
  public boolean isClosed() {
    return closed;
  }

  @Override
  public synchronized void close() throws IOException {
    if (closed) return;
    super.close();
    closed = true;
    closeRead();
    closeWrite();
  }

  public void dispose() throws IOException {
    if (!isClosed()) {
      close();
    }
  }

  public void onWrite() {
    //
  }

  public void closeRead() throws IOException {
    inputPipe.sink().close();
    inputPipe.source().close();
  }

  public void closeWrite() throws IOException {
    outputPipe.sink().close();
    outputPipe.source().close();
  }

  private final class PipeSocketOutputStream extends OutputStream {

    private final OutputStream delegate;

    PipeSocketOutputStream(OutputStream delegate) {
      this.delegate = delegate;
    }

    @Override
    public void write(int b) throws IOException {
      delegate.write(b);
      onWrite();
    }
  }
}
