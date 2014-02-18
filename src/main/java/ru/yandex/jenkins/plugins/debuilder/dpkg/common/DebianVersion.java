package ru.yandex.jenkins.plugins.debuilder.dpkg.common;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Store a debian version, that can be used as a package version or a format
 * version of the control files or other kind of version
 * 
 * @author caiocezar
 */
public class DebianVersion {
    private Long epoch;
    private String upstream;
    private String revision;

    /**
     * @return A copy of epoch or null
     */
    public Long getEpoch() {
        if (epoch == null)
            return null;
        return new Long(epoch);
    }

    /**
     * Set a valid epoch or null. See {@link #isEpoch(Long)}
     * 
     * @param epoch
     *            The epoch
     * @return True if set, false if not
     */
    public boolean setEpoch(Long epoch) {
        if (epoch == null) {
            this.epoch = null;
            return true;
        }
        if (isEpoch(epoch)) {
            this.epoch = epoch;
            return true;
        }
        return false;
    }

    /**
     * The epoch must be not null and positive
     * 
     * @param epoch
     * @return True if is valid, false if not
     */
    public static boolean isEpoch(Long epoch) {
        return (epoch != null && epoch > 0);
    }

    /**
     * Set a valid epoch
     * 
     * @param epoch
     *            The epoch
     * @return True if the parameter is positive and not null
     */
    public boolean setEpoch(String epoch) {
        if (epoch == null) {
            Long l = null;
            return setEpoch(l);
        }
        if (isEpoch(epoch))
            return setEpoch(Long.decode(epoch));
        return false;
    }

    /**
     * Parse the string and use {@link #isEpoch(Long)}
     * 
     * @param epoch
     * @return True if is valid, false if not
     */
    public static boolean isEpoch(String epoch) {
        try {
            return isEpoch(Long.decode(epoch));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * @return The upstream or null
     */
    public String getUpstream() {
        return upstream;
    }

    /**
     * Set a valid upstream. See {@link #isUpstream(String)}
     * 
     * @param upstream
     * @return True if set, false if invalid upstream
     */
    public boolean setUpstream(String upstream) {
        if (upstream == null || isUpstream(upstream)) {
            this.upstream = upstream;
            return true;
        }
        return false;
    }

    /**
     * Test if the string is a upstream. See <a href=
     * "https://www.debian.org/doc/debian-policy/ch-controlfields.html#s-f-Version"
     * >debian-policy</a>
     * 
     * @param upstream
     *            The text to be tested
     * @return True if is a valid upstream, false if not
     */
    public static boolean isUpstream(String upstream) {
        return (upstream != null && upstream.matches("^[0-9][\\w\\.+-:~]*$"));
    }

    /**
     * @return the revision or null
     */
    public String getRevision() {
        return revision;
    }

    /**
     * Set a valid revision. See {@link #isRevision(String)}
     * 
     * @param revision
     *            The revision text
     * @return True if the revision is valid, false if not
     */
    public boolean setRevision(String revision) {
        if (revision == null || isRevision(revision)) {
            this.revision = revision;
            return true;
        }
        return false;
    }

    /**
     * Test if the string is a revision. See <a href=
     * "https://www.debian.org/doc/debian-policy/ch-controlfields.html#s-f-Version"
     * >debian-policy</a>
     * 
     * @param revision
     *            The text to be tested
     * @return True if is a valid revision, false if not
     */
    public static boolean isRevision(String revision) {
        return (revision != null && revision.matches("^[\\w+\\.~]+$"));
    }

    /**
     * Returns version in the format 'epoch:upstream-revision'. If the required
     * upstream is not set return null
     * 
     * @return The full version string or null
     */
    public String getFullVersion() {
        String fullVersion = "";
        if (epoch != null && epoch.longValue() > 0)
            fullVersion = epoch + ":";

        if (upstream != null)
            fullVersion = fullVersion + upstream;
        else
            return null;

        if (revision != null)
            fullVersion = fullVersion + "-" + revision;

        return fullVersion;
    }

    public String toString() {
        return getFullVersion();
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
     * @boolean True if the text is a valid version
     */
    public boolean parseVersion(String versionData) {
        if (versionData == null)
            return false;

        String epoch = null;
        String upstream = null;
        String revision = null;

        // If there is : the first portion is epoch
        String[] epochs = versionData.split(":");

        if (epochs.length > 1) {
            epoch = epochs[0];
            if (!isEpoch(epoch))
                return false;

            // remove epoch and :
            versionData = versionData.substring(epoch.length() + 1);
        }

        // If there is - the last portion is revision
        String[] revisions = versionData.split("-");

        if (revisions.length > 1) {
            revision = revisions[revisions.length - 1];
            if (!isRevision(revision))
                return false;
            // remove revision and -
            versionData = versionData.substring(0, versionData.length() - (revision.length() + 1));
        }

        // The remain part should be the version
        upstream = versionData;
        if (!isUpstream(upstream))
            return false;

        // Set what was found or null
        setEpoch(epoch);
        setUpstream(upstream);
        setRevision(revision);

        return true;
    }

    /**
     * Compares two Debian versions as
     * {@link #versionCompare(DebianVersion, DebianVersion)}
     * 
     * @param versionText1
     *            The first version text
     * @param versionText2
     *            The second version text
     * @return The same as {@link #versionCompare(DebianVersion, DebianVersion)}
     *         . If some of this versions is invalid an empty version is used
     */
    public static Integer versionTextCompare(String versionText1, String versionText2) {
        DebianVersion v1 = new DebianVersion();
        DebianVersion v2 = new DebianVersion();

        v1.parseVersion(versionText1);
        v2.parseVersion(versionText2);

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
    public static Integer versionCompare(DebianVersion version1, DebianVersion version2) {
        if (version1 == null && version2 == null)
            return 0;
        else if (version1 != null && version2 == null)
            return 1;
        else if (version1 == null && version2 != null)
            return -1;

        // If the right epoch is >0 and the left is null, right is bigger.
        // If the right epoch is null and the left is >0, left is bigger.
        // If both are not null compare his values.
        if (version1.getEpoch() != null && version1.getEpoch().longValue() > 0 && version2.getEpoch() == null)
            return 1;

        else

        if (version1.getEpoch() == null && version2.getEpoch() != null && version2.getEpoch().longValue() > 0)
            return -1;
        else if (version1.getEpoch() != null && version1.getEpoch() != null) {
            if (version1.getEpoch() > version2.getEpoch())
                return 1;
            if (version1.getEpoch() < version2.getEpoch())
                return -1;
        }

        // Compare versions
        int r = versionRevisionCompare(version1.getUpstream(), version2.getUpstream());
        if (r != 0)
            return r;

        // Compare revisions
        return versionRevisionCompare(version1.getRevision(), version2.getRevision());
    }

    /**
     * Converts the String to an ArrayList an call
     * {@link #versionRevisionCompare(ArrayList, ArrayList)}
     * 
     * @param v1
     *            The first version/revision String
     * @param v2
     *            The second version/revision String
     * @return Same as {@link #versionRevisionCompare(ArrayList, ArrayList)}
     * 
     */
    private static int versionRevisionCompare(String v1, String v2) {
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
    private static int versionRevisionCompare(ArrayList<Character> v1, ArrayList<Character> v2) {
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
    private static int getCharacterWeight(Character c) {
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
    private static boolean isDigit(Character c) {
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
    private static boolean isAlpha(Character c) {
        if (c == null)
            return false;
        return (c >= 'a' && c <= 'Z') || (c >= 'A' && c <= 'Z');
    }

    /**
     * Get the next char or null
     * 
     * @param i
     *            The iterator
     * @return The next char. If there is none return null. It the
     *         {@link Iterator} is null return null
     */
    private static Character getNextOrNull(Iterator<Character> i) {
        if (i == null)
            return null;
        return i.hasNext() ? i.next() : null;
    }
}