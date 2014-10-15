package edu.hawaii.senin.postgre.db;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.joda.time.Days;
import org.joda.time.LocalDateTime;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import edu.hawaii.senin.postgre.persistence.Change;
import edu.hawaii.senin.postgre.persistence.ChangePeople;
import edu.hawaii.senin.postgre.persistence.ChangeProject;

/**
 * Implements Android database IO, by providing both test and production methods.
 * 
 * @author psenin
 * 
 */
public class PostgreDB {

  /** The mapper instance key. */
  public static final String PRODUCTION_INSTANCE = "production";

  /** The logger we will use. */
  private static final String DB_LOGGER_NAME = "postgre.db";

  /** The db configuration constants. */
  private static final String STACK_DB_CONFIGNAME = "mybatis-postgre.xml";
  private static final String STACK_DB_ENVIRONMENT = "production_pooled";

  /** Test database SQL factory. */
  private SqlSessionFactory sessionFactory;

  @SuppressWarnings("unused")
  private String instanceType;

  /** Mapper config file location. */
  private String dbConfigFileName;
  private String dbEnvironmentKey;

  // the session
  private SqlSession session;

  private static Logger consoleLogger;
  private static Level LOGGING_LEVEL = Level.INFO;

  static {
    consoleLogger = (Logger) LoggerFactory.getLogger(PostgreDB.class);
    consoleLogger.setLevel(LOGGING_LEVEL);
  }

  /**
   * Constructor.
   * 
   * @param isTestInstance The test instance semaphore.
   * @throws IOException if error occurs.
   */
  protected PostgreDB(String instanceType) throws IOException {
    this.instanceType = instanceType;
    initialize();
  }

  /**
   * Lazy initialization, takes care about set-up.
   * 
   * @throws IOException if error occurs.
   */
  private synchronized void initialize() throws IOException {

    this.dbConfigFileName = STACK_DB_CONFIGNAME;
    this.dbEnvironmentKey = STACK_DB_ENVIRONMENT;

    consoleLogger.info("Getting connected to the database, myBATIS config: " + dbConfigFileName
        + ", environment key: " + dbEnvironmentKey);

    // do check for the file existence
    //

    InputStream in = this.getClass().getClassLoader().getResourceAsStream(this.dbConfigFileName);
    if (null == in) {
      throw new RuntimeException("Unable to locate " + this.dbConfigFileName);

    }

    // proceed with configuration
    //
    this.sessionFactory = new SqlSessionFactoryBuilder().build(in, this.dbEnvironmentKey);

    this.session = this.sessionFactory.openSession(ExecutorType.REUSE);

    consoleLogger.info("Connected to database.");

  }

  /**
   * Get the session factory used.
   * 
   * @return The session factory used in this instance.
   */
  public synchronized SqlSessionFactory getSessionFactory() {
    return this.sessionFactory;
  }

  /**
   * Get the session.
   * 
   * @return The active SQL session.
   */
  public synchronized SqlSession getSession() {
    return this.sessionFactory.openSession();
  }

  /**
   * Commits and closes the open session.
   */
  public synchronized void shutDown() {
    this.session.commit(true);
    this.session.close();
  }

  /**
   * Commit pending transactions.
   */
  public synchronized void commit() {
    this.session.commit(true);
  }

  public synchronized ChangeProject getProject(String projectName) {
    return this.session.selectOne("getProjectByName", projectName);
  }

  public synchronized List<ChangePeople> getAllProjectUsers() {
    return this.session.selectList("getPeople");
  }

  public List<ChangePeople> getAuthorsForInterval(Integer projectId, LocalDateTime start,
      LocalDateTime end) {
    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("project_id", projectId);
    params.put("start", start);
    params.put("end", end);
    return session.selectList("getAuthorsForInterval", params);
  }

  public synchronized Change getFirstChangeForUser(Integer projectId, Integer userId) {
    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("project_id", projectId);
    params.put("author_id", userId);
    return session.selectOne("getFirstChangeForAuthor", params);
  }

  public synchronized Change getLastChangeForUser(Integer projectId, Integer userId) {
    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("project_id", projectId);
    params.put("author_id", userId);
    return session.selectOne("getLastChangeForAuthor", params);
  }

  public synchronized Integer countChangesForUser(Integer projectId, Integer userId) {
    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("project_id", projectId);
    params.put("author_id", userId);
    return this.session.selectOne("countChangesForUser", params);
  }

  public List<HashMap<String, Object>> getFrequencies(Integer projectId, Integer authorId,
      String tagStr, String paramStr, LocalDateTime start, LocalDateTime end, String target_metrics) {
    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("project_id", projectId);
    params.put("author_id", authorId);
    params.put("params", paramStr);
    params.put("tag", tagStr);
    params.put("start", start);
    params.put("end", end);
    params.put("target_metrics", target_metrics);
    return this.session.selectList("getFrequencies", params);
  }

  public double[] getMetricAsSeries(Integer projectId, String field, LocalDateTime start,
      LocalDateTime end) {
    HashMap<String, Object> params = new HashMap<String, Object>();
    params.put("project_id", projectId);
    params.put("field", field);
    params.put("start", start);
    params.put("end", end.plusDays(1));
    List<HashMap<String, Object>> res = this.session.selectList("getSumOfField", params);
    System.out.println(res.size());
    double[] resSeries = new double[Days.daysBetween(start, end).getDays() + 1];
    for (int i = 0; i < res.size(); i++) {
      resSeries[Days.daysBetween(start, LocalDateTime.parse((String) res.get(i).get("date")))
          .getDays()] = ((BigDecimal) res.get(i).get("value")).doubleValue();
    }
    return resSeries;
  }

}
