/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.electronicbilling.fiscus.client.dto;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = StatusResponseDto.Builder.class)
@EFapsUUID("0014f2da-83c3-4bd5-bbbd-c0d184f19afc")
@EFapsApplication("eFapsApp-ElectronicBilling")
public class StatusResponseDto
{

    private final Integer code;
    private final String confirmation;
    private final Integer confirmationIndicator;
    private final StatusErrorDto error;

    private StatusResponseDto(Builder builder)
    {
        this.code = builder.code;
        this.confirmation = builder.confirmation;
        this.confirmationIndicator = builder.confirmationIndicator;
        this.error = builder.error;
    }

    public Integer getCode()
    {
        return code;
    }

    public String getConfirmation()
    {
        return confirmation;
    }

    public Integer getConfirmationIndicator()
    {
        return confirmationIndicator;
    }

    public StatusErrorDto getError()
    {
        return error;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {

        private Integer code;
        private String confirmation;
        private Integer confirmationIndicator;
        private StatusErrorDto error;

        private Builder()
        {
        }

        @JsonProperty("codRespuesta")
        public Builder withCode(Integer code)
        {
            this.code = code;
            return this;
        }

        @JsonProperty("arcCdr")
        public Builder withConfirmation(String confirmation)
        {
            this.confirmation = confirmation;
            return this;
        }

        @JsonProperty("indCdrGenerado")
        public Builder withConfirmationIndicator(Integer confirmationIndicator)
        {
            this.confirmationIndicator = confirmationIndicator;
            return this;
        }

        @JsonProperty("error")
        public Builder withErrors(StatusErrorDto error)
        {
            this.error = error;
            return this;
        }

        public StatusResponseDto build()
        {
            return new StatusResponseDto(this);
        }
    }
}
