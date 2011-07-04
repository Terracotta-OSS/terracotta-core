
class Util {
        
    static boolean rename(String oldFile, String newFile) {
      return new File(oldFile).renameTo(new File(newFile))  
    }
}