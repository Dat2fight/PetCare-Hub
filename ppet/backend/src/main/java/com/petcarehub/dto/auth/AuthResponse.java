package com.petcarehub.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private UserDTO user;

    @JsonProperty("accessToken")
    public String getAccessToken() {
        return token;
    }

    @JsonProperty("accessToken")
    public void setAccessToken(String accessToken) {
        this.token = accessToken;
    }
}
