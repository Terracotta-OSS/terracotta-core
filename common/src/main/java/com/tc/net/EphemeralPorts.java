/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net;

import com.tc.process.StreamCollector;
import com.tc.util.FindbugsSuppressWarnings;
import com.tc.util.runtime.Os;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// http://www.ncftp.com/ncftpd/doc/misc/ephemeral_ports.html can tell you alot about what this class is about

public class EphemeralPorts {

  private static Range range = null;

  public synchronized static Range getRange() {
    if (range == null) {
      range = findRange();
    }
    return range;
  }

  private static Range findRange() {
    if (Os.isLinux()) { return new Linux().getRange(); }
    if (Os.isSolaris()) { return new Solaris().getRange(); }
    if (Os.isMac()) { return new Mac().getRange(); }
    if (Os.isWindows()) { return new Windows().getRange(); }
    if (Os.isAix()) { return new Aix().getRange(); }

    throw new AssertionError("No support for this OS: " + Os.getOsName());
  }

  public static class Range {
    private final int upper;
    private final int lower;

    private Range(int lower, int upper) {
      this.lower = lower;
      this.upper = upper;
    }

    public int getUpper() {
      return upper;
    }

    public int getLower() {
      return lower;
    }

    @Override
    public String toString() {
      return lower + " " + upper;
    }

  }

  private interface RangeGetter {
    Range getRange();
  }

