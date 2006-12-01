/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package dso;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author steve
 */
public class SimpleSharedQueueExample {
  private final static int MAX_RESULTS_SIZE = 15;
  private String           putterName;
  private List             workQueue        = new LinkedList();
  private Map              allWorkers       = new HashMap();
  private List             results          = new LinkedList();
  //ARI: Keep summary info as we add results so that we can print a pretty
  // summary
  private Map              resultsSummary   = new HashMap();

  public SimpleSharedQueueExample() {
    synchronized (allWorkers) {
      int myID = allWorkers.size() + 1;
      this.putterName = "Worker:" + myID;
      this.allWorkers.put(putterName, new Integer(myID));
    }
    Thread t = new WorkerThread(this, putterName);
    t.setDaemon(true);
    t.start();
  }

  /**
   * Add a calculation to the work queue to be picked up later by what ever node gets it first and executed.
   * 
   * @param number1 - first item in the calculation
   * @param number2 - second item in the calculation
   * @param sign - calculation to perform
   */
  public void addAction(float number1, float number2, char sign, int timesRepeat) {
    for (int i = 0; i < timesRepeat; i++) {
      put(new Action(putterName, number1, number2, sign));
    }
  }

  /**
   * The name of this node.
   * 
   * @return
   */
  public String getPutterName() {
    return putterName;
  }

  /**
   * remove all the results from the results collection
   */
  public void clearResults() {
    synchronized (results) {
      results.clear();
    }
  }

  /**
   * Get all the results so far from all nodes and return them
   * 
   * @return List of results
   */
  public List getResults() {
    synchronized (results) {
      return new LinkedList(results);
    }
  }

  public HashMap getResultsSummary() {
    synchronized (resultsSummary) {
      return new HashMap(resultsSummary);
    }
  }

  private void addResultSummary(Result result) {
    synchronized (resultsSummary) {
      ResultsSummary rs = null;
      if (!resultsSummary.containsKey(result.getPutterName())) {
        resultsSummary.put(result.getPutterName(), new ResultsSummary(result.getPutterName(), 0, 0));
      }
      rs = (ResultsSummary) resultsSummary.get(result.getPutterName());
      rs.incrementPutCount();

      if (!resultsSummary.containsKey(result.getExecuterName())) {
        resultsSummary.put(result.getExecuterName(), new ResultsSummary(result.getExecuterName(), 0, 0));
      }
      rs = (ResultsSummary) resultsSummary.get(result.getExecuterName());
      rs.incrementExecuteCount();
    }
  }

  private void addResult(Result result) {
    synchronized (results) {
      results.add(result.toString());
      if (results.size() > MAX_RESULTS_SIZE) {
        results.remove(0);
      }
    }
    addResultSummary(result);
  }

  private void put(Action action) {
    synchronized (workQueue) {
      workQueue.add(action);
      workQueue.notifyAll();
    }
  }

  private Result execute(String executerName) {
    synchronized (workQueue) {
      while (workQueue.size() == 0) {
        try {
          workQueue.wait();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
      Action action = (Action) workQueue.remove(0);
      String result = action.execute(executerName);
      return new Result(action, executerName, result);
    }
  }

  public static class Action {
    private float  number1, number2;
    private char   sign;
    private String putterName;

    public Action(String putter, float number1, float number2, char sign) {
      this.putterName = putter;
      this.number1 = number1;
      this.number2 = number2;
      this.sign = sign;
    }

    public String execute(String executerName) {
      try {
        switch (sign) {
          case '+':
            return "" + (number1 + number2);
          case '-':
            return "" + (number1 - number2);
          case '*':
            return "" + (number1 * number2);
          case '/':
            return "" + (number1 / number2);
          default:
            return "error:" + sign;
        }
      } catch (Exception e) {
        return "Invalid calculation:" + number1 + "" + sign + number2;
      }
    }

    public String toString() {
      return "Sign:" + sign + " putter:" + putterName + " number1:" + number1 + " number2:" + number2;
    }

    public float getNumber1() {
      return number1;
    }

    public float getNumber2() {
      return number2;
    }

    public char getSign() {
      return sign;
    }

    public String getPutterName() {
      return putterName;
    }
  }

  private static class ResultsSummary {
    private String name;
    private int    putCount;
    private int    executeCount;

    public String toString() {
      return "<tr>\n<td>" + this.name + "</td><td>" + this.executeCount + "</td><td>" + this.putCount + "</td></tr>";
    }

    public ResultsSummary(String name, int p, int e) {
      this.name = name;
      this.putCount = p;
      this.executeCount = e;
    }

    public void incrementExecuteCount() {
      this.executeCount++;
    }

    public void incrementPutCount() {
      this.putCount++;
    }

    public String getName() {
      return this.name;
    }

    public int getExecuteCount() {
      return this.executeCount;
    }

    public int getPutCount() {
      return this.putCount;
    }
  }

  private static class Result {
    private String putterName;
    private String executerName;
    private float  number1;
    private float  number2;
    private char   sign;
    private String result;

    public String toString() {
      return "Created by: " + putterName + "  Executed by: " + executerName + " equation ( " + number1 + " " + sign
             + " " + number2 + " = " + result + " )";
    }

    public Result(Action action, String executerName, String result) {
      this.putterName = action.getPutterName();
      this.executerName = executerName;
      this.number1 = action.getNumber1();
      this.number2 = action.getNumber2();
      this.sign = action.getSign();
      this.result = result;
    }

    public String getPutterName() {
      return this.putterName;
    }

    public String getExecuterName() {
      return this.executerName;
    }
  }

  private static class WorkerThread extends Thread {
    private String                   putterName;
    private SimpleSharedQueueExample parent;

    public WorkerThread(SimpleSharedQueueExample parent, String putterName) {
      this.parent = parent;
      this.putterName = putterName;
    }

    public void run() {
      while (true) {
        Result result = null;
        result = parent.execute(putterName);
        if (result != null) {
          parent.addResult(result);
        }
      }
    }
  }
}