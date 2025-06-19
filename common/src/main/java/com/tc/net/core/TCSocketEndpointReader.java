/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.net.core;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.bytes.TCDirectByteBufferCache;
import com.tc.bytes.TCReference;
import com.tc.bytes.TCReferenceSupport;
import static com.tc.net.core.SocketEndpoint.ResultType.EOF;
import static com.tc.net.core.SocketEndpoint.ResultType.OVERFLOW;
import static com.tc.net.core.SocketEndpoint.ResultType.SUCCESS;
import static com.tc.net.core.SocketEndpoint.ResultType.UNDERFLOW;
import static com.tc.net.core.SocketEndpoint.ResultType.ZERO;
import com.tc.util.Assert;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Sits on top of a socket endpoint in order to read messages from the channel
 *  and manage memory via references for the rest of the pipeline.
 *
 *  not synchronized in any way.  Expected to be used by a single thread only including
 *  close
 */
public class TCSocketEndpointReader implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(TCSocketEndpointReader.class);
  private final Function<Integer, TCByteBuffer> allocator;
  private final Consumer<TCByteBuffer> returns;
  private TCByteBuffer readBuffer;   //  raw buffer to read from the socket
  private TCReference current;  // complete reference to slice off references to be used by the rest of the system
  private int readTo = 0;   // read cursor in the raw buffer

  public TCSocketEndpointReader() {
    this.allocator = (s)->TCByteBufferFactory.getInstance(s);
    this.returns = (b)->{};
  }

  public TCSocketEndpointReader(TCDirectByteBufferCache cache) {
    this.allocator = (s)->cache.poll();
    this.returns = cache::offer;
  }

  // for testing
  TCSocketEndpointReader(Function<Integer, TCByteBuffer> allocator, Consumer<TCByteBuffer> returns) {
    this.allocator = allocator;
    this.returns = returns;
  }

  public TCReference readFromSocket(SocketEndpoint endpoint, int len) throws IOException {
    LOGGER.debug("{} requesting:{} {} {}", endpoint, len, readBuffer, readTo);
    if (readBuffer == null) {
      TCByteBuffer newBuf = allocator.apply(len);
      replaceCurrent(newBuf, createCompleteReference(newBuf), 0);
    }
    if (readBuffer.position() - readTo >= len) {
    //  bytes are already off the network, return a slice for reading
      current.stream().findFirst().get().position(readTo).limit(readTo + len);
      TCReference ref = current.duplicate();
      readTo += len;
      LOGGER.debug("returning from cached bytes:{} {}",ref.available(),readBuffer);
      return ref;
    } else {
      // need to fetch bytes from the network
      LinkedList<TCByteBuffer> newBufs = new LinkedList<>();
      int capacity = readBuffer.limit() - readTo;
      // make sure there is enough capacity
      while (capacity < len) {
        TCByteBuffer next = allocator.apply(len - capacity);
        newBufs.add(next);
        capacity += next.limit();
      }
      // add the current buffer at the head
      newBufs.addFirst(readBuffer);
      long received = readBuffer.position() - readTo;
      // read bytes from the network until the requested bytes are in
      int rotations = 0;
      while (received < len) {
        try {
          rotations += 1;
          received += doRead(endpoint, newBufs);
          LOGGER.debug("rotation:{} received:{}", rotations, received);
        } catch (NoBytesAvailable no) {
      //  no bytes in the channel, peer may not have written yet,
      //  if no bytes are cached, return null here and let the
      //  caller call again if needed
          if (received == 0) {
            newBufs.removeFirst();
            if (!newBufs.isEmpty()) {
              newBufs.forEach(b->returns.accept(b.reInit()));
            }
            LOGGER.debug("returning null");
            return null;
          } else {
       // still expecting bytes, might be something wrong here.
       // go around again after a short pause, maybe should consider
       // failing at some point
            if (rotations > 1_000_000) {
              // give up, something is wrong
              throw new IOException("incomplete bytes in channel");
            } else {
              try {
                Thread.sleep(500);
              } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IOException(ie);
              }
            }
          }
        }
      }
      //  create the correct references
      // remove the current from the list of new buffers
      newBufs.removeFirst();
      if (newBufs.isEmpty()) {
      // if no new buffers, return a slice of bytes needed
        current.stream().findFirst().get().position(readTo).limit(readTo + len);
        TCReference ref = current.duplicate();
        readTo += len;
        LOGGER.debug("returning from read socket read with no new buffers: {}", ref.available());
        return ref;
      } else {
     // created new buffers
     // make sure the current limit matches the underlying incase an OVERFLOW occurred
        current.stream().findFirst().get().limit(readBuffer.limit());
     // remove the last buffer, this will be the new current buffer
        TCByteBuffer last = newBufs.removeLast();
     // position the front buffer to the right spot
        int built = current.stream().findFirst().get().position(readTo).remaining();
     // flip the whole middle, these will only serve the current request
        for (TCByteBuffer b : newBufs) {
          b.flip();
          built += b.remaining();
        }
     // create what will be the new current buffer
        TCReference lastRef = createCompleteReference(last);
     // set the end of the buffer for slicing
        int lastLim = lastRef.stream().findFirst().get().position(0).limit(len - built).limit();

        try (TCReference newRefs = TCReferenceSupport.createReference(newBufs, returns)) {
     // piece together all the buffers and return a reference
          TCReference retRef = TCReferenceSupport.createAggregateReference(current, newRefs, lastRef);
          Assert.assertEquals(retRef.available(), len);
          LOGGER.debug("returning from socket read with new buffers: {}", retRef.available());
          return retRef;
        } finally {
     // set the last buffer to the current, it may have bytes for the next message
          replaceCurrent(last, lastRef, lastLim);
        }
      }
    }
  }

  private void replaceCurrent(TCByteBuffer raw, TCReference ref, int pos) {
    LOGGER.debug("replacing: {} {} with: {} {}", this.readBuffer, this.readTo, raw, pos);
    if (raw == this.readBuffer) {
      Assert.fail();
    }
    this.readBuffer = raw;
    if (this.current != null) {
      this.current.close();
    }
    this.current = ref;
    this.readTo = pos;
  }

  private TCReference createCompleteReference(TCByteBuffer buffer) {
    LOGGER.debug("creating complete ref: {} {}", buffer, this.readTo);
    int pos = buffer.position();
    Assert.assertEquals(buffer.limit(), buffer.capacity());
    try {
      // return a base reference that spans the whole bytebuffer
      return TCReferenceSupport.createReference(returns, buffer.clear());
    } finally {
      buffer.position(pos);
    }
  }

  private static void returnByteBuffers(List<TCByteBuffer> dest, ByteBuffer[] raw) {
    for (int x=0;x<raw.length;x++) {
      dest.get(x).returnNioBuffer(raw[x]);
    }
  }

  private static ByteBuffer[] extractByteBuffers(List<TCByteBuffer> dest) {
    return dest.stream().map(TCByteBuffer::getNioBuffer).toArray(ByteBuffer[]::new);
  }

  private long doRead(SocketEndpoint endpoint, List<TCByteBuffer> dest) throws IOException {
    long remain = dest.stream().mapToInt(TCByteBuffer::remaining).asLongStream().sum();
    ByteBuffer[] nioBytes = extractByteBuffers(dest);
    try {
      switch (endpoint.readTo(nioBytes)) {
        case EOF:
          throw new EOFException();
        case OVERFLOW:
     // overflow, don't accept any more bytes in the current buffer
          dest.forEach(b->b.limit(b.position()));
     // add a buffer to capture bytes
          dest.add(allocator.apply(TCByteBufferFactory.getFixedBufferSize()));
          break;
        case UNDERFLOW:
        case SUCCESS:
          break;
        case ZERO:
          throw new NoBytesAvailable();
      }
    } finally {
      remain -= dest.stream().mapToInt(TCByteBuffer::remaining).asLongStream().sum();
      returnByteBuffers(dest, nioBytes);
    }
    LOGGER.debug("read from socket: {}", remain);
    return remain;
  }

  public void close() {
    if (current != null) {
      current.close();
    }
  }

  private static class NoBytesAvailable extends IOException {

  }


}
