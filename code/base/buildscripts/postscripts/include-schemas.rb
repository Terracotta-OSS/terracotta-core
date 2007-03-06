#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  # - include the schema files for the configuration into the kit
  # - generate a javadoc style documentation for the schemas
  protected
  def postscript(ant, build_environment, product_directory, *args)
    return if @static_resources.docflex_home.nil? 
    
    srcdir = @static_resources.config_schema_source_directory(@module_set)
    tmpdir = FilePath.new(product_directory, 'tmp').ensure_directory

    ant.copy(:todir => tmpdir.to_s) do
      ant.fileset(:dir => srcdir.to_s, :includes => '*.xsd')
    end

    docflex           = FilePath.new(@static_resources.docflex_home)
    docflex_lib       = FilePath.new(docflex, "lib")
    docflex_template  = FilePath.new(docflex, "templates", "XSDDocFrames.tpl")
    docflex_classpath = "#{docflex_lib.to_s}/xercesImpl.jar:#{docflex_lib.to_s}/xml-apis.jar:#{docflex_lib.to_s}/docflex-xml-kit.jar"
    srcfiles          = FilePath.new(srcdir, "terracotta-2.3.xsd")

    ant.java(
      :classname   => 'com.docflex.xml.Generator',
      :classpath   => docflex_classpath,
      :fork        => true,
      :failonerror => true,
      :dir         => tmpdir.to_s,
      :maxmemory   => '512m') do
      ant.jvmarg(:value => "-Djava.awt.headless=true")
      ant.arg(:line => "-xmlconfig xmltypes.config")
      ant.arg(:line => "-template #{docflex_template.canonicalize.to_s}")
      ant.arg(:line => "-format HTML")
      ant.arg(:line => "-p:docTitle=\"Terracotta Schemas\"")
      ant.arg(:line => "-d #{tmpdir.to_s}")
      ant.arg(:line => "-f tc-config")
      ant.arg(:line => "-nodialog=true")
      ant.arg(:line => "-quiet=true")
      ant.arg(:line => "-launchviewer=false")
      ant.arg(:line => srcfiles.to_s)
    end

    index_file = FilePath.new(tmpdir.to_s, 'index.html')
    File.open(index_file.to_s, 'w') do |out|
      out.puts "<html>"
      out.puts " <head>"
      out.puts "   <META HTTP-EQUIV=\"REFRESH\" CONTENT=\"0; URL=tc-config.html\">"
      out.puts " </head>"
      out.puts "</html>"
    end

    args.each do |arg|
      destdir = FilePath.new(product_directory, *arg.split('/'))
      ant.copy(:todir => destdir.to_s) do
        ant.fileset(:dir => tmpdir.to_s)
      end
    end
    tmpdir.delete
  end
end
