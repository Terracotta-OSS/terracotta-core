#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

module BundledDemos
  class AntBuildScriptError < Exception; end
  def bundled_demos(name, directory, spec)
    srcdir = FilePath.new(@static_resources.demos_directory, name)
    fail "The source for the named demo set: `#{name}' does not exists in #{@static_resources.demos_directory}" unless File.directory?(srcdir.to_s)

    include_dir = FilePath.new(srcdir, 'include')
    has_include_dir = include_dir.directory?

    wildcard = '/**/*'
    manifest = (spec[:manifest] || []).join("#{wildcard}, ")
    puts :warn, "The demo set: `#{name}' has an empty manifest." if manifest.empty?

    unless manifest.empty?
      # copy demo sources but exclude non-native scripts
      destdir    = FilePath.new(product_directory, directory).ensure_directory
      includes   = ["#{manifest}#{wildcard}", (Dir.entries(srcdir.to_s).delete_if { |entry|
            #File.directory?(FilePath.new(srcdir, entry).to_s) || non_native.include?(entry.gsub(/.+\./, '*.'))
            File.directory?(FilePath.new(srcdir, entry).to_s)
          }).join(", ")].join(", ").sub(/, $/, "")
      ant.copy(:todir => destdir.to_s) do
        #ant.fileset(:dir => srcdir.to_s, :excludes => "**/.svn/**, **/.*, **/*/#{non_native.join(', **/*/')}", :includes => includes)
        ant.fileset(:dir => srcdir.to_s, :excludes => "**/.svn/**, **/.*", :includes => includes)
      end

      # check that every demo listed in the manifest was copied      
      demos = Dir.entries(destdir.to_s)
      manifest.split("#{wildcard}, ").each do |entry|
        fail "The demo `#{name}/#{entry}' was not copied. Please check to make sure that the sources for this demo exist.'" unless demos.include? entry
        demo_directory = FilePath.new(product_directory, directory, entry).to_s
        Dir.chdir(demo_directory) do
          # and make sure it can be rebuilt
          begin
            build_script = 'build.xml'

            # inject api version to build.xml of chatter and sharedqueue
            @ant.replace(:token => "@tim-api.version@", :value => @config_source['tim-api.version'], :file => File.expand_path(build_script))
            
            raise AntBuildScriptError.new, "Unable to pre-compile the sample application `#{demo_directory}, the Ant build script `#{build_script}' is missing." unless File.exists?(build_script)
            if File.exists?('DO-NOT-PRE-COMPILE')
              puts :warn, "I did not pre-compile the sample application `#{demo_directory}'; "
              puts :warn, "remove the `DO-NOT-PRE-COMPILE' file from this sample application's directory "
              puts :warn, "if you want it to be."
              File.delete('DO-NOT-PRE-COMPILE')
            else
              ant_script = @static_resources.ant_script
              result = %x[#{ant_script}]
              puts "#{result}"
              unless result =~ /BUILD SUCCESSFUL/
                fail("Error running ant in #{Dir.getwd}")
              end
            end

            if has_include_dir
              ant.copy(:todir => demo_directory) do
                ant.fileset(:dir => include_dir.to_s)
              end
            end
          rescue AntBuildScriptError => error
            fail "There was a problem compiling the demo `#{name}/#{entry}';\n#{error.message}"
          end
        end
      end
    end
  end
end
