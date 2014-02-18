package ru.yandex.jenkins.plugins.debuilder.dpkg.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Store data of file entries. This is used in '.changes' and '.dsc' control
 * files.
 * 
 * @author caiocezar
 * 
 */
/**
 * @author caiocezar
 * 
 */
public class DebianFileEntry {
    private String md5;
    private String sha1;
    private String sha256;
    private Long size;
    private String section;
    private String priority;
    private String name;

    /**
     * @return A copy of the md5 string
     */
    public String getMd5() {
        return md5;
    }

    /**
     * Set the md5
     * 
     * @param md5
     * @return True if set, false if md5 is invalid (See
     *         {@link #isAllHex(String,Integer)})
     * 
     */
    public boolean setMd5(String md5) {
        if (md5 == null || isAllHex(md5, 32)) {
            this.md5 = md5;
            return true;
        }
        return false;
    }

    /**
     * @return A copy of the sha1 string
     */
    public String getSha1() {
        return sha1;
    }

    /**
     * Set the sha1
     * 
     * @param sha1
     * @return True if set, false if sha1 is invalid (See
     *         {@link #isAllHex(String,Integer)})
     */
    public boolean setSha1(String sha1) {
        if (sha1 == null || isAllHex(sha1, 40)) {
            this.sha1 = sha1;
            return true;
        }
        return false;
    }

    /**
     * @return A copy of the sha256 string
     */
    public String getSha256() {
        return sha256;
    }

    /**
     * Set the sha256
     * 
     * @param sha256
     * @return True if set, false if sha56 is invalid (See
     *         {@link #isAllHex(String,Integer)})
     */
    public boolean setSha256(String sha256) {
        if (sha256 == null || isAllHex(sha256, 64)) {
            this.sha256 = sha256;
            return true;
        }
        return false;
    }

    /**
     * @return A copy of the size
     */
    public Long getSize() {
        if (size == null)
            return null;
        return new Long(size);
    }

    /**
     * Set the size
     * 
     * @param size
     * @return True if set, false if size is negative
     */
    public boolean setSize(Long size) {
        if (size == null) {
            this.size = null;
            return true;
        }
        if (isSize(size)) {
            this.size = new Long(size);
            return true;
        }
        return false;
    }

    /**
     * Parse the string and set the size using {@link #setSize(Long)}
     * 
     * @param size
     * @return True if set, false if size is invalid
     */
    public boolean setSize(String size) {
        if (size == null)
            return setSize(new Long(null));
        if (isSize(size))
            return setSize(Long.decode(size));
        return false;

    }

    /**
     * @return A copy of the section string
     */
    public String getSection() {
        return section;
    }

    /**
     * Set the section
     * 
     * @param section
     * @return True if set, false if section is invalid
     */
    public boolean setSection(String section) {
        if (section == null || isSection(section)) {
            this.section = section;
            return true;
        }
        return false;
    }

    /**
     * @return A copy of the priority string
     */
    public String getPriority() {
        return priority;
    }

    /**
     * Set the priority
     * 
     * @param priority
     * @return True if set, false if priority is invalid
     */
    public boolean setPriority(String priority) {
        if (priority == null || isPriority(priority)) {
            this.priority = priority;
            return true;
        }
        return false;
    }

    /**
     * @return A copy of the name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the file name
     * 
     * @param name
     * @return
     */
    public boolean setName(String name) {
        if (name == null || isName(name)) {
            this.name = name;
            return true;
        }
        return false;
    }

    /**
     * Return if all characters are hexadecimal and the length is equal
     * 
     * @param hash
     *            The string to test
     * @return True if is the string has only hexadecimal characters, false if
     *         not
     */
    private static boolean isAllHex(String hash, Integer chars) {
        if (hash == null)
            return false;
        if (hash.matches("^[a-fA-f0-9]+$"))
            return (hash.length() == chars);
        return false;
    }

    public static boolean isMd5(String md5) {
        if (!isAllHex(md5, 32))
            return false;
        return true;
    }

    public static boolean isSha1(String md5) {
        if (!isAllHex(md5, 40))
            return false;
        return true;
    }

    public static boolean isSha256(String md5) {
        if (!isAllHex(md5, 64))
            return false;
        return true;
    }

    public static boolean isSize(Long size) {
        return (size != null && size >= 0);
    }

