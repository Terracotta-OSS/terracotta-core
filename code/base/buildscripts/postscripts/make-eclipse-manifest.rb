#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
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

    plugin_version = version = get_config(:version, "1.0.0")
    plugin_version += ".0" if /[0-9]+.[0-9]+.[0-9]+/ !~ version 

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
      out.puts "Require-Bundle: org.eclipse.ui.editors,"
      out.puts " org.eclipse.debug.core,"
      out.puts " org.eclipse.debug.ui,"
      out.puts " org.eclipse.jdt.core,"
      out.puts " org.eclipse.jdt.debug,"
      out.puts " org.eclipse.jdt.debug.ui,"
      out.puts " org.eclipse.jdt.launching,"
      out.puts " org.eclipse.ui.workbench.texteditor,"
      out.puts " org.eclipse.core.resources,"
      out.puts " org.eclipse.jface.text,"
      out.puts " org.eclipse.core.runtime,"
      out.puts " org.eclipse.ui,"
      out.puts " org.eclipse.ui.ide,"
      out.puts " org.eclipse.jdt.ui,"
      out.puts " org.eclipse.help,"
      out.puts " org.eclipse.update.core,"
      out.puts " org.eclipse.ltk.core.refactoring"
      libfiles = Dir.entries(common_lib_directory.to_s).delete_if { |item| /\.jar$/ !~ item }
      out.puts "Bundle-ClassPath: #{relative_libpath}/#{libfiles.first},"
      libfiles[1..-2].each { |item| out.puts " #{relative_libpath}/#{item}," }
      out.puts " #{relative_libpath}/#{libfiles.last}"
      out.puts "Bundle-Activator: org.terracotta.dso.TcPlugin"
    end  

    destdir = dso_directory.to_s + "_" + version
    ant.move(:file => dso_directory.to_s, :tofile => destdir.to_s)
  end
end
