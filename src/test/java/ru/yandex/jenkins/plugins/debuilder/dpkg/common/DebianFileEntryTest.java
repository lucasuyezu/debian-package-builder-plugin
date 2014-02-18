/**
 * 
 */
package ru.yandex.jenkins.plugins.debuilder.dpkg.common;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

/**
 * @author caiocezar
 * 
 */
public class DebianFileEntryTest {
    @Test
    public void startWithNull() {
        DebianFileEntry entry = new DebianFileEntry();

        assertNull("Name should be null", entry.getName());
        assertNull("Size should be null", entry.getSize());
        assertNull("Md5 should be null", entry.getMd5());
        assertNull("Sha1 should be null", entry.getSha1());
        assertNull("Sha256 should be null", entry.getSha256());
        assertNull("Priority should be null", entry.getPriority());
        assertNull("Section should be null", entry.getSection());
    }

    @Test
    public void clear() {
        DebianFileEntry entry = new DebianFileEntry();

        entry.setName("name");
        entry.setSize((long) 0);
        entry.setMd5("123456789a123456789a123456789a12");
        entry.setSha1("123456789a123456789a123456789a123456789a");
        entry.setSha256("123456789a123456789a123456789a123456789a123456789a123456789a1234");
        entry.setSection("section");
        entry.setPriority("extra");

        entry.clean();

        assertNull("Name should be null", entry.getName());
        assertNull("Size should be null", entry.getSize());
        assertNull("Md5 should be null", entry.getMd5());
        assertNull("Sha1 should be null", entry.getSha1());
        assertNull("Sha256 should be null", entry.getSha256());
        assertNull("Priority should be null", entry.getPriority());
        assertNull("Section should be null", entry.getSection());
    }

    @Test
    public void settingValidInputs() {
        DebianFileEntry entry = new DebianFileEntry();

        // Testing valid names
        List<String> names = Arrays.asList("valid:'`()$#%^!@.,;<>|\\-+*");

        for (String name : names) {
            assertTrue("Fail setting a valid name", entry.setName(name));
            assertTrue("Name is setted but with a incorrect value", name.equals(entry.getName()));
        }

        // Testing valid string sizes

        List<String> sizesString = Arrays.asList("0", "100");

        for (String size : sizesString) {
            assertTrue("Fail setting a valid size", entry.setSize(size));
            assertTrue("Size is setted but with a incorrect value", size.equals(entry.getSize().toString()));
        }

        // Testing valid Long sizes

        List<Long> sizesLong = Arrays.asList((long) 0, (long) 100);

        for (long size : sizesLong) {
            assertTrue("Fail setting a valid size", entry.setSize(size));
            assertTrue("Size is setted but with a incorrect value", size == entry.getSize());
            assertNotSame("The object from 'getSize' should not be the same", size, entry.getSize());
            size += 1;
            assertTrue("The size object inside the class should not be the same", size != entry.getSize());
        }

        // Testing valid md5s
        List<String> md5s = Arrays.asList("c6f698f19f2a2aa07dbb9bbda90a2754");

        for (String md5 : md5s) {
            assertTrue("Fail setting a valid md5", entry.setMd5(md5));
            assertTrue("Md5 is setted but with a incorrect value", md5.equals(entry.getMd5()));

        }

        // Testing valid sha1s
        List<String> sha1s = Arrays.asList("1f418afaa01464e63cc1ee8a66a05f0848bd155c");

        for (String sha1 : sha1s) {
            assertTrue("Fail setting a valid sha1", entry.setSha1(sha1));
            assertTrue("Sha1 is setted but with a incorrect value", sha1.equals(entry.getSha1()));
        }

        // Testing valid sha256s
        List<String> sha256s = Arrays.asList("ac9d57254f7e835bed299926fd51bf6f534597cc3fcc52db01c4bffedae81272");

        for (String sha256 : sha256s) {
            assertTrue("Fail setting a valid sha256", entry.setSha256(sha256));
            assertTrue("Sha256 is setted but with a incorrect value", sha256.equals(entry.getSha256()));
        }

        // Testing valid sections
        List<String> sections = Arrays.asList("section");

        for (String section : sections) {
            assertTrue("Fail setting a valid section", entry.setSection(section));
            assertTrue("Section is setted but with a incorrect value", section.equals(entry.getSection()));
        }

        // Testing valid priorities
        List<String> priorities = Arrays.asList("extra");

        for (String priority : priorities) {
            assertTrue("Fail setting a valid priority", entry.setPriority(priority));
            assertTrue("Priority is setted but with a incorrect value", priority.equals(entry.getPriority()));
        }
    }

