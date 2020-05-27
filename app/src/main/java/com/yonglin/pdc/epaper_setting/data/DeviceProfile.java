package com.yonglin.pdc.epaper_setting.data;

import java.util.UUID;

public class DeviceProfile implements Comparable<DeviceProfile> {

    // fixed file name in each profile folder, and toggled by flag
    public static final String CA_CERT_FILE = "ca.pem";
    public static final String CLIENT_CERT_FILE = "client.pem";
    public static final String PRIVATE_KEY_FILE = "private.key";

    // unique id
    private UUID uuid;

    // basic data
    private String name;
    private DeviceType deviceType;
    private String serverIp;
    private String serverPort;
    private String serverSsid;
    private SecurityType securityType;
    private String password;

    // advanced data
    private IpType ipType = IpType.DHCP;
    private String staticIp;
    private String gatewayIp;
    private String subnetMask;
    private String dnsServer;

    // eap
    private String eapIdentity; // Enterprise user name
    private String eapAnonymousIdentity; // Enterprise anonymous
    private boolean isEapCaCertSaved = false;
    private boolean isEapClientCertSaved = false;
    private boolean isEapPrivateKeySaved = false;
    private EapMethod eapMethod = EapMethod.PEAP;
    private int eapPhase2Auth; // we only support MSCHAPV2 now so no enum created.

    // MQTT
    private String rtmMqttTopic;
    private String rtmMqttUsername;
    private String rtmMqttPassword;


    /**
     * compares the contents of a profile.
     * Note that uuid, name, and the uploaded cert files contents are not checked.
     * @param p profile
     * @return see standard compareTo()
     */
    @Override
    public int compareTo(DeviceProfile p) {
        if(p == null) return -1;

        int rc = compare(this.deviceType.toString(), p.getDeviceType().toString());
        if (rc != 0) return rc;

        rc = compare(this.serverIp, p.getServerIp());
        if (rc != 0) return rc;

        rc = compare(this.serverPort, p.getServerPort());
        if (rc != 0) return rc;

        rc = compare(this.serverSsid, p.getServerSsid());
        if (rc != 0) return rc;

        rc = compare(this.securityType.toString(), p.getSecurityType().toString());
        if (rc != 0) return rc;

        rc = compare(this.password, p.getPassword());
        if (rc != 0) return rc;

        rc = compare(this.ipType.toString(), p.getIpType().toString());
        if (rc != 0) return rc;

        rc = compare(this.staticIp, p.getStaticIp());
        if (rc != 0) return rc;

        rc = compare(this.gatewayIp, p.getGatewayIp());
        if (rc != 0) return rc;

        rc = compare(this.subnetMask, p.getSubnetMask());
        if (rc != 0) return rc;

        rc = compare(this.dnsServer, p.getDnsServer());
        if (rc != 0) return rc;

        rc = compare(this.eapIdentity, p.getEapIdentity());
        if (rc != 0) return rc;

        rc = compare(this.eapAnonymousIdentity, p.getEapAnonymousIdentity());
        if (rc != 0) return rc;

        rc = compare(this.isEapCaCertSaved, p.isEapCaCertSaved());
        if (rc != 0) return rc;

        rc = compare(this.isEapClientCertSaved, p.isEapClientCertSaved());
        if (rc != 0) return rc;

        rc = compare(this.isEapPrivateKeySaved, p.isEapPrivateKeySaved());
        if (rc != 0) return rc;

        rc = compare(this.eapMethod.toString(), p.getEapMethod().toString());
        if (rc != 0) return rc;

        rc = compare(this.rtmMqttTopic, p.getRtmMqttTopic());
        if (rc != 0) return rc;

        rc = compare(this.rtmMqttUsername, p.getRtmMqttUsername());
        if (rc != 0) return rc;

        rc = compare(this.rtmMqttPassword, p.getRtmMqttPassword());
        if (rc != 0) return rc;

        return this.eapPhase2Auth - p.getEapPhase2Auth();
    }

    private int compare(String a, String b) {
        if (a == b) return 0; // both null or the same reference
        if (a == null) return -1;
        if (b == null) return 1;
        return a.compareTo(b);
    }

    private int compare(boolean a, boolean b) {
        if (a == b) return 0;
        if (b) return -1;
        else return 1;
    }

