Why is this a separate module? Good Question

This is the only tests in which we need a portable version of
StringBuffer in the boot jar. At the moment we have a mechanism to
build a "custom" boot jar per module, thus this module was born 

I've seen added some tests that require non-default boot jar contents:
- autolocked versions of collections
- Some java.io.* classes that really shouldn't be made portable, but
  since someone might really want to, there is a test for it here
  
