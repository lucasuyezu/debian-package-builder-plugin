package ru.yandex.jenkins.plugins.debuilder.dpkg;

import hudson.FilePath;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import ru.yandex.jenkins.plugins.debuilder.dpkg.common.DebianDate;
import ru.yandex.jenkins.plugins.debuilder.dpkg.common.DebianDistributions;
import ru.yandex.jenkins.plugins.debuilder.dpkg.common.DebianFileEntry;
import ru.yandex.jenkins.plugins.debuilder.dpkg.common.DebianFiles;
import ru.yandex.jenkins.plugins.debuilder.dpkg.common.DebianVersion;

/**
 * Stores all data from .changes files used to control the upload process of
 * debian packages
 * 
 * @author caiocezar
 * 
 */
public class DebianChanges {
    public static String FORMAT_KEY = "Format";
    private String format;

    public static String DATE_KEY = "Date";
    private DebianDate date;

    public static String SOURCE_KEY = "Source";
    private String source;

    public static String BINARY_KEY = "Binary";
    private ArrayList<String> binarys;

    public static String ARCHITECTURE_KEY = "Architecture";
    private ArrayList<String> architectures;

    public static String VERSION_KEY = "Version";
    private DebianVersion version;

    public static String DISTRIBUTION_KEY = "Distribution";
    private DebianDistributions distributions;

    public static String URGENCY_KEY = "Urgency";
    private String urgency;

    public static String MAINTAINER_KEY = "Maintainer";
    private List<InternetAddress> maintainers;

    public static String CHANGED_BY_KEY = "Changed-by";
    private InternetAddress changedBy;

    public static String DESCRIPTION_KEY = "Description";
    private String shortDesc;
    private String longDesc;

    public static String CLOSES_KEY = "Closes";
    private ArrayList<String> closes;

    public static String CHANGES_KEY = "Changes";
    private String changes;

    public static String CHECKSUMS_SHA1_KEY = "Checksums-Sha1";
    public static String CHECKSUMS_SHA256_KEY = "Checksums-Sha256";
    public static String FILES_KEY = "Files";
    private DebianFiles files;

    public static String KEY_SEPARATOR = ":";

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public DebianDate getDate() {
        return date;
    }

