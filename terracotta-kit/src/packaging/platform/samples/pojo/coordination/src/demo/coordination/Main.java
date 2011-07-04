/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 */
package demo.coordination;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Sample to demonstrate how to use CyclicBarrier to coordinate between JVMs
 */
public class Main {
  // banner text displayed on startup
  private static String text0                         = "\n"
                                                          + "JVM Coordination\n"
                                                          + "\n"
                                                          + "This sample application show how to coordinate threads in a multi-VM\n"
                                                          + "environment using the same patterns one would use in a multi-threaded\n"
                                                          + "single-VM environment.\n";

  // these are the text messages we use to display the state of the application
  private static String text1                         = "Application started; I expect a total of @TOKEN@ VMs that will be participating.\n"
                                                          + "At this point the application is waiting for the other pariticipants (or VMs) to startup.\n"
                                                          + "When all of the participants are available, it will perform its task and exit.\n\n"
                                                          + "Notice that all the other participants also come into a wait state just like the first VM that\n"
                                                          + "you launched; they will only proceed as soon as the number of VMs that you have launched\n"
                                                          + "matches the number of participants it expects.\n\n"
                                                          + "Waiting for all other VMs to join...\n";
  private static String text2                         = "I am node: @TOKEN@\n"
                                                          + "The number of VMs that I expect to participate has launched.\n"
                                                          + "I will now perform my task by printing today's date and current time:\n\n"
                                                          + "Here it is:\n@TOKEN@\n\n"
                                                          + "I have completed my task."
                                                          + "I am now waiting for all the other VMs finish their task...\n";
  private static String text3                         = "All of the participating VMs have completed their task.\n"
                                                          + "I am stopping now.";

  private int           expectedParticipants;
  private CyclicBarrier enterBarrier;
  private CyclicBarrier exitBarrier;
  private static int    MINIMUM_EXPECTED_PARTICIPANTS = 2;

  /**
   * Create an instance, setting the number of VMs expected to participate in
   * the demo.
   */
  public Main(int expectedParticipants) {
    // enforce minimum number of participants
    if (expectedParticipants < MINIMUM_EXPECTED_PARTICIPANTS) {
      expectedParticipants = MINIMUM_EXPECTED_PARTICIPANTS;
      System.out.println("(You did not pass an argument, I'm assuming "
          + expectedParticipants + " VMs will be participating)\n");
    }
    this.expectedParticipants = expectedParticipants;
    this.enterBarrier = new CyclicBarrier(expectedParticipants);
    this.exitBarrier = new CyclicBarrier(expectedParticipants);
  }

  /**
   * Start up multiple threads and wait. Once all the theads have started,
   * execute some code. When all threads have finished executing the code,
   * coordinate the shutdown of the participants.
   */
  public void run() {
    try {
      // wait for all participants before performing tasks
      System.out.println(text1.replaceFirst("@TOKEN@", Integer
          .toString(expectedParticipants)));
      enterBarrier.await();

      // perform task once all of the expected participants is present
      String currentDateAndTime = DateFormat.getDateTimeInstance(
          DateFormat.SHORT, DateFormat.SHORT).format(new Date());
      System.out.println(text2.replaceFirst("@TOKEN@",
          this + Integer.toString(expectedParticipants)).replaceFirst(
          "@TOKEN@", currentDateAndTime));

      // wait for all participants to complete their task before exiting
      exitBarrier.await();
      System.out.println(text3);
    } catch (InterruptedException ie) {
      throw new RuntimeException(ie);
    } catch (BrokenBarrierException bbe) {
      throw new RuntimeException(bbe);
    }
  }

  public static final void main(String[] args) throws Exception {
    System.out.println(text0);

    int expectedParticipants = 0;
    try {
      expectedParticipants = Integer.parseInt(args[0]);
    } catch (Exception e) {
    }

    (new Main(expectedParticipants)).run();
  }
}
