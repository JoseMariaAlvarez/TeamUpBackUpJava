Java project to make a security copy of a Team Up calendar, given an API key, the calendar id, and the initial and final dates, in YYYY-MM-DD format.

Calendar events are saved to a CSV file, one event per line, and a ICalc file. This latter version is to be improved.

To create the executable jar file, run Maven package. The resulting jar file is target/teamupbackup-1.0-jar-with-dependencies.jar. To run it from the commmand line type in a terminal:

$ jar -Dlog.file.path=./teamupbackup.log -jar target/teamupbackup-1.0-jar-with-dependencies.jar

The path to the log file must be written according to the operating system. For Windows it would be something like -Dlog.file.path=.\teamupbackup.log.

Output logged to teamupbackup.log log file.