    @Override
    public String toString() {
        return "DeviceProfile{" +
                "uuid=" + uuid +
                ", name='" + name + '\'' +
                ", deviceType=" + deviceType +
                ", serverIp='" + serverIp + '\'' +
                ", serverPort='" + serverPort + '\'' +
                ", serverSsid='" + serverSsid + '\'' +
                ", securityType=" + securityType +
                ", password='" + password + '\'' +
                ", ipType=" + ipType +
                ", staticIp='" + staticIp + '\'' +
                ", gatewayIp='" + gatewayIp + '\'' +
                ", subnetMask='" + subnetMask + '\'' +
                ", dnsServer='" + dnsServer + '\'' +
                ", eapIdentity='" + eapIdentity + '\'' +
                ", eapAnonymousIdentity='" + eapAnonymousIdentity + '\'' +
                ", isEapCaCertSaved=" + isEapCaCertSaved +
                ", isEapClientCertSaved=" + isEapClientCertSaved +
                ", isEapPrivateKeySaved=" + isEapPrivateKeySaved +
                ", eapMethod=" + eapMethod +
                ", eapPhase2Auth=" + eapPhase2Auth +
                ", rtmMqttTopic='" + rtmMqttTopic + '\'' +
                ", rtmMqttUsername='" + rtmMqttUsername + '\'' +
                ", rtmMqttPassword='" + rtmMqttPassword + '\'' +
                '}';
    }

    public DeviceProfile() {
        this.uuid = UUID.randomUUID();
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public String getServerPort() {
        return serverPort;
    }

    public void setServerPort(String serverPort) {
        this.serverPort = serverPort;
    }

    public String getServerSsid() {
        return serverSsid;
    }

    public void setServerSsid(String serverSsid) {
        this.serverSsid = serverSsid;
    }

    public SecurityType getSecurityType() {
        return securityType;
    }

    public void setSecurityType(SecurityType securityType) {
        this.securityType = securityType;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public IpType getIpType() {
        return ipType;
    }

    public void setIpType(IpType ipType) {
        this.ipType = ipType;
    }

    public String getStaticIp() {
        return staticIp;
    }

    public void setStaticIp(String staticIp) {
        this.staticIp = staticIp;
    }

    public String getGatewayIp() {
        return gatewayIp;
    }

    public void setGatewayIp(String gatewayIp) {
        this.gatewayIp = gatewayIp;
    }

    public String getSubnetMask() {
        return subnetMask;
    }

    public void setSubnetMask(String subnetMask) {
        this.subnetMask = subnetMask;
    }

    public String getDnsServer() {
        return dnsServer;
    }

    public void setDnsServer(String dnsServer) {
        this.dnsServer = dnsServer;
    }

    public String getEapIdentity() {
        return eapIdentity;
    }

    public void setEapIdentity(String eapIdentity) {
        this.eapIdentity = eapIdentity;
    }

    public String getEapAnonymousIdentity() {
        return eapAnonymousIdentity;
    }

    public void setEapAnonymousIdentity(String eapAnonymousIdentity) {
        this.eapAnonymousIdentity = eapAnonymousIdentity;
    }

    public EapMethod getEapMethod() {
        return eapMethod;
    }

    public void setEapMethod(EapMethod eapMethod) {
        this.eapMethod = eapMethod;
    }

    public int getEapPhase2Auth() {
        return eapPhase2Auth;
    }

    public void setEapPhase2Auth(int eapPhase2Auth) {
        this.eapPhase2Auth = eapPhase2Auth;
    }

    public boolean isEapCaCertSaved() {
        return isEapCaCertSaved;
    }

    public void setEapCaCertSaved(boolean eapCaCertSaved) {
        isEapCaCertSaved = eapCaCertSaved;
    }

    public boolean isEapClientCertSaved() {
        return isEapClientCertSaved;
    }

    public void setEapClientCertSaved(boolean eapClientCertSaved) {
        isEapClientCertSaved = eapClientCertSaved;
    }

    public boolean isEapPrivateKeySaved() {
        return isEapPrivateKeySaved;
    }

    public void setEapPrivateKeySaved(boolean eapPrivateKeySaved) {
        isEapPrivateKeySaved = eapPrivateKeySaved;
    }

    public String getRtmMqttTopic() {
        return rtmMqttTopic;
    }

    public void setRtmMqttTopic(String rtmMqttTopic) {
        this.rtmMqttTopic = rtmMqttTopic;
    }

    public String getRtmMqttUsername() {
        return rtmMqttUsername;
    }

    public void setRtmMqttUsername(String rtmMqttUsername) {
        this.rtmMqttUsername = rtmMqttUsername;
    }

    public String getRtmMqttPassword() {
        return rtmMqttPassword;
    }

    public void setRtmMqttPassword(String rtmMqttPassword) {
        this.rtmMqttPassword = rtmMqttPassword;
    }
}
