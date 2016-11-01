package ik.analyzer.help;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Sleep {
    private static Log logger = LogFactory.getLog(Sleep.class);


	public enum Type{MSEC,SEC,MIN,HOUR};
	public static void sleep(Type type,int num){
		try {
			switch(type){
				case MSEC:
					Thread.sleep(num);
					return;
				case SEC:
					Thread.sleep(num*1000);
					return;
				case MIN:
					Thread.sleep(num*60*1000);
					return;
				case HOUR:
					Thread.sleep(num*60*60*1000);
					return;
				default:
					System.err.println("输入类型错误，应为MSEC,SEC,MIN,HOUR之一");
					return;
			}
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
	}


}
