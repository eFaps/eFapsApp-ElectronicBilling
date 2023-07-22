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
package org.efaps.esjp.electronicbilling.rest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.core.Response;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Checkin;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIEBilling;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.electronicbilling.rest.dto.EInvoiceDto;
import org.efaps.util.EFapsException;

@EFapsUUID("c6c73fba-d527-408c-a315-b46203cf4d0e")
@EFapsApplication("eFapsApp-ElectronicBilling")
public abstract class AbstractUBLController_Base
{

    public Response addEInvoice(final EInvoiceDto _einvoice)
        throws EFapsException
    {
        final var docInst = Instance.get(_einvoice.getDocOid());
        Instance eDocInst = null;
        if (CISales.Invoice.isType(docInst.getType())) {
            final var statusKey = getUploadStatusKey(CIEBilling.Invoice);
            eDocInst = EQL.builder().insert(CIEBilling.Invoice)
                            .set(CIEBilling.Invoice.InvoiceLink, docInst)
                            .set(CIEBilling.Invoice.Status, Status.find(CIEBilling.InvoiceStatus, statusKey).getId())
                            .stmt()
                            .execute();
        } else if (CISales.Receipt.isType(docInst.getType())) {
            final var statusKey = getUploadStatusKey(CIEBilling.Receipt);
            eDocInst = EQL.builder().insert(CIEBilling.Receipt)
                            .set(CIEBilling.Receipt.ReceiptLink, docInst)
                            .set(CIEBilling.Receipt.Status, Status.find(CIEBilling.ReceiptStatus, statusKey).getId())
                            .stmt()
                            .execute();
        } else if (CISales.CreditNote.isType(docInst.getType())) {
            final var statusKey = getUploadStatusKey(CIEBilling.CreditNote);
            eDocInst = EQL.builder().insert(CIEBilling.CreditNote)
                            .set(CIEBilling.CreditNote.CreditNoteLink, docInst)
                            .set(CIEBilling.CreditNote.Status,
                                            Status.find(CIEBilling.CreditNoteStatus, statusKey).getId())
                            .stmt()
                            .execute();
        }
        if (InstanceUtils.isValid(eDocInst)) {
            final var fileInst = EQL.builder()
                            .insert(getUBLFileType())
                            .set(CIEBilling.UBLFileAbstract.DocumentLinkAbstract, eDocInst)
                            .stmt()
                            .execute();

            final var is = new ByteArrayInputStream(_einvoice.getUbl().getBytes(StandardCharsets.UTF_8));
            final var checkin = new Checkin(fileInst);
            checkin.execute("EInvoice.xml", is, is.available());
        }

        final Response ret = Response.ok()
                        .entity(eDocInst == null ? null : eDocInst.getOid())
                        .build();
        return ret;
    }

    public abstract String getUploadStatusKey(CIType eInvoiceType)
        throws EFapsException;

    public abstract CIType getUBLFileType()
        throws EFapsException;

}
