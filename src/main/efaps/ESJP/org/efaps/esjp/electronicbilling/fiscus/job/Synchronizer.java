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

import org.apache.commons.lang.StringUtils;
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
import org.efaps.esjp.electronicbilling.fiscus.client.dto.DeliveryNoteResponseDto;
import org.efaps.esjp.electronicbilling.fiscus.client.dto.StatusResponseDto;
import org.efaps.esjp.electronicbilling.fiscus.client.rest.DeliveryNoteClient;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
                final Instance eDocInst = eval.inst();
                String documentType = null;
                DeliveryNoteClient restClient = null;
                if (InstanceUtils.isType(eDocInst, CIEBilling.Invoice)) {
                    documentType = "01";
                } else if (InstanceUtils.isType(eDocInst, CIEBilling.Receipt)) {
                    documentType = "03";
                } else if (InstanceUtils.isType(eDocInst, CIEBilling.CreditNote)) {
                    documentType = "07";
                } else if (InstanceUtils.isType(eDocInst, CIEBilling.DeliveryNote)) {
                    documentType = "09";
                    restClient = new DeliveryNoteClient();
                }
                if (documentType != null) {

                        final var xmlEval = EQL.builder().print()
                                        .query(CIEBilling.UBLFile)
                                        .where().attribute(CIEBilling.UBLFile.DocumentLinkAbstract).eq(eDocInst)
                                        .select()
                                        .instance()
                                        .evaluate();
                        if (xmlEval.next()) {
                            final var checkout = new Checkout(xmlEval.inst());
                            final ByteArrayOutputStream os = new ByteArrayOutputStream();
                            checkout.execute(os);
                            final String xml = new String(os.toByteArray(), StandardCharsets.UTF_8);
                            LOG.info("xml: \n {}", xml);
                            final var dto =  restClient.sendUbl(documentType, docName, xml);
                            LOG.info("dto: {}", dto);
                            if (dto instanceof DeliveryNoteResponseDto) {
                                final var ticketNumber = ((DeliveryNoteResponseDto) dto).getNumTicket();
                                setStatus(eDocInst, "Issued", ticketNumber, null);
                            }
                            logResponse(eDocInst, dto);
                        }
                }
            }
        }
    }

    public void syncIssued(final Parameter _parameter)
        throws EFapsException
    {
        LOG.info("Syncing issued EDocuments");
        final var eval = EQL.builder().print()
                        .query(CIEBilling.DeliveryNote)
                        .where().attribute(CIEBilling.DeliveryNote.StatusAbstract)
                        .in(Status.find(CIEBilling.DeliveryNoteStatus.Issued).getId())
                        .select()
                        .attribute(CIEBilling.DeliveryNote.Identifier)
                        .evaluate();
        while (eval.next()) {
            final Instance eDocInst = eval.inst();
            final String identifier = eval.get(CIEBilling.DeliveryNote.Identifier);
            if (StringUtils.isNotEmpty(identifier)) {
                DeliveryNoteClient restClient = null;
                if (InstanceUtils.isType(eval.inst(), CIEBilling.DeliveryNote)) {
                    restClient = new DeliveryNoteClient();
                }
                if (restClient != null) {
                    final var dto =  restClient.getStatus(identifier);
                    LOG.info("dto: {}", dto);
                    if (dto instanceof StatusResponseDto) {
                        if (((StatusResponseDto) dto).getCode().equals(0)) {
                            setStatus(eDocInst, "Successful", null, ((StatusResponseDto) dto).getConfirmation());
                        }
                    }
                    logResponse(eDocInst, dto);
                }
            }
        }
    }

    protected void logResponse(final Instance eDocIns,
                               final Object object)
        throws EFapsException
    {

        String json = null;
        try {
            json = getObjectMapper().writeValueAsString(object);
        } catch (final JsonProcessingException e) {
            LOG.error("Catched", e);
        }
        LOG.info("log: {}", json);
        EQL.builder().insert(CIEBilling.LogResponse)
                        .set(CIEBilling.LogResponse.DocumentLinkAbstract, eDocIns)
                        .set(CIEBilling.LogResponse.Content, json)
                        .stmt()
                        .execute();
    }

    protected ObjectMapper getObjectMapper()
    {
        final var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }

    public void setStatus(final Instance _eDocInst,
                          final String status,
                          final String identifier,
                          final String confirmation)
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
        } else if (InstanceUtils.isType(_eDocInst, CIEBilling.CreditNote)) {
            switch (status) {
                case "Issued":
                    targetStatus = CIEBilling.CreditNoteStatus.Issued;
                    break;
                default:
                    targetStatus = CIEBilling.CreditNoteStatus.Successful;
                    break;
            }
        } else {
            switch (status) {
                case "Issued":
                    targetStatus = CIEBilling.DeliveryNoteStatus.Issued;
                    break;
                default:
                    targetStatus = CIEBilling.DeliveryNoteStatus.Successful;
                    break;
            }
        }
        final var eql = EQL.builder().update(_eDocInst)
                        .set(CIEBilling.DocumentAbstract.StatusAbstract, targetStatus);
        if (identifier != null) {
            eql.set(CIEBilling.DocumentAbstract.Identifier, identifier);
        }
        if (confirmation != null) {
            eql.set(CIEBilling.DocumentAbstract.Confirmation, StringUtils.left(confirmation, 60));
        }
        eql.stmt().execute();
    }
}
