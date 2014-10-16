package edu.hawaii.senin.postgre.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class PostgreEvolution {

  private static final String RELEASES_FNAME = "resources/commit_fest.csv";

  private static final DateTimeFormatter fmt = ISODateTimeFormat.dateHourMinuteSecondMillis();

  private static LinkedHashMap<String, ArrayList<DateTime>> commitFests;

  public static Map<String, ArrayList<DateTime>> getCommitFestsAsMap() {
    commitFests = new LinkedHashMap<String, ArrayList<DateTime>>();
    try {
      BufferedReader br = new BufferedReader(new FileReader(new File(RELEASES_FNAME)));
      String str = null;
      while (null != (str = br.readLine())) {
        String[] split = str.split("\\,\\s+");

        String name = split[0].replace("\"", "");

        DateTime start = fmt.parseDateTime(split[1]);
        DateTime end = fmt.parseDateTime(split[2]);

        ArrayList<DateTime> times = new ArrayList<DateTime>();
        times.add(start);
        times.add(end);

        commitFests.put(name, times);
      }
      br.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }

    return commitFests;
  }
}
