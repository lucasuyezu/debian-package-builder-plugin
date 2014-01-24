package ru.yandex.jenkins.plugins.debuilder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Stores all data from .changes files used to control the upload process of
 * debian packages
 * 
 * @author caiocezar
 * 
 */
public class DebChanges {
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

    public static class Version {
        private Long epoch;
        private String upstream;
        private String revision;

        public long getEpoch() {
            return epoch;
        }

        public void setEpoch(Long epoch) {
            this.epoch = epoch;
        }

        public String getUpstream() {
            return upstream;
        }

        public void setUpstream(String upstream) {
            this.upstream = upstream;
        }

        public String getRevision() {
            return revision;
        }

        public void setRevision(String revision) {
            this.revision = revision;
        }

        public String getFullVersion() {
            String fullVersion = "";
            if (epoch != null)
                fullVersion = epoch.toString() + ":";

            fullVersion = fullVersion + this.upstream;
            if (this.revision != null && !this.revision.isEmpty()) {
                fullVersion = fullVersion + "-" + this.revision;
            }
            return fullVersion;
        }
    }

    public static class Contact {
        private String name;
        private String email;

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

    public Contact getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Contact changedBy) {
        this.changedBy = changedBy;
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

    public String getBody() {
        String body = "";

        String notMandatory = "";
        if (this.urgency != null)
            notMandatory += URGENCY_KEY + KEY_SEPARATOR + " " + urgency + "\n";
        if (this.changedBy != null)
            notMandatory += CHANGED_BY_KEY + KEY_SEPARATOR + " " + changedBy.getName() + " <" + changedBy.getEmail() + ">,";
        if (this.closes != null)
            notMandatory += CLOSES_KEY + KEY_SEPARATOR + " " + closes + "\n";

        body += FORMAT_KEY + KEY_SEPARATOR + " " + format + "\n";
        body += DATE_KEY + KEY_SEPARATOR + " " + dateFormat.format(date) + "\n";
        body += SOURCE_KEY + KEY_SEPARATOR + " " + source + "\n";
        body += BINARY_KEY + KEY_SEPARATOR;
        for (String bin : binarys)
            body += " " + bin;
        body += "\n";

        body += ARCHITECTURE_KEY + KEY_SEPARATOR;
        for (String arch : architectures)
            body += " " + arch;
        body += "\n";

        body += VERSION_KEY + KEY_SEPARATOR + " ";
        if (version != null)
            body += version.getFullVersion();
        body += "\n";

        body += DISTRIBUTION_KEY + KEY_SEPARATOR;
        for (String dist : distributions)
            body += " " + dist;
        body += "\n";

        body += URGENCY_KEY + KEY_SEPARATOR + " " + urgency + "\n";
        body += MAINTAINER_KEY + KEY_SEPARATOR;
        for (Contact maint : maintainers)
            body += " " + maint.getName() + " <" + maint.getEmail() + ">,";
        body += "\n";

        body += notMandatory;
        body += DESCRIPTION_KEY + KEY_SEPARATOR + " " + shortDesc + "\n";
        if (longDesc != null)
            body += longDesc + "\n";

        body += CHANGES_KEY + KEY_SEPARATOR + "\n" + changes + "\n";
        body += CHECKSUMS_SHA1_KEY + KEY_SEPARATOR + "\n";
        if (files != null)
            for (ChangeFile f : files.values())
                body += " " + f.getSha1() + " " + f.getSize() + " " + f.getName() + "\n";

        body += CHECKSUMS_SHA256_KEY + KEY_SEPARATOR + "\n";
        if (files != null)
            for (ChangeFile f : files.values())
                body += " " + f.getSha256() + " " + f.getSize() + " " + f.getName() + "\n";

        body += FILES_KEY + KEY_SEPARATOR + "\n";
        if (files != null)
            for (ChangeFile f : files.values())
                body += " " + f.getMd5() + " " + f.getSection() + " " + f.getPriority() + " " + f.getSize() + " " + f.getName()
                        + "\n";

        return body;
    }
}