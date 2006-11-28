class BaseCodeTerracottaBuilder <  TerracottaBuilder
  # - Remove samples directory
  protected
  def postscript(ant, build_environment, product_directory, *args)
    args.each do |arg|
      destdir = FilePath.new(product_directory, *arg.split('/'))
      destdir.delete_recursively(ant)
    end
  end
end
