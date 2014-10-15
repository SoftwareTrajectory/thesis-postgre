package edu.hawaii.senin.postgre.workflow;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import edu.hawaii.jmotif.timeseries.TSException;
import edu.hawaii.jmotif.timeseries.TSUtils;
import edu.hawaii.senin.postgre.db.PostgreDB;
import edu.hawaii.senin.postgre.db.PostgreDBManager;
import edu.hawaii.senin.postgre.persistence.ChangePeople;
import edu.hawaii.senin.postgre.persistence.ChangeProject;

/**
 * The first step of the STA workflow, prints software trajectories.
 * 
 * @author psenin
 * 
 */
public class Step01PrintReleaseSeries {

  // this is the project name
  //
  private static final String PROJECT_OF_INTEREST = "postgre";

  // this is the window size that shall be considered before and after the release date
  //
  private static final int DAYS = 28;

  // this is the set of metrics we'll be retrieving data for
  //
  private static final String[] METRICS_OF_INTEREST = { "added_lines", "edited_lines",
      "removed_lines", "added_files", "edited_files", "removed_files" };

  // this is the output prefix
  //
  private static final String OUTPUT_PREFIX = "results/cf_" + String.valueOf(DAYS);

  private static TimeZone tz;

  // logger business
  private static Logger consoleLogger;
  private static Level LOGGING_LEVEL = Level.INFO;

  static {
    consoleLogger = (Logger) LoggerFactory.getLogger(Step01PrintReleaseSeries.class);
    consoleLogger.setLevel(LOGGING_LEVEL);
  }

  /**
   * @param args
   * @throws IOException
   * @throws TSException
   * @throws ParseException
   */
  public static void main(String[] args) throws IOException, TSException, ParseException {

    // make GMT - where data was acquired the default timezone
    //
    tz = TimeZone.getTimeZone("UTC");
    TimeZone.setDefault(tz);

    // this is a set of release we are working on
    //
    Map<String, ArrayList<DateTime>> commiFests = PostgreEvolution.getCommitFestsAsMap();
    int counter = 0;
    DateTime firstCommitFestStart = null;
    for (Entry<String, ArrayList<DateTime>> e : commiFests.entrySet()) {
      System.out.println(e.getKey() + ": " + e.getValue().get(0) + ", " + e.getValue().get(1));
      if (0 == counter) {
        firstCommitFestStart = e.getValue().get(0);
      }
      counter++;
    }

    // get the DB connection
    //
    PostgreDB db = PostgreDBManager.getProductionInstance();
    ChangeProject project = db.getProject(PROJECT_OF_INTEREST);

    // first, select the metric
    //
    for (String metricString : METRICS_OF_INTEREST) {

      // make data arrays
      //
      ArrayList<double[]> preFest = new ArrayList<double[]>();
      ArrayList<double[]> commitFest = new ArrayList<double[]>();

      // First extract the series from the beginning to the commitFest itself
      DateTime intervalStart = new DateTime().withDate(2009, 1, 1);
      double[] preSummary = db.getMetricAsSeries(project.getId(), metricString,
          intervalStart.toLocalDateTime(), firstCommitFestStart.toLocalDateTime());
      preFest.add(preSummary);

      // looping over commitFest intervals
      //
      for (Entry<String, ArrayList<DateTime>> e : commiFests.entrySet()) {
        System.out.println(e.getKey() + ": " + e.getValue().get(0) + ", " + e.getValue().get(1));

      }

    }

  }

  /**
   * Saves the data set.
   * 
   * @param preRelease
   * @param postRelease
   * @param outputPrefix
   * @throws IOException
   */
  private static void saveSet(ArrayList<Entry<String, double[]>> preRelease,
      ArrayList<Entry<String, double[]>> postRelease, String outputPrefix) throws IOException {

    BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputPrefix + ".csv")));

    for (Entry<String, double[]> s : preRelease) {
      if (TSUtils.mean(s.getValue()) == 0) {
        continue;
      }
      bw.write("pre_" + s.getKey() + ", "
          + Arrays.toString(s.getValue()).replace("[", "").replace("]", "").replace(",", "") + "\n");
    }

    for (Entry<String, double[]> s : postRelease) {
      if (TSUtils.mean(s.getValue()) == 0) {
        continue;
      }
      bw.write("post_" + s.getKey() + ", "
          + Arrays.toString(s.getValue()).replace("[", "").replace("]", "").replace(",", "") + "\n");
    }

    bw.close();
  }

}
