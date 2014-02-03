package ru.yandex.jenkins.plugins.debuilder.dpkg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Store a Debian version
 * 
 * @author caiocezar
 */
public class Version {
    private Long epoch;
    private String upstream;
    private String revision;

    public Long getEpoch() {
        if (epoch == null)
            epoch = (long) 0;
        return epoch;
    }

    public void setEpoch(Long epoch) {
        if (epoch == null)
            this.epoch = (long) 0;
        else
            this.epoch = epoch;
    }

    public String getUpstream() {
        if (upstream == null)
            upstream = "";
        return upstream;
    }

    private void setUpstream(String upstream) {
        // TODO validated input and make it public
        this.upstream = upstream;
    }

    public String getRevision() {
        if (revision == null)
            revision = "";
        return revision;
    }

    private void setRevision(String revision) {
        // TODO validate input and make it public
        this.revision = revision;
    }

    /**
     * Returns version in the format 'epoch:upstream-revision', epoch and
     * revision aref optional
     * 
     * @return The full version string
     */
    public String getFullVersion() {
        String fullVersion = "";
        if (getEpoch() > 0)
            fullVersion = getEpoch() + ":";

        fullVersion = fullVersion + getUpstream();
        if (!getRevision().isEmpty()) {
            fullVersion = fullVersion + "-" + getRevision();
        }
        return fullVersion;
    }

    /**
     * Read a version. The expected format is
     * [epoch:]upstreamVersion[-revision].<br/>
     * <b>Epoch:</b> Is optional and must contain only numbers and ends with a
     * colon(:).<br/>
     * <b>Upstream version:</b> Is mandatory and must start with a numeric value
     * and contain only alphanumerics and the characters . + ~ <br/>
     * <b>Revision:</b> Is optional must start with a '-' and use the same for
     * characters as upstream<br/>
     * 
     * @param versionData
     *            The version
     * @throws VersionFormatException
     *             If the text is an invalid version
     */
    public void parseVersion(String versionData) throws VersionFormatException {
        // TODO parse the version like the c library
        // TODO : and - are not allowed in the upstream but they should if a
        // epoch or a revision respectively are present
        Pattern versionPattern = Pattern.compile("^\\s*(?:(\\d+):)?([0-9][\\w\\.+~]*)(?:-([\\w+\\.~]+))?\\s*$");
        Matcher versionMatch = versionPattern.matcher(versionData);

        if (versionMatch.matches()) {
            if (versionMatch.group(1) != null)
                this.setEpoch(Long.decode(versionMatch.group(1)));

            if (versionMatch.group(2) != null)
                this.setUpstream(versionMatch.group(2));
            else
                throw new VersionFormatException("Required upstream version not found");

            if (versionMatch.group(3) != null)
                this.setRevision(versionMatch.group(3));
        } else {
            throw new VersionFormatException("Invalid version");
        }
    }

    /**
     * Compares two Debian versions as
     * {@link Version#versionCompare(Version, Version)}
     * 
     * @param versionText1
     *            The first version text
     * @param versionText2
     *            The second version text
     * @return The same as {@link Version#versionCompare(Version, Version)}
     * @throws VersionFormatException
     *             If same version has parsing erros
     */
    public static Integer versionTextCompare(String versionText1, String versionText2) throws VersionFormatException {
        Version v1 = new Version();
        Version v2 = new Version();

        try {
            v1.parseVersion(versionText1);
        } catch (VersionFormatException e) {
            throw new VersionFormatException("The first version has erros: " + e.getLocalizedMessage());
        }
        try {
            v2.parseVersion(versionText2);
        } catch (VersionFormatException e) {
            throw new VersionFormatException("The second version has erros: " + e.getLocalizedMessage());
        }

        return versionCompare(v1, v2);
    }

    /**
     * Compares two Debian versions.
     * 
     * This function follows the convention of the comparator functions used by
     * qsort().
     * 
     * @param version1
     *            The first version.
     * @param version2
     *            The second version.
     * @return 0 If version1 and version2 are equal. <0 If version1 is smaller
     *         than version2. >0 If version1 is greater than version2.
     * @see deb-version(5)
     */
    public static Integer versionCompare(Version version1, Version version2) {
        if (version1.getEpoch() > version2.getEpoch())
            return 1;
        if (version1.getEpoch() < version2.getEpoch())
            return -1;

        int r = versionRevisionCompare(version1.getUpstream(), version2.getUpstream());
        if (r != 0)
            return r;

        return versionRevisionCompare(version1.getRevision(), version2.getRevision());
    }

