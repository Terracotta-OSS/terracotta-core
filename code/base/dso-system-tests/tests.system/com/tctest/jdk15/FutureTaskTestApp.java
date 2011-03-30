/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.exception.TCRuntimeException;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FutureTaskTestApp extends AbstractTransparentApp {

  private final CyclicBarrier       barrier;
  private final CyclicBarrier       barrier2     = new CyclicBarrier(2);

  private final int                 NUM_OF_ITEMS = 500;
  private final DataRoot            root         = new DataRoot();
  private final LinkedBlockingQueue workerQueue  = new LinkedBlockingQueue();
  private final LinkedBlockingQueue resultQueue  = new LinkedBlockingQueue();

  public FutureTaskTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.await();

      testSharedDuringRunning(index);
      testWithCallable(index);
      testWithRunnable(index);
      testWithMyFutureTask(index);
      testWithLinkedBlockingQueue(index);
      testWithExecutorService(index);

    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void testSharedDuringRunning(int index) throws Exception {
    if (index == 0) {
      FutureTask task = new MyFutureTask(new MyCallable());
      Thread thread1 = new Thread(new GetUnSharedRunnable(task));
      thread1.start();
      Thread.sleep(1000);
      root.setTask(task);
      task.run();
      barrier2.await();
    }

    barrier.await();
  }

  private void testWithExecutorService(int index) throws Exception {
    long startTime = System.currentTimeMillis();
    if (index == 0) {
      List list = new ArrayList();
      for (int i = 0; i < NUM_OF_ITEMS; i++) {
        Callable callable = new MyCallable(i);
        list.add(callable);
      }
      root.setList(list);
    }

    barrier.await();

    if (index == 1) {
      ExecutorService service = new ThreadPoolExecutor(10, 10, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue());
      List futures = service.invokeAll(root.getList());
      root.setTasksList(futures);
    } else {
      List tasksList = root.getTasksList();
      while (tasksList == null) {
        tasksList = root.getTasksList();
      }
      for (int i = 0; i < NUM_OF_ITEMS; i++) {
        System.err.println("Getting Task " + i);
        Assert.assertEquals(root, ((FutureTask) tasksList.get(i)).get());
      }
    }

    long endTime = System.currentTimeMillis();
    System.err.println("Elapsed time in ExecutorService: " + (endTime - startTime));

    barrier.await();
  }

  private void testWithLinkedBlockingQueue(int index) throws Exception {
    long startTime = System.currentTimeMillis();
    if (index == 0) {
      for (int i = 0; i < NUM_OF_ITEMS; i++) {
        System.err.println("Putting task " + i);
        FutureTask task = new MyFutureTask(new MyCallable(i));
        workerQueue.put(task);
      }
      workerQueue.put("STOP");
      workerQueue.put("STOP");
    } else {
      while (true) {
        Object o = workerQueue.take();
        if ("STOP".equals(o)) {
          break;
        } else {
          FutureTask task = (FutureTask) o;
          task.run();
          resultQueue.put(task);
        }
      }
    }
    if (index == 0) {
      for (int i = 0; i < NUM_OF_ITEMS; i++) {
        FutureTask task = (FutureTask) resultQueue.take();

        Assert.assertEquals(root, task.get());
      }
    }

    long endTime = System.currentTimeMillis();
    System.err.println("Elapsed time in LinkedBlockingQueue: " + (endTime - startTime));

    barrier.await();
  }

  private void testWithMyFutureTask(int index) throws Exception {
    FutureTask task = new MyFutureTask(new MyCallable());
    basicRunTask(index, task);

    task = new MyFutureTask(new MyLongCallable());
    basicCancelTask(index, task);

    task = new MyFutureTask(new MySemiLongCallable());
    basicCancelTaskWithCompletion(index, task);

    task = new MyFutureTask(new MyLongCallable());
    timeoutGetTask(index, task);

    task = new MyFutureTask(new MyCallable());
    basicSet(index, (MyFutureTask) task);

    task = new MyFutureTask(new MyCallable());
    basicSetException(index, (MyFutureTask) task);

    task = new MyFutureTask(new MyCallable());
    basicRunAndResetException(index, (MyFutureTask) task);
  }

  private void testWithRunnable(int index) throws Exception {
    FutureTask task = new FutureTask(new MyRunnable(), root);
    basicRunTask(index, task);

    task = new FutureTask(new MyLongRunnable(), root);
    basicCancelTask(index, task);

    task = new FutureTask(new MySemiLongRunnable(), root);
    basicCancelTaskWithCompletion(index, task);

    task = new FutureTask(new MyLongRunnable(), root);
    basicCancelTask(index, task);

    task = new FutureTask(new MyLongRunnable(), root);
    timeoutGetTask(index, task);
  }

  private void testWithCallable(int index) throws Exception {
    FutureTask task = new FutureTask(new MyCallable());
    basicRunTask(index, task);

    task = new FutureTask(new MyLongCallable());
    basicCancelTask(index, task);

    task = new FutureTask(new MySemiLongCallable());
    basicCancelTaskWithCompletion(index, task);

    task = new FutureTask(new MyLongCallable());
    timeoutGetTask(index, task);

    task = new FutureTask(new MyThrowable());
    basicRunTaskWithException(index, task);
  }

  private void basicSet(int index, MyFutureTask task) throws Exception {
    if (index == 0) {
      root.setTask(task);
    }

    barrier.await();

    if (index == 1) {
      ((MyFutureTask) root.getTask()).set(root);
    }

    Object o = root.getTask().get();
    while (o == null) {
      o = root.getTask().get();
    }
    Assert.assertEquals(root, o);

    Assert.assertTrue(root.getTask().isDone());

    barrier.await();
  }

  private void basicRunAndResetException(int index, MyFutureTask task) throws Exception {
    if (index == 0) {
      root.setTask(task);
    }

    barrier.await();

    if (index == 1) {
      boolean flag = ((MyFutureTask) root.getTask()).runAndReset();
      Assert.assertTrue(flag);
    }

    barrier.await();

    Assert.assertFalse(root.getTask().isDone());

    barrier.await();
  }

  private void basicSetException(int index, MyFutureTask task) throws Exception {
    final String exceptionMsg = "Test setting InterruptedException";
    if (index == 0) {
      root.setTask(task);
    }

    barrier.await();

    if (index == 1) {
      ((MyFutureTask) root.getTask()).setException(new InterruptedException(exceptionMsg));
    }

    barrier.await();

    try {
      root.getTask().get();
      throw new AssertionError("Should have thrown an ExecutionException.");
    } catch (ExecutionException e) {
      Assert.assertEquals(exceptionMsg, e.getCause().getMessage());
    }

    barrier.await();
  }

  private void timeoutGetTask(int index, FutureTask longTask) throws Exception {
    if (index == 0) {
      root.setTask(longTask);
    }

    barrier.await();

    if (index == 1) {
      root.getTask().run();
    } else if (index == 0) {
      try {
        root.getTask().get(10000, TimeUnit.MILLISECONDS);
        throw new AssertionError("Should have thrown a TimeoutException.");
      } catch (TimeoutException e) {
        root.getTask().cancel(true);
      }
    }

    barrier.await();

    Assert.assertTrue(root.getTask().isCancelled());

    Assert.assertTrue(root.getTask().isDone());

    barrier.await();

  }

  private void basicCancelTask(int index, FutureTask longTask) throws Exception {
    if (index == 0) {
      root.setTask(longTask);
    }

    barrier.await();

    if (index == 1) {
      root.getTask().run();
    } else if (index == 0) {
      root.getTask().cancel(true);
    }

    barrier.await();

    Assert.assertTrue(root.getTask().isCancelled());

    Assert.assertTrue(root.getTask().isDone());

    try {
      root.getTask().get();
      throw new AssertionError("Could have thrown a CancellationException.");
    } catch (CancellationException e) {
      // Expected
    }

    barrier.await();

  }

  private void basicCancelTaskWithCompletion(int index, FutureTask longTask) throws Exception {
    if (index == 0) {
      root.setTask(longTask);
    }

    barrier.await();

    if (index == 1) {
      root.getTask().run();
    } else if (index == 0) {
      root.getTask().cancel(false);
    }

    barrier.await();

    Assert.assertTrue(root.getTask().isCancelled());

    Assert.assertTrue(root.getTask().isDone());

    try {
      root.getTask().get();
      throw new AssertionError("Could have thrown a CancellationException.");
    } catch (CancellationException e) {
      // Expected
    }

    barrier.await();

  }

  private void basicRunTaskWithException(int index, FutureTask task) throws Exception {
    if (index == 0) {
      root.setTask(task);
    }

    barrier.await();

    if (index == 1) {
      root.getTask().run();
    }

    try {
      root.getTask().get();
      throw new AssertionError("Should have thrown an ExecutionException");
    } catch (ExecutionException e) {
      Assert.assertTrue(e.getCause() instanceof InterruptedException);
      Assert.assertEquals(MyThrowable.EXCEPTION_MSG, e.getCause().getMessage());
    }

    barrier.await();
  }

  private void basicRunTask(int index, FutureTask task) throws Exception {
    if (index == 0) {
      root.setTask(task);
    }
    barrier.await();

    if (index == 1) {
      root.getTask().run();
    }
    Assert.assertEquals(root, root.getTask().get());
    Assert.assertTrue(root.getTask().isDone());
    barrier.await();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = FutureTaskTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("barrier2", "barrier2");
    spec.addRoot("root", "root");
    spec.addRoot("workerQueue", "workerQueue");
    spec.addRoot("resultQueue", "resultQueue");
  }

  private static class DataRoot {
    private FutureTask task;
    private List       list;
    private List       tasksList;

    public DataRoot() {
      super();
    }

    public synchronized void setTask(FutureTask task) {
      this.task = task;
    }

    public synchronized FutureTask getTask() {
      return this.task;
    }

    public List getList() {
      return list;
    }

    public synchronized void setList(List list) {
      this.list = list;
    }

    public synchronized List getTasksList() {
      return tasksList;
    }

    public synchronized void setTasksList(List tasksList) {
      this.tasksList = tasksList;
    }
  }

  private class MyLongCallable implements Callable {
    public Object call() throws Exception {
      while (true) {
        if (Thread.interrupted()) { throw new InterruptedException(); }
        Thread.sleep(10000);
      }
    }
  }

  private class MySemiLongCallable implements Callable {
    public Object call() throws Exception {
      Thread.sleep(30000);
      return root;
    }
  }

  private class MyCallable implements Callable {
    private Integer value;

    public MyCallable(int i) {
      this.value = Integer.valueOf(i);
    }

    public MyCallable() {
      super();
    }

    public Object call() throws Exception {
      if (value != null) {
        System.err.println("Running call() in MyCallable: " + value);
      }
      return root;
    }
  }

  private class MyThrowable implements Callable {
    public static final String EXCEPTION_MSG = "Test InterruptException";

    public Object call() throws Exception {
      throw new InterruptedException(EXCEPTION_MSG);
    }
  }

  private class MyLongRunnable implements Runnable {
    public void run() {
      try {
        while (true) {
          if (Thread.interrupted()) { throw new InterruptedException(); }
          Thread.sleep(10000);
        }
      } catch (Exception e) {
        throw new TCRuntimeException(e);
      }
    }
  }

  private class MySemiLongRunnable implements Runnable {
    public void run() {
      try {
        Thread.sleep(30000);
      } catch (Exception e) {
        throw new TCRuntimeException(e);
      }
    }
  }

  private class MyRunnable implements Runnable {
    public void run() {
      // do nothing
    }
  }

  private class MyFutureTask extends FutureTask {
    public MyFutureTask(Callable callable) {
      super(callable);
    }

    public MyFutureTask(Runnable runnable, Object result) {
      super(runnable, result);
    }

    @Override
    public synchronized void set(Object v) {
      super.set(v);
    }

    @Override
    public synchronized void setException(Throwable t) {
      super.setException(t);
    }

    @Override
    public boolean runAndReset() {
      return super.runAndReset();
    }
  }

  private class GetUnSharedRunnable implements Runnable {
    private final FutureTask task;

    public GetUnSharedRunnable(FutureTask task) {
      this.task = task;
    }

    public void run() {
      try {
        Assert.assertEquals(root, task.get());
        barrier2.await();
      } catch (Exception e) {
        throw new TCRuntimeException(e);
      }
    }
  }

}