    @Test
    public void settingInvalidInputs() {
        DebianFileEntry entry = new DebianFileEntry();

        List<String> md5s = Arrays.asList("c 6f698f19f2a2aa07dbb9bbda90a2754", "}^{", "  c6f698f19f2a2aa07dbb9bbda90a2754  ");
        List<String> sha1s = Arrays.asList("1 f418afaa01464e63cc1ee8a66a05f0848bd155c", "akls87as8d7ff67&&",
                "  1f418afaa01464e63cc1ee8a66a05f0848bd155c  ");
        List<String> sha256s = Arrays.asList("a c9d57254f7e835bed299926fd51bf6f534597cc3fcc52db01c4bffedae81272",
                "sakdjfhadjklsfh88*", "  ac9d57254f7e835bed299926fd51bf6f534597cc3fcc52db01c4bffedae81272 ");

        for (String md5 : md5s)
            assertFalse("Fail rejecting a invalid md5: " + md5, entry.setMd5(md5));
        for (String sha1 : sha1s)
            assertFalse("Fail rejecting a invalid sha1: " + sha1, entry.setSha1(sha1));
        for (String sha256 : sha256s)
            assertFalse("Fail rejecting a invalid sha56: " + sha256, entry.setSha256(sha256));
    }

    @Test
    public void parsingValidLines() {
        DebianFileEntry entry = new DebianFileEntry();
        Map<String, List<String>> lines = new HashMap<String, List<String>>();

        List<String> md5Dsc = Arrays.asList(" c6f698f19f2a2aa07dbb9bbda90a2754 571925 example_1.2.orig.tar.gz",
                "  c6f698f19f2a2aa07dbb9bbda90a2754   571925   example_1.2.orig.tar.gz  ");
        lines.put("md5.dsc", md5Dsc);

        List<String> md5Changes = Arrays.asList(" 4c31ab7bfc40d3cf49d7811987390357 1428 text extra example_1.2-1.dsc",
                "  4c31ab7bfc40d3cf49d7811987390357   1428   text   extra   example_1.2-1.dsc  ");
        lines.put("md5.changes", md5Changes);

        List<String> sha1Changes = Arrays.asList(" 1f418afaa01464e63cc1ee8a66a05f0848bd155c 1276 example_1.0-1.dsc",
                "  1f418afaa01464e63cc1ee8a66a05f0848bd155c    1276    example_1.0-1.dsc  ");
        lines.put("sha1.changes", sha1Changes);

        List<String> sha256Changes = Arrays.asList(
                " ac9d57254f7e835bed299926fd51bf6f534597cc3fcc52db01c4bffedae81272 1276 example_1.0-1.dsc",
                "  ac9d57254f7e835bed299926fd51bf6f534597cc3fcc52db01c4bffedae81272    1276    example_1.0-1.dsc  ");
        lines.put("sha256.changes", sha256Changes);

        for (String type : lines.keySet()) {
            for (String line : lines.get(type)) {
                assertTrue("Should parse the line type '" + type + "': " + line, entry.parseLine(line));

                Pattern pattern = Pattern.compile("^\\s+([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)(?:\\s+([^\\s]+)\\s+([^\\s]+))?\\s*$");
                Matcher matcher = pattern.matcher(line);

                assertTrue("The line'" + line + "' should match this regex", matcher.matches());

                String g1 = matcher.group(1);
                String g2 = matcher.group(2);
                String g3 = matcher.group(3);
                String g4 = matcher.group(4);
                String g5 = matcher.group(5);

                if (type.equals("md5.changes")) {

                    assertTrue("The md5 should matches '" + g1 + "' but his value is '" + entry.getMd5() + "'",
                            g1.equals(entry.getMd5()));
                    assertTrue("The size should matches '" + g2 + "' but his value is '" + entry.getSize() + "'",
                            g2.equals(entry.getSize().toString()));
                    assertTrue("The section should matches '" + g3 + "' but his value is '" + entry.getSection() + "'",
                            g3.equals(entry.getSection()));
                    assertTrue("The priotity should matches '" + g4 + "' but his value is '" + entry.getPriority() + "'",
                            g4.equals(entry.getPriority()));
                    assertTrue("The name should matches '" + g5 + "' but his value is '" + entry.getName() + "'",
                            g5.equals(entry.getName()));

                } else {

                    if (type.equals("md5.dsc"))
                        assertTrue("The md5 should matches '" + g1 + "' but his value is '" + entry.getMd5() + "'",
                                g1.equals(entry.getMd5()));
                    else if (type.equals("sha1.changes"))
                        assertTrue("The sha1 should matches '" + g1 + "' but his value is '" + entry.getSha1() + "'",
                                g1.equals(entry.getSha1()));
                    else if (type.equals("sha256.changes"))
                        assertTrue("The sha256 should matches '" + g1 + "' but his value is '" + entry.getSha256() + "'",
                                g1.equals(entry.getSha256()));
                    else
                        fail("The type of the line is not implemented");

                    assertTrue("The size should matches '" + g2 + "' but his value is '" + entry.getSize() + "'",
                            g2.equals(entry.getSize().toString()));
                    assertTrue("The name should matches '" + g3 + "' but his value is '" + entry.getName() + "'",
                            g3.equals(entry.getName()));

                }

            }
        }
    }

