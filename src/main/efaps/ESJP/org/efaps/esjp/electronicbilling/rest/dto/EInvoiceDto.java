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
package org.efaps.esjp.electronicbilling.rest.dto;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@EFapsUUID("ef0c51f8-f7a6-44ca-971b-b5cb56c4cda4")
@EFapsApplication("eFapsApp-ElectronicBilling")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EInvoiceDto
{

    private String ubl;

    private String hash;

    private String docOid;

    public String getUbl()
    {
        return ubl;
    }

    public void setUbl(final String ubl)
    {
        this.ubl = ubl;
    }

    public String getHash()
    {
        return hash;
    }

    public void setHash(final String hash)
    {
        this.hash = hash;
    }

    public String getDocOid()
    {
        return docOid;
    }

    public void setDocOid(final String docOid)
    {
        this.docOid = docOid;
    }

}
