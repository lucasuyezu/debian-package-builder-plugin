package ru.yandex.jenkins.plugins.debuilder.dpkg;

import hudson.FilePath;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;

import ru.yandex.jenkins.plugins.debuilder.DebUtils.Runner;

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
    private Date date;
    public static DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

    public static String SOURCE_KEY = "Source";
    private String source;

    public static String BINARY_KEY = "Binary";
    private String[] binarys;

    public static String ARCHITECTURE_KEY = "Architecture";
    private String[] architectures;

    public static String VERSION_KEY = "Version";
    private Version version;

    public static String DISTRIBUTION_KEY = "Distribution";
    private String[] distributions;

    public static String URGENCY_KEY = "Urgency";
    private String urgency;

    public static String MAINTAINER_KEY = "Maintainer";
    private List<Contact> maintainers;

    public static String CHANGED_BY_KEY = "Changed-by";
    private Contact changedBy;

    public static String DESCRIPTION_KEY = "Description";
    private String shortDesc;
    private String longDesc;

    public static String CLOSES_KEY = "Closes";
    private String[] closes;

    public static String CHANGES_KEY = "Changes";
    private String changes;

    public static String CHECKSUMS_SHA1_KEY = "Checksums-Sha1";
    public static String CHECKSUMS_SHA256_KEY = "Checksums-Sha256";
    public static String FILES_KEY = "Files";
    private Map<String, ChangeFile> files;

    public static String KEY_SEPARATOR = ":";

    public DebianChanges() {

    }

    /**
     * @param dotChangesFile
     * @param runner
     * @throws Exception
     * @throws IOException
     * @throws InterruptedException
     */
    public DebianChanges(FilePath dotChangesFile, Runner runner) throws Exception, IOException, InterruptedException {
        if (readDotChangesFile(dotChangesFile, runner))
            throw new Exception("a");
    }

    public static class Contact {
        private String name;
        private String email;

        public Contact() {

        }

        public Contact(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class ChangeFile {
        private String md5;
        private String sha1;
        private String sha256;
        private Long size;
        private String section;
        private String priority;
        private String name;

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public String getSha1() {
            return sha1;
        }

        public void setSha1(String sha1) {
            this.sha1 = sha1;
        }

        public String getSha256() {
            return sha256;
        }

        public void setSha256(String sha256) {
            this.sha256 = sha256;
        }

        public Long getSize() {
            return size;
        }

        public void setSize(Long size) {
            this.size = size;
        }

        public String getSection() {
            return section;
        }

        public void setSection(String section) {
            this.section = section;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String[] getBinarys() {
        return binarys;
    }

    public void setBinarys(String[] binarys) {
        this.binarys = binarys;
    }

    public String[] getArchitectures() {
        return architectures;
    }

    public void setArchitectures(String[] architectures) {
        this.architectures = architectures;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public String[] getDistributions() {
        return distributions;
    }

    public void setDistributions(String[] distributions) {
        this.distributions = distributions;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public List<Contact> getMaintainers() {
        return maintainers;
    }

    public void setMaintainers(List<Contact> maintainers) {
        this.maintainers = maintainers;
    }

    public void addMaintainer(String name, String email) {
        if (maintainers == null)
            setMaintainers(new ArrayList<Contact>());
        maintainers.add(new Contact(name, email));
    }

    public Contact getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Contact changedBy) {
        this.changedBy = changedBy;
    }

    public void setChangedBy(String name, String email) {
        Contact contact = new Contact(name, email);
        setChangedBy(contact);
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

    public String[] getCloses() {
        return closes;
    }

    public void setCloses(String[] closes) {
        this.closes = closes;
    }

    public String getChanges() {
        return changes;
    }

    public void setChanges(String changes) {
        this.changes = changes;
    }

    public Map<String, ChangeFile> getFiles() {
        return files;
    }

    public void setFiles(Map<String, ChangeFile> files) {
        this.files = files;
    }

    // public String getBody() {
    // String body = "";
    //
    // String notMandatory = "";
    // if (this.urgency != null)
    // notMandatory += URGENCY_KEY + KEY_SEPARATOR + " " + urgency + "\n";
    // if (this.changedBy != null)
    // notMandatory += CHANGED_BY_KEY + KEY_SEPARATOR + " " +
    // changedBy.getName() + " <" + changedBy.getEmail() + ">,";
    // if (this.closes != null)
    // notMandatory += CLOSES_KEY + KEY_SEPARATOR + " " + closes + "\n";
    //
    // body += FORMAT_KEY + KEY_SEPARATOR + " " + format + "\n";
    // body += DATE_KEY + KEY_SEPARATOR + " " + dateFormat.format(date) + "\n";
    // body += SOURCE_KEY + KEY_SEPARATOR + " " + source + "\n";
    // body += BINARY_KEY + KEY_SEPARATOR;
    // for (String bin : binarys)
    // body += " " + bin;
    // body += "\n";
    //
    // body += ARCHITECTURE_KEY + KEY_SEPARATOR;
    // for (String arch : architectures)
    // body += " " + arch;
    // body += "\n";
    //
    // body += VERSION_KEY + KEY_SEPARATOR + " ";
    // if (version != null)
    // body += version.getFullVersion();
    // body += "\n";
    //
    // body += DISTRIBUTION_KEY + KEY_SEPARATOR;
    // for (String dist : distributions)
    // body += " " + dist;
    // body += "\n";
    //
    // body += URGENCY_KEY + KEY_SEPARATOR + " " + urgency + "\n";
    // body += MAINTAINER_KEY + KEY_SEPARATOR;
    // for (Contact maint : maintainers)
    // body += " " + maint.getName() + " <" + maint.getEmail() + ">,";
    // body += "\n";
    //
    // body += notMandatory;
    // body += DESCRIPTION_KEY + KEY_SEPARATOR + " " + shortDesc + "\n";
    // if (longDesc != null)
    // body += longDesc + "\n";
    //
    // body += CHANGES_KEY + KEY_SEPARATOR + "\n" + changes + "\n";
    // body += CHECKSUMS_SHA1_KEY + KEY_SEPARATOR + "\n";
    // if (files != null)
    // for (ChangeFile f : files.values())
    // body += " " + f.getSha1() + " " + f.getSize() + " " + f.getName() + "\n";
    //
    // body += CHECKSUMS_SHA256_KEY + KEY_SEPARATOR + "\n";
    // if (files != null)
    // for (ChangeFile f : files.values())
    // body += " " + f.getSha256() + " " + f.getSize() + " " + f.getName() +
    // "\n";
    //
    // body += FILES_KEY + KEY_SEPARATOR + "\n";
    // if (files != null)
    // for (ChangeFile f : files.values())
    // body += " " + f.getMd5() + " " + f.getSection() + " " + f.getPriority() +
    // " " + f.getSize() + " " + f.getName()
    // + "\n";
    //
    // return body;
    // }

    public Boolean readDotChangesFile(FilePath dotChangesFile, Runner runner) throws IOException, InterruptedException,
            DpkgException {
        runner.announce("Parsing .changes file");
        DebianChanges tmpDebianChanges = DebianChanges.parseChanges(dotChangesFile, runner);
        runner.announce("Validating .changes informations");
        if (DebianChanges.validate(tmpDebianChanges, dotChangesFile.getParent(), runner)) {
            this.override(tmpDebianChanges);
            return true;
        }
        return false;
    }

    /**
     * Copy all fields from another DebianChanges to this
     * 
     * @param debianChanges
     *            The object to copy from
     * @return Success(true) or fail(false)
     */
    public Boolean override(DebianChanges debianChanges) {
        if (debianChanges == null)
            return false;
        this.format = debianChanges.getFormat();
        this.date = debianChanges.getDate();
        this.source = debianChanges.getSource();
        this.binarys = debianChanges.getBinarys();
        this.architectures = debianChanges.getArchitectures();
        this.version = debianChanges.getVersion();
        this.distributions = debianChanges.getDistributions();
        this.urgency = debianChanges.getUrgency();
        this.maintainers = debianChanges.getMaintainers();
        this.changedBy = debianChanges.getChangedBy();
        this.shortDesc = debianChanges.getShortDesc();
        this.longDesc = debianChanges.getLongDesc();
        this.closes = debianChanges.getCloses();
        this.changes = debianChanges.getChanges();
        this.files = debianChanges.getFiles();

        return true;
    }

    /**
     * Parse a '.changes' file.
     * 
     * @param dotChangesFile
     *            The .'changes' file
     * @param runner
     *            The runner to log
     * @return The DebianChanges with the parsed fields
     * @throws IOException
     *             If something goes wrong reading the file
     * @throws DpkgException
     * @throws InterruptedException
     */
    public static DebianChanges parseChanges(FilePath dotChangesFile, Runner runner) throws IOException, DpkgException,
            InterruptedException {

        if (dotChangesFile == null || !dotChangesFile.exists())
            throw new DpkgException("The .changes file name is empty or not exists");

        if (dotChangesFile.isDirectory())
            throw new DpkgException("The .changes received is a directory");

        DebianChanges debianChanges = new DebianChanges();

        Pattern dotChangesPattern = Pattern.compile("([\\w-]+:)\\s*(.*)");

        ArrayList<String> dotChangesLines = new ArrayList<String>(Arrays.asList(dotChangesFile.readToString().split("\n")));

        ListIterator<String> dotChangesLinesIterator = dotChangesLines.listIterator();

        while (dotChangesLinesIterator.hasNext()) {

            String row = dotChangesLinesIterator.next();

            if (row.matches(".*-----BEGIN PGP SIGNED MESSAGE-----.*"))
                continue;

            if (row.matches(".*-----BEGIN PGP SIGNATURE-----.*"))
                break;

            Matcher dotChangesMatch = dotChangesPattern.matcher(row);

            if (dotChangesMatch.matches()) {
                String key = dotChangesMatch.group(1);
                String data = dotChangesMatch.group(2);

                if (key.equalsIgnoreCase(DebianChanges.FORMAT_KEY + DebianChanges.KEY_SEPARATOR)) {
                    debianChanges.setFormat(data);

                } else if (key.equalsIgnoreCase(DebianChanges.DATE_KEY + DebianChanges.KEY_SEPARATOR)) {
                    DateFormat df = DebianChanges.dateFormat;
                    Date date;
                    try {
                        date = df.parse(data);
                    } catch (ParseException e) {
                        // ignore
                        runner.announce("Fail parsing date: " + e.getMessage());
                        e.printStackTrace();
                        continue;
                    }
                    debianChanges.setDate(date);

                } else if (key.equalsIgnoreCase(DebianChanges.SOURCE_KEY + DebianChanges.KEY_SEPARATOR)) {
                    debianChanges.setSource(data);

                } else if (key.equalsIgnoreCase(DebianChanges.BINARY_KEY + DebianChanges.KEY_SEPARATOR)) {
                    debianChanges.setBinarys(data.split("\\s"));
                    // for (String s : data.split("\\s"))
                    // logger.println("binary: " + s);

                } else if (key.equalsIgnoreCase(DebianChanges.ARCHITECTURE_KEY + DebianChanges.KEY_SEPARATOR)) {
                    debianChanges.setArchitectures(data.split("\\s"));
                    // for (String s : data.split("\\s"))
                    // logger.println("arch: " + s);

                } else if (key.equalsIgnoreCase(DebianChanges.VERSION_KEY + DebianChanges.KEY_SEPARATOR)) {
                    Version version = new Version();
                    try {
                        version.parseVersion(data);
                    } catch (VersionFormatException e) {
                        runner.announce("Fail parsing version: " + e.getMessage());
                        e.printStackTrace();
                    }
                    debianChanges.setVersion(version);

                } else if (key.equalsIgnoreCase(DebianChanges.DISTRIBUTION_KEY + DebianChanges.KEY_SEPARATOR)) {
                    debianChanges.setDistributions(data.split("\\s"));
                    // for (String s : data.split("\\s"))
                    // logger.println("distr: " + s);

                } else if (key.equalsIgnoreCase(DebianChanges.URGENCY_KEY + DebianChanges.KEY_SEPARATOR)) {
                    debianChanges.setUrgency(data);

                } else if (key.equalsIgnoreCase(DebianChanges.MAINTAINER_KEY + DebianChanges.KEY_SEPARATOR)) {

                    Pattern maintainerPattern = Pattern.compile("([^<]*)\\s*<(.*)");

                    for (String maintainer : data.split(">\\s*")) {
                        Matcher maintainerMatch = maintainerPattern.matcher(maintainer);
                        if (maintainerMatch.matches())
                            debianChanges.addMaintainer(maintainerMatch.group(1), maintainerMatch.group(2));
                    }

                } else if (key.equalsIgnoreCase(DebianChanges.CHANGED_BY_KEY + DebianChanges.KEY_SEPARATOR)) {

                    Pattern changedByPattern = Pattern.compile("([^<]*)\\s*<([^>]*)>");
                    Matcher changedByMatch = changedByPattern.matcher(data);

                    if (changedByMatch.matches())
                        debianChanges.setChangedBy(changedByMatch.group(1), changedByMatch.group(2));

                } else if (key.equalsIgnoreCase(DebianChanges.DESCRIPTION_KEY + DebianChanges.KEY_SEPARATOR)) {
                    debianChanges.setShortDesc(data);
                    String longDesc = getTextBlock(dotChangesLinesIterator);
                    debianChanges.setLongDesc(longDesc);
                    // logger.println("long desc: '" + longDesc + "'");

                } else if (key.equalsIgnoreCase(DebianChanges.CLOSES_KEY + DebianChanges.KEY_SEPARATOR)) {
                    debianChanges.setCloses(data.split("\\s*"));
                    // for (String s : data.split("\\s"))
                    // logger.println("closes: " + s);

                } else if (key.equalsIgnoreCase(DebianChanges.CHANGES_KEY + DebianChanges.KEY_SEPARATOR)) {
                    String changesText = getTextBlock(dotChangesLinesIterator);
                    debianChanges.setChanges(changesText);
                    // logger.println("changes: " + changesText);

                } else if (key.equalsIgnoreCase(DebianChanges.CHECKSUMS_SHA1_KEY + DebianChanges.KEY_SEPARATOR)
                        || key.equalsIgnoreCase(DebianChanges.CHECKSUMS_SHA256_KEY + DebianChanges.KEY_SEPARATOR)
                        || key.equalsIgnoreCase(DebianChanges.FILES_KEY + DebianChanges.KEY_SEPARATOR)) {
                    HashMap<String, DebianChanges.ChangeFile> fileEntrys;

                    if (debianChanges.getFiles() != null)
                        fileEntrys = (HashMap<String, ChangeFile>) debianChanges.getFiles();
                    else
                        fileEntrys = new HashMap<String, ChangeFile>();

                    String filesText = getTextBlock(dotChangesLinesIterator);
                    // logger.println("files: " + filesText);

                    for (String fileRow : filesText.split("\\n")) {
                        // logger.println("file line: " + fileRow);

                        DebianChanges.ChangeFile file;

                        Pattern sha = Pattern.compile("^ +([^ ]+) +([^ ]+) +([^ ]+) *$");
                        Pattern md5 = Pattern.compile("^ +([^ ]+) +([^ ]+) +([^ ]+) +([^ ]+) +([^ ]+) *$");
                        Matcher mChangeFile = sha.matcher(fileRow);
                        if (!mChangeFile.matches())
                            mChangeFile = md5.matcher(fileRow);

                        if (mChangeFile.matches()) {
                            // validar porque este loop esta pegando null
                            // for (int ii = 1; ii <=
                            // mChangeFile.groupCount(); ii++) {
                            // runner.announce("file field " +
                            // String.valueOf(ii) + ": " +
                            // mChangeFile.group(ii));
                            // }
                            // if the file (aka last group) exists, edit
                            // that entry
                            if (fileEntrys.containsKey(mChangeFile.group(mChangeFile.groupCount())))
                                file = fileEntrys.get(mChangeFile.group(mChangeFile.groupCount()));
                            else
                                file = new DebianChanges.ChangeFile();

                            if (key.equalsIgnoreCase("Checksums-Sha1:"))
                                file.setSha1(mChangeFile.group(1));
                            else if (key.equalsIgnoreCase("Checksums-Sha256:"))
                                file.setSha256(mChangeFile.group(1));
                            else if (key.equalsIgnoreCase("Files:"))
                                file.setMd5(mChangeFile.group(1));

                            file.setSize(Long.decode(mChangeFile.group(2)));

                            if (mChangeFile.groupCount() == 3)
                                file.setName(mChangeFile.group(3));
                            else if (mChangeFile.groupCount() == 5) {
                                file.setSection(mChangeFile.group(3));
                                file.setPriority(mChangeFile.group(4));
                                file.setName(mChangeFile.group(5));
                            }

                            fileEntrys.put(file.getName(), file);
                        } else {
                            // runner.announce("file fields not matched");
                        }
                    }
                    debianChanges.setFiles(fileEntrys);

                } else {
                    // runner.announce("Match but no key found");
                }
            } else {
                // runner.announce("line not matched: " + row);
            }
        }
        return debianChanges;
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

    // TODO the validate method should validate fields
    private static Boolean validate(DebianChanges changes, FilePath rootDir, Runner runner) throws IOException,
            InterruptedException {
        // TODO test messages
        String msgPrefix = "Validating .changes parse process:";

        if (changes == null || rootDir == null) {
            runner.announce("{0} Internal error (changes or rootDir is null)!", msgPrefix);
            return false;
        }

        if (changes.getDistributions() == null) {
            runner.announce("{0} None distributions found.", msgPrefix);
            return false;
        } else if (changes.getDistributions().length < 1) {
            runner.announce("{0} None distributions found.", msgPrefix);
            return false;
        }

        for (String dist : changes.getDistributions()) {
            if (dist.equals("UNRELEASED")) {
                runner.announce("{0} Distribution UNRELEASED found", msgPrefix);
                return false;
            }
        }

        if (changes.getFiles() == null) {
            runner.announce("{0} None file found.", msgPrefix);
            return false;
        }

        for (DebianChanges.ChangeFile changeFile : changes.getFiles().values()) {

            if (changeFile.getName() == null) {
                runner.announce("{0} File name is empty.", msgPrefix);
                return false;
            }
            runner.announce("Validating file " + changeFile.getName() + " size and digests.");

            FilePath file = rootDir.child(changeFile.getName());
            if (!validateSize(changeFile.getSize(), file)) {
                runner.announce("{0} Size of {1} not match ({2})", msgPrefix, file.getRemote(), file.length());
                return false;
            } else if (!validateMd5(changeFile.getMd5(), file)) {
                runner.announce("{0} Md5 of {1} not match ({2})", msgPrefix, file.getRemote(), DigestUtils.md5(file.read()));
                return false;
            } else if (!validateSha1(changeFile.getSha1(), file)) {
                runner.announce("{0} Sha1 of {1} not match ({2})", msgPrefix, file.getRemote(), DigestUtils.sha1Hex(file.read()));
                return false;
            } else if (!validateSha256(changeFile.getSha256(), file)) {
                runner.announce("{0} Sha256 of {1} not match ({2}) ", msgPrefix, file.getRemote(),
                        DigestUtils.sha256Hex(file.read()));
                return false;
            }

        }
        return true;
    }

    public static boolean validateMd5(String md5, FilePath file) throws IOException {
        if (md5.equals("") || md5 == null)
            return false;
        if (md5.equals(DigestUtils.md5Hex(file.read())))
            return true;
        return false;
    }

    public static boolean validateSha1(String sha1, FilePath file) throws IOException {
        if (sha1.equals("") || sha1 == null)
            return false;
        if (sha1.equals(DigestUtils.sha1Hex(file.read())))
            return true;
        return false;
    }

    public static boolean validateSha256(String sha256, FilePath file) throws IOException {
        if (sha256.equals("") || sha256 == null)
            return false;
        if (sha256.equals(DigestUtils.sha256Hex(file.read())))
            return true;
        return false;
    }

    public static boolean validateSize(Long size, FilePath file) throws IOException, InterruptedException {
        if (size == null)
            return false;
        if (file.length() == size)
            return true;
        return false;
    }
}