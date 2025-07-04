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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Checkout;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.eql.builder.Selectables;
import org.efaps.esjp.ci.CIEBilling;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.file.FileUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.electronicbilling.FiscusMapper;
import org.efaps.esjp.electronicbilling.fiscus.client.rest.AbstractRestClient;
import org.efaps.esjp.electronicbilling.util.ElectronicBilling;
import org.efaps.esjp.erp.rest.client.OAuth2Client;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.util.EFapsException;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("f7f560e3-1f45-4367-a51b-ae3166239ae8")
@EFapsApplication("eFapsApp-ElectronicBilling")
public class Publish
    extends AbstractRestClient
{

    private static final Logger LOG = LoggerFactory.getLogger(Publish.class);

    private OAuth2Client ssoClient;

    public Return publishDocument(final Parameter parameter)
        throws EFapsException
    {
        publishDocument(Instance.get("49394.467"));
        return new Return();
    }

    public Return scan4Documents(final Parameter parameter)
        throws EFapsException
    {
        scan4Documents();
        return new Return();
    }

    public void scan4Documents()
        throws EFapsException
    {

        final var eval = EQL.builder().print()
                        .query(CIEBilling.Invoice, CIEBilling.Receipt, CIEBilling.CreditNote)
                        .where()
                        .attribute(CIEBilling.DocumentAbstract.ID).in(
                                        EQL.builder().nestedQuery(CIEBilling.UBLFileAbstract)
                                                        .where()
                                                        .attribute(CIEBilling.UBLFileAbstract.Created)
                                                        .greater("2025-06-30")
                                                        .and()
                                                        .attribute(CIEBilling.UBLFileAbstract.UBLHash).isNull()
                                                        .up()
                                                        .selectable(Selectables.attribute(
                                                                        CIEBilling.UBLFileAbstract.DocumentLinkAbstract)))
                        .and()
                        .attribute(CIEBilling.DocumentAbstract.ID).notin(
                                        EQL.builder().nestedQuery(CIEBilling.UBLFileAbstract)
                                                        .where()
                                                        .attribute(CIEBilling.UBLFileAbstract.UBLHash).notIsNull()
                                                        .up()
                                                        .selectable(Selectables.attribute(
                                                                        CIEBilling.UBLFileAbstract.DocumentLinkAbstract)))
                        .select()
                        .id()
                        .evaluate();

        while (eval.next()) {
            final var edocInst = eval.inst();
            publishDocument(edocInst);
        }
    }

    public void publishDocument(final Instance edocInst)
        throws EFapsException
    {
        final var eval = EQL.builder().print()
                        .query(CIEBilling.UBLFileAbstract)
                        .where()
                        .attribute(CIEBilling.UBLFileAbstract.DocumentLinkAbstract).eq(edocInst)
                        .select()
                        .attribute(CIEBilling.UBLFileAbstract.Created)
                        .orderBy(CIEBilling.UBLFileAbstract.Created, true)
                        .evaluate();
        if (eval.next()) {
            final var docEval = EQL.builder().print(edocInst)
                            .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract).instance().as("docInst")
                            .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                            .attribute(CISales.DocumentSumAbstract.Name)
                            .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                            .attribute(CISales.DocumentSumAbstract.RateCrossTotal)
                            .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                            .attribute(CISales.DocumentSumAbstract.Date)
                            .evaluate();
            if (docEval.next()) {
                final var docType = FiscusMapper.getDocumentType4Document(docEval.get("docInst"));
                final var ublInst = eval.inst();
                final var id = publishUbl(docType,
                                docEval.get(CISales.DocumentSumAbstract.Name),
                                docEval.get(CISales.DocumentSumAbstract.Date),
                                docEval.get(CISales.DocumentSumAbstract.RateCrossTotal),
                                ublInst);
                if (id != null) {
                    EQL.builder().update(ublInst).set(CIEBilling.UBLFileAbstract.UBLHash, id).execute();
                }
            }
        }
    }

    public String publishUbl(final String docType,
                             final String number,
                             final LocalDate date,
                             final BigDecimal total,
                             final Instance ublInst)
        throws EFapsException
    {
        String ret = null;
        if (InstanceUtils.isKindOf(ublInst, CIEBilling.UBLFileAbstract)) {
            final var token = getOAuth2Client().getToken();

            final var request = getClient().target(ElectronicBilling.RECORDMGTM_ENDPOINTURI.get())
                            .request(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + token);

            try {
                final var checkout = new Checkout(ublInst);

                final InputStream input = checkout.execute();
                final File temp = new FileUtil().getFile(checkout.getFileName());
                final OutputStream out = new FileOutputStream(temp);
                IOUtils.copy(input, out);
                input.close();
                out.close();

                final var filePart = new FileDataBodyPart("ubl", temp);

                try (var multipart = new FormDataMultiPart()
                                .field("clientId", ERP.COMPANY_TAX.get())
                                .field("docType", docType)
                                .field("number", number)
                                .field("date", date.toString())
                                .field("total", total.toString())
                                .bodyPart(filePart)) {
                    final var response = request.buildPost(Entity.entity(multipart, multipart.getMediaType()))
                                    .invoke(PublishResponseDto.class);
                    LOG.info("{}", response);
                    ret = response.getId();
                }
            } catch (EFapsException | IOException e) {
                LOG.error("Catched", e);
            }
        } else {
            LOG.warn("Invalid instance: {}", ublInst);
        }
        return ret;
    }

    protected OAuth2Client getOAuth2Client()
        throws EFapsException
    {
        if (ssoClient == null) {
            ssoClient = OAuth2Client.builder()
                            .withTarget(URI.create(ElectronicBilling.RECORDMGTM_SSO_ENDPOINTURI.get()))
                            .withClientId(ElectronicBilling.RECORDMGTM_SSO_CLIENTID.get())
                            .withClientSecret(ElectronicBilling.RECORDMGTM_SSO_CLIENTSECRET.get())
                            .withUsername(ElectronicBilling.RECORDMGTM_SSO_USERNAME.get())
                            .withPassword(ElectronicBilling.RECORDMGTM_SSO_PWD.get())
                            .build();
        }
        return ssoClient;
    }
}
