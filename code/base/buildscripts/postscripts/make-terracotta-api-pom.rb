#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  # - create the eclipse update site
  protected
  def postscript(ant, build_environment, product_dir, *args)
    require 'builder'
    File.open(FilePath.new(product_dir, "pom.xml").to_s, File::CREAT) do |out|
      xml = Builder::XmlMarkup.new(:target => out, :indent => 3)
      xml.project do
        xml.modelVersion "4.0.0"
        xml.groupId "org.terracotta"
        xml.artifactId "terracotta-api"
        xml.version interpolate(args[0])
        xml.packaging "jar"
        xml.name "Terracotta API"
        xml.url "http://kong/maven2"
        xml.description "Terracotta API"
        xml.licenses do
          xml.license do 
            xml.name "The Terracotta API License, Version 1.0"
            xml.url "http://www.terracotta.org/licenses/Terracotta-API-LICENSE-1.0.txt"
            xml.distribution "repo"
          end
        end
        xml.scm do
          xml.url "https://svn.terracotta.org/repo/tc/dso/trunk"
        end
      end
    end
  end
end
