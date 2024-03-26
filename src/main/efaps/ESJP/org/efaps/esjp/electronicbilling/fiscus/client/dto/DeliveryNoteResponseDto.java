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

@JsonDeserialize(builder = DeliveryNoteResponseDto.Builder.class)
@EFapsUUID("eeba7f1e-5688-4391-b369-551bc8c8749a")
@EFapsApplication("eFapsApp-ElectronicBilling")
public class DeliveryNoteResponseDto
{

    private final String numTicket;
    private final String fecRecepcion;

    private DeliveryNoteResponseDto(Builder builder)
    {
        this.numTicket = builder.numTicket;
        this.fecRecepcion = builder.fecRecepcion;
    }

    public String getNumTicket()
    {
        return numTicket;
    }

    public String getFecRecepcion()
    {
        return fecRecepcion;
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

        private String numTicket;
        private String fecRecepcion;

        private Builder()
        {
        }

        public Builder withNumTicket(String numTicket)
        {
            this.numTicket = numTicket;
            return this;
        }

        public Builder withFecRecepcion(String fecRecepcion)
        {
            this.fecRecepcion = fecRecepcion;
            return this;
        }

        public DeliveryNoteResponseDto build()
        {
            return new DeliveryNoteResponseDto(this);
        }
    }
}
