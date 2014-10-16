package edu.hawaii.senin.postgre.workflow;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDateTime;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import edu.hawaii.jmotif.timeseries.TSException;
import edu.hawaii.jmotif.timeseries.TSUtils;
import edu.hawaii.senin.postgre.db.PostgreDB;
import edu.hawaii.senin.postgre.db.PostgreDBManager;
import edu.hawaii.senin.postgre.persistence.ChangeProject;

public class Step03PrintReleaseSeries {

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
  private static final String OUTPUT_PREFIX = "results/release_";

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
   */
  public static void main(String[] args) throws IOException, TSException {

    // make GMT - where data was acquired the default timezone
    //
    tz = TimeZone.getTimeZone("UTC");
    TimeZone.setDefault(tz);

    ArrayList<String[]> releases = readCSV("resources/releases_wiki");

    // get the DB connection
    //
    PostgreDB db = PostgreDBManager.getProductionInstance();
    ChangeProject project = db.getProject(PROJECT_OF_INTEREST);

    // first, select the metric
    //
    for (String metricString : METRICS_OF_INTEREST) {

      // make data arrays
      //
      ArrayList<double[]> postReleaseData = new ArrayList<double[]>();
      ArrayList<double[]> preReleaseData = new ArrayList<double[]>();

      // looping over selected dates
      //
      for (int counter = 0; counter < releases.size() - 1; counter = counter + 1) {

        String[] s = releases.get(counter);

        // dates business
        //
        DateTime releaseMonday = LocalDateTime.parse(s[1]).withDayOfWeek(DateTimeConstants.MONDAY)
            .toDateTime();
        DateTime postReleaseStartMonday = releaseMonday.plusDays(7);
        DateTime preReleaseStartMonday = releaseMonday.minusDays(DAYS);

        // pre-release
        //
        double[] preSummary = db.getMetricAsSeries(project.getId(), metricString,
            preReleaseStartMonday.toLocalDateTime(), preReleaseStartMonday.plusDays(DAYS)
                .toLocalDateTime());
        if (TSUtils.mean(preSummary) != 0) {
          preReleaseData.add(preSummary);
        }

        // post-release
        //
        double[] postSummary = db.getMetricAsSeries(project.getId(), metricString,
            postReleaseStartMonday.toLocalDateTime(), postReleaseStartMonday.plusDays(28)
                .toLocalDateTime());
        if (TSUtils.mean(postSummary) != 0) {
          postReleaseData.add(postSummary);
        }

      }

      saveSet(preReleaseData, postReleaseData, OUTPUT_PREFIX + metricString);
    }

  }

  private static void saveSet(ArrayList<double[]> preRelease, ArrayList<double[]> postRelease,
      String outputPrefix) throws IOException {

    BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputPrefix + ".csv")));

    for (double[] s : preRelease) {
      if (TSUtils.mean(s) == 0) {
        continue;
      }
      bw.write("pre " + Arrays.toString(s).replace("[", "").replace("]", "").replace(",", "")
          + "\n");
    }

    for (double[] s : postRelease) {
      if (TSUtils.mean(s) == 0) {
        continue;
      }
      bw.write("post " + Arrays.toString(s).replace("[", "").replace("]", "").replace(",", "")
          + "\n");
    }

    bw.close();
  }

  private static double[] processPeaks(double[] sumSeries) {
    double mean = TSUtils.mean(sumSeries);
    double sd = TSUtils.stDev(sumSeries);
    for (int i = 0; i < sumSeries.length; i++) {
      if (sumSeries[i] > 2. * sd) {
        sumSeries[i] = 2. * sd;
      }
    }
    return sumSeries;
  }

  private static double[] sumSeries(double[] sumSeries, double[] series) {
    for (int i = 0; i < sumSeries.length; i++) {
      sumSeries[i] = sumSeries[i] + series[i];
    }
    return sumSeries;
  }

  private static ArrayList<String[]> readCSV(String fname) throws IOException {
    ArrayList<String[]> res = new ArrayList<String[]>();
    String line = null;
    BufferedReader br = new BufferedReader(new FileReader(new File(fname)));
    while ((line = br.readLine()) != null) {
      String[] r = line.split("\\s*,\\s*");
      res.add(line.split("\\s*,\\s*"));
    }
    return res;
  }

  private static double sum(double[] changeLinesSeries) {
    double sum = 0;
    for (double i : changeLinesSeries)
      sum += i;
    return sum;
  }
}
