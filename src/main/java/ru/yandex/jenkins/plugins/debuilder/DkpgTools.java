package ru.yandex.jenkins.plugins.debuilder;

import hudson.FilePath;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;

import ru.yandex.jenkins.plugins.debuilder.DebChanges.ChangeFile;
import ru.yandex.jenkins.plugins.debuilder.DebUtils.Runner;

public class DkpgTools {

    public static class changesFile {
        public static DebChanges parseChanges(FilePath changesFile, Runner runner) throws IOException {

            DebChanges changes = new DebChanges();

            Pattern changesFormat = Pattern.compile("([\\w-]+:)\\s*(.*)");

            ArrayList<String> lines = new ArrayList<String>(Arrays.asList(changesFile.readToString().split("\n")));

            ListIterator<String> changesLinesIterator = lines.listIterator();

            while (changesLinesIterator.hasNext()) {

                String row = changesLinesIterator.next();

                Matcher matcher = changesFormat.matcher(row);

                if (matcher.matches()) {
                    String key = matcher.group(1);
                    String data = matcher.group(2);

                    // runner.announce("key: '" + key + "' data: '" + data +
                    // "'");
                    if (key.equalsIgnoreCase(DebChanges.FORMAT_KEY + DebChanges.KEY_SEPARATOR)) {
                        changes.setFormat(data);

                    } else if (key.equalsIgnoreCase(DebChanges.DATE_KEY + DebChanges.KEY_SEPARATOR)) {
                        DateFormat df = DebChanges.dateFormat;
                        Date date;
                        try {
                            date = df.parse(data);
                        } catch (ParseException e) {
                            // logger.println("failed parsing date");
                            // ignore;
                            e.printStackTrace();
                            continue;
                        }
                        changes.setDate(date);

                    } else if (key.equalsIgnoreCase(DebChanges.SOURCE_KEY + DebChanges.KEY_SEPARATOR)) {
                        changes.setSource(data);

                    } else if (key.equalsIgnoreCase(DebChanges.BINARY_KEY + DebChanges.KEY_SEPARATOR)) {
                        changes.setBinarys(data.split("\\s"));
                        // for (String s : data.split("\\s"))
                        // logger.println("binary: " + s);

                    } else if (key.equalsIgnoreCase(DebChanges.ARCHITECTURE_KEY + DebChanges.KEY_SEPARATOR)) {
                        changes.setArchitectures(data.split("\\s"));
                        // for (String s : data.split("\\s"))
                        // logger.println("arch: " + s);

                    } else if (key.equalsIgnoreCase(DebChanges.VERSION_KEY + DebChanges.KEY_SEPARATOR)) {
                        // logger.println("Match Version:");
                        Pattern v = Pattern.compile("(?:(\\d+):)?([\\w\\.+~]+)(?:-([\\w+\\.~]+)?)\\s*");
                        Matcher mVersion = v.matcher(data);

                        DebChanges.Version version = new DebChanges.Version();

                        if (mVersion.matches()) {
                            // runner.announce("found " + mVersion.groupCount()
                            // + "groups");

                            // for (int ii = 1; ii <= mVersion.groupCount();
                            // ii++) {
                            // logger.println("group " + String.valueOf(ii) +
                            // ": " + mVersion.group(ii));
                            // }

                            if (mVersion.group(1) != null)
                                version.setEpoch(Long.decode(mVersion.group(1)));
                            if (mVersion.group(2) != null)
                                version.setUpstream(mVersion.group(2));
                            if (mVersion.group(3) != null)
                                version.setRevision(mVersion.group(3));

                        } else {
                            // logger.println("fail matching version fields");
                        }
                        changes.setVersion(version);

                    } else if (key.equalsIgnoreCase(DebChanges.DISTRIBUTION_KEY + DebChanges.KEY_SEPARATOR)) {
                        changes.setDistributions(data.split("\\s"));
                        // for (String s : data.split("\\s"))
                        // logger.println("distr: " + s);

                    } else if (key.equalsIgnoreCase(DebChanges.URGENCY_KEY + DebChanges.KEY_SEPARATOR)) {
                        changes.setUrgency(data);

                    } else if (key.equalsIgnoreCase(DebChanges.MAINTAINER_KEY + DebChanges.KEY_SEPARATOR)) {

                        Pattern m = Pattern.compile("([^<]*)\\s*<(.*)");

                        ArrayList<DebChanges.Contact> maintainers = new ArrayList<DebChanges.Contact>();

                        for (String maintainer : data.split(">\\s*")) {
                            // logger.println("maint: " + maintainer);
                            DebChanges.Contact contact = new DebChanges.Contact();
                            Matcher mMaintainer = m.matcher(maintainer);
                            if (mMaintainer.matches()) {
                                contact.setName(mMaintainer.group(1));
                                contact.setEmail(mMaintainer.group(2));
                                // logger.println("name: '" +
                                // mMaintainer.group(1) + "' email: '" +
                                // mMaintainer.group(2) + "'");
                            } else {
                                // logger.println("fail matching maintainers");
                            }
                            maintainers.add(contact);
                        }

                        changes.setMaintainers(maintainers);

                    } else if (key.equalsIgnoreCase(DebChanges.CHANGED_BY_KEY + DebChanges.KEY_SEPARATOR)) {

                        Pattern m = Pattern.compile("([^<]*)\\s*<([^>]*)>");

                        DebChanges.Contact changedBy = new DebChanges.Contact();
                        Matcher mChangedBy = m.matcher(data);
                        if (mChangedBy.matches()) {
                            changedBy.setName(mChangedBy.group(1));
                            changedBy.setEmail(mChangedBy.group(2));
                        }

                        changes.setChangedBy(changedBy);

                    } else if (key.equalsIgnoreCase(DebChanges.DESCRIPTION_KEY + DebChanges.KEY_SEPARATOR)) {
                        changes.setShortDesc(data);
                        String longDesc = getTextBlock(changesLinesIterator);
                        changes.setLongDesc(longDesc);
                        // logger.println("long desc: '" + longDesc + "'");

                    } else if (key.equalsIgnoreCase(DebChanges.CLOSES_KEY + DebChanges.KEY_SEPARATOR)) {
                        changes.setCloses(data.split("\\s*"));
                        // for (String s : data.split("\\s"))
                        // logger.println("closes: " + s);

                    } else if (key.equalsIgnoreCase(DebChanges.CHANGES_KEY + DebChanges.KEY_SEPARATOR)) {
                        String changesText = getTextBlock(changesLinesIterator);
                        changes.setChanges(changesText);
                        // logger.println("changes: " + changesText);

                    } else if (key.equalsIgnoreCase(DebChanges.CHECKSUMS_SHA1_KEY + DebChanges.KEY_SEPARATOR)
                            || key.equalsIgnoreCase(DebChanges.CHECKSUMS_SHA256_KEY + DebChanges.KEY_SEPARATOR)
                            || key.equalsIgnoreCase(DebChanges.FILES_KEY + DebChanges.KEY_SEPARATOR)) {
                        HashMap<String, DebChanges.ChangeFile> files;

                        if (changes.getFiles() != null)
                            files = (HashMap<String, ChangeFile>) changes.getFiles();
                        else
                            files = new HashMap<String, ChangeFile>();

                        String filesText = getTextBlock(changesLinesIterator);
                        // logger.println("files: " + filesText);

                        for (String fileRow : filesText.split("\\n")) {
                            // logger.println("file line: " + fileRow);

                            DebChanges.ChangeFile file;

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
                                if (files.containsKey(mChangeFile.group(mChangeFile.groupCount())))
                                    file = files.get(mChangeFile.group(mChangeFile.groupCount()));
                                else
                                    file = new DebChanges.ChangeFile();

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

                                files.put(file.getName(), file);
                            } else {
                                // runner.announce("file fields not matched");
                            }
                        }
                        changes.setFiles(files);

                    } else {
                        // runner.announce("Match but no key found");
                    }
                } else {
                    // runner.announce("line not matched: " + row);
                }
            }
            return changes;
        }

