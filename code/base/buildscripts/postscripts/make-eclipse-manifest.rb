#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  # - ensure that the contents of the META-INF/MANIFEST.MF file for the
  #   ecliplse plugin contains the up-to-date information in it's 
  #   Bundle-ClassPath entry
  protected
  def postscript(ant, build_environment, product_directory, *args)
    relative_libpath     = args[0]
    eclipse_directory    = FilePath.new(product_directory.to_s, 'eclipse')
    dso_directory        = FilePath.new(eclipse_directory.to_s, 'org.terracotta.dso')
    common_lib_directory = FilePath.new(dso_directory.to_s, *relative_libpath.split('/'))

    plugin_version = create_eclipse_plugin_version(build_environment)

    meta_directory = FilePath.new(dso_directory, 'META-INF').ensure_directory
    manifest_path = FilePath.new(meta_directory, 'MANIFEST.MF')
    File.open(manifest_path.to_s, 'w') do |out|
      out.puts "Manifest-Version: 1.0"
      out.puts "Eclipse-LazyStart: true"
      out.puts "Bundle-ManifestVersion: 2"
      out.puts "Bundle-Name: Terracotta Plugin"
      out.puts "Bundle-SymbolicName: org.terracotta.dso; singleton:=true"
      out.puts "Bundle-Version: " + plugin_version
      out.puts "Bundle-Vendor: Terracotta, Inc."
      out.puts "Bundle-Localization: plugin"
      out.puts "Bundle-RequiredExecutionEnvironment: J2SE-1.5"
      out.puts "Require-Bundle: org.eclipse.ui.editors,"
      out.puts " org.eclipse.debug.core;bundle-version=\"[3.3.0,4.0.0)\","
      out.puts " org.eclipse.debug.ui;bundle-version=\"[3.3.0,4.0.0)\","
      out.puts " org.eclipse.jdt.core;bundle-version=\"[3.3.0,4.0.0)\","
      out.puts " org.eclipse.jdt.debug;bundle-version=\"[3.3.0,4.0.0)\","
      out.puts " org.eclipse.jdt.debug.ui;bundle-version=\"[3.2.100,4.0.0)\","
      out.puts " org.eclipse.jdt.launching;bundle-version=\"[3.3.0,4.0.0)\","
      out.puts " org.eclipse.ui.workbench.texteditor;bundle-version=\"[3.3.0,4.0.0)\","
      out.puts " org.eclipse.core.resources;bundle-version=\"[3.3.0,4.0.0)\","
      out.puts " org.eclipse.jface.text;bundle-version=\"[3.3.0,4.0.0)\","
      out.puts " org.eclipse.core.runtime;bundle-version=\"[3.3.100,4.0.0)\","
      out.puts " org.eclipse.ui;bundle-version=\"[3.3.0,4.0.0)\","
      out.puts " org.eclipse.ui.ide;bundle-version=\"[3.3.0,4.0.0)\","
      out.puts " org.eclipse.jdt.ui;bundle-version=\"[3.3.0,4.0.0)\","
      out.puts " org.eclipse.ltk.core.refactoring;bundle-version=\"[3.3.0,4.0.0)\","
      out.puts " org.eclipse.ui.console;bundle-version=\"[3.2.0,4.0.0)\","
      out.puts " org.eclipse.search;bundle-version=\"[3.3.0,4.0.0)\","
      out.puts " org.eclipse.pde.core;bundle-version=\"[3.3.0,4.0.0)\","
      out.puts " org.eclipse.pde.ui;bundle-version=\"[3.3.0,4.0.0)\","
      out.puts " org.junit;bundle-version=\"[3.8.2,4.0.0)\","
      out.puts " org.eclipse.jdt.junit;bundle-version=\"[3.3.0,4.0.0)\","
      out.puts " org.eclipse.core.variables;bundle-version=\"[3.2.0,4.0.0)\","
      out.puts " org.eclipse.ui.forms;bundle-version=\"[3.2.0,4.0.0)\""

      libfiles = Dir.entries(common_lib_directory.to_s).delete_if { |item| /\.jar$/ !~ item } << "resources/"
      libfiles.sort!
      out.puts "Bundle-ClassPath: #{relative_libpath}/#{libfiles.first},"
      libfiles[1..-1].each { |item| out.puts " #{relative_libpath}/#{item}," }
      out.puts " #{relative_libpath}/resources"
      out.puts "Bundle-Activator: org.terracotta.dso.TcPlugin"
    end  

    destdir = dso_directory.to_s + "_" + plugin_version
    ant.move(:file => dso_directory.to_s, :tofile => destdir.to_s)
  end

  def create_eclipse_plugin_version(build_environment)
    tc_version = build_environment.version
    if tc_version =~ /trunk/
      tc_version = build_environment.maven_version
    end
    if tc_version =~ /(\d)+\.(\d)+\.(\d)+(-\w+)?/
      main_version = "#{$1}.#{$2}.#{$3}"
      tag = $4 || ''
      tag = tag.gsub(/-/, '_')
      qualifier = "r#{build_environment.os_revision}_v#{Time.now.strftime('%Y%m%d')}#{tag}"
      eclipse_version = "#{main_version}.#{qualifier}"
    else
      fail("TC version #{tc_version} doesn't match expected pattern: x.y.z or x.y.z-tag")
    end
    eclipse_version
  end
end
