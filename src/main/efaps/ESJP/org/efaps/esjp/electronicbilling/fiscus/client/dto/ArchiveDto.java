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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ArchiveDto.Builder.class)
@EFapsUUID("d654382a-0c87-483f-8743-0a9def7476d8")
@EFapsApplication("eFapsApp-ElectronicBilling")
public class ArchiveDto
{

    private final String name;
    private final String base64Zip;
    private final String hashZip;

    private ArchiveDto(Builder builder)
    {
        this.name = builder.name;
        this.base64Zip = builder.base64Zip;
        this.hashZip = builder.hashZip;
    }

    @JsonProperty("nomArchivo")
    public String getName()
    {
        return name;
    }

    @JsonProperty("arcGreZip")
    public String getBase64Zip()
    {
        return base64Zip;
    }

    @JsonProperty("hashZip")
    public String getHashZip()
    {
        return hashZip;
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

        private String name;
        private String base64Zip;
        private String hashZip;

        private Builder()
        {
        }

        public Builder withName(String name)
        {
            this.name = name;
            return this;
        }

        public Builder withBase64Zip(String base64Zip)
        {
            this.base64Zip = base64Zip;
            return this;
        }

        public Builder withHashZip(String hashZip)
        {
            this.hashZip = hashZip;
            return this;
        }

        public ArchiveDto build()
        {
            return new ArchiveDto(this);
        }
    }
}
