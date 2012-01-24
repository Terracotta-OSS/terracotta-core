/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import java.io.File;

/**
 * O/S functions
 * 
 * @author teck
 */
public class Os {

  private Os() {
    // nada
  }

  public static String getOsName() {
    return System.getProperty("os.name", "unknown");
  }

  public static String getOsArch() {
    return System.getProperty("os.arch", "unknown");
  }

  public static String platform() {
    String osname = System.getProperty("os.name", "generic").toLowerCase();
    if (osname.startsWith("windows")) {
      return "win32";
    } else if (osname.startsWith("linux")) {
      return "linux";
    } else if (osname.startsWith("sunos")) {
      return "solaris";
    } else if (osname.startsWith("mac") || osname.startsWith("darwin")) {
      return "mac";
    } else if (osname.startsWith("aix")) {
      return "aix";
    } else return "generic";
  }

  public static boolean isWindows() {
    return (getOsName().toLowerCase().indexOf("windows") >= 0);
  }

  public static boolean isLinux() {
    return getOsName().toLowerCase().indexOf("linux") >= 0;
  }

  public static boolean isUnix() {
    final String os = getOsName().toLowerCase();

    // XXX: this obviously needs some more work to be "true" in general (see bottom of file)
    if ((os.indexOf("sunos") >= 0) || (os.indexOf("linux") >= 0)) { return true; }

    if (isMac() && (System.getProperty("os.version", "").startsWith("10."))) { return true; }

    return false;
  }

  public static boolean isMac() {
    final String os = getOsName().toLowerCase();
    return os.startsWith("mac") || os.startsWith("darwin");
  }

  public static boolean isSolaris() {
    final String os = getOsName().toLowerCase();
    return os.indexOf("sunos") >= 0;
  }

  public static boolean isAix() {
    return getOsName().toLowerCase().indexOf("aix") >= 0;
  }

  public static boolean isArchx86() {
    return getOsArch().toLowerCase().indexOf("x86") >= 0;
  }

  public static String findWindowsSystemRoot() {
    if (!isWindows()) { return null; }

    // commenting this out until we actually need it. I'm sick of seeing the
    // "use of deprecated API" warnings in our compiler output
    //
    // if (System.getProperty("java.version", "").startsWith("1.5.")) {
    // // System.getEnv(String name) is deprecated in java 1.2 through 1.4.
    // // Not only is it deprecated, it throws java.lang.Error upon invocation!
    // // It is has been un-deprecated in 1.5 though, so use it if we can
    // String root = System.getenv("SYSTEMROOT");
    // if (root != null) { return root; }
    // }

    // try to find it by looking at the file system
    final char begin = 'c';
    final char end = 'z';

    for (char drive = begin; drive < end; drive++) {
      File root = new File(drive + ":\\WINDOWS");
      if (root.exists() && root.isDirectory()) { return root.getAbsolutePath().toString(); }

      root = new File(drive + ":\\WINNT");
      if (root.exists() && root.isDirectory()) { return root.getAbsolutePath().toString(); }
    }

    return null;
  }

}

// Source: http://www.tolstoy.com/samizdat/sysprops.html

