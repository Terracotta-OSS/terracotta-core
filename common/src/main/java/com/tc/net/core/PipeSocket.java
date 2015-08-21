/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
