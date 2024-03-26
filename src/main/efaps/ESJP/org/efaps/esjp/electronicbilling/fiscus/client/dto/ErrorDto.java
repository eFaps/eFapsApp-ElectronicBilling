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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ErrorDto.Builder.class)
@EFapsUUID("b663695a-994a-44fa-9e66-6e42563101d3")
@EFapsApplication("eFapsApp-ElectronicBilling")
public class ErrorDto
{

    private final Integer cod;
    private final String msg;

    private ErrorDto(Builder builder)
    {
        this.cod = builder.cod;
        this.msg = builder.msg;
    }

    public Integer getCod()
    {
        return cod;
    }

    public String getMsg()
    {
        return msg;
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

        private Integer cod;
        private String msg;

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

        public ErrorDto build()
        {
            return new ErrorDto(this);
        }
    }
}