// Windows VMs
//
//
//
// ------------------------------------------------------
// OS: Windows95
// Processor: Pentium
// VM: SunJDK1.0.2
// Notes:
// Contributor: CK
//
// os.name= "Windows 95" "windows 95"
// os.arch= "x86" "x86"
// os.version= "4.0" "4.0"
// java.vendor= "Sun Microsystems Inc." "sun microsystems inc."
// java.class.version= "45.3" "45.3"
// java.version= "1.0.2" "1.0.2"
// file.separator= "\" "\"
// path.separator= ";" ";"
// line.separator= "0xd,0xa" "0xd,0xa"
//
//
// ------------------------------------------------------
// OS: Windows95
// Processor: Pentium
// VM: SunJDK1.1.4
// Notes:
// Contributor: CK
//
// os.name= "Windows 95" "windows 95"
// os.arch= "x86" "x86"
// os.version= "4.0" "4.0"
// java.vendor= "Sun Microsystems Inc." "sun microsystems inc."
// java.class.version= "45.3" "45.3"
// java.version= "1.1.4" "1.1.4"
// file.separator= "\" "\"
// path.separator= ";" ";"
// line.separator= "0xd,0xa" "0xd,0xa"
//
//
// ------------------------------------------------------
// OS: Windows NT
// Processor: x86
// VM: Microsoft1.1
// Notes:
// Contributor: AB
//
// os.name= "Windows NT" "windows nt"
// os.arch= "x86" "x86"
// os.version= "4.0" "4.0"
// java.vendor= "Microsoft Corp." "microsoft corp."
// java.class.version= "45.3" "45.3"
// java.version= "1.1" "1.1"
// file.separator= "\" "\"
// path.separator= ";" ";"
// line.separator= "0xd,0xa" "0xd,0xa"
//
//
// ------------------------------------------------------
// OS: Windows NT 4.0
// Processor: Pentium II
// VM: JDK 1.1.6
// Notes:
// Contributor: NB
//
// os.name= "Windows NT" "windows nt"
// os.arch= "x86" "x86"
// os.version= "4.0" "4.0"
// java.vendor= "Sun Microsystems Inc." "sun microsystems inc."
// java.class.version= "45.3" "45.3"
// java.version= "1.1.6" "1.1.6"
// file.separator= "\" "\"
// path.separator= ";" ";"
// line.separator= "0xd,0xa" "0xd,0xa"
//
//
// ------------------------------------------------------
// My Windows NT 4.0 box with Java 1.1.6 reports:
// Contributor: RG
//
// osName = WindowsNT
// osArch = x86
// osVersion = 4.0
// vendor = Sun Microsystems Inc.
// APIVersion = 45.3
// interpeterVersion = 1.1.6
//
//
// ------------------------------------------------------
// OS: Windows CE 2.0
// Processor: SH3
// VM: Microsoft CE JDK Version 1.0
// Notes: This was the February release - The line separator is interesting for
// a windows machine...
// Contributor: AW
//
// os.name= "Windows CE" "windows ce"
// os.arch= "Unknown" "unknown"
// os.version= "2.0 Beta" "2.0 beta"
// java.vendor= "Microsoft" "microsoft"
// java.class.version= "JDK1.1" "jdk1.1"
// java.version= "JDK1.1" "jdk1.1"
// file.separator= "\" "\"
// path.separator= ";" ";"
// line.separator= "0xa" "0xa"
//
//
// ------------------------------------------------------
// OS: NT Workstation 4.0
// Processor: Pentium MMX 200Mhz
// VM: SuperCede 2.03
// Notes:
// Contributor: AL
//
// os.name= "Windows NT" "windows nt"
// os.arch= "x86" "x86"
// os.version= "4.0" "4.0"
// java.vendor= "SuperCede Inc." "supercede inc."
// java.class.version= "45.3" "45.3"
// java.version= "1.1.4" "1.1.4"
// file.separator= "\" "\"
// path.separator= ";" ";"
// line.separator= "0xd,0xa" "0xd,0xa"
//
//
// ------------------------------------------------------
// OS: Windows 95
// Processor: Pentium 166
// VM: Netscape Communications Corporation -- Java 1.1.2
// Notes: Obtained in Netscape Navigator 4.03.
// Contributor: DG
//
// os.name= "Windows 95" "windows 95"
// os.arch= "x86" "x86"
// os.version= "4.0" "4.0"
// java.vendor= "Netscape Communications Corporation""netscape
// communications corporation"
// java.class.version= "45.3" "45.3"
// java.version= "1.1.2" "1.1.2"
// file.separator= "\" "\"
// path.separator= ";" ";"
// line.separator= "0xa" "0xa"
//
//
// ------------------------------------------------------
// OS: Windows 95
// Processor: Pentium 166
// VM: Microsoft SDK for Java 2.01
// Notes: Obtained in Internet Explorer 4 (version 4.71.1712.6).
// Contributor: DG
//
// os.name= "Windows 95" "windows 95"
// os.arch= "x86" "x86"
// os.version= "4.0" "4.0"
// java.vendor= "Microsoft Corp." "microsoft corp."
// java.class.version= "45.3" "45.3"
// java.version= "1.1" "1.1"
// file.separator= "\" "\"
// path.separator= ";" ";"
// line.separator= "0xd,0xa" "0xd,0xa"
//
//
// ------------------------------------------------------
// OS: Windows 95
// Processor: Pentium
// VM: Netscape
// Notes: Created in Netscape Navigator 3.01 for Win95.
// Contributor: DG
//
// os.name= "Windows 95" "windows 95"
// os.arch= "Pentium" "pentium"
// os.version= "4.0" "4.0"
// java.vendor= "Netscape Communications Corporation""netscape communications corporation"
// java.class.version= "45.3" "45.3"
// java.version= "1.02" "1.02"
// file.separator= "/" "/"
// path.separator= ";" ";"
// line.separator= "0xa" "0xa"
//
//
// ------------------------------------------------------
// OS: Windows NT Workstation 4.0 Service Pack 3
// Processor: Pentium II 266
// VM: Sun JDK 1.2 beta 4
// Notes:
// Contributor: JH2
//
// os.name= "Windows NT" "windows nt"
// os.arch= "x86" "x86"
// os.version= "4.0" "4.0"
// java.vendor= "Sun Microsystems Inc." "sun microsystems inc."
// java.class.version= "45.3" "45.3"
// java.version= "1.2beta4" "1.2beta4"
// file.separator= "\" "\"
// path.separator= ";" ";"
// line.separator= "0xd,0xa" "0xd,0xa"
//
//
// ------------------------------------------------------
// OS: Windows NT Workstation 4.0 Service Pack 3
// Processor: Pentium II 266
// VM: Symantec Java! JustInTime Compiler Version 3.00.029(i) for JDK 1.1.x
// Notes:
// Contributor: JH2
//
// os.name= "Windows NT" "windows nt"
// os.arch= "x86" "x86"
// os.version= "4.0" "4.0"
// java.vendor= "Symantec Corporation" "symantec corporation"
// java.class.version= "45.3" "45.3"
// java.version= "1.1.5" "1.1.5"
// file.separator= "\" "\"
// path.separator= ";" ";"
// line.separator= "0xd,0xa" "0xd,0xa"
//
//
// ------------------------------------------------------
// OS: WinNT 4.00.1381
// Processor: x86 Family 5 Model 6
// VM: IE 3.02
// Notes:
// Contributor: PB
//
// os.name= "Windows NT" "windows nt"
// os.arch= "x86" "x86"
// os.version= "4.0" "4.0"
// java.vendor= "Microsoft Corp." "microsoft corp."
// java.class.version= "45.3" "45.3"
// java.version= "1.0.2" "1.0.2"
// file.separator= "\" "\"
// path.separator= ";" ";"
// line.separator= "0xd,0xa" "0xd,0xa"
//
//
//
//
// --------------------------------------------------------------------------------
//
//
// Mac VMs
//
//
//
// ------------------------------------------------------
// OS: MacOS 7.5.1
// Processor: PowerMac
// VM: Metrowerks CodeWarrior Pro2, standard and JIT
// Notes:
// Contributor: CK
//
// os.name= "Mac OS" "mac os"
// os.arch= "PowerPC" "powerpc"
// os.version= "7.5.1" "7.5.1"
// java.vendor= "Metrowerks Corp." "metrowerks corp."
// java.class.version= "45.3" "45.3"
// java.version= "1.1.3" "1.1.3"
// file.separator= "/" "/"
// path.separator= ";" ";"
// line.separator= "0xd" "0xd"
//
//
// ------------------------------------------------------
// OS: MacOS 7.5.1
// Processor: PowerMac
// VM: MRJ 1.0.2, 1.5, 2.0d2 (values are the same for all three)
// Notes:
// Contributor: CK
//
// os.name= "Mac OS" "mac os"
// os.arch= "PowerPC" "powerpc"
// os.version= "7.5.1" "7.5.1"
// java.vendor= "Apple Computer, Inc." "apple computer, inc."
// java.class.version= "45.3" "45.3"
// java.version= "1.0.2" "1.0.2"
// file.separator= "/" "/"
// path.separator= ":" ":"
// line.separator= "0xd" "0xd"
//
//
// ------------------------------------------------------
// OS: MacOS 8.1
// Processor: PowerPC 604e
// VM: MRJ 2.0
// Notes:
// Contributor: BG
//
// os.name= "Mac OS" "mac os"
// os.arch= "PowerPC" "powerpc"
// os.version= "8.1" "8.1"
// java.vendor= "Apple Computer, Inc." "apple computer, inc."
// java.class.version= "45.3" "45.3"
// java.version= "1.1.3" "1.1.3"
// file.separator= "/" "/"
// path.separator= ":" ":"
// line.separator= "0xd" "0xd"
//
//
// ------------------------------------------------------
// OS: MacOS 8.0
// Processor: PowerPC 603e
// VM: MRJ 2.1 ea1
// Notes:
// Contributor: MJ
//
// os.name= "Mac OS" "mac os"
// os.arch= "PowerPC" "powerpc"
// os.version= "8" "8"
// java.vendor= "Apple Computer, Inc." "apple computer, inc."
// java.class.version= "45.3" "45.3"
// java.version= "1.1.5" "1.1.5"
// file.separator= "/" "/"
// path.separator= ":" ":"
// line.separator= "0xd" "0xd"
//
//
// ------------------------------------------------------
// OS: MacOS version 8.1
// Processor: PowerPC 750 (?)
// VM: Netscape Navigator
// Notes: Obtained in Netscape Navigator 4.05-98085.
// Contributor: DG
//
// os.name= "Mac OS" "mac os"
// os.arch= "PowerPC" "powerpc"
// os.version= "7.5" "7.5"
// java.vendor= "Netscape Communications Corporation""netscape communications corporation"
// java.class.version= "45.3" "45.3"
// java.version= "1.1.2" "1.1.2"
// file.separator= "/" "/"
// path.separator= ":" ":"
// line.separator= "0xa" "0xa"
//
//
// ------------------------------------------------------
// OS: MacOS version 8.1
// Processor: PowerPC 750 (?)
// VM: Microsoft Virtual Machine
// Notes: Obtained in Internet Explorer 4.01 (PowerPC) with "Java virtual Machine"
// pop-up preference set to "Microsoft Virtual Machine".
// Contributor: DG
//
// os.name= "MacOS" "macos"
// os.arch= "PowerPC" "powerpc"
// os.version= "8.1.0" "8.1.0"
// java.vendor= "Microsoft Corp." "microsoft corp."
// java.class.version= "45.3" "45.3"
// java.version= "1.1.4" "1.1.4"
// file.separator= "/" "/"
// path.separator= ";" ";"
// line.separator= "0xd" "0xd"
//
//
// ------------------------------------------------------
// OS: MacOS version 8.1
// Processor: PowerPC 750 (?)
// VM: Apple MRJ
// Notes: Obtained in Internet Explorer 4.01 (PowerPC) with "Java virtual Machine"
// pop-up preference set to "Apple MRJ".
// Contributor: DG
//
// os.name= "Mac OS" "mac os"
// os.arch= "PowerPC" "powerpc"
// os.version= "8.1" "8.1"
// java.vendor= "Apple Computer, Inc." "apple computer, inc."
// java.class.version= "45.3" "45.3"
// java.version= "1.1.3" "1.1.3"
// file.separator= "/" "/"
// path.separator= ":" ":"
// line.separator= "0xd" "0xd"
//
//
//
//
// --------------------------------------------------------------------------------
//
//
// Linux VMs
//
//
//
// ------------------------------------------------------
// OS: Redhat Linux 5.0
// Processor: Pentium
// VM: blackdown.org JDK1.1.6 v2
// Notes:
// Contributor: CK
//
// os.name= "Linux" "linux"
// os.arch= "x86" "x86"
// os.version= "2.0.31" "2.0.31"
// java.vendor= "Sun Microsystems Inc., ported by Randy Chapman and Steve Byrne""sun microsystems inc., ported by randy
// chapman and steve byrne"
// java.class.version= "45.3" "45.3"
// java.version= "1.1.6" "1.1.6"
// file.separator= "/" "/"
// path.separator= ":" ":"
// line.separator= "0xa" "0xa"
//
//
//
//
//
//
//
//
//
//
// --------------------------------------------------------------------------------
//
//
// Solaris
//
//
//
// ------------------------------------------------------
// OS: Solaris 2.5.1
// Processor: Ultra1
// VM: Sun JDK 1.1.6
// Notes:
// Contributor: MJ
//
// os.name= "Solaris" "solaris"
// os.arch= "sparc" "sparc"
// os.version= "2.x" "2.x"
// java.vendor= "Sun Microsystems Inc." "sun microsystems inc."
// java.class.version= "45.3" "45.3"
// java.version= "1.1.6" "1.1.6"
// file.separator= "/" "/"
// path.separator= ":" ":"
// line.separator= "0xa" "0xa"
//
//
// ------------------------------------------------------
// OS: Solaris 2.5.1
// Processor: Sparc Ultra 1
// VM: Sun JDK 1.1.6
// Notes:
// Contributor: JH
//
// os.name= "Solaris" "solaris"
// os.arch= "sparc" "sparc"
// os.version= "2.x" "2.x"
// java.vendor= "Sun Microsystems Inc." "sun microsystems inc."
// java.class.version= "45.3" "45.3"
// java.version= "1.1.6" "1.1.6"
// file.separator= "/" "/"
// path.separator= ":" ":"
// line.separator= "0xa" "0xa"
//
//
// ------------------------------------------------------
// OS: Solaris
// Processor: sparc
// VM: jdk1.1.6
// Notes:
// Contributor: AB
//
// os.name= "Solaris" "solaris"
// os.arch= "sparc" "sparc"
// os.version= "2.x" "2.x"
// java.vendor= "Sun Microsystems Inc." "sun microsystems inc."
// java.class.version= "45.3" "45.3"
// java.version= "1.1.6" "1.1.6"
// file.separator= "/" "/"
// path.separator= ":" ":"
// line.separator= "0xa" "0xa"
//
//
// ------------------------------------------------------
// OS: SunOS 5.6 (Solaris 2.6)
// Processor: sparc
// VM: JDK 1.1.3
// Notes:
// Contributor: NB
//
// os.name= "Solaris" "solaris"
// os.arch= "sparc" "sparc"
// os.version= "2.x" "2.x"
// java.vendor= "Sun Microsystems Inc." "sun microsystems inc."
// java.class.version= "45.3" "45.3"
// java.version= "1.1.3" "1.1.3"
// file.separator= "/" "/"
// path.separator= ":" ":"
// line.separator= "0xa" "0xa"
//
//
// ------------------------------------------------------
// On my Sun with JDK 1.1.6 I get -
// Contributor: CA
//
// osName=Solaris
// osArch=sparc
// osVersion=2.x
// vendor=Sun Microsystems Inc.
// APIVersion=45.3
// interpreterVersion=1.1.6
//
//
// ------------------------------------------------------
// OS: Solaris 2.5.1
// Processor: UltraSparc
// VM: Javasoft JDK 1.0.2
// Notes:
// Contributor: CA
//
// os.name= "Solaris" "solaris"
// os.arch= "sparc" "sparc"
// os.version= "2.x" "2.x"
// java.vendor= "Sun Microsystems Inc." "sun microsystems inc."
// java.class.version= "45.3" "45.3"
// java.version= "1.0.2" "1.0.2"
// file.separator= "/" "/"
// path.separator= ":" ":"
// line.separator= "0xa" "0xa"
//
//
//
//
//
//
//
//
// --------------------------------------------------------------------------------
//
//
// OS/2
//
//
//
// ------------------------------------------------------
// OS: OS/2
// Processor: Pentium
// VM: OS/2 JDK 1.1.6
// Notes: JDK 1.1.6 IBM build o116-19980728 (JIT: javax), OS/2 Warp 4, FP7
// Contributor: SP
//
// os.name= "OS/2" "os/2"
// os.arch= "x86" "x86"
// os.version= "20.40" "20.40"
// java.vendor= "IBM Corporation" "ibm corporation"
// java.class.version= "45.3" "45.3"
// java.version= "1.1.6" "1.1.6"
// file.separator= "\" "\"
// path.separator= ";" ";"
// line.separator= "0xd,0xa" "0xd,0xa"
//
//
//
//
//
//
//
//
//
//
//
//
// --------------------------------------------------------------------------------
//
//
// Other Unix Systems
//
//
//
// ------------------------------------------------------
// OS: MPE/iX 5.5 (PowerPatch 3)
// Processor: HP 3000/968 (PA-RISC)
// VM: (unknown - HP provided - JDK 1.1.5)
// Notes:
// Contributor: SS
//
// os.name= "MPE/iX" "mpe/ix"
// os.arch= "PA-RISC" "pa-risc"
// os.version= "C.55.00" "c.55.00"
// java.vendor= "HP CSY (freeware)." "hp csy (freeware)."
// java.class.version= "45.3" "45.3"
// java.version= "JDK 1.1.5" "jdk 1.1.5"
// file.separator= "/" "/"
// path.separator= ":" ":"
// line.separator= "0xa" "0xa"
//
//
// ------------------------------------------------------
// here are the results for the latest HP-UX JDK (HP-UX 10.20):
// AS told me (CK) that for HP-UX java.version will always have the same general format;
// the letter ( "C" in the sample ) might change, and there may or may
// not be a date after the version number.
// Contributor: AS
//
// os.name= "HP-UX" "hp-ux"
// os.arch= "PA-RISC" "pa-risc"
// os.version= "B.10.20" "b.10.20"
// java.vendor= "Hewlett Packard Co." "hewlett packard co."
// java.class.version= "45.3" "45.3"
// java.version= "HP-UX Java C.01.15.03 07/07/98""hp-ux java c.01.15.03 07/07/98"
// file.separator= "/" "/"
// path.separator= ":" ":"
// line.separator= "0xa" "0xa"
//
//
// ------------------------------------------------------
// OS: HP-UX
// Processor: PA-RISC
// VM: HP-UX Java C.01.15.01
// Notes: (see the notes in the previous entry)
// Contributor: NB
//
// os.name= "HP-UX" "hp-ux"
// os.arch= "PA-RISC" "pa-risc"
// os.version= "B.10.20" "b.10.20"
// java.vendor= "Hewlett Packard Co." "hewlett packard co."
// java.class.version= "45.3" "45.3"
// java.version= "HP-UX Java C.01.15.01" "hp-ux java c.01.15.01"
// file.separator= "/" "/"
// path.separator= ":" ":"
// line.separator= "0xa" "0xa"
//
//
// ------------------------------------------------------
// OS: AIX 4.3
// Processor: Power
// VM: JDK 1.1.2
// Notes:
// Contributor: NB
//
// os.name= "AIX" "aix"
// os.arch= "Power" "power"
// os.version= "4.3" "4.3"
// java.vendor= "IBM Corporation" "ibm corporation"
// java.class.version= "45.3" "45.3"
// java.version= "1.1.2" "1.1.2"
// file.separator= "/" "/"
// path.separator= ":" ":"
// line.separator= "0xa" "0xa"
//
//
// ------------------------------------------------------
// Contributor: RG
// My AIX 4.1.5 box with Java 1.1.4 reports:
//
// osName = AIX
// osArch = POWER_RS
// osVersion = 4.1
// vendor = IBM Corporation
// APIVersion = 45.3
// interpeterVersion = 1.1.4
//
//
// ------------------------------------------------------
// OS: FreeBSD 2.2.2
// Processor: Intel Pentium
// VM: FreeBSD port of JDK1.0.2 (Jeff Hsu?)
// Notes: This is actually for FreeBSD with the JDK 1.0.2. It probably says Solaris
// since it was a really early port.
// Contributor: CA
//
// os.name= "Solaris" "solaris"
// os.arch= "sparc" "sparc"
// os.version= "2.x" "2.x"
// java.vendor= "Sun Microsystems Inc." "sun microsystems inc."
// java.class.version= "45.3" "45.3"
// java.version= "hsu:11/21/21-22:43" "hsu:11/21/21-22:43"
// file.separator= "/" "/"
// path.separator= ":" ":"
// line.separator= "0xa" "0xa"
//
//
// ------------------------------------------------------
// OS: FreeBSD 2.2.2
// Processor: Intel Pentium
// VM: JDK1.1.6 FreeBSD port
// Notes:
// Contributor: CA
//
// os.name= "FreeBSD" "freebsd"
// os.arch= "x86" "x86"
// os.version= "2.2.2-RELEASE" "2.2.2-release"
// java.vendor= "Sun Microsystems Inc., port by java-port@FreeBSD.org""sun microsystems inc., port by
// java-port@freebsd.org"
// java.class.version= "45.3" "45.3"
// java.version= "1.1.6" "1.1.6"
// file.separator= "/" "/"
// path.separator= ":" ":"
// line.separator= "0xa" "0xa"
//
//
// ------------------------------------------------------
// OS: IRIX 6.3
// Processor: MIPS R10000
// VM: JDK 1.1.6
// Notes:
// Contributor: YA
//
// os.name= "Irix" "irix"
// os.arch= "mips" "mips"
// os.version= "6.3" "6.3"
// java.vendor= "Silicon Graphics Inc." "silicon graphics inc."
// java.class.version= "45.3" "45.3"
// java.version= "3.1.1 (Sun 1.1.6)" "3.1.1 (sun 1.1.6)"
// file.separator= "/" "/"
// path.separator= ":" ":"
// line.separator= "0xa" "0xa"
//
//
// ------------------------------------------------------
// OS: DIGITAL UNIX 4.0
// Processor: ALPHA
// VM: JDK 1.1.5
// Notes:
// Contributor: MK
//
// os.name= "Digital Unix" "digital unix"
// os.arch= "alpha" "alpha"
// os.version= "4.0" "4.0"
// java.vendor= "Digital Equipment Corp." "digital equipment corp."
// java.class.version= "45.3" "45.3"
// java.version= "1.1.5" "1.1.5"
// file.separator= "/" "/"
// path.separator= ":" ":"
// line.separator= "0xa" "0xa"
//
//
//
//
//
//
// --------------------------------------------------------------------------------
//
//
// Other Platforms
//
//
//
// ------------------------------------------------------
// OS: NetWare 4.11
// Processor: Pentium 200
// VM: Novell JVM for NetWare 1.1.5
// Notes:
// Contributor: FJ
//
// os.name= "NetWare 4.11" "netware 4.11"
// os.arch= "x86" "x86"
// os.version= "4.11" "4.11"
// java.vendor= "Novell Inc." "novell inc."
// java.class.version= "45.3" "45.3"
// java.version= "1.1.5 " "1.1.5 "
// file.separator= "\" "\"
// path.separator= ";" ";"
// line.separator= "0xd,0xa" "0xd,0xa"
//
//
