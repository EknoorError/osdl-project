public class ChargingSession {
    private String slot;
    private String user;
    private String protocol;
    private String startTime;

    public ChargingSession(String slot, String user, String protocol, String startTime) {
        this.slot = slot;
        this.user = user;
        this.protocol = protocol;
        this.startTime = startTime;
    }

    public String toCSV() {
        return slot + "," + user + "," + protocol + "," + startTime;
    }

    // Optional getters (useful later)
    public String getSlot() {
        return slot;
    }

    public String getUser() {
        return user;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getStartTime() {
        return startTime;
    }
}