/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.util.dbperf;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;

import bsh.EvalError;
import bsh.Interpreter;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.api.DBFactory;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class DBPerf {
  private static final TCLogger          logger    = TCLogging.getLogger(DBPerf.class);
  private final DBEnvironment            dbEnvironment;
  private final AbstractTCDatabaseTester objectDBTester;
  private final AbstractTCDatabaseTester mapsDBTester;
  private final AbstractTCDatabaseTester clientStateDBTester;
  private String                         workload  = "for (int i = 0; i < 100; i++) { objectDB.insert(tx); mapsDB.insert(tx); }";
  private int                            runtime   = 60;
  private int                            noWorkers = 4;

  public DBPerf(File envHome, boolean paranoid) throws IOException, TCDatabaseException {
    dbEnvironment = getDBFactory().createEnvironment(paranoid, envHome, SampledCounter.NULL_SAMPLED_COUNTER, false);
    dbEnvironment.open();
    objectDBTester = new TCObjectDatabaseTester(dbEnvironment.getObjectDatabase());
    mapsDBTester = new TCMapsDatabaseTester(dbEnvironment.getMapsDatabase());
    clientStateDBTester = new TCLongDatabaseTester(dbEnvironment.getClientStateDatabase());
  }

  public void setWorkload(String workload) {
    this.workload = workload;
  }

  public void setRuntime(int runtime) {
    this.runtime = runtime;
  }

  public void setWorkers(int workers) {
    this.noWorkers = workers;
  }

  private DBFactory getDBFactory() {
    String factoryName = TCPropertiesImpl.getProperties().getProperty(TCPropertiesConsts.L2_DB_FACTORY_NAME);
    DBFactory dbFactory = null;
    try {
      Class dbClass = Class.forName(factoryName);
      Constructor<DBFactory> constructor = dbClass.getConstructor(TCProperties.class);
      dbFactory = constructor.newInstance(TCPropertiesImpl.getProperties().getPropertiesFor("l2"));
    } catch (Exception e) {
      logger.error("Failed to create db factory of class: " + factoryName, e);
    }
    return dbFactory;
  }

  public void runDBPerf() throws InterruptedException, EvalError {
    System.out.println("Starting benchmark...");
    System.out.println("Using workload: " + workload);
    Controller controller = new Controller(dbEnvironment, runtime);
    controller.start();
    controller.join();
  }

  private static Options createOptions() {
    Options options = new Options();
    options.addOption("t", "tempDirectory", true, "Folder to use as for the DB's disk store.");
    options.addOption("s", "tempSwap", false, "Set to turn off db persistence.");
    options.addOption("r", "runtime", true, "Number of seconds to run the test.");
    options.addOption("w", "workers", true, "Number of workers to create for the test.");
    options.addOption("l", "workload", true, "Beanshell workload to execute on each worker.");
    options.addOption("h", "help", false, "Print the help message.");
    return options;
  }

  private static void printUsage() {
    HelpFormatter hf = new HelpFormatter();
    hf.printHelp("DBPerf", createOptions());
  }

  public static void main(String[] args) throws IOException, TCDatabaseException, InterruptedException, EvalError {
    Options options = createOptions();
    CommandLineParser parser = new PosixParser();
    CommandLine commandLine = null;
    try {
      commandLine = parser.parse(options, args);
    } catch (ParseException e) {
      printUsage();
      System.exit(-1);
    }
    if (commandLine.hasOption("help")) {
      printUsage();
      System.exit(-1);
    }
    if (!commandLine.hasOption("tempDirectory")) {
      System.out.println("Missing tempDirectory argument.");
      printUsage();
      System.exit(-1);
    }
    File envHome = new File(commandLine.getOptionValue("tempDirectory"), "DBPerf_temp");
    if (!envHome.exists()) {
      System.out.println("Temp directory not found, making it.");
      envHome.mkdirs();
    } else {
      System.out.println("Temp directory already exists, cleaning it.");
      FileUtils.cleanDirectory(envHome);
    }
    DBPerf dbPerf = new DBPerf(envHome, !commandLine.hasOption("tempSwap"));
    if (commandLine.hasOption("workload")) {
      dbPerf.setWorkload(commandLine.getOptionValue("workload"));
    }
    if (commandLine.hasOption("runtime")) {
      dbPerf.setRuntime(Integer.parseInt(commandLine.getOptionValue("runtime")));
    }
    if (commandLine.hasOption("workers")) {
      dbPerf.setWorkers(Integer.parseInt(commandLine.getOptionValue("workers")));
    }
    dbPerf.runDBPerf();
  }

  private class Controller extends Thread {
    private final Worker[]   workers   = new Worker[noWorkers];
    private final Calendar   timeout   = Calendar.getInstance();
    private final int        runtimeSeconds;
    private static final int POLL_TIME = 10;

    private Controller(DBEnvironment env, int runtimeSeconds) throws EvalError {
      this.runtimeSeconds = runtimeSeconds;
      timeout.add(Calendar.SECOND, runtimeSeconds);
      for (int i = 0; i < noWorkers; i++) {
        workers[i] = new Worker(i);
        workers[i].start();
      }
    }

    @Override
    public void run() {
      while (Calendar.getInstance().before(timeout)) {
        ThreadUtil.reallySleep(POLL_TIME * 1000);
        int totalTransactionsThisCycle = 0;
        Date date = new Date(System.currentTimeMillis());
        System.out.println("\n\n" + DateFormat.getTimeInstance().format(date));
        for (Worker worker : workers) {
          int workerTransactions = worker.getAndResetTransactionsThisCycle();
          totalTransactionsThisCycle += workerTransactions;
          System.out.println(worker.getName() + " TPS " + ((double) workerTransactions) / POLL_TIME);
        }
        System.out.println("Total TPS this cycle: " + ((double) totalTransactionsThisCycle) / POLL_TIME);

        System.out.println("objectDB");
        objectDBTester.printCycleReport("o->", POLL_TIME);
        System.out.println("mapsDB");
        mapsDBTester.printCycleReport("m->", POLL_TIME);
        System.out.println("clientStateDB");
        clientStateDBTester.printCycleReport("c->", POLL_TIME);
      }
      int totalTransactions = 0;
      for (Worker worker : workers) {
        worker.stopWorker();
        totalTransactions += worker.getTotalTransactions();
      }
      System.out.println("\n\nOverall TPS: " + ((double) totalTransactions) / runtimeSeconds);
      System.out.println("objectDB");
      objectDBTester.printTotalReport("o=>", runtimeSeconds);
      System.out.println("mapsDB");
      mapsDBTester.printTotalReport("m=>", runtimeSeconds);
      System.out.println("clientStateDB");
      clientStateDBTester.printTotalReport("c=>", runtimeSeconds);
    }
  }

  private class Worker extends Thread {
    private volatile boolean    running;
    private final AtomicInteger transactionsThisCycle = new AtomicInteger();
    private final AtomicInteger totalTransactions     = new AtomicInteger();
    private final Interpreter   interpreter           = new Interpreter();

    private Worker(int id) throws EvalError {
      this.running = true;
      interpreter.set("objectDB", objectDBTester);
      interpreter.set("mapsDB", mapsDBTester);
      interpreter.set("clientStateDB", clientStateDBTester);
      interpreter.set("id", id);
      setName("Worker " + id);
    }

    @Override
    public void run() {
      PersistenceTransactionProvider ptp = dbEnvironment.getPersistenceTransactionProvider();
      while (running) {
        PersistenceTransaction tx = ptp.newTransaction();
        try {
          interpreter.set("tx", tx);
          interpreter.eval(workload);
        } catch (EvalError e) {
          logger.error("Failed to evaluate workload!", e);
          stopWorker();
        }
        tx.commit();
        transactionsThisCycle.incrementAndGet();
        totalTransactions.incrementAndGet();
      }
    }

    public int getAndResetTransactionsThisCycle() {
      return transactionsThisCycle.getAndSet(0);
    }

    public int getTotalTransactions() {
      return totalTransactions.get();
    }

    private void stopWorker() {
      this.running = false;
    }
  }
}
