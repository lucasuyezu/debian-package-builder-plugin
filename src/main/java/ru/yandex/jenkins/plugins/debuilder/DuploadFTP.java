package ru.yandex.jenkins.plugins.debuilder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPHTTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.io.CopyStreamException;
import org.apache.commons.net.util.TrustManagerUtils;

import ru.yandex.jenkins.plugins.debuilder.DebUtils.Runner;

/**
 * This class upload files using the ftp method with password, replacing the
 * dupload tool that can't use an stored password to authenticate.
 * 
 * @author caiocezar
 * 
 */
public class DuploadFTP {
    private String server = "";
    private int port = 0;

    private FTPClient ftp;

    private Boolean binaryTransfer = true;
    private Boolean passive = true;
    private Boolean useEpsvWithIPv4 = false;
    private Long keepAliveTimeout = -1L;
    private Integer controlKeepAliveReplyTimeout = -1;
    private String protocol = null; // SSL protocol
    private Boolean implicit = null;
    private String trustmgr = null;
    private String proxyHost = null;
    private int proxyPort = 80;
    private String proxyUser = null;
    private String proxyPassword = null;
    private String username = "anonymous";
    private String password = "anonymous";

    /**
     * Initialize a FTPClient or a FTPHTTPClient if you set a proxy
     * 
     * @param server
     *            The server url in the format ftp.server.com or with the port
     *            ftp.server.com:1234
     */
    public DuploadFTP(String server) {
        setServer(server);
    }

    /**
     * Initialize a FTPSClient and set the protocol
     * 
     * @param server
     *            The server url in the format ftp.server.com or with the port
     *            ftp.server.com:1234
     * @param isImplicit
     *            Set if the FTPS is implicit or not
     */
    public DuploadFTP(String server, Boolean isImplicit) {
        setServer(server);
        this.implicit = new Boolean(isImplicit);
    }

    /**
     * Initialize a FTPSClient and set the protocol
     * 
     * @param server
     *            The server url in the format ftp.server.com or with the port
     *            ftp.server.com:1234
     * @param isImplicit
     *            Set if the FTPS is implicit or not
     * @param protocol
     *            Set the SSL protocol to use on the FTPS connection
     */
    public DuploadFTP(String server, Boolean isImplicit, String protocol) {
        setServer(server);
        this.implicit = new Boolean(isImplicit);
        this.protocol = new String(protocol);
    }

    public static class ReturnStatus {
        private Boolean status;
        private String message;

        public Boolean isOK() {
            return status;
        }

        public void setStatus(Boolean status) {
            this.status = new Boolean(status);
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = new String(message);
        }

        public void addMessageline(String line) {
            if (this.message == "")
                this.message = new String(line);
            this.message += "\n" + line;
        }
    }

    public String getServerUrl() {
        if (port > 0)
            return server + ":" + String.valueOf(port);
        return server;
    }

    private void setServer(String server) {
        String parts[] = server.split(":");
        if (parts.length == 2) {
            this.server = parts[0];
            this.port = Integer.parseInt(parts[1]);
        }
        this.server = new String(server);
        this.port = 0;
    }

    public boolean isBinaryTransfer() {
        return binaryTransfer;
    }

    public void setBinaryTransfer(Boolean binaryTransfer) {
        this.binaryTransfer = new Boolean(binaryTransfer);
    }

    public boolean isPassive() {
        return passive;
    }

    public void setPassive(Boolean passive) {
        this.passive = new Boolean(passive);
    }

    public boolean isUseEpsvWithIPv4() {
        return useEpsvWithIPv4;
    }

    public void setUseEpsvWithIPv4(Boolean useEpsvWithIPv4) {
        this.useEpsvWithIPv4 = new Boolean(useEpsvWithIPv4);
    }

    public long getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    public void setKeepAliveTimeout(Long keepAliveTimeout) {
        this.keepAliveTimeout = new Long(keepAliveTimeout);
    }

    public int getControlKeepAliveReplyTimeout() {
        return controlKeepAliveReplyTimeout;
    }

    public void setControlKeepAliveReplyTimeout(Integer controlKeepAliveReplyTimeout) {
        this.controlKeepAliveReplyTimeout = new Integer(controlKeepAliveReplyTimeout);
    }

    public String getProtocol() {
        return protocol;
    }

    public String getTrustmgr() {
        return trustmgr;
    }

