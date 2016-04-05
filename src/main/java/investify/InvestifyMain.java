package investify;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;

import de.uniluebeck.itm.util.logging.Logging;

public class InvestifyMain {

	static {
		setupLogging();
	}
		
	public static void setupLogging() {
		// Optionally remove existing handlers attached to j.u.l root logger
		SLF4JBridgeHandler.removeHandlersForRootLogger(); // (since SLF4J 1.6.5)

		// add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
		// the initialization phase of your application
		SLF4JBridgeHandler.install();

		Logging.setLoggingDefaults();
	}

	public static void streamToEsper(EPRuntime esper, String file, String key) {
		InputStream instream = InvestifyMain.class.getClassLoader().getResourceAsStream(file);
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

		Iterable<CSVRecord> records;
		try {
			records = CSVFormat.DEFAULT.withHeader("Date", "Open", "High", "Low", "Close", "Volume", "Adj Close")
					.withSkipHeaderRecord()
					.parse(new InputStreamReader(instream));

			for (CSVRecord record : records) {
				Map<String, Object> event = new HashMap<>();
				event.put("key", key);
				event.put("closing", Double.parseDouble(record.get("Close")));
				event.put("date", format.parse(record.get("Date")));

				esper.sendEvent(event, "StockEvent");
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void setupQuery(EPAdministrator esper, Logger log) {
		String expression = "select date, AVG(closing) as avg " + "from StockEvent.win:time(10 seconds)";

		EPStatement epStatement = esper.createEPL(expression);

		epStatement.addListener((EventBean[] newEvents, EventBean[] oldEvents) -> {
			if (newEvents == null || newEvents.length < 1) {
				log.warn("Received null event or length < 1: " + newEvents);
				return;
			}

			EventBean event = newEvents[0];

			log.info("" + event.get("date"));
			log.info("" + event.get("avg"));
		});

		epStatement.start();
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		Logger log = LoggerFactory.getLogger(InvestifyMain.class);
		Configuration esperClientConfiguration = new Configuration();
		
		//Get Dates for URL-driven download of .csv
		int currentDay = new GregorianCalendar().get(Calendar.DAY_OF_MONTH);
		int currentMonth = new GregorianCalendar().get(Calendar.MONTH);
		int currentYear = new GregorianCalendar().get(Calendar.YEAR);
		
		//Add one to month because GregorianCalendar sees January as 0 - Yahoo Finance sees January as 1
		currentMonth++;
		
		//Logic for past 210 days
		int pastDay = currentDay;
		int pastMonth = currentMonth - 7;
		int pastYear;
		if(pastMonth <= 0) {
			pastMonth = 12 + pastMonth;
			pastYear = currentYear - 1;
		} else {
			pastYear = currentYear;
		}
		
		//Creating URLs for the download of .csv files
		URL baseURLGoogle = null;
		URL baseURLApple = null;
		URL baseURLAmazon = null;
		URL baseURLFacebook = null;
		URL baseURLMicrosoft = null;
		URL baseURLTwitter = null;
		
		//Add-on for correct download
		String dateString = String.format("%d&e=%d&f=%d&g=d&a=%d&b=%d&c=%d&ignore=.csv", currentMonth, currentDay, currentYear, pastMonth, pastDay, pastYear );
		try {
			baseURLGoogle = new URL("http://real-chart.finance.yahoo.com/table.csv?s=GOOG&d=%d" +dateString);
			baseURLApple = new URL("http://real-chart.finance.yahoo.com/table.csv?s=APPL&d=" +dateString);
			baseURLAmazon = new URL("http://real-chart.finance.yahoo.com/table.csv?s=AMZN&d=" +dateString);
			baseURLFacebook = new URL("http://real-chart.finance.yahoo.com/table.csv?s=FB&d=" +dateString);
			baseURLMicrosoft = new URL("http://real-chart.finance.yahoo.com/table.csv?s=MSFT&d=" +dateString);
			baseURLTwitter = new URL("http://real-chart.finance.yahoo.com/table.csv?s=TWTR&d=" +dateString);
			
			InputStream in = baseURLGoogle.openStream();
			Path path = Paths.get("./src/main/resources/google.csv");
			Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
			in.close();
			} catch (MalformedURLException e) {
			   log.info("MalformedURLException");
			} catch (NoSuchFileException e) {
				log.info("FileException occured");
			}

		// Setup Esper and define a message Type "StockEvent"
		EPServiceProvider epServiceProvider = EPServiceProviderManager.getDefaultProvider(esperClientConfiguration);
		{
			Map<String, Object> eventDef = new HashMap<>();
			eventDef.put("key", String.class);
			eventDef.put("closing", Double.class);
			eventDef.put("date", java.util.Date.class);
			epServiceProvider.getEPAdministrator().getConfiguration().addEventType("StockEvent", eventDef);
		}

		EPRuntime esper = epServiceProvider.getEPRuntime();

		setupQuery(epServiceProvider.getEPAdministrator(), log);
		new Thread(() -> streamToEsper(esper, "google-table.csv", "google")).start();

	}

}
