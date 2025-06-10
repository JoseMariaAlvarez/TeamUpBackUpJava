import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
// import net.fortuna.ical4j.data.CalendarOutputter;
// import net.fortuna.ical4j.model.Calendar;
// import net.fortuna.ical4j.model.component.VEvent;
// import net.fortuna.ical4j.model.property.DtEnd;
// import net.fortuna.ical4j.model.property.DtStart;
// import net.fortuna.ical4j.model.property.ProdId;
// import net.fortuna.ical4j.model.property.Summary;
// import net.fortuna.ical4j.model.property.Uid;
// import net.fortuna.ical4j.model.property.Version;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
// import java.nio.charset.StandardCharsets;
// import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TeamUpBackup {

    private static final String API_URL = "https://api.teamup.com/";
    private final static String CONFIG_FILE_PATH = "./configuration.properties";

    private static final Logger logger = LoggerFactory.getLogger(TeamUpBackup.class);

    public static void main(String[] args) {
        Properties properties = new Properties();
        String apiKey = null;
        String calendarId = null;
        String startDate = null;
        String endDate = null;
        String backupFilePathPrefix = null;

        logger.info("Starting Teamup backup {}", new Date());
        try {
            properties.load(new FileInputStream(new File(CONFIG_FILE_PATH)));

            apiKey = (String) properties.getProperty("API_KEY", "");
            calendarId = (String) properties.getProperty("CALENDAR_ID", "");
            startDate = (String) properties.getProperty("START_DATE", "");
            endDate = (String) properties.getProperty("END_DATE", "");
            backupFilePathPrefix = (String) properties.getProperty("BACKUP_FILE_PATH_PREFIX", "");
            if (apiKey.equals("")) {
                logger.error("API key not found in configuration file {}", CONFIG_FILE_PATH);
                System.exit(1);
            }
            if (calendarId.equals("")) {
                logger.error("Calendar id. not found in configuration file {}", CONFIG_FILE_PATH);
                System.exit(1);
            }
            if (startDate.equals("")) {
                logger.error("Start date not found in configuration file {}", CONFIG_FILE_PATH);
                System.exit(1);
            }
            if (endDate.equals("")) {
                logger.error("End date not found in configuration file {}", CONFIG_FILE_PATH);
                System.exit(1);
            }
            if (backupFilePathPrefix.equals("")) {
                logger.error("Backup file path prefix not found in configuration file {}", CONFIG_FILE_PATH);
                System.exit(1);
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            try {
                LocalDate.parse(startDate, formatter);
            } catch (DateTimeParseException e) {
                logger.error("Invalid start date format in configuration file {}. Expected yyyy-MM-dd: {}", 
                             CONFIG_FILE_PATH, e.getMessage());
                System.exit(1);
            }
            try {
                LocalDate.parse(endDate, formatter);
            } catch (DateTimeParseException e) {
                logger.error("Invalid end date format in configuration file {}. Expected yyyy-MM-dd: {}", 
                             CONFIG_FILE_PATH, e.getMessage());
                System.exit(1);
            }
        } catch (FileNotFoundException e) {
            logger.error("Configuration file {} not found: {}", CONFIG_FILE_PATH, e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            logger.error("Error reading configuration file {}: {}", CONFIG_FILE_PATH, e.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException e) {
            logger.error("Input stream contains a malformed Unicode escape sequence in configuration file {}: {}", 
                         CONFIG_FILE_PATH, e.getMessage());
            System.exit(1);
        }

        // get current day, month and year as a string separated by a dash
        LocalDate date = LocalDate.now();
        // String backupFilePathIcs = backupFilePathPrefix
        // + calendarId + "-" + date.getYear()
        // + "-" + date.getMonthValue()
        // + "-" + date.getDayOfMonth()
        // + ".ics";
        String backupFilePathCsv = backupFilePathPrefix
                + calendarId + "-" + date.getYear()
                + "-" + date.getMonthValue()
                + "-" + date.getDayOfMonth()
                + ".csv";

        Map<Long, String> subCalendarsMap = null;
        try {
            String subCalendars = fetchSubCalendars(apiKey, calendarId);
            subCalendarsMap = getSubcalendarIds(subCalendars);
        } catch (IOException e) {
            logger.error("Error getting subcalendars: {}", e.getMessage());
        }
        String eventsJson = null;
        try {
            eventsJson = fetchCalendarEvents(apiKey, calendarId, startDate,
                    endDate);
        } catch (IOException e) {
            logger.error("Error fetching calendar events: {}", e.getMessage());
            System.exit(1);
        }
        // try {
        // String eventsExampleFile = "./events-example.json";
        // StringBuilder sb = new StringBuilder();
        // Files.readAllLines(Paths.get(eventsExampleFile),
        // StandardCharsets.UTF_8).stream().forEach(l -> sb.append(l));
        // eventsJson = sb.toString();
        // } catch (IOException e) {
        // logger.error("Error reading events example file: {}", e.getMessage());
        // System.exit(1);
        // }
        // try {
        // Files.writeString(Path.of(backupFilePathIcs),
        // saveEventsToIcs(eventsJson),
        // StandardOpenOption.CREATE);
        // logger.info("Events saved to {}", backupFilePathIcs);
        // } catch (IOException e) {
        // logger.error("Error saving events to iCalendar file: {}", e.getMessage());
        // }
        try {
            Files.writeString(Path.of(backupFilePathCsv),
                    saveEventsToCsv(eventsJson, subCalendarsMap),
                    StandardOpenOption.CREATE);
            logger.info("Events saved to {}", backupFilePathCsv);
        } catch (IOException e) {
            logger.error("Error saving events to csv file: {}", e.getMessage());
        }
    }

    private static Map<Long, String> getSubcalendarIds(String subCalendars) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(subCalendars, JsonObject.class);
        JsonArray subCalendarsArray = jsonObject.getAsJsonArray("subcalendars");
        Map<Long, String> subCalendarsMap = new HashMap<>();
        for (int i = 0; i < subCalendarsArray.size(); i++) {
            JsonObject subCalendar = subCalendarsArray.get(i).getAsJsonObject();
            long id = subCalendar.get("id").getAsLong();
            String name = subCalendar.get("name").getAsString();
            subCalendarsMap.put(id, name);
        }
        return subCalendarsMap;
    }

    private static String fetchCalendarEvents(String apiKey, String calendarId, String startDate, String endDate)
            throws IOException {
        OkHttpClient client = new OkHttpClient();
        String url = String.format("%s%s/events?startDate=%s&endDate=%s", API_URL, calendarId, startDate, endDate);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Teamup-Token", apiKey)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            return response.body().string();
        }
    }

    private static String fetchSubCalendars(String apiKey, String calendarId) throws IOException {
        OkHttpClient client = new OkHttpClient();
        String url = String.format("%s%s/subcalendars", API_URL, calendarId);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json, text/html")
                // .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Teamup-Token", apiKey)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            return response.body().string();
        }
    }

    private static String saveEventsToCsv(String eventsJson, Map<Long, String> subCalendarsMap) {
        String csvEventsString = null;
        try (StringWriter sw = new StringWriter(); PrintWriter writer = new PrintWriter(sw)) {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(eventsJson, JsonObject.class);
            JsonArray events = jsonObject.getAsJsonArray("events");

            writer.println("Calendar Name,Subject,Start Date,Start Time,End Date,End Time,Who,Location,Description");

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            for (JsonElement element : events) {
                try {
                    JsonObject event = element.getAsJsonObject();
                    Long calendarid = event.get("subcalendar_id").getAsLong();
                    String calendarName = "\"" + subCalendarsMap.get(calendarid) + "\"";
                    String subject = "\"" + event.get("title").getAsString() + "\"";
                    OffsetDateTime start = OffsetDateTime.parse(event.get("start_dt").getAsString());
                    OffsetDateTime end = OffsetDateTime.parse(event.get("end_dt").getAsString());
                    String who = event.get("who").getAsString();
                    String location = event.get("location").getAsString();
                    String htmlDescription = event.get("notes").getAsString();
                    String description = org.jsoup.Jsoup.parse(htmlDescription).text();

                    if (calendarName != null && subject != null && start != null && end != null && who != null
                            && location != null && description != null) {
                        writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                                calendarName,
                                subject,
                                start.format(dateFormatter),
                                start.format(timeFormatter),
                                end.format(dateFormatter),
                                end.format(timeFormatter),
                                who,
                                location,
                                description);
                    } else {
                        logger.error("Missing data in event: {}", element.toString());
                    }
                } catch (Exception e) {
                    logger.error("Error processing event {}: {}", element.toString(), e.getMessage());
                }
            }
            csvEventsString = sw.toString();
        } catch (IOException e) {
            logger.error("Error closing string writer: {}", e.getMessage());
        }
        return csvEventsString;
    }

    // private static String saveEventsToIcs(String eventsJson) throws IOException {
    // String icsEventsString = null;
    // try (StringWriter sw = new StringWriter();
    // PrintWriter writer = new PrintWriter(sw)) {
    // Gson gson = new Gson();
    // JsonObject jsonObject = gson.fromJson(eventsJson, JsonObject.class);
    // JsonArray events = jsonObject.getAsJsonArray("events");

    // Calendar calendar = new Calendar();
    // calendar.add(new ProdId("-//Teamup//EN"));
    // Version v = new Version();
    // v.setValue(Version.VALUE_2_0);
    // calendar.add(v);
    // DateTimeFormatter formatter =
    // DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    // for (int i = 0; i < events.size(); i++) {
    // JsonObject event = events.get(i).getAsJsonObject();
    // String uid = event.get("id").getAsString();
    // String summary = event.get("title").getAsString();
    // String start = event.get("start_dt").getAsString();
    // String end = event.get("end_dt").getAsString();

    // OffsetDateTime startDateTime = OffsetDateTime.parse(start, formatter);
    // OffsetDateTime endDateTime = OffsetDateTime.parse(end, formatter);

    // VEvent vEvent = new VEvent();
    // vEvent.add(new DtStart<OffsetDateTime>(startDateTime));
    // vEvent.add(new DtEnd<OffsetDateTime>(endDateTime));
    // vEvent.add(new Summary(summary));
    // vEvent.add(new Uid(uid));

    // calendar.add(vEvent);
    // }

    // CalendarOutputter outputter = new CalendarOutputter();
    // outputter.output(calendar, writer);
    // icsEventsString = sw.toString();
    // } catch (IOException e) {
    // logger.error("Error closing string writer: {}", e.getMessage());
    // }
    // return icsEventsString;
    // }

    // private static void saveEventsToFileJson(String eventsJson, String filePath)
    // {
    // try (FileWriter fileWriter = new FileWriter(filePath)) {
    // Gson gson = new Gson();
    // JsonObject jsonObject = gson.fromJson(eventsJson, JsonObject.class);
    // gson.toJson(jsonObject, fileWriter);
    // logger.info("Events saved to {}", filePath);
    // } catch (IOException e) {
    // logger.error("Error saving events to file: {}", e.getMessage());
    // }
    // }
}
