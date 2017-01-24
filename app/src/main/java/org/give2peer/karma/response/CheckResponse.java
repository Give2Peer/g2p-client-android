package org.give2peer.karma.response;

public class CheckResponse {

    private String status = "";

    public String getStatus() {
        return status;
    }

    public boolean isOk() {
        return status.equals("ok");
    }

}