  private static class Solaris implements RangeGetter {
    public Range getRange() {
      Exec exec = new Exec(new String[] { "/usr/sbin/ndd", "/dev/tcp", "tcp_smallest_anon_port" });
      final String lower;
      try {
        lower = exec.execute(Exec.STDOUT);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      exec = new Exec(new String[] { "/usr/sbin/ndd", "/dev/tcp", "tcp_largest_anon_port" });
      final String upper;
      try {
        upper = exec.execute(Exec.STDOUT);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      int low = Integer.parseInt(lower.replaceAll("\n", ""));
      int high = Integer.parseInt(upper.replaceAll("\n", ""));

      return new Range(low, high);
    }
  }

  private static class Windows implements RangeGetter {

    public Range getRange() {
      String osName = System.getProperty("os.name");

      // windows XP and server 2003
      if (osName.equalsIgnoreCase("windows xp") || osName.equalsIgnoreCase("windows 2003")) { return getLegacySettings(); }

      // Assume all other windows OS use the new network parameters
      return getNetshRange();
    }

    private Range getNetshRange() {
      final int DEFAULT_LOWER = 49151;
      final int DEFAULT_UPPER = 65535;

      try {
        // and use netsh to determine dynamic port range
        File netshExe = new File(new File(Os.findWindowsSystemRoot(), "system32"), "netsh.exe");

        String[] cmd = new String[] { netshExe.getAbsolutePath(), "int", "ipv4", "show", "dynamicport", "tcp" };
        Exec exec = new Exec(cmd);
        BufferedReader reader = new BufferedReader(new StringReader(exec.execute(Exec.STDOUT)));

        Pattern startPattern = Pattern.compile("^Start Port.*: (\\p{XDigit}+)");
        Pattern numPattern = Pattern.compile("^Number of Ports.*: (\\p{XDigit}+)");
        int start = -1;
        int num = -1;
        String line = null;
        while ((line = reader.readLine()) != null) {
          Matcher matcher = startPattern.matcher(line);
          if (matcher.matches()) {
            if (start != -1) { throw new AssertionError("start already seen: " + start); }
            start = Integer.parseInt(matcher.group(1));
          }

          matcher = numPattern.matcher(line);
          if (matcher.matches()) {
            if (num != -1) { throw new AssertionError("number already seen: " + num); }
            num = Integer.parseInt(matcher.group(1));
          }
        }

        if ((num == -1) || (start == -1)) { throw new AssertionError("start: " + start + ", num = " + num); }

        return new Range(start, start + num - 1);
      } catch (Exception e) {
        e.printStackTrace();
      }

      return new Range(DEFAULT_LOWER, DEFAULT_UPPER);
    }

    private Range getLegacySettings() {
      final int DEFAULT_LOWER = 1024;
      final int DEFAULT_UPPER = 5000;

      try {
        // use reg.exe if available to see if MaxUserPort is tweaked
        String sysRoot = Os.findWindowsSystemRoot();
        if (sysRoot != null) {
          File regExe = new File(new File(sysRoot, "system32"), "reg.exe");
          if (regExe.exists()) {
            String[] cmd = new String[] { regExe.getAbsolutePath(), "query",
                "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Services\\Tcpip\\Parameters", "/v", "MaxUserPort" };
            Exec exec = new Exec(cmd);
            BufferedReader reader = new BufferedReader(new StringReader(exec.execute(Exec.STDOUT)));

            Pattern pattern = Pattern.compile("^.*MaxUserPort\\s+REG_DWORD\\s+0x(\\p{XDigit}+)");
            String line = null;
            while ((line = reader.readLine()) != null) {
              Matcher matcher = pattern.matcher(line);
              if (matcher.matches()) {
                int val = Integer.parseInt(matcher.group(1), 16);
                return new Range(DEFAULT_LOWER, val);
              }
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

      return new Range(DEFAULT_LOWER, DEFAULT_UPPER);
    }

  }

  private static class Mac implements RangeGetter {
    public Range getRange() {
      Exec exec = new Exec(new String[] { "sysctl", "net.inet.ip.portrange" });
      final String output;
      try {
        output = exec.execute(Exec.STDOUT);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      Properties props = new Properties();
      try {
        props.load(new StringBufferInputStream(output));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      int low = Integer.parseInt(props.getProperty("net.inet.ip.portrange.hifirst"));
      int high = Integer.parseInt(props.getProperty("net.inet.ip.portrange.hilast"));

      return new Range(low, high);
    }
  }

  private static class Linux implements RangeGetter {
    private static final String source = "/proc/sys/net/ipv4/ip_local_port_range";

    /*
     * File creation with a proc filesystem path - this is okay as this is linux specific code.
     */
    @FindbugsSuppressWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public Range getRange() {
      File src = new File(source);
      if (!src.exists() || !src.canRead()) { throw new RuntimeException("Cannot access " + source); }

      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(src)));
        String data = reader.readLine();
        if (data == null) { throw new RuntimeException("Unexpected EOF at " + source); }
        String[] parts = data.split("[ \\t]");
        if (parts.length != 2) { throw new RuntimeException("Wrong number of tokens (" + parts.length + ") in " + data); }

        int low = Integer.parseInt(parts[0]);
        int high = Integer.parseInt(parts[1]);

        return new Range(low, high);
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      } finally {
        if (reader != null) {
          try {
            reader.close();
          } catch (IOException e) {
            //
          }
        }
      }
    }
  }

  private static class Aix implements RangeGetter {
    public Range getRange() {
      Exec exec = new Exec(new String[] { "/usr/sbin/no", "-a" });
      final String output;
      try {
        output = exec.execute(Exec.STDOUT);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      Properties props = new Properties();
      try {
        props.load(new StringBufferInputStream(output));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      int low = Integer.parseInt(props.getProperty("tcp_ephemeral_low"));
      int high = Integer.parseInt(props.getProperty("tcp_ephemeral_high"));

      return new Range(low, high);
    }
  }

  private static class Exec {
    static final int       STDOUT = 1;
    static final int       STDERR = 2;

    private final String[] cmd;

    Exec(String cmd[]) {
      this.cmd = cmd;
    }

    String execute(int stream) throws IOException, InterruptedException {
      if ((stream != STDOUT) && (stream != STDERR)) { throw new IllegalArgumentException("bad stream: " + stream); }

      Process proc = Runtime.getRuntime().exec(cmd);
      proc.getOutputStream().close();

      StreamCollector out = new StreamCollector(proc.getInputStream());
      StreamCollector err = new StreamCollector(proc.getErrorStream());
      out.start();
      err.start();

      proc.waitFor(); // ignores process exit code

      out.join();
      err.join();

      if (stream == STDOUT) { return out.toString(); }
      return err.toString();
    }

  }

  public static void main(String[] args) {
    System.err.println(getRange());
  }

}