    /**
     * Converts the String to an ArrayList an call
     * {@link Version#versionRevisionCompare(ArrayList, ArrayList)}
     * 
     * @param v1
     *            The first version/revision String
     * @param v2
     *            The second version/revision String
     * @return Same as
     *         {@link Version#versionRevisionCompare(ArrayList, ArrayList)}
     * 
     */
    private static Integer versionRevisionCompare(String v1, String v2) {
        ArrayList<Character> array1 = new ArrayList<Character>();
        ArrayList<Character> array2 = new ArrayList<Character>();
        if (v1 == null)
            v1 = "";
        if (v2 == null)
            v2 = "";

        for (Character c : v1.toCharArray())
            array1.add(c);

        for (Character c : v2.toCharArray())
            array2.add(c);

        return versionRevisionCompare(array1, array2);
    }

    /**
     * Compare the version or revision strings. This method do not validated the
     * inputs.
     * 
     * @param v1
     *            The first version/revision
     * @param v2
     *            The second version/revision
     * @return 0 If v1 and v2 are equal. <0 If v1 is smaller than v2. >0 If v1
     *         is greater than v2.
     */
    private static Integer versionRevisionCompare(ArrayList<Character> v1, ArrayList<Character> v2) {
        if (v1 == null)
            v1 = new ArrayList<Character>();
        if (v2 == null)
            v2 = new ArrayList<Character>();

        Iterator<Character> i1 = v1.iterator();
        Iterator<Character> i2 = v2.iterator();

        Character char1 = getNextOrNull(i1);
        Character char2 = getNextOrNull(i2);

        while (char1 != null || char2 != null) {
            int rt = 0;

            // If same of the chars are not a digit and not null compare using
            // the weight
            while ((char1 != null && !isDigit(char1)) || (char2 != null && !isDigit(char2))) {
                int char1Order = getCharacterWeight(char1);
                int char2Order = getCharacterWeight(char2);

                if (char1Order != char2Order)
                    return char1Order - char2Order;

                char1 = getNextOrNull(i1);
                char2 = getNextOrNull(i2);
            }
            // If reached here we have only numbers or null

            // Ignore all leading zeros
            while (char1 != null && char1 == '0')
                char1 = getNextOrNull(i1);
            while (char2 != null && char2 == '0')
                char2 = getNextOrNull(i2);

            // if both as numbers store the first difference and goes until some
            // of the are not digit
            while (isDigit(char1) && isDigit(char2)) {
                if (rt == 0)
                    rt = Integer.decode(char1.toString()) - Integer.decode(char2.toString());
                char1 = getNextOrNull(i1);
                char2 = getNextOrNull(i2);
            }

            // If there is a remain digit the first is bigger EX: 1121x112,
            // 0011x01, 1aa0023x1aa2
            if (isDigit(char1))
                return 1;
            // If there is a remain digit second is bigger
            if (isDigit(char2))
                return -1;
            // If there is no remain digit the and there is a difer
            if (rt != 0)
                return rt;
        }

        return 0;
    }

    /**
     * Give a weight to the character to order in the version comparison.
     * 
     * @param c
     *            An ASCII character.
     */
    private static Integer getCharacterWeight(Character c) {
        if (isDigit(c))
            return 0;
        else if (isAlpha(c))
            return (int) c;
        else if (c != null && c == '~')
            return -1;
        else if (c != null)
            return 256 + (int) c;
        else
            return 0;
    }

    /**
     * Test is the char is a digit
     * 
     * @param c
     *            The char
     * @return Return true if the char is a ASCII digit or false
     */
    private static Boolean isDigit(Character c) {
        if (c == null)
            return false;
        return (c >= '0' && c <= '9');
    }

    /**
     * Test if the char is a letter
     * 
     * @param c
     *            The char
     * @return Return true if the char is a ASCII letter([a-z][A-Z]) or false
     */
    private static Boolean isAlpha(Character c) {
        if (c == null)
            return false;
        return (c >= 'a' && c <= 'Z') || (c >= 'A' && c <= 'Z');
    }

    /**
     * Get the next char or null
     * 
     * @param i
     *            The iterator
     * @return The next char if there is none return null
     */
    private static Character getNextOrNull(Iterator<Character> i) {
        return i.hasNext() ? i.next() : null;
    }
}