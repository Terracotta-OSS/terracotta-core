#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

module BundledPlugins
    def bundled_plugins(name, directory, spec)
        # verify that the plugins were compiled
        srcdir = @build_results.plugins_home.to_s
        fail "The source directory for the plugins: `#{srcdir}' does not exists" unless File.directory?(srcdir)

        # make sure all the plugins listed in the manifest were built
        spec[:manifest].each { |plugin|
          fail "Unable to locate the jar file for the plugin #{plugin}" unless File.exists?(FilePath.new(srcdir, "#{plugin}.jar").to_s)
        }

        # now copy everything over to the kits' plugins directory
        destdir = FilePath.new(product_directory, directory).ensure_directory
        ant.copy(:todir => destdir.to_s) do
          ant.fileset(:dir => srcdir.to_s, :includes => "*.jar", :excludes => "**/.svn/**, **/.*")
        end
    end
end