        public static String getTextBlock(ListIterator<String> fileIterator) {
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

        // TODO the validate method should validate fields
        public static boolean validate(DebChanges changes, FilePath rootDir, Runner runner) throws IOException,
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

            for (DebChanges.ChangeFile changeFile : changes.getFiles().values()) {

                if (changeFile.getName() == null) {
                    runner.announce("{0} File name is empty.", msgPrefix);
                    return false;
                }

                FilePath file = rootDir.child(changeFile.getName());
                if (!validateSize(changeFile.getSize(), file)) {
                    runner.announce("{0} Size of {1} not match ({2})", msgPrefix, file.getRemote(), file.length());
                    return false;
                } else if (!validateMd5(changeFile.getMd5(), file)) {
                    runner.announce("{0} Md5 of {1} not match ({2})", msgPrefix, file.getRemote(), DigestUtils.md5(file.read()));
                    return false;
                } else if (!validateSha1(changeFile.getSha1(), file)) {
                    runner.announce("{0} Sha1 of {1} not match ({2})", msgPrefix, file.getRemote(),
                            DigestUtils.sha1Hex(file.read()));
                    return false;
                } else if (!validateSha256(changeFile.getSha256(), file)) {
                    runner.announce("{0} Sha256 of {1} not match ({2}) ", msgPrefix, file.getRemote(),
                            DigestUtils.sha256Hex(file.read()));
                    return false;
                }

            }
            return true;
        }
    }
}