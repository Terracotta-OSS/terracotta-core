/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.core;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.test.TCTestCase;

import java.util.Random;

import junit.framework.Assert;

public class PackedMessageTest extends TCTestCase {

  private static final int MIN_SIZE_PER_BYTE_BUFFER   = 100;
  private static final int MAX_SIZE_PER_BYTE_BUFFER   = 1000;
  private static final int MIN_NUM_INPUT_BYTE_BUFFERS = 10;
  private static final int MAX_NUM_INPUT_BYTE_BUFFERS = 1000;

  public void testPackedMessage() {

    long seed = System.currentTimeMillis();
    System.out.println("Starting test with seed: " + seed);
    Random random = new Random(seed);

    final int numInputByteBuffers = MIN_NUM_INPUT_BYTE_BUFFERS + random.nextInt(MAX_NUM_INPUT_BYTE_BUFFERS);
    SameSequenceGenerator payloadGenerator = new SameSequenceGenerator(seed);

    TCByteBuffer[] inputs = generateInputByteBuffers(numInputByteBuffers, payloadGenerator, random);
    long inputBuffersLength = 0;
    for (TCByteBuffer buf : inputs) {
      inputBuffersLength += buf.rewind().remaining();
    }
    System.out.println("Generated input byte buffers: " + inputs.length + ", length: " + inputBuffersLength);

    randomizePositions(inputs, random);
    payloadGenerator.reset();

    TCByteBuffer[] packedBuffers = TCConnectionImpl.WriteContext.getPackedUpMessage(inputs);
    long packedLength = 0;
    System.out.println("Packed buffers size: " + packedBuffers.length);
    for (int i = 0; i < packedBuffers.length; i++) {
      TCByteBuffer buf = packedBuffers[i];
      final boolean last = i == packedBuffers.length - 1;
      if (last) {
        Assert.assertTrue(TCByteBufferFactory.FIXED_BUFFER_SIZE >= buf.capacity());
      } else {
        Assert.assertEquals(TCByteBufferFactory.FIXED_BUFFER_SIZE, buf.capacity());
      }
      while (buf.hasRemaining()) {
        Assert.assertEquals(payloadGenerator.nextByte(), buf.get());
        packedLength++;
      }
    }
    System.out.println("Input length: " + inputBuffersLength + ", packed length: " + packedLength);
    long numExpectedBlocks = packedLength / TCByteBufferFactory.FIXED_BUFFER_SIZE;
    if (packedLength % TCByteBufferFactory.FIXED_BUFFER_SIZE != 0) numExpectedBlocks++;
    Assert.assertEquals(numExpectedBlocks, packedBuffers.length);

    Assert.assertEquals("Packed count should be same - input length: " + inputBuffersLength + ", packed length: "
                        + packedLength, inputBuffersLength, packedLength);
    System.out.println("Done with test");
  }

  private void randomizePositions(TCByteBuffer[] inputs, Random random) {
    System.out.println("Randomizing positions...");
    int count = 1;
    for (TCByteBuffer buffer : inputs) {
      buffer.position(random.nextInt(buffer.limit()));
      System.out.print(buffer.position() + " ");
      if (count++ % 20 == 0) {
        System.out.println();
      }
    }
    System.out.println();
    System.out.println("Done with random positions");
  }

  private TCByteBuffer[] generateInputByteBuffers(int numInputByteBuffers, SameSequenceGenerator payloadGenerator,
                                                  Random random) {
    TCByteBuffer[] inputs = new TCByteBuffer[numInputByteBuffers];
    for (int i = 0; i < inputs.length; i++) {
      int size = MIN_SIZE_PER_BYTE_BUFFER + random.nextInt(MAX_SIZE_PER_BYTE_BUFFER);
      inputs[i] = TCByteBufferFactory.getInstance(false, size);
      for (int j = 0; j < size; j++) {
        inputs[i].put(payloadGenerator.nextByte());
      }
      inputs[i].flip();
    }
    return inputs;
  }

  private static class SameSequenceGenerator {
    private final long seed;
    private Random     random;

    public SameSequenceGenerator(long seed) {
      this.seed = seed;
      random = new Random(seed);
    }

    public void reset() {
      random = new Random(seed);
      System.out.println("Resetting sequence generator!");
    }

    public byte nextByte() {
      byte rv = (byte) random.nextInt();
      // System.out.println("Next byte: " + rv);
      return rv;
    }
  }

}
