#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  protected

  include MavenConstants

  # package sources for terracotta core artifacts
  # only run when maven artifacts are being built
  def postscript(ant, build_environment, product_directory, *args)
    return unless config_source[MAVEN_REPO_CONFIG_KEY]

    args.each do |arg|
      FilePath.new(arg['dest']).ensure_directory
      ant.jar(:jarfile => "#{arg['dest']}/#{arg['artifact']}.jar") do
        ant.fileset(:dir => @basedir.to_s, :includes => arg['includes'], :excludes => arg['excludes'])
      end
    end


    # package terracotaa
  end
end
