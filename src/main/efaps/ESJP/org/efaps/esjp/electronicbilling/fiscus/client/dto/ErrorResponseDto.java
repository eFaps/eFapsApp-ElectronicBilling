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

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ErrorResponseDto.Builder.class)
@EFapsUUID("08a420ae-e27f-4470-af26-eeabaf1becce")
@EFapsApplication("eFapsApp-ElectronicBilling")
public class ErrorResponseDto
{

    private final Integer cod;
    private final String msg;
    private final String exc;
    private final List<ErrorDto> errors;

    private ErrorResponseDto(Builder builder)
    {
        this.cod = builder.cod;
        this.msg = builder.msg;
        this.exc = builder.exc;
        this.errors = builder.errors;
    }

    public Integer getCod()
    {
        return cod;
    }

    public String getMsg()
    {
        return msg;
    }

    public String getExc()
    {
        return exc;
    }

    public List<ErrorDto> getErrors()
    {
        return errors;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Builder
    {

        private Integer cod;
        private String msg;
        private String exc;
        private List<ErrorDto> errors;

        private Builder()
        {
        }

        public Builder withCod(Integer cod)
        {
            this.cod = cod;
            return this;
        }

        public Builder withMsg(String msg)
        {
            this.msg = msg;
            return this;
        }

        public Builder withExc(String exc)
        {
            this.exc = exc;
            return this;
        }

        public Builder withErrors(List<ErrorDto> errors)
        {
            this.errors = errors;
            return this;
        }

        public ErrorResponseDto build()
        {
            return new ErrorResponseDto(this);
        }
    }
}