    public void setTrustmgr(String trustmgr) {
        this.trustmgr = new String(trustmgr);
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = new String(proxyHost);
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = new Integer(proxyPort);
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public void setProxyUser(String proxyUser) {
        this.proxyUser = new String(proxyUser);
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = new String(proxyPassword);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = new String(username);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = new String(password);
    }

    private Boolean connect(Runner runner) {

        Boolean rc = false;
        rc = false;

        if (ftp != null) {
            if (ftp.isConnected()) {
                try {
                    // check that control connection is working OK
                    ftp.noop();

                    runner.announce("Connected");
                    rc = true;
                    return rc;

                } catch (FTPConnectionClosedException e) {
                    runner.announce("The server closed an existing connection , reconnecting...");
                    disconnect();
                } catch (IOException e) {
                    runner.announce("Could not connect to server.");
                    disconnect();
                }
            }
        }

        if (implicit == null) {
            if (proxyHost != null) {
                runner.announce("Using HTTP proxy server: " + proxyHost);
                ftp = new FTPHTTPClient(proxyHost, proxyPort, proxyUser, proxyPassword);
            } else {
                ftp = new FTPClient();
            }
        } else {
            FTPSClient ftps;
            if (protocol == null) {
                ftps = new FTPSClient(implicit);
            } else {
                ftps = new FTPSClient(protocol, implicit);
            }
            ftp = ftps;
            if ("all".equals(trustmgr)) {
                ftps.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
            } else if ("valid".equals(trustmgr)) {
                ftps.setTrustManager(TrustManagerUtils.getValidateServerCertificateTrustManager());
            } else if ("none".equals(trustmgr)) {
                ftps.setTrustManager(null);
            }
        }

        if (keepAliveTimeout >= 0) {
            ftp.setControlKeepAliveTimeout(keepAliveTimeout);
        }
        if (controlKeepAliveReplyTimeout >= 0) {
            ftp.setControlKeepAliveReplyTimeout(controlKeepAliveReplyTimeout);
        }
        // For future use
        // ftp.setListHiddenFiles(hidden);

        // suppress login details
        ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out), true));

        try {
            int reply;
            if (port > 0) {
                ftp.connect(server, port);
            } else {
                ftp.connect(server);
            }
            runner.announce("Connected to " + server + " on " + (port > 0 ? port : ftp.getDefaultPort()));
            runner.announce(ftp.getReplyString());

            // After connection attempt, you should check the reply code to
            // verify
            // success.
            reply = ftp.getReplyCode();

            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                runner.announce("FTP server refused connection.\n" + ftp.getReplyString());
                return false;
            }

            if (!ftp.login(username, password)) {
                ftp.logout();
                runner.announce("FTP server login error.\n" + ftp.getReplyString());
                return false;
            }

            runner.announce("Remote system is " + ftp.getSystemType());

            if (binaryTransfer) {
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
            } else {
                // in theory this should not be necessary as servers should
                // default to ASCII
                // but they don't all do so - see NET-500
                ftp.setFileType(FTP.ASCII_FILE_TYPE);
            }

            // Use passive mode as default because most of us are
            // behind firewalls these days.
            if (passive) {
                ftp.enterLocalPassiveMode();
            } else {
                ftp.enterLocalActiveMode();

            }

            ftp.setUseEPSVwithIPv4(useEpsvWithIPv4);

            // check that control connection is working OK
            ftp.noop();

        } catch (FTPConnectionClosedException e) {
            runner.announce("Server closed connection.\n" + e.getMessage());
            disconnect();
            return false;

        } catch (IOException e) {
            runner.announce("Could not connect to server.\n" + e.getMessage());
            disconnect();
            return false;
        }

        return rc;
    }

    private void disconnect() {
        if (ftp != null) {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException f) {
                    // do nothing
                }
            }
        }
    }

    public Boolean storeFile(Map<String, String> files, Runner runner) {
        Boolean rc = null;

        if (connect(runner)) {
            runner.announce("Failed uploading.");
            rc = false;
            return rc;
        }

        Map<String, String> myFiles = new HashMap<String, String>(files);
        for (Map.Entry<String, String> entry : myFiles.entrySet()) {
            String local = entry.getKey();
            String remote = entry.getValue();
            try {
                InputStream input = new FileInputStream(local);
                if (ftp.storeFile(remote, input)) {
                    runner.announce("File '" + local + "' uploaded");
                    if (rc == null)
                        rc = true;
                } else {
                    runner.announce("File '" + local + "' not uploaded");
                    rc = false;
                }
                runner.announce(ftp.getReplyString());

                input.close();

                ftp.noop(); // check that control connection is working OK

            } catch (FTPConnectionClosedException e) {
                runner.announce("Server closed connection.\n" + e.getMessage());
                rc = false;
                e.printStackTrace();
                return rc;
            } catch (SecurityException e) {
                runner.announce("Can not read the file.\n" + e.getMessage());
                rc = false;
            } catch (FileNotFoundException e) {
                runner.announce("File not found.\n" + e.getMessage());
                rc = false;
            } catch (CopyStreamException e) {
                runner.announce("The upload failed, " + FileUtils.byteCountToDisplaySize(e.getTotalBytesTransferred())
                        + " transfered.\n" + e.getMessage());
                rc = false;
            } catch (IOException e) {
                runner.announce("IO error.\n" + e.getMessage());
                rc = false;
                disconnect();
                return rc;
            }
        }
        disconnect();
        return rc;
    }

    public Boolean storeFile(String local, String remote, Runner runner) {
        HashMap<String, String> files = new HashMap<String, String>();
        files.put(local, remote);
        return storeFile(files, runner);
    }
}
