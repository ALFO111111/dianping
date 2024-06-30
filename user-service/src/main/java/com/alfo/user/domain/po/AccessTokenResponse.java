package com.alfo.user.domain.po;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AccessTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
