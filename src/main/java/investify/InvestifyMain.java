package investify;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
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
				event.put("volume", Double.parseDouble(record.get("Volume")));

				esper.sendEvent(event, "StockEvent");
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void setupQuery(EPAdministrator esper, Logger log) {
		String expression = "select volume, date, AVG(closing) as avg " + "from StockEvent.win:time(10 seconds)";

		EPStatement epStatement = esper.createEPL(expression);

		epStatement.addListener((EventBean[] newEvents, EventBean[] oldEvents) -> {
			if (newEvents == null || newEvents.length < 1) {
				log.warn("Received null event or length < 1: " + newEvents);
				return;
			}

			EventBean event = newEvents[0];

			log.info("" + event.get("volume"));
			log.info("" + event.get("date"));
			log.info("" + event.get("avg"));
		});

		epStatement.start();
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		Logger log = LoggerFactory.getLogger(InvestifyMain.class);
		Configuration esperClientConfiguration = new Configuration();

		// Setup Esper and define a message Type "StockEvent"
		EPServiceProvider epServiceProvider = EPServiceProviderManager.getDefaultProvider(esperClientConfiguration);
		{
			Map<String, Object> eventDef = new HashMap<>();
			eventDef.put("key", String.class);
			eventDef.put("closing", Double.class);
			eventDef.put("date", java.util.Date.class);
			eventDef.put("volume", Double.class);
			epServiceProvider.getEPAdministrator().getConfiguration().addEventType("StockEvent", eventDef);
		}

		EPRuntime esper = epServiceProvider.getEPRuntime();

		setupQuery(epServiceProvider.getEPAdministrator(), log);
		new Thread(() -> streamToEsper(esper, "google-table.csv", "google")).start();

	}

}
