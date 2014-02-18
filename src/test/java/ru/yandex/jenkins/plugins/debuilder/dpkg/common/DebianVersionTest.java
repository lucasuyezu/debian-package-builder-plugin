/**
 * 
 */
package ru.yandex.jenkins.plugins.debuilder.dpkg.common;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * @author caiocezar
 * 
 */
public class DebianVersionTest {

    @Test
    public void settingValidInputs() {
        DebianVersion v = new DebianVersion();

        String epoch = "10";
        String upstream = "1.0:0+build-1~alpha";
        String revision = "wheezy.2~alpha";

        assertTrue("Fail setting a valid epoch", v.setEpoch(epoch));
        assertTrue("Fail setting a valid upstream", v.setUpstream(upstream));
        assertTrue("Fail setting a valid revision", v.setRevision(revision));

        assertTrue("Epoch is set but the value is wrong", v.getEpoch().longValue() == Long.parseLong(epoch));
        assertTrue("Upstream is set but the value is upstream", upstream.equals(v.getUpstream()));
        assertTrue("Revision is set but the value is revision", revision.equals(v.getRevision()));

        // Parsing a full version
        String fullVersion = epoch + ":" + upstream + "-" + revision;

        assertTrue("Fail setting a valid full version", v.parseVersion(fullVersion));
        assertTrue("Epoch is set but the value is wrong", v.getEpoch().longValue() == Long.parseLong(epoch));
        assertTrue("Upstream is set but the value is upstream", upstream.equals(v.getUpstream()));
        assertTrue("Revision is set but the value is revision", revision.equals(v.getRevision()));

        // Parsing a revision
        String revisionVersion = upstream.replace(':', '.') + "-" + revision;

        assertTrue("Fail setting a valid version with revision", v.parseVersion(revisionVersion));
        assertTrue("Epoch is set but the value is wrong", v.getEpoch() == null);
        assertTrue("Upstream is set but the value is upstream", upstream.replace(':', '.').equals(v.getUpstream()));
        assertTrue("Revision is set but the value is revision", revision.equals(v.getRevision()));

        // Parsing an epoch
        String epochVersion = epoch + ":" + upstream.replace('-', '.');

        assertTrue("Fail setting a valid version with epoch", v.parseVersion(epochVersion));
        assertTrue("Epoch is set but the value is wrong", v.getEpoch().longValue() == Long.parseLong(epoch));
        assertTrue("Upstream is set but the value is upstream", upstream.replace('-', '.').equals(v.getUpstream()));
        assertTrue("Revision is set but the value is revision", v.getRevision() == null);

        // Parsing an upstream
        String simpleVersion = upstream.replace('-', '.').replace(':', '.');

        assertTrue("Fail setting a valid simple version", v.parseVersion(simpleVersion));
        assertTrue("Epoch is set but the value is wrong", v.getEpoch() == null);
        assertTrue("Upstream is set but the value is upstream",
                upstream.replace(':', '.').replace('-', '.').equals(v.getUpstream()));
        assertTrue("Revision is set but the value is revision", v.getRevision() == null);

    }

    @Test
    public void comparingVersions() {
        DebianVersion v = new DebianVersion();

        // Testing all combinations of version types, using some characters
        // combinations. The first field should be bigger
        @SuppressWarnings("serial")
        Map<String, String> versions = new HashMap<String, String>() {

            {
                // upstream x upstream
                put("2", "1");
                // upstream x revision
                put("2.2", "1.99-10");
                // upstream x epoch
                put("1:1.99", "2.2a");
                // upstream x full version
                put("1:1.99-g-10", "2.2a+build.1~alpha");
                // upstream x null
                put("2ab-~l", null);

                // revision x revision
                put("2-2b", "2-2aa");
                // revision x epoch
                put("1:1", "2-50~a");
                // revision x full version
                put("1:1-1", "2-10.qw");
                // revision x null
                put("1.-sd~-3", null);

                // epoch x epoch
                put("3:3", "2:283");
                // epoch x full version
                put("3:3", "2:2.a+build~2-df-wheezy.3g");
                // epoch x null
                put("9:0.0a~g", null);

                // full version x full version
                put("2:3.28+build.1-1+b~beta", "2:3.28+build.1-1+b~alpha");
                // full version x null
                put("12:891-f~.3", null);

                // null x null;
                put(null, null);
            }

        };

        for (Map.Entry<String, String> entry : versions.entrySet()) {
            String v1 = entry.getKey();
            String v2 = entry.getValue();

            // null and valid versions are allowed
            if ((v1 == null || v.parseVersion(v1)) && (v2 == null || v.parseVersion(v2))) {
                if (v1 != null && v2 != null) {
                    assertTrue("The version '" + v1 + "' should be bigger that '" + v2 + "'",
                            DebianVersion.versionTextCompare(v1, v2).intValue() > 0);

                    assertTrue("The version '" + v2 + "' should be lower that '" + v1 + "'",
                            DebianVersion.versionTextCompare(v2, v1).intValue() < 0);

                    assertTrue("The version '" + v1 + "' should be equal to '" + v1 + "'",
                            DebianVersion.versionTextCompare(v1, v1).intValue() == 0);
                }

                assertTrue("The version '" + v2 + "' should be equal to '" + v2 + "'", DebianVersion.versionTextCompare(v2, v2)
                        .intValue() == 0);

            } else {
                fail("Some of this versions '" + v1 + "', '" + v2 + "' are invalid");
            }
        }
    }
}
