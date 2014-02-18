package ru.yandex.jenkins.plugins.debuilder.dpkg;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.InternetAddress;


import ru.yandex.jenkins.plugins.debuilder.DebUtils.Runner;
import ru.yandex.jenkins.plugins.debuilder.dpkg.common.DebianDate;
import ru.yandex.jenkins.plugins.debuilder.dpkg.common.DebianDistributions;
import ru.yandex.jenkins.plugins.debuilder.dpkg.common.DebianVersion;

/**
 * Stores all data from
 * 
 * @author caiocezar
 * 
 */
public class DebianChangelog {
    private ArrayList<ChangeEntry> entries;

    public static class ChangeEntry {
        private String packageName;
        private DebianVersion version;
        private DebianDistributions distributions;
        private String urgency;
        private ArrayList<String> details;
        private InternetAddress maintainer;
        private DebianDate date;

        /**
         * The changelog header regex '^(\\w[^\\s*]) +\\(([^\\)]+)\\) +([^;]+);s
         * *(.*)$'.
         */
        public static String HEADER_REGEX = "^(\\w[^\\s*]) +\\(([^\\)]+)\\) +([^;]+); *(.*)$";
        /**
         * The changelog trailer regex '^ -- (.*) <(.*)> (.*)$'
         */
        public static String TRAILER_REGEX = "^ -- (.*) <(.*)>  (.*)$";

        /**
         * @return the packageName
         */
        public String getPackageName() {
            return packageName;
        }

        /**
         * @param packageName
         *            The name of the package
         */
        public void setPackageName(String packageName) throws IllegalArgumentException {
            if (packageName != null && (packageName.isEmpty() || packageName.matches("[//s]")))
                throw new IllegalArgumentException("Invalid package name");
            this.packageName = packageName;
        }

        /**
         * @return the version
         */
        public DebianVersion getVersion() {
            return version;
        }

        /**
         * @param version
         *            The version to set
         */
        public void setVersion(DebianVersion version) {
            this.version = version;
        }

        /**
         * @return the distributions
         */
        public DebianDistributions getDistributions() {
            return distributions;
        }

        /**
         * @param distributions
         *            the distributions to set
         */
        public void setDistributions(DebianDistributions distributions) {
            this.distributions = distributions;
        }

        /**
         * @return the urgency
         */
        public String getUrgency() {
            return urgency;
        }

        /**
         * @param urgency
         *            the urgency to set
         */
        public void setUrgency(String urgency) {
            this.urgency = urgency;
        }

        /**
         * @return the details
         */
        public ArrayList<String> getDetails() {
            return details;
        }

        /**
         * @param details
         *            the details to set
         */
        public void setDetails(ArrayList<String> details) {
            this.details = details;
        }

        /**
         * @return the maintainer
         */
        public InternetAddress getMaintainer() {
            return maintainer;
        }

        /**
         * @param maintainer
         *            the maintainer to set
         */
        public void setMaintainer(InternetAddress maintainer) {
            this.maintainer = maintainer;
        }

        /**
         * @return the date
         */
        public DebianDate getDate() {
            return date;
        }

        /**
         * @param date
         *            the date to set
         */
        public void setDate(DebianDate date) {
            this.date = date;
        }
    }

    /**
     * @return the entries
     */
    public ArrayList<ChangeEntry> getEntries() {
        return entries;
    }

    /**
     * @param entries
     *            the entries to set
     */
    public void setEntries(ArrayList<ChangeEntry> entries) {
        this.entries = entries;
    }

    public void parseChangelog(String changelogText, Runner runner) {

        String headerRegex = "^(\\w[^\\s*]) +\\(([^\\)]+)\\) +([^;]+); *(.*)$";
//        String trailerRegex = "^ -- (.*) <(.*)>  (.*)$";
        // my $name_chars = qr/[-+0-9a-z.]/i;
        // our $regex_header = qr/^(\w$name_chars*) \(([^\(\)
        // \t]+)\)((?:\s+$name_chars+)+)\;(.*?)\s*$/i;
        // our $regex_trailer = qr/^ \-\- (.*) <(.*)>(
        // ?)((\w+\,\s*)?\d{1,2}\s+\w+\s+\d{4}\s+\d{1,2}:\d\d:\d\d\s+[-+]\d{4}(\s+\([^\\\(\)]\))?)\s*$/o;

        Pattern headerPattern = Pattern.compile(headerRegex);
//        Pattern trailerPattern = Pattern.compile(trailerRegex);

        ArrayList<String> changelogLines = new ArrayList<String>(Arrays.asList(changelogText.split("\n")));

        ListIterator<String> changelogLinesIterator = changelogLines.listIterator();

        while (changelogLinesIterator.hasNext()) {

            String row = changelogLinesIterator.next();

            Matcher matcher = headerPattern.matcher(row);

            if (matcher.matches()) {
                // TODO set the fields

            } else {
                // TODO continue
            }
        }
    }
}