package za.ac.uct.sakai.healthcheck;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.email.api.EmailService;

public class ServerHealthCheck  {
	private static Log log = LogFactory.getLog(ServerHealthCheck.class);

	private SqlService sqlService;
	public void setSqlService(SqlService sqlService) {
		this.sqlService = sqlService;
	}

	private Integer seconds;
	public void setSeconds(Integer sec) {
		this.seconds = sec;
	}

	private EmailService emailService;	
	public void setEmailService(EmailService emailService) {
		this.emailService = emailService;
	}

	private ServerConfigurationService serverConfigurationService;
	public void setServerConfigurationService(
			ServerConfigurationService serverConfigurationService) {
		this.serverConfigurationService = serverConfigurationService;
	}

	private CheckRunner checkRunner;

	public void init() {
		log.info("init()");
		if (seconds == null) {
			seconds = 30;
		}
		checkRunner = new CheckRunner(seconds.intValue());
	}

	public void destroy() {
		checkRunner.setThreadStop(true);
	}




	private class CheckRunner implements Runnable {

		private Thread thread;
		
		private boolean stopThread = false;
		/**
		 * threshold minutes
		 */
		private int threshold =  30;
		
		public CheckRunner() {
			thread = new Thread(this);
			thread.start();
		}
		
		public CheckRunner(int threshold) {
			thread = new Thread(this);
			thread.start();
			this.threshold = threshold;
		}
		
		public void run() {
			while (!stopThread) {
				try {
					checkServerHealth();
					checkNTP();
					Thread.sleep(5*60*1000);
					//for testing
					//Thread.sleep(10*1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
		
		public void setThreadStop(boolean val) {
			stopThread = val;
		}
		
		private void checkServerHealth() {
			DateTime dt = new DateTime();
			DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
			String strDate = fmt.print(dt);
			Object[] fields = new Object[]{strDate};
			String sql = "select UNIX_TIMESTAMP(?) - UNIX_TIMESTAMP(now())S";
			List<String> ret = sqlService.dbRead(sql, fields, null);
			int seconds = threshold;
			if (ret.size() > 0) {
				String val = ret.get(0);
				Integer intVal = Integer.valueOf(val);
				log.info("got a drift of: " + intVal.toString());
				if (intVal.intValue() > seconds || intVal.intValue() < (seconds * -1)) {
					log.error("Drift is " + intVal + "exceepting threashold of " + threshold);
					String nodeId = serverConfigurationService.getServerId();
					String body = "Server: " + nodeId + " exceeded time drift of " + seconds + " with a value of: " + intVal.intValue();
					emailService.send("help@vula.uct.ac.za", "help-team@vula.uct.ac.za", "Server clock alert", 
							body, null, null, null);
				} else {
					log.debug("in range : " + intVal.toString() + " threshold: " + seconds);
				}
			} else {
				log.warn("query returned no result");
			}
		}
		
		
		private void checkNTP() {
			log.debug("checkNTP()");
			NTPUDPClient client = new NTPUDPClient();
			try {
				String ntpHost = "137.158.128.10";
				InetAddress address = InetAddress.getByName(ntpHost);
				TimeInfo timeInfo = client.getTime(address);
				timeInfo.computeDetails();
				DateTime returnDate = new DateTime(timeInfo.getReturnTime());
				DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
				String strDate = fmt.print(returnDate);
				log.info("Offset to " + ntpHost +" is: " + timeInfo.getOffset() + "ms ntp host time is: " + strDate);
				double offset = timeInfo.getOffset().longValue()/1000D;
				if (offset > seconds || offset < (seconds * -1)) {
					log.error("Drift is from " + ntpHost + " is: "  + offset + "exceeding threashold of " + threshold);
					String nodeId = serverConfigurationService.getServerId();
					String body = "Server: " + nodeId + " exceeded time drift of " + seconds + " with a value of: " + offset + " from: " + ntpHost;
					emailService.send("help@vula.uct.ac.za", "help-team@vula.uct.ac.za", "Server clock alert", 
							body, null, null, null);
				}
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally {
				client.close();
			}
		}
	}

}
