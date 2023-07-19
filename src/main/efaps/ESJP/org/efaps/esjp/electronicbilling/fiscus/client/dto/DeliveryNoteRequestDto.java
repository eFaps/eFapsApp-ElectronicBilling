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

@JsonDeserialize(builder = DeliveryNoteRequestDto.Builder.class)
@EFapsUUID("f69635bf-67f6-4c6d-a3de-a34f207c314f")
@EFapsApplication("eFapsApp-ElectronicBilling")
public class DeliveryNoteRequestDto
{

    private final ArchiveDto archive;

    private DeliveryNoteRequestDto(Builder builder)
    {
        this.archive = builder.archive;
    }

    @JsonProperty("archivo")
    public ArchiveDto getArchive()
    {
        return archive;
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

        private ArchiveDto archive;

        private Builder()
        {
        }

        public Builder withArchive(ArchiveDto archive)
        {
            this.archive = archive;
            return this;
        }

        public DeliveryNoteRequestDto build()
        {
            return new DeliveryNoteRequestDto(this);
        }
    }
}
