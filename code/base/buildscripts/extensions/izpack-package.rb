#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  protected
  def make_package(srcdir, destdir, filename, install_name, internal_name)
    puts "-"*80
    puts "srcdir  : #{srcdir}"
    puts "destdir : #{destdir}"
    puts "filename: #{filename}"
    puts "filename: #{install_name}"
    puts "filename: #{internal_name}"
    puts @static_resources.izpack_installer_template.canonicalize.to_s
    puts "-"*80
    
    template = File.read(@static_resources.izpack_installer_template.canonicalize.to_s)
    files   = Dir.entries(srcdir.to_s)
    scripts = scriptfiles(srcdir.to_s) { |entry| entry =~ /\.(sh|bat)$/ }
    template = ERB.new(template, 0, "%<>").result(binding)
    File.open('installer.xml', template) do |installer|
    end
    
    puts template
    puts "-"*80
  end
  
  private
  def scriptfiles(srcdir)
    result   = []
    excludes = []
    Find.find(srcdir) do |path|
      if FileTest.directory?(path)
        next unless excludes.include?(File.basename(path))
        Find.prune
      else
        next if block_given? && !yield(path) 
        result << path
      end
    end
    result.reverse
  end
end
