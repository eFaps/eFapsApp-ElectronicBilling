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
package org.efaps.esjp.electronicbilling.recordmanagement;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@EFapsUUID("ee7214be-87fe-4d73-99f3-2f7c4112cdab")
@EFapsApplication("eFapsApp-ElectronicBilling")
@JsonDeserialize(builder = PublishResponseDto.Builder.class)
public class PublishResponseDto
{

    private final String id;

    private PublishResponseDto(Builder builder)
    {
        this.id = builder.id;
    }

    public String getId()
    {
        return id;
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

        private String id;

        private Builder()
        {
        }

        public Builder withId(String id)
        {
            this.id = id;
            return this;
        }

        public PublishResponseDto build()
        {
            return new PublishResponseDto(this);
        }
    }
}
