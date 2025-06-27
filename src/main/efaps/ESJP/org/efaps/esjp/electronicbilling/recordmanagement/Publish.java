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
import java.net.URI;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Checkout;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIEBilling;
import org.efaps.esjp.common.file.FileUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.electronicbilling.fiscus.client.rest.AbstractRestClient;
import org.efaps.esjp.electronicbilling.util.ElectronicBilling;
import org.efaps.esjp.erp.rest.client.OAuth2Client;
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

    public Return publishDocument(final Parameter parameter)
        throws EFapsException
    {
        publishUbl("107958.468");
        return new Return();
    }

    public void publishUbl(final String oid)
        throws EFapsException
    {
        final var ublInst = Instance.get(oid);
        if (InstanceUtils.isKindOf(ublInst, CIEBilling.UBLFileAbstract)) {
            final var ssoClient = OAuth2Client.builder()
                            .withTarget(URI.create(ElectronicBilling.RECORDMGTM_SSO_ENDPOINTURI.get()))
                            .withClientId(ElectronicBilling.RECORDMGTM_SSO_CLIENTID.get())
                            .withClientSecret(ElectronicBilling.RECORDMGTM_SSO_CLIENTSECRET.get())
                            .withUsername(ElectronicBilling.RECORDMGTM_SSO_USERNAME.get())
                            .withPassword(ElectronicBilling.RECORDMGTM_SSO_PWD.get())
                            .build();
            final var token = ssoClient.getToken();

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

                final var multipart = new FormDataMultiPart()
                                .field("clientId", "11111111111")
                                .field("name", "F191-000000815")
                                .field("date", "2025-07-27")
                                .field("total", "10.99")
                                .bodyPart(filePart);

                final var response = request.buildPost(Entity.entity(multipart, multipart.getMediaType()))
                                .invoke(new GenericType<>()
                                {
                                });
                LOG.info("{}", response);
            } catch (EFapsException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        } else {
            LOG.warn("Invalid oid: {}", oid);
        }
    }
}
