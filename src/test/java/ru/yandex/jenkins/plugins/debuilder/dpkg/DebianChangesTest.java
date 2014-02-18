package ru.yandex.jenkins.plugins.debuilder.dpkg;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class DebianChangesTest {

    @Test
    public void parsingValidChanges() {
        List<String> changesFiles = new ArrayList<String>();
        //@formatter:off
        changesFiles.add(
                "Format: 1.8\n" +
                "Date: Thu, 06 Feb 2014 23:04:27 -0200\n" +
                "Source: linuxstatus\n"+ 
                "Binary: linuxstatus\n" +
                "Architecture: source amd64\n" +
                "Version: 2+build.11\n" +
                "Distribution: testing stable\n"+
                "Urgency: low\n" +
                "Maintainer: root <root@vagrant-debian-wheezy.vagrantup.com>\n" +
                "Changed-By: Jenkins <teste@teste.com>\n" +
                "Description: \n" +
                " linuxstatus - <insert up to 60 chars description>\n" +
                "Changes: \n" +
                " linuxstatus (2+build.11) testing; urgency=low\n" +
                " .\n" +
                "   * Build #177. Started by user anonymous.\n" +
                "Checksums-Sha1: \n" +
                " 5caea8d66bac66995ff0606392cb7843a82360a8 1138 linuxstatus_2+build.11.dsc\n" +
                " 94f03bc4a080e75057676089479451eada476a7a 29342 linuxstatus_2+build.11.tar.gz\n" +
                " 805f43add5dd41b0c9875f0225fab210f55aa17c 2450 linuxstatus_2+build.11_amd64.deb\n" +
                "Checksums-Sha256: \n" +
                " 75de3ece92a8c2a55f76a9225e41b7bdb8a02f58529b14f507374536f0540b65 1138 linuxstatus_2+build.11.dsc\n" +
                " 13ae2e1dcff3d7dcfeb1d76b6168d5158583b5f19e3298f198a79aa6527970ac 29342 linuxstatus_2+build.11.tar.gz\n" +
                " 649e0841340f8ba7c6585e78b58c6f6c8b8084518dadd496cfb6de3750b09950 2450 linuxstatus_2+build.11_amd64.deb\n" + 
                "Files: \n" +
                " 795f7e38cc80faf568fbd55ab56831f6 1138 unknown extra linuxstatus_2+build.11.dsc\n" +
                " a17e3415f7d4efd105061191f98397d1 29342 unknown extra linuxstatus_2+build.11.tar.gz\n" +
                " 1377d1679f16c1d5ef39047cc7b821a3 2450 unknown extra linuxstatus_2+build.11_amd64.deb\n"
                );
        
        changesFiles.add(
                "-----BEGIN PGP SIGNED MESSAGE-----\n" +
                "Hash: SHA1\n" +
                "\n"+
                "Format: 1.8\n" +
                "Date: Wed, 12 Feb 2014 20:43:17 -0200\n" +
                "Source: linuxstatus\n" +
                "Binary: linuxstatus\n" +
                "Architecture: source amd64\n" +
                "Version: 2+build.12\n" +
                "Distribution: testing stable\n" +
                "Urgency: low\n" +
                "Maintainer: root <root@vagrant-debian-wheezy.vagrantup.com>\n" +
                "Changed-By: Jenkins <teste@teste.com>\n" +
                "Description: \n" +
                " linuxstatus - <insert up to 60 chars description>\n" +
                "Changes: \n" +
                " linuxstatus (2+build.12) testing; urgency=low\n" +
                " .\n" +
                "   * Build #182. Started by user anonymous.\n" +
                "Checksums-Sha1: \n" +
                " aa1c1a18008b4742450f470ab70b9e08bf0b630d 1138 linuxstatus_2+build.12.dsc\n" +
                " 079539f1bac9a6393800b9cf10a0d177c7c3e854 33333 linuxstatus_2+build.12.tar.gz\n" +
                " a895a6e57dbf8f4c0c0eeddad6ae788e4fbd121b 2474 linuxstatus_2+build.12_amd64.deb\n" +
                "Checksums-Sha256: \n" +
                " 31e3a09effd857edf83058613a422fc88fa0c6824d800e81bb8a7adf4f89b6fa 1138 linuxstatus_2+build.12.dsc\n" +
                " 3f13af9932f03092659788f3b42642a6108dd7e334f06b681567ad028a3d3708 33333 linuxstatus_2+build.12.tar.gz\n" +
                " ec297a4fc66b40fcbe3337ba5b598f520b4a81386eb7cab6a68be3e3e3c8a8e8 2474 linuxstatus_2+build.12_amd64.deb\n" +
                "Files: \n" +
                " 8f7fe7eef42ab81c00c8bbe7a326fd22 1138 unknown extra linuxstatus_2+build.12.dsc\n" +
                " 4cda1a189d406b1e0f85711c4e42c6e1 33333 unknown extra linuxstatus_2+build.12.tar.gz\n" +
                " a02d57fa9f80ffd026ea2fb980b8b49f 2474 unknown extra linuxstatus_2+build.12_amd64.deb\n" +
                "\n" +
                "-----BEGIN PGP SIGNATURE-----\n" +
                "Version: GnuPG v1.4.12 (GNU/Linux)\n" +
                "\n" +
                "iQEcBAEBAgAGBQJS+/kMAAoJEH6XbpNx64nRTKwH/3Bv8sd1C+s0BSQPeAZsQkYX\n" +
                "KjUHFueyeNtJrdAq/VjMBDB/NnlzFiom8J9WK6MYJ0BPuIkkCpdAaDbAd/ePjMAg\n" +
                "W0tJ6yaSFGRzHgqDWmQwL3hUVGfUpMwqj+tONlCPm4UAg785lzR1ttpx+UfXbbtG\n" +
                "VuTP4pcQYddFHCCiuLWom91Grj61tlPICCmGQK4ENBUPe/s4EFa3BDGsJ6XqI+SX\n" +
                "3HOFkz7Mpmj6t+U4BDDtd3tQONDUEoAX7cBuloJMR9mr0BknnJdI4JHKNj4OtFSg\n" +
                "53nq3ToqbW0vLG1iaF01UbPizuV972IhYK0j2xKuQOxGnVA89qda0RAyPBAf8HM=\n" +
                "=iBCa\n" +
                "-----END PGP SIGNATURE-----\n"
                );
        
        changesFiles.add(
                "Format: 1.8\n" +
                "Date: Mon, 04 Feb 2013 14:49:54 -0300\n" +
                "Source: iaas-api\n" +
                "Binary: iaas-api\n" +
                "Architecture: source all\n" +
                "Version: 1.0\n" +
                "Distribution: testing development\n" +
                "Urgency: low\n" +
                "Maintainer: Bricklayer Builder <bricklayer@locaweb.com.br>\n" +
                "Changed-By: Bricklayer Builder <bricklayer@locaweb.com.br>\n" +
                "Description: \n" +
                " iaas-api   - IaaS API (Locaweb)\n" +
                "Changes: \n" +
                " iaas-api (1.0) testing; urgency=low\n" +
                " .\n" +
                "     * initial package\n" +
                "Checksums-Sha1: \n" +
                " d120b291d152d41c1514e9d3055379297d8e92c6 602 iaas-api_1.0.dsc\n" +
                " b5452d8b59c750c2e3bd3baf98c83bd396696196 151808494 iaas-api_1.0.tar.gz\n" +
                " 9db54cfcbde8092c3f627568247580806d50c794 69994388 iaas-api_1.0_all.deb\n" +
                "Checksums-Sha256: \n" +
                " 055c6e9bdffc65f5e65c147fea0c3510bc637b5c856149cc71ee3fadf9ffb524 602 iaas-api_1.0.dsc\n" +
                " 76f0d4c200e74a88393897c7b92a2eab8fea1bbb71c9842b9aeefd3943c4a869 151808494 iaas-api_1.0.tar.gz\n" +
                " 93ab31a1eab9f049a487e7e27a676824e2ca36e6fa743b93cc5a70c34f1ea38f 69994388 iaas-api_1.0_all.deb\n" +
                "Files: \n" +
                " 792796cc033e7a65e0a731aa431f06b1 602 external_applications extra iaas-api_1.0.dsc\n" +
                " d71d73f04457badbb5af6fc252f9bfaf 151808494 external_applications extra iaas-api_1.0.tar.gz\n" +
                " b8ba4cbc3afaae89f4cb7f95e29ed102 69994388 external_applications extra iaas-api_1.0_all.deb\n"
                );
        //@formatter:on

        DebianChanges changes = new DebianChanges();

        for (String changesFile : changesFiles) {

            List<Pair<String, String>> rejected = changes.parse(changesFile);

            if (rejected.size() != 0) {

                String lines = "";
                for (Pair<String, String> entry : rejected)
                    lines += entry.getLeft() + ": " + entry.getRight() + "\n";

                fail("There is same erros:\n" + lines);
            }
        }
    }
}
