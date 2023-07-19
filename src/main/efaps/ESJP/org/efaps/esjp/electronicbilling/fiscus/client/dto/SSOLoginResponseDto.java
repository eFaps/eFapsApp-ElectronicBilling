/*
 * Copyright 2003 - 2023 The eFaps Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.efaps.esjp.electronicbilling.fiscus.client.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = SSOLoginResponseDto.Builder.class)
@EFapsUUID("f2385a4d-2fed-4ca7-8dd9-28c2c550698d")
@EFapsApplication("eFapsApp-ElectronicBilling")
public class SSOLoginResponseDto
{

    private final String accessToken;
    private final String tokenType;
    private final int expiresIn;

    private SSOLoginResponseDto(Builder builder)
    {
        this.accessToken = builder.accessToken;
        this.tokenType = builder.tokenType;
        this.expiresIn = builder.expiresIn;
    }

    public String getAccessToken()
    {
        return accessToken;
    }

    public String getTokenType()
    {
        return tokenType;
    }

    public int getExpiresIn()
    {
        return expiresIn;
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {

        private String accessToken;
        private String tokenType;
        private int expiresIn;

        private Builder()
        {
        }

        @JsonProperty("access_token")
        public Builder withAccessToken(String accessToken)
        {
            this.accessToken = accessToken;
            return this;
        }

        @JsonProperty("token_type")
        public Builder withTokenType(String tokenType)
        {
            this.tokenType = tokenType;
            return this;
        }

        @JsonProperty("expires_in")
        public Builder withExpiresIn(int expiresIn)
        {
            this.expiresIn = expiresIn;
            return this;
        }

        public SSOLoginResponseDto build()
        {
            return new SSOLoginResponseDto(this);
        }
    }
}
