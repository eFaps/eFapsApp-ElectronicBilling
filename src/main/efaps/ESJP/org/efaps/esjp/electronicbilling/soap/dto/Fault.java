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
package org.efaps.esjp.electronicbilling.soap.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@EFapsUUID("faf5be7f-fee4-4b72-9627-69df4b143513")
@EFapsApplication("eFapsApp-ElectronicBilling")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Fault
{

    @JsonProperty(value = "faultcode")
    String faultcode;
    @JsonProperty(value = "faultstring")
    String faultstring;
    @JsonProperty(value = "detail")
    FaultDetail detail;

    public String getFaultcode()
    {
        return faultcode;
    }

    public String getFaultstring()
    {
        return faultstring;
    }

    public FaultDetail getDetail()
    {
        return detail;
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
    }
}
