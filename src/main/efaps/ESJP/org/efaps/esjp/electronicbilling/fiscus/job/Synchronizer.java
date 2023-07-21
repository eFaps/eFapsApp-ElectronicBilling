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
package org.efaps.esjp.electronicbilling.fiscus.job;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIStatus;
import org.efaps.db.Checkout;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIEBilling;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.electronicbilling.fiscus.client.rest.DeliveryNoteClient;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("fa7cde86-d871-4ba1-8d38-ab77265514cc")
@EFapsApplication("eFapsApp-ElectronicBilling")
public class Synchronizer
{

    private static final Logger LOG = LoggerFactory.getLogger(Synchronizer.class);

    public void syncPending(final Parameter _parameter)
        throws EFapsException
    {
        LOG.info("Syncing pending EDocuments");
        final var eval = EQL.builder().print()
                        .query(CIEBilling.DeliveryNote)
                        .where().attribute(CIEBilling.DeliveryNote.StatusAbstract)
                        .in(Status.find(CIEBilling.DeliveryNoteStatus.Pending).getId())
                        .select()
                        .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                        .attribute(CISales.DocumentAbstract.Name).as("DocName")
                        .evaluate();

        while (eval.next()) {
            final String docName = eval.get("DocName");
            if (docName != null) {
                String documentType = null;
                DeliveryNoteClient restClient = null;
                if (InstanceUtils.isType(eval.inst(), CIEBilling.Invoice)) {
                    documentType = "01";
                } else if (InstanceUtils.isType(eval.inst(), CIEBilling.Receipt)) {
                    documentType = "03";
                } else if (InstanceUtils.isType(eval.inst(), CIEBilling.CreditNote)) {
                    documentType = "07";
                } else if (InstanceUtils.isType(eval.inst(), CIEBilling.DeliveryNote)) {
                    documentType = "09";
                    restClient = new DeliveryNoteClient();
                }
                if (documentType != null) {
                        final var xmlEval = EQL.builder().print()
                                        .query(CIEBilling.UBLFile)
                                        .where().attribute(CIEBilling.UBLFile.DocumentLinkAbstract).eq(eval.inst())
                                        .select()
                                        .instance()
                                        .evaluate();
                        if (xmlEval.next()) {
                            final var checkout = new Checkout(xmlEval.inst());
                            final ByteArrayOutputStream os = new ByteArrayOutputStream();
                            checkout.execute(os);
                            final String xml = new String(os.toByteArray(), StandardCharsets.UTF_8);
                            LOG.info("xml: \n {}", xml);
                             restClient.sendUbl(documentType, docName, xml);
                        }
                }
            }
        }
    }

    public void setStatus(final Instance _eDocInst,
                          final String status)
        throws EFapsException
    {
        CIStatus targetStatus;
        if (InstanceUtils.isType(_eDocInst, CIEBilling.Invoice)) {
            switch (status) {
                case "Issued":
                    targetStatus = CIEBilling.InvoiceStatus.Issued;
                    break;
                default:
                    targetStatus = CIEBilling.InvoiceStatus.Successful;
                    break;
            }
        } else if (InstanceUtils.isType(_eDocInst, CIEBilling.Receipt)) {
            switch (status) {
                case "Issued":
                    targetStatus = CIEBilling.ReceiptStatus.Issued;
                    break;
                default:
                    targetStatus = CIEBilling.ReceiptStatus.Successful;
                    break;
            }
        } else {
            switch (status) {
                case "Issued":
                    targetStatus = CIEBilling.CreditNoteStatus.Issued;
                    break;
                default:
                    targetStatus = CIEBilling.CreditNoteStatus.Successful;
                    break;
            }
        }
        EQL.builder().update(_eDocInst)
                        .set(CIEBilling.DocumentAbstract.StatusAbstract, targetStatus)
                        .stmt()
                        .execute();
    }
}
