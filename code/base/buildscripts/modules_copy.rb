#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# Extends BuildSubtree with some methods that allow copying various pieces of it to
# any desired directory. This is largely used by the distribution-building system.

class BuildSubtree
  # Copies all .class files from this subtree into the given destdir. If there
  # aren't any -- for example, if you haven't called #compile first -- this will
  # do absolutely nothing.
  def copy_classes(build_results, destdir, ant)
      ant.copy(:todir => destdir.to_s) {
          ant.fileset(:dir => build_results.classes_directory(self).to_s)
      } if FileTest.directory?(build_results.classes_directory(self).to_s)
  end
  
  # Copies the runtime libraries (JAR files), if any, from this build subtree into the given
  # destination directory.
  def copy_runtime_libraries(destdir, ant)
      copy_directories_to(destdir, subtree_only_library_roots(:runtime), ant)
  end

  # Copies the native libraries, if any, from this build subtree into the given
  # destination directory.
  def copy_native_runtime_libraries(destdir, ant, build_environment)
      copy_directories_to(destdir, subtree_only_native_library_roots(build_environment), ant)
  end
  
  private
  # Copies files from a given set of directories into a given destination directory.
  def copy_directories_to(destdir, directories, ant)
      ant.copy(:todir => destdir.to_s) {
          directories.each do |directory|
              ant.fileset(:dir => directory.to_s)
          end
      } unless directories.empty?
  end
end
