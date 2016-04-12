package investify;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
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
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;

import de.uniluebeck.itm.util.logging.Logging;

public class InvestifyMain {

	static {
		setupLogging();
	}

	public static void setupLogging() {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		Logging.setLoggingDefaults();
	}

	public static class StreamToEsper extends AbstractExecutionThreadService {
		private EPRuntime esper;
		private String file;
		String key;

		public StreamToEsper(EPRuntime esper, String file, String key) {
			super();
			this.esper = esper;
			this.file = file;
			this.key = key;
		}

		@Override
		protected void run() throws Exception {
			InputStream instream = InvestifyMain.class.getClassLoader().getResourceAsStream(file);
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

			Iterable<CSVRecord> records;
			try {
				records = CSVFormat.DEFAULT.withHeader("Date", "Open", "High", "Low", "Close", "Volume", "Adj Close")
						.withSkipHeaderRecord().parse(new InputStreamReader(instream));

				for (CSVRecord record : records) {
					Map<String, Object> event = new HashMap<>();
					event.put("stockSymbol", key);
					event.put("closing", Double.parseDouble(record.get("Close")));
					event.put("date", format.parse(record.get("Date")));

					esper.sendEvent(event, "StockEvent");
				}

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

	}

	public static void setupQuery(EPAdministrator esper, Logger log, String stockSymbol, FileWriter writer) {
		String expression = "SELECT date, closing, AVG(closing) AS avgFiftyDays " + "FROM StockEvent(stockSymbol='"
				+ stockSymbol + "').win:length_batch(252)";

		EPStatement epStatement = esper.createEPL(expression);

		// Creating a .csv file depending on the stock in which the output of
		// each esper stream gets saved
		// Format of .csv file: stockSymbol(will become important at a later
		// point in time), date, closing, avg50days, avg200days
		try {
			writer.append("stockSymbol");
			writer.append(',');
			writer.append("date");
			writer.append(',');
			writer.append("closing");
			writer.append(',');
			writer.append("avgFiftyDays");
			writer.append(',');
			writer.append("avg200days");
			writer.append('\n');

			epStatement.addListener((EventBean[] newEvents, EventBean[] oldEvents) -> {
				if (newEvents == null || newEvents.length < 1) {
					log.warn("Received null event or length < 1: " + newEvents);
					return;
				}

				EventBean event = newEvents[0];

				// Appending the values to the .csv files - according to the
				// structure of the header
				try {
					writer.append(stockSymbol);
					writer.append(',');
					writer.append(String.valueOf(event.get("date")));
					writer.append(',');
					writer.append(String.valueOf(event.get("closing")));
					writer.append(',');
					writer.append(String.valueOf(event.get("avgFiftyDays")));
					writer.append(',');
					writer.append(String.valueOf(event.get("avgFiftyDays")));
					writer.append('\n');

					writer.flush();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// This is just for debugging purposes - delete later
				log.info("{}: date = {}, avg = {}",
						new Object[] { stockSymbol, event.get("date"), event.get("avgFiftyDays") });
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		epStatement.start();
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		Logger log = LoggerFactory.getLogger(InvestifyMain.class);
		Configuration esperClientConfiguration = new Configuration();

		// Setup Esper and define a message Type "StockEvent"
		EPServiceProvider epServiceProvider = EPServiceProviderManager.getDefaultProvider(esperClientConfiguration);
		{
			Map<String, Object> eventDef = new HashMap<>();
			eventDef.put("stockSymbol", String.class);
			eventDef.put("closing", Double.class);
			eventDef.put("date", java.util.Date.class);
			epServiceProvider.getEPAdministrator().getConfiguration().addEventType("StockEvent", eventDef);
		}

		EPRuntime esper = epServiceProvider.getEPRuntime();

		// Get Dates for URL-driven download of .csv
		int currentDay = new GregorianCalendar().get(Calendar.DAY_OF_MONTH);
		int currentMonth = new GregorianCalendar().get(Calendar.MONTH);
		int currentYear = new GregorianCalendar().get(Calendar.YEAR);

		// Logic for past 210 days
		int pastDay = currentDay;
		int pastMonth = currentMonth;
		int pastYear = currentYear - 1;

		// Add-on for correct download
		String dateString = String.format("&d=%d&e=%d&f=%d&g=d&a=%d&b=%d&c=%d&ignore=.csv", currentMonth, currentDay,
				currentYear, pastMonth, pastDay, pastYear);

		// Stock symbols array - these Stocks will be analyzed
		String[] stockSymbols = { "AAPL", "AMZN", "FB", "GOOG", "MSFT", "TWTR" };
		List<Service> services = new ArrayList<>();
		List<Writer> writers = new ArrayList<>();

		// Here the csv files are downloaded into the resource folder naming
		// convention: stockSymbol.csv
		for (String currentStockSymbol : stockSymbols) {
			URL baseURL = new URL(String.format("http://real-chart.finance.yahoo.com/table.csv?s=%s%s",
					currentStockSymbol, dateString));

			FileWriter writer = new FileWriter(
					String.format("./src/main/resources/%sIncludingAvg.csv", currentStockSymbol));
			writers.add(writer);
			setupQuery(epServiceProvider.getEPAdministrator(), log, currentStockSymbol, writer);

			// Writing the .csv file
			InputStream in = baseURL.openStream();
			Path path = Paths.get(String.format("./src/main/resources/%s.csv", currentStockSymbol));
			Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
			in.close();

			StreamToEsper streamToEsper = new StreamToEsper(esper, String.format("%s.csv", currentStockSymbol),
					currentStockSymbol);
			services.add(streamToEsper);
		}

		ServiceManager manager = new ServiceManager(services);
		log.info("Starting services");

		manager.startAsync().awaitHealthy();

		log.info("Started {} services", services.size());
		manager.awaitStopped();
		log.info("All {} services stopped", services.size());

		for (Writer w : writers) {
			w.flush();
			w.close();
		}

	}

}
