package ru.yandex.jenkins.plugins.debuilder.dpkg.common;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Stores a date and the accepted format
 * 
 * @author caiocezar
 * 
 */
public class DebianDate {
    private Date date;

    // TODO extend the support to use full support RFC5322
    // https://tools.ietf.org/html/rfc5322#page-14 and
    // https://tools.ietf.org/html/rfc5322#page-33 and full support RFC2822
    // http://www.ietf.org/rfc/rfc2822.txt sections 3.3 and 4.3

    /**
     * The pattern required. EX: Mon, 01 Dec 2000 22:05:08 -0300
     */
    public static String dateFormatPattern = "EEE, dd MMM yyyy HH:mm:ss Z";

    /**
     * The {@link Locale#US}
     */
    public static Locale dateLocale = Locale.US;
    /**
     * The {@link DateFormat} using {@link #dateFormatPattern} and
     * {@link #dateLocale}
     */
    public static DateFormat dateFormat = new SimpleDateFormat(dateFormatPattern, dateLocale);

    /**
     * Create an empty debian date
     */
    public DebianDate() {

    }

    /**
     * Create a debian date with a java date
     * 
     * @param date
     */
    public DebianDate(Date date) {
        setDate(date);
    }

    /**
     * @return the date
     */
    public Date getDate() {
        return date;
    }

    /**
     * @param date
     *            the date to set
     */
    public void setDate(Date date) {
        this.date = date;
    }

    /**
     * Set the Date by a String
     * 
     * @param dateText
     *            A date in the correct format. See:
     *            {@link DebianDate#dateFormatPattern}
     * @return True if the date is valid, false if not
     */
    public boolean setDate(String dateText) {
        try {
            setDate(dateFormat.parse(dateText));
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

}
