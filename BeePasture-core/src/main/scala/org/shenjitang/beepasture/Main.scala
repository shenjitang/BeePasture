package org.shenjitang.beepasture;
import java.io.File;
import java.io.FileNotFoundException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.shenjitang.beepasture.core._

object Main {
  val MAIN_LOGGER = LogFactory.getLog("org.shenjitang.beepasture.Main")
  def main(args: Array[String]) {
     val fileEncoding = System.getProperty("fileEncoding", "utf8");
     val fileName = if (args.length > 0) args(0) else "D:\\workspace\\神机堂\\GitHub\\BeePasture\\examples\\163_news.yml"
     MAIN_LOGGER.info("start fileEncoding=" + fileEncoding + " script=" + fileName);
     try {
         val file = new File(fileName);
         val yaml = FileUtils.readFileToString(file, fileEncoding);
         val webGather = new BeeGather(yaml);
         webGather.init();
         webGather.doGather();
         webGather.saveTo();
         MAIN_LOGGER.info("finish fileEncoding=" + fileEncoding + " script=" + fileName);
         if (args.length > 1 && "-d".equalsIgnoreCase(args(1))) {
             while(true) {
                 try {
                     Thread.sleep(60000L);
                 } catch {
                   case ex: Exception => MAIN_LOGGER.warn("sleep", ex)
                 }
             }
         } else {
             System.exit(0);
         }
     } catch  {
         case ex: FileNotFoundException => MAIN_LOGGER.warn(fileName + " 文件不存在！")
     }
  }
}
