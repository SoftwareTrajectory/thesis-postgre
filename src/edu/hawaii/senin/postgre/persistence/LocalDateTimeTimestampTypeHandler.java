package edu.hawaii.senin.postgre.persistence;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.joda.time.LocalDateTime;

public class LocalDateTimeTimestampTypeHandler implements TypeHandler<LocalDateTime> {

  @Override
  public void setParameter(PreparedStatement ps, int i, LocalDateTime parameter, JdbcType jdbcType)
      throws SQLException {
    if (null != parameter) {
      Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
      cal.clear();
      cal.set(parameter.getYear(), parameter.getMonthOfYear() - 1, parameter.getDayOfMonth(),
          parameter.getHourOfDay(), parameter.getMinuteOfHour(), parameter.getSecondOfMinute());
      ps.setTimestamp(i, new Timestamp(cal.getTimeInMillis()));
    }
    else {
      ps.setTimestamp(i, null);
    }
  }

  @Override
  public LocalDateTime getResult(ResultSet rs, String columnName) throws SQLException {
    Date date = rs.getDate(columnName);
    if (null == date) {
      return null;
    }
    return new LocalDateTime(date.getTime());
  }

  @Override
  public LocalDateTime getResult(CallableStatement cs, int columnIndex) throws SQLException {
    Date date = cs.getDate(columnIndex);
    return new LocalDateTime(date.getTime());
  }

  @Override
  public LocalDateTime getResult(ResultSet rs, int columnIndex) throws SQLException {
    Date date = rs.getDate(columnIndex);
    return new LocalDateTime(date.getTime());
  }

}