    public void setDate(DebianDate date) {
        this.date = date;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public ArrayList<String> getBinarys() {
        return binarys;
    }

    public void setBinarys(ArrayList<String> binarys) {
        this.binarys = binarys;
    }

    public ArrayList<String> getArchitectures() {
        return architectures;
    }

    public void setArchitectures(ArrayList<String> architectures) {
        this.architectures = architectures;
    }

    public DebianVersion getVersion() {
        return version;
    }

    public void setVersion(DebianVersion version) {
        this.version = version;
    }

    public DebianDistributions getDistributions() {
        return distributions;
    }

    public void setDistributions(DebianDistributions distributions) {
        this.distributions = distributions;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public List<InternetAddress> getMaintainers() {
        return maintainers;
    }

    public void setMaintainers(List<InternetAddress> maintainers) {
        this.maintainers = maintainers;
    }

    public void setMaintainers(String maintainersList) throws AddressException {
        this.maintainers = Arrays.asList(InternetAddress.parse(maintainersList, true));
    }

    public InternetAddress getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(InternetAddress changedBy) {
        this.changedBy = changedBy;
    }

    /**
     * Set the changed-by
     * 
     * @param line
     * @return True if line is a valid {@link InternetAddress}
     */
    public boolean setChangedBy(String line) {
        try {
            InternetAddress contact = new InternetAddress(line, true);
            setChangedBy(contact);
            return true;
        } catch (AddressException e) {
            return false;
        }

    }

    public String getShortDesc() {
        return shortDesc;
    }

    public void setShortDesc(String shortDesc) {
        this.shortDesc = shortDesc;
    }

    public String getLongDesc() {
        return longDesc;
    }

    public void setLongDesc(String longDesc) {
        this.longDesc = longDesc;
    }

    public ArrayList<String> getCloses() {
        return closes;
    }

    public void setCloses(ArrayList<String> closes) {
        this.closes = closes;
    }

    public String getChanges() {
        return changes;
    }

    public void setChanges(String changes) {
        this.changes = changes;
    }

    public DebianFiles getFiles() {
        return files;
    }

    public void setFiles(DebianFiles files) {
        this.files = files;
    }

    private String bodyLine(Object... objs) {
        return MessageFormat.format("{0}" + KEY_SEPARATOR + " {1}\n", objs);
    }

    public String getBody() {
        String body = "";

        body += format != null ? bodyLine(FORMAT_KEY, format) : "";
        body += date != null ? bodyLine(DATE_KEY, DebianDate.dateFormat.format(date.getDate())) : "";
        body += source != null ? bodyLine(SOURCE_KEY, source) : "";

        if (binarys != null && !binarys.isEmpty()) {
            body += BINARY_KEY + KEY_SEPARATOR;
            for (String bin : binarys)
                body += " " + bin;
            body += "\n";
        }

        if (architectures != null && !architectures.isEmpty()) {
            body += ARCHITECTURE_KEY + KEY_SEPARATOR;
            for (String arch : architectures)
                body += " " + arch;
            body += "\n";
        }

        body += version != null ? bodyLine(VERSION_KEY, version.getFullVersion()) : "";

        if (distributions != null && distributions.getCopy() != null && !distributions.getCopy().isEmpty()) {
            body += DISTRIBUTION_KEY + KEY_SEPARATOR;
            for (String dist : distributions.getCopy())
                body += " " + dist;
            body += "\n";
        }

        body += urgency != null ? bodyLine(URGENCY_KEY, urgency) : "";

        if (maintainers != null && !maintainers.isEmpty()) {
            body += MAINTAINER_KEY + KEY_SEPARATOR;
            for (InternetAddress maint : maintainers)
                body += " " + maint.getPersonal() + " <" + maint.getAddress() + ">,";
            body += "\n";
        }

        body += urgency != null ? bodyLine(URGENCY_KEY, urgency) : "";
        body += changedBy != null ? bodyLine(CHANGED_BY_KEY, changedBy.getPersonal() + " <" + changedBy.getAddress() + ">") : "";
        body += closes != null ? bodyLine(CLOSES_KEY, closes) : "";

        if (shortDesc != null) {
            body += bodyLine(DESCRIPTION_KEY, shortDesc);
            body += longDesc != null ? longDesc + "\n" : "";
        }

        body += changes != null ? bodyLine(CHANGES_KEY, "") + changes + "\n" : "";

        if (files != null && !files.getFileEntries().isEmpty()) {

            body += bodyLine(CHECKSUMS_SHA1_KEY, "");
            if (files != null)
                for (DebianFileEntry f : files.getFileEntries())
                    body += " " + f.getSha1() + " " + f.getSize() + " " + f.getName() + "\n";

            body += bodyLine(CHECKSUMS_SHA256_KEY, "");
            if (files != null)
                for (DebianFileEntry f : files.getFileEntries())
                    body += " " + f.getSha256() + " " + f.getSize() + " " + f.getName() + "\n";

            body += bodyLine(FILES_KEY, "");
            if (files != null)
                for (DebianFileEntry f : files.getFileEntries())
                    body += " " + f.getMd5() + " " + f.getSize() + " " + f.getSection() + " " + f.getPriority() + " "
                            + f.getName() + "\n";
        }

        return body;
    }

    /**
     * Parse and validate the file
     * 
     * @param file
     *            The file
     * @return An empty string if it successes or the multi-line error message.
     */
    public String read(FilePath file) {

        if (parse(file) == null)
            return "Fail reading the file";

        String msg = validateAll(file.getParent());
        if (!msg.isEmpty())
            return "The validation has erros: " + msg;

        return "";
    }

    /**
     * Read a file and use {@link #parse(String)} to parse the content
     * 
     * @param file
     *            The .changes file
     * @return The same as {@link #parse(String)} or null if there is same
     *         problem reading the file
     */
    public List<Pair<String, String>> parse(FilePath file) {
        if (file == null)
            return null;

        try {
            return parse(file.readToString());
        } catch (IOException e1) {
            e1.printStackTrace();
            return null;
        }
    }

    /**
     * Parse a '.changes' content.
     * 
     * @param content
     *            The content of a .'changes' file
     * @return A {@link List} with a {@link Pair} that holds the message and the
     *         rejected line, if all lines are valid the List is empty. Never
     *         return null;
     */
    public List<Pair<String, String>> parse(String content) {
        if (content == null)
            content = "";

        List<Pair<String, String>> rejected = new ArrayList<Pair<String, String>>();

        ArrayList<String> lines = new ArrayList<String>(Arrays.asList(content.split("\n")));
        Pattern pattern = Pattern.compile("^([\\w-]+:)\\s*(.*)\\s*$");
        ListIterator<String> i = lines.listIterator();

        while (i.hasNext()) {

            String row = i.next();

            if (row.matches(".*-----BEGIN PGP SIGNED MESSAGE-----.*") || row.matches("^\\s*$") || row.matches("(?i)^\\s*HASH:.*"))
                continue;

            if (row.matches(".*-----BEGIN PGP SIGNATURE-----.*"))
                break;

            Matcher matcher = pattern.matcher(row);

            if (matcher.matches()) {
                String key = matcher.group(1).trim();
                String data;
                if (matcher.group(2) != null)
                    data = matcher.group(2).trim();
                else
                    data = null;

                if (key.equalsIgnoreCase(FORMAT_KEY + KEY_SEPARATOR)) {
                    setFormat(data);

                } else if (key.equalsIgnoreCase(DATE_KEY + KEY_SEPARATOR)) {
                    DebianDate date = new DebianDate();
                    if (date.setDate(data))
                        setDate(date);
                    else
                        rejected.add(new ImmutablePair<String, String>("Ignoring invalid date '" + data + "'", row));

                } else if (key.equalsIgnoreCase(SOURCE_KEY + KEY_SEPARATOR)) {
                    setSource(data);

                } else if (key.equalsIgnoreCase(BINARY_KEY + KEY_SEPARATOR)) {
                    setBinarys(new ArrayList<String>(Arrays.asList(data.split("\\s+"))));

                } else if (key.equalsIgnoreCase(ARCHITECTURE_KEY + KEY_SEPARATOR)) {
                    setArchitectures(new ArrayList<String>(Arrays.asList(data.split("\\s+"))));

                } else if (key.equalsIgnoreCase(VERSION_KEY + KEY_SEPARATOR)) {
                    DebianVersion v = new DebianVersion();

                    if (v.parseVersion(data))
                        setVersion(v);
                    else
                        rejected.add(new ImmutablePair<String, String>("Ignoring invalid version '" + data + "'", row));

                } else if (key.equalsIgnoreCase(DISTRIBUTION_KEY + KEY_SEPARATOR)) {
                    DebianDistributions dists = new DebianDistributions();

                    for (String dist : data.split("\\s+"))
                        if (!dists.merge(dist))
                            rejected.add(new ImmutablePair<String, String>("Rejected distribution '" + dist + "'", row));

                    setDistributions(dists);

                } else if (key.equalsIgnoreCase(URGENCY_KEY + KEY_SEPARATOR)) {
                    setUrgency(data);

                } else if (key.equalsIgnoreCase(MAINTAINER_KEY + KEY_SEPARATOR)) {
                    try {
                        setMaintainers(data);
                    } catch (AddressException e) {
                        rejected.add(new ImmutablePair<String, String>("Fail parsing maintainers", row));
                        e.printStackTrace();
                    }

                } else if (key.equalsIgnoreCase(CHANGED_BY_KEY + KEY_SEPARATOR)) {

                    if (!setChangedBy(data))
                        rejected.add(new ImmutablePair<String, String>("Ignoring invalid changed-by", row));

                } else if (key.equalsIgnoreCase(DESCRIPTION_KEY + KEY_SEPARATOR)) {

                    setShortDesc(data);
                    setLongDesc(getTextBlock(i));

                } else if (key.equalsIgnoreCase(CLOSES_KEY + KEY_SEPARATOR)) {

                    setCloses(new ArrayList<String>(Arrays.asList(data.split("\\s*"))));

                } else if (key.equalsIgnoreCase(CHANGES_KEY + KEY_SEPARATOR)) {

                    setChanges(getTextBlock(i));

                } else if (key.equalsIgnoreCase(CHECKSUMS_SHA1_KEY + KEY_SEPARATOR)
                        || key.equalsIgnoreCase(CHECKSUMS_SHA256_KEY + KEY_SEPARATOR)
                        || key.equalsIgnoreCase(FILES_KEY + KEY_SEPARATOR)) {
                    DebianFiles files;

                    if (getFiles() != null)
                        files = getFiles();
                    else
                        files = new DebianFiles();

                    String filesText = getTextBlock(i);

                    for (String fileRow : filesText.split("\\n")) {

                        DebianFileEntry entry = new DebianFileEntry();
                        if (entry.parseLine(fileRow))
                            files.mergeEntryByName(entry);
                        else
                            rejected.add(new ImmutablePair<String, String>("Ignoring invalid file line", fileRow));

                    }
                    setFiles(files);
                } else {
                    rejected.add(new ImmutablePair<String, String>("Ignoring an invalid line", row));
                }
            } else {
                rejected.add(new ImmutablePair<String, String>("Ignoring an invalid line", row));
            }
        }
        return rejected;
    }

    private static String getTextBlock(ListIterator<String> fileIterator) {
        String block = "";
        while (fileIterator.hasNext()) {
            String line = fileIterator.next();

            if (!line.startsWith(" ")) {
                fileIterator.previous();
                break;
            }
            if (block != "")
                block += '\n';
            block += line;
        }
        return block;
    }

    // TODO the validate method should validate all fields
    /**
     * Validate all required fields and files data
     * 
     * @param basePath
     *            The location where the files are
     * @return An empty string if there is no problem or the error message
     */
    private String validateAll(FilePath basePath) {

        if (basePath == null)
            return ("Invalid directory '" + basePath + "'");

        if (getDistributions() == null || getDistributions().getCopy().size() <= 0)
            return ("None distributions found.");

        for (String dist : getDistributions().getCopy())
            if (dist.equals("UNRELEASED"))
                return ("Distribution UNRELEASED found.");

        if (getFiles() == null || getFiles().getFileEntries() == null || getFiles().getFileEntries().size() <= 0)
            return ("None file to publish.");

        for (DebianFileEntry changeFile : getFiles().getFileEntries()) {

            if (changeFile.getName() == null)
                return ("There is a file entry without a name.");

            FilePath file = basePath.child(changeFile.getName());

            if (!validateSize(changeFile.getSize(), file))
                return ("File '" + file + "' has incorrect size.");

            if (!validateMd5(changeFile.getMd5(), file))
                return ("File '" + file + "' has incorrect md5.");

            if (!validateSha1(changeFile.getSha1(), file))
                return ("File '" + file + "' has incorrect sha1");

            if (!validateSha256(changeFile.getSha256(), file))
                return ("File '" + file + "' has incorrect sha256");

        }
        return "";
    }

    /**
     * Validate the md5 hash
     * 
     * @param md5
     *            The hash
     * @param file
     *            The file
     * @return True the hash is equal, false if is not, null for file reading
     *         problems
     */
    public static Boolean validateMd5(String md5, FilePath file) {
        if (md5 == null || md5.equals(""))
            return false;
        if (file == null)
            return null;

        try {
            if (md5.equals(DigestUtils.md5Hex(file.read())))
                return true;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return false;
    }

    /**
     * Validate the sha1 hash
     * 
     * @param sha1
     *            The hash
     * @param file
     *            The file
     * @return True the hash is equal, false if is not, null for file reading
     *         problems
     */
    public static Boolean validateSha1(String sha1, FilePath file) {
        if (sha1 == null || sha1.equals(""))
            return false;
        if (file == null)
            return null;

        try {
            if (sha1.equals(DigestUtils.sha1Hex(file.read())))
                return true;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return false;
    }

    /**
     * Validate the sha256 hash
     * 
     * @param sha256
     *            The hash
     * @param file
     *            The file
     * @return True the hash is equal, false if is not, null for file reading
     *         problems
     */
    public static Boolean validateSha256(String sha256, FilePath file) {
        if (sha256 == null || sha256.equals(""))
            return false;
        if (file == null)
            return null;

        try {
            if (sha256.equals(DigestUtils.sha256Hex(file.read())))
                return true;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return false;
    }

    /**
     * Validate the file size
     * 
     * @param size
     *            The size to compare
     * @param file
     *            The file
     * @return True the size is equal, false if is not, null for file reading
     *         problems
     */
    public static Boolean validateSize(Long size, FilePath file) {
        if (size == null)
            return false;
        if (file == null)
            return null;

        try {
            if (file.length() == size)
                return true;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        return false;
    }

}