    public static boolean isSize(String size) {
        try {
            return isSize(Long.decode(size));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isSection(String section) {
        if (section == null || !section.matches("^\\p{Print}+$"))
            return false;
        return true;

    }

    public static boolean isPriority(String priority) {
        if (priority == null
                || !(priority.equals("required") || priority.equals("important") || priority.equals("standard")
                        || priority.equals("optional") || priority.equals("extra")))
            return false;
        return true;

    }

    public static boolean isName(String name) {
        if (name == null || name.equals(".") || name.equals("..") || !name.matches("^\\p{Print}+$"))
            return false;
        return true;
    }

    /**
     * Parse a <a href=
     * "https://www.debian.org/doc/debian-policy/ch-controlfields.html#s-f-Files"
     * >md5</a>, <a href=
     * "https://www.debian.org/doc/debian-policy/ch-controlfields.html#s-f-Checksums"
     * >sha1</a> or <a href=
     * "https://www.debian.org/doc/debian-policy/ch-controlfields.html#s-f-Checksums"
     * >sha256</a> file line. If the file name do not change it imports all
     * valid fields, but if the name differ all other fields will be null.
     * </br>Examples:</br> 4c31ab7bfc40d3cf49d7811987390357 1428 text extra
     * example_1.2-1.dsc</br> c6f698f19f2a2aa07dbb9bbda90a2754 571925
     * example_1.2.orig.tar.gz
     * 
     * @param fileLine
     *            The file line
     * @return True is the was successful parsed, false if not
     */
    public boolean parseLine(String fileLine) {

        // Get 3 or 5 block of non spaces divided by spaces
        Pattern pattern = Pattern.compile("^\\s+([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)(?:\\s+([^\\s]+)\\s+([^\\s]+))?\\s*$");
        Matcher matcher = pattern.matcher(fileLine);

        if (!matcher.matches())
            return false;

        String g1 = matcher.group(1);
        String g2 = matcher.group(2);
        String g3 = matcher.group(3);
        String g4 = matcher.group(4);
        String g5 = matcher.group(5);

        String md5 = null;
        String sha1 = null;
        String sha256 = null;
        String size = null;
        String section = null;
        String priority = null;
        String name = null;

        // Test if every group is valid and store for latter validation
        if (g4 != null && g5 != null) {
            // Probably the md5 in .changes
            if (isMd5(g1))
                md5 = g1;

            if (isSize(g2))
                size = g2;

            if (isSection(g3))
                section = g3;

            if (isPriority(g4))
                priority = g4;

            if (isName(g5))
                name = g5;
        } else {
            // Probably a sha1, sha256 or md5 in a .dsc file
            if (isMd5(g1))
                md5 = g1;
            else if (isSha1(g1))
                sha1 = g1;
            else if (isSha256(g1))
                sha256 = g1;

            if (isSize(g2))
                size = g2;

            if (isName(g3))
                name = g3;
        }

        // The md5 in .changes format
        if (md5 != null && size != null && name != null && priority != null && name != null) {
            if (this.name == null || !this.name.equals(name))
                clean();
            setMd5(md5);
            setSize(size);
            setSection(section);
            setPriority(priority);
            setName(name);
        } else
        // The md5 in .dsc format
        if (md5 != null && size != null && name != null) {
            if (this.name == null || !this.name.equals(name))
                clean();
            setMd5(md5);
            setSize(size);
            setName(name);
        } else
        // The sha1 format
        if (sha1 != null && size != null && name != null) {
            if (this.name == null || !this.name.equals(name))
                clean();
            setSha1(sha1);
            setSize(size);
            setName(name);
        } else
        // The sha256 format
        if (sha256 != null && size != null && name != null) {
            if (this.name == null || !this.name.equals(name))
                clean();
            setSha256(sha256);
            setSize(size);
            setName(name);
        } else {
            return false;
        }

        return true;
    }

    /**
     * Set all fields to null
     */
    public void clean() {
        setSha1(null);
        setSha256(null);
        setMd5(null);
        Long l = null;
        setSize(l);
        setSection(null);
        setPriority(null);
        setName(null);
    }

    /**
     * Import all non empty fields from debianFileEntry if the file name is the
     * same
     * 
     * @param debianFileEntry
     *            The entry to compare
     * @return True if the file name is equal, false if not
     */
    public boolean mergeByName(DebianFileEntry debianFileEntry) {
        if (debianFileEntry.getName().equals(this.name)) {
            if (debianFileEntry.getMd5() != null)
                setMd5(debianFileEntry.getMd5());

            if (debianFileEntry.getSha1() != null)
                setSha1(debianFileEntry.getSha1());

            if (debianFileEntry.getSha256() != null)
                setSha256(debianFileEntry.getSha256());

            if (debianFileEntry.getSize() != null)
                setSize(debianFileEntry.getSize());

            if (debianFileEntry.getSection() != null)
                setSection(debianFileEntry.getSection());

            if (debianFileEntry.getPriority() != null)
                setPriority(debianFileEntry.getPriority());

            return true;
        }
        return false;
    }
}