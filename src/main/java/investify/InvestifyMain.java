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

	// This Esper class is responsible for the average calculating stock event
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

	// This is the query setup of the average calculating stock event
	public static void setupQuery(EPAdministrator esper, EPRuntime esper2, Logger log, String stockSymbol, FileWriter writer) {
		String expression = "SELECT longAverage.date, longAverage.closing, AVG(longAverage.closing) AS avg250days, AVG(shortAverage.closing) AS avg50days "
				+ "FROM StockEvent(stockSymbol='" + stockSymbol
				+ "').win:length_batch(250) AS longAverage LEFT OUTER JOIN StockEvent(stockSymbol='" + stockSymbol
				+ "').win:length_batch(50) AS shortAverage";

		EPStatement epStatement = esper.createEPL(expression);

		// Creating a .csv file depending on the stock in which the output of
		// each esper stream gets saved
		// Format of .csv file: stockSymbol(will become important at a later
		// point in time), date, closing, avg50days, avg250days
		try {
			writer.append("StockSymbol");
			writer.append(',');
			writer.append("Date");
			writer.append(',');
			writer.append("Closing");
			writer.append(',');
			writer.append("Avg50Days");
			writer.append(',');
			writer.append("Avg250Days");
			writer.append('\n');

			epStatement.addListener((EventBean[] newEvents, EventBean[] oldEvents) -> {
				if (newEvents == null || newEvents.length < 1) {
					log.warn("Received null event or length < 1: " + newEvents);
					return;
				}

				EventBean event = newEvents[0];

				// Sending the data as a Correlation Event
				Map<String, Object> eventCorrelation = new HashMap<>();
				eventCorrelation.put("stockSymbol", stockSymbol);
				eventCorrelation.put("date", event.get("longAverage.date"));
				eventCorrelation.put("closing", event.get("longAverage.closing"));
				eventCorrelation.put("avg50days", event.get("avg50days"));
				eventCorrelation.put("avg250days", event.get("avg250days"));

				esper2.sendEvent(eventCorrelation, "ResultEvent");
				// Appending the values to the .csv files - according to the
				// structure of the header
				try {
					writer.append(stockSymbol);
					writer.append(',');
					writer.append(String.valueOf(event.get("longAverage.date")));
					writer.append(',');
					writer.append(String.valueOf(event.get("longAverage.closing")));
					writer.append(',');
					writer.append(String.valueOf(event.get("avg50days")));
					writer.append(',');
					writer.append(String.valueOf(event.get("avg250days")));
					writer.append('\n');

					writer.flush();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		epStatement.start();
	}
	
	public static void setupQueryResult(EPAdministrator esper, EPRuntime esper2 ,Logger log) {
		String expressionResult = "select *, SUM(avg250days - avg50days) AS result " + "from ResultEvent";

		EPStatement epStatementResult = esper.createEPL(expressionResult);

		epStatementResult.addListener((EventBean[] newEvents, EventBean[] oldEvents) -> {
			if (newEvents == null || newEvents.length < 1) {
				log.warn("Received null event or length < 1: " + newEvents);
				return;
			}

			EventBean event = newEvents[0];
			
			Map<String, Object> eventCorrelation = new HashMap<>();
			eventCorrelation.put("stockSymbol", event.get("stockSymbol"));
			eventCorrelation.put("date", event.get("date"));
			eventCorrelation.put("closing", event.get("closing"));
			eventCorrelation.put("avg50days", event.get("avg50days"));
			eventCorrelation.put("avg250days", event.get("avg250days"));
			eventCorrelation.put("result", event.get("result"));

			esper2.sendEvent(eventCorrelation, "CorrelationEvent");

		});

		epStatementResult.start();
	}

	// This is the query setup of the average calculating stock event
	public static void setupQueryCorrelation(EPAdministrator esper, Logger log, FileWriter resultWriter,
			int numberOfStocks) {
		String expressionCorrelation = "SELECT * FROM CorrelationEvent.win:length_batch("
				+ numberOfStocks + ") ORDER BY result DESC ";

		EPStatement epStatementCorrelation = esper.createEPL(expressionCorrelation);

		try {
			resultWriter.append("StockSymbol");
			resultWriter.append(',');
			resultWriter.append("Date");
			resultWriter.append(',');
			resultWriter.append("Closing");
			resultWriter.append(',');
			resultWriter.append("Avg50Days");
			resultWriter.append(',');
			resultWriter.append("Avg250Days");
			resultWriter.append(',');
			resultWriter.append("Result");
			resultWriter.append('\n');
			
			epStatementCorrelation.addListener((EventBean[] newEvents, EventBean[] oldEvents) -> {
				if (newEvents == null || newEvents.length < 1) {
					log.warn("Received null event or length < 1: " + newEvents);
					return;
				}
				for(int i = 0; i < numberOfStocks; i++) {
				EventBean event = newEvents[i];
				try {
					resultWriter.append(String.valueOf(event.get("stockSymbol")));
					resultWriter.append(',');
					resultWriter.append(String.valueOf(event.get("date")));
					resultWriter.append(',');
					resultWriter.append(String.valueOf(event.get("closing")));
					resultWriter.append(',');
					resultWriter.append(String.valueOf(event.get("avg50days")));
					resultWriter.append(',');
					resultWriter.append(String.valueOf(event.get("avg250days")));
					resultWriter.append(',');
					resultWriter.append(String.valueOf(event.get("result")));
					resultWriter.append('\n');
					
					resultWriter.flush();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				}
			});
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		epStatementCorrelation.start();
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
		{
			Map<String, Object> eventDef = new HashMap<>();
			eventDef.put("stockSymbol", String.class);
			eventDef.put("date", java.util.Date.class);
			eventDef.put("closing", Double.class);
			eventDef.put("avg50days", Double.class);
			eventDef.put("avg250days", Double.class);
			epServiceProvider.getEPAdministrator().getConfiguration().addEventType("ResultEvent", eventDef);
		}
		{
			Map<String, Object> eventDef = new HashMap<>();
			eventDef.put("stockSymbol", String.class);
			eventDef.put("date", java.util.Date.class);
			eventDef.put("closing", Double.class);
			eventDef.put("avg50days", Double.class);
			eventDef.put("avg250days", Double.class);
			eventDef.put("result", Double.class);
			epServiceProvider.getEPAdministrator().getConfiguration().addEventType("CorrelationEvent", eventDef);
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

		// QuerySetup for Correlation Event
		int numberOfStocks = stockSymbols.length;
		FileWriter resultWriter = new FileWriter("./src/main/resources/Result.csv");
		setupQueryCorrelation(epServiceProvider.getEPAdministrator(), log, resultWriter, numberOfStocks);
		setupQueryResult(epServiceProvider.getEPAdministrator(), esper, log);

		// Here the csv files are downloaded into the resource folder naming
		// convention: stockSymbol.csv
		for (String currentStockSymbol : stockSymbols) {
			URL baseURL = new URL(String.format("http://real-chart.finance.yahoo.com/table.csv?s=%s%s",
					currentStockSymbol, dateString));

			FileWriter writer = new FileWriter(
					String.format("./src/main/resources/%sIncludingAvg.csv", currentStockSymbol));
			writers.add(writer);
			setupQuery(epServiceProvider.getEPAdministrator(), esper, log, currentStockSymbol, writer);

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

		manager.startAsync().awaitHealthy();

		manager.awaitStopped();

		for (Writer w : writers) {
			w.flush();
			w.close();
		}
		resultWriter.flush();
		resultWriter.close();
	}

}