    @Test
    public void mergeByName() {

        // The entry to compare
        DebianFileEntry entry = new DebianFileEntry();

        String name = "name";
        long size = (long) 0;
        String md5 = "123456789a123456789a123456789a12";
        String sha1 = "123456789a123456789a123456789a123456789a";
        String sha256 = "123456789a123456789a123456789a123456789a123456789a123456789a1234";
        String section = "section";
        String priority = "extra";

        entry.setName(name);
        entry.setSize(size);
        entry.setMd5(md5);
        entry.setSha1(sha1);
        entry.setSha256(sha256);
        entry.setSection(section);
        entry.setPriority(priority);

        // The entry with same values and the same name
        DebianFileEntry entryEqual = new DebianFileEntry();

        long newSize = (long) 55;
        String newPriority = "optional";

        entryEqual.setName(name);
        entryEqual.setSize(newSize);
        entryEqual.setPriority(newPriority);

        // The merge
        assertTrue("This merge should work", entry.mergeByName(entryEqual));

        // This should be changed
        assertTrue("The size should be changed from " + size + " to " + newSize, newSize == entry.getSize().longValue());
        assertTrue("The priority should be changed from " + priority + " to " + newPriority,
                newPriority.equals(entry.getPriority()));

        // This should not be changed
        assertTrue("The name should not be changed", name.equals(entry.getName()));
        assertTrue("The md5 should not be changed", md5.equals(entry.getMd5()));
        assertTrue("The sha1 should not be changed", sha1.equals(entry.getSha1()));
        assertTrue("The sha256 should not be changed", sha256.equals(entry.getSha256()));
        assertTrue("The section should not be changed", section.equals(entry.getSection()));

        // The entry with other name
        DebianFileEntry entryNotEqual = new DebianFileEntry();

        String otherName = "other_name";
        long otherSize = (long) 66;
        String otherPriority = "optinal";

        assertTrue("New size and other size should differ", newSize != otherSize);
        assertTrue("New priority and other priority should differ", !newPriority.equals(otherPriority));

        entryNotEqual.setName(otherName);
        entryNotEqual.setSize(otherSize);
        entryNotEqual.setSection(otherPriority);

        // The merge
        assertFalse("This merge should work", entry.mergeByName(entryNotEqual));

        // This should not be changed
        assertTrue("The name should not be changed", name.equals(entry.getName()));
        assertTrue("The md5 should not be changed", md5.equals(entry.getMd5()));
        assertTrue("The sha1 should not be changed", sha1.equals(entry.getSha1()));
        assertTrue("The sha256 should not be changed", sha256.equals(entry.getSha256()));
        assertTrue("The section should not be changed", section.equals(entry.getSection()));
        assertTrue("The size should not be changed", newSize == entry.getSize().longValue());
        assertTrue("The priority should  not be changed", newPriority.equals(entry.getPriority()));

    }
}
