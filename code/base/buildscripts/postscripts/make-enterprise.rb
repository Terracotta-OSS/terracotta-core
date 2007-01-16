#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  # - set the execute permission of all the script files in the kit
  protected
  def postscript(ant, build_environment, product_directory, *args)
    ent_top = FilePath.new(@basedir, '..', '..', '..').canonicalize
    notices_dir = FilePath.new(ent_top, 'kits', 'source', 'docs', 'distribute', 'notices').canonicalize
    ant.copy(:todir => product_directory.to_s, :overwrite => true) {
      ant.fileset(:dir => notices_dir.to_s, :includes => '*.txt')
    }
  end
end
