#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  protected
  def make_package(srcdir, destdir, filename, install_name, internal_name)
    destfile = FilePath.new("#{destdir}", "#{filename}.tar.gz")
    ant.tar(:destfile => destfile.to_s, :compression => 'gzip', :longfile => 'gnu') do
      ant.tarfileset(:dir => srcdir.to_s, :prefix => install_name, :excludes => "**/*.dll **/*.sh **/*.bat **/*.exe **/bin/** **/libexec/**") 
      ant.tarfileset(:dir => srcdir.to_s, :prefix => install_name, :includes => "**/*.dll **/*.sh **/*.bat **/*.exe **/bin/** **/libexec/**", :mode => 755) 
    end
    destfile
  end
end
