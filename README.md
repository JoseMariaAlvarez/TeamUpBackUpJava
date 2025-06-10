Java project to make a security copy of a Team Up calendar, given an API key, the calendar id, and the initial and final dates, in YYYY-MM-DD format.

Calendar events are saved to a CSV file, one event per line, and a ICalc file. This latter version is to be improved.

To create the executable jar file, run Maven package. The resulting jar file is target/teamupbackup-1.0-jar-with-dependencies.jar. To run it from the commmand line type in a terminal:

$ java -Dlog.file.path=./teamupbackup.log -jar target/teamupbackup-1.0-jar-with-dependencies.jar

The path to the log file must be written according to the operating system. For Windows it would be something like -Dlog.file.path=.\teamupbackup.log.

The name of the backup file could also be defined in the logback.xml file as a property:
  <property name="log.file.path" value="./teamupbackup.log"/>

This value overrides the value defined as a system property with the -D option in console. I have still to check if the path style also depends on the operating system.

Make sure that the folder for the output CSV file exists. Otherwise, an IOException will be thrown and the file will not be created. The path and prefix of the output CSV file is hardcoded in the java file. It should be moved to the configuration file in a later version.


