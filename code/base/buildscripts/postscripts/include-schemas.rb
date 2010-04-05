#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  # - include the schema files for the configuration into the kit
  # - generate a javadoc style documentation for the schemas
  protected
  
  def extract_schema_version(srcdir)
    xsd_file = nil
    version = "1"
    Dir.entries(srcdir.to_s).each do |f|
      if f =~ /terracotta-(\d+)\.xsd/i
        xsd_file = FilePath.new(srcdir, f)
        version = $1
      end
    end
    raise("Cannot find any schema file in #{srcdir}") if xsd_file.nil?
    return xsd_file, version
  end
  
  def postscript(ant, build_environment, product_directory, *args)
    if @no_schema || @no_extra
      loud_message("--no-schema found. Skipping")
      return
    end

    unless @static_resources.docflex_home.nil?
      tmp_schema_dir = File.join(@distribution_results.build_dir.to_s, "schema")
      puts "creating schema in temp schema dir: #{tmp_schema_dir}"
      create_schema(tmp_schema_dir)
      args.each do | arg |
        puts "arg #{arg}"
        destdir = FilePath.new(product_directory, *arg.split('/')).ensure_directory
        puts "copy schema into #{destdir.to_s}"
        ant.copy(:todir => destdir.to_s) do
          ant.fileset(:dir => tmp_schema_dir)
        end
      end
      puts "Deleting tmp schema dir..."
      FileUtils.rm_rf(tmp_schema_dir)
    end
  end

  def create_schema(destdir)
    srcdir = @static_resources.config_schema_source_directory(@module_set)

    ant.copy(:todir => destdir.to_s) do
      ant.fileset(:dir => srcdir.to_s, :includes => 'terracotta*.xsd')
    end

    docflex           = FilePath.new(@static_resources.docflex_home)
    docflex_lib       = FilePath.new(docflex, "lib")
    docflex_template  = FilePath.new(docflex, "templates", "XSDDoc", "FramedDoc.tpl")
    docflex_classpath = "#{docflex_lib.to_s}/xercesImpl.jar:#{docflex_lib.to_s}/xml-apis.jar:#{docflex_lib.to_s}/docflex-xml-re.jar"
    xsd_file, schema_version  = extract_schema_version(destdir)

    ant.java(
      :classname   => 'com.docflex.xml.Generator',
      :classpath   => docflex_classpath,
      :fork        => true,
      :failonerror => true,
      :dir         => destdir.to_s,
      :maxmemory   => '512m') do
      ant.jvmarg(:value => "-Djava.awt.headless=true")
      ant.arg(:line => "-template #{docflex_template.canonicalize.to_s}")
      ant.arg(:line => "-format HTML")
      ant.arg(:line => "-p:docTitle=\"Terracotta Configuration Schema Version #{schema_version}\"")
      ant.arg(:line => "-d #{destdir.to_s}")
      ant.arg(:line => "-f index.html")
      ant.arg(:line => "-nodialog=true")
      ant.arg(:line => "-quiet=true")
      ant.arg(:line => "-launchviewer=false")
      ant.arg(:line => File.basename(xsd_file.to_s))
    end
  end
end
