import operator
import os
import os.path
import string
import java.lang.System

class DSO:

    "Utility to enable/disable DSO in WebSphere AS 6.1"

    def __init__(self, adminTask, verbose=1):
        # Verify our Terracotta environment
        # DSO_JVMARGS will be replaced by real jvmargs from the container test
        self.dsoArgs = ["-Xshareclasses:none", __DSO_JVMARGS]
        self.verbose = verbose
        self.p_info("Terracotta settings: " + str(self.dsoArgs))

        # Keep track of AdminTask, as it is not global to us
        self.AdminTask = adminTask

    def isEnabled(self):
        currentArgList = self.getGenericJvmArguments()
        argListMinusDsoArgs = filter(self.p_filterOutDsoArgs, currentArgList)
        return len(currentArgList) - len(argListMinusDsoArgs) == len(self.dsoArgs)

    def enable(self):
        self.p_info("Enabling DSO...")
        self.p_toggleDso(1)
        
    def disable(self):
        self.p_info("Disabling DSO...")
        self.p_toggleDso(None)

    def getGenericJvmArguments(self):
        return string.split(self.AdminTask.showJVMProperties(['-propertyName', 'genericJvmArguments']))

    def p_toggleDso(self, enable):
        jvmArgList = self.getGenericJvmArguments()
        self.p_info("Original value of genericJvmArguments: " + str(jvmArgList))
        if enable:
            for dsoArg in self.dsoArgs:
                if not operator.contains(jvmArgList, dsoArg): jvmArgList.append(dsoArg)
        else:
            jvmArgList = filter(self.p_filterOutDsoArgs, jvmArgList)
        self.p_info("New value of genericJvmArguments: " + str(jvmArgList))
        self.p_setGenericJvmArguments(jvmArgList)

    def p_setGenericJvmArguments(self, argList):
        self.AdminTask.setJVMProperties(["-genericJvmArguments '" + string.join(argList) + "'"])

    def p_filterOutDsoArgs(self, arg):
        for dsoArg in self.dsoArgs:
            if dsoArg == arg: return None
        return 1

    def p_info(self, s):
        if self.verbose: print "[INFO] " + s

class AppUtil:

    "Utility to deploy WAR files in WebSphere"

    def __init__(self, adminApp, webappDir):
        self.AdminApp = adminApp
        self.webappDir = os.path.abspath(webappDir)

    def installAll(self):
        warFiles = filter(lambda x: len(x) > 4 and string.rfind(x, ".war") == len(x) - 4, os.listdir(self.webappDir))
        for warFile in warFiles:
            if not self.installed(self.appName(warFile)):
                self.install(warFile)
            else:
                print "WebApp[" + warFile + "] is already installed"

    def install(self, warFile):
        appName = self.appName(warFile)
        self.AdminApp.install(os.path.join(self.webappDir, warFile), "-appname " + appName + " -contextroot " + appName + " -usedefaultbindings")
        print "WebApp[" + appName + "] installed"

    def installed(self, appName):
        return operator.contains(string.split(self.AdminApp.list()), appName)

    def appName(self, warFile):
        return string.split(warFile, ".")[0]

