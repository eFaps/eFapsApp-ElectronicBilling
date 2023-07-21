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
package org.efaps.esjp.electronicbilling.fiscus.client.rest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.common.file.FileUtil;
import org.efaps.esjp.electronicbilling.fiscus.client.dto.ArchiveDto;
import org.efaps.esjp.electronicbilling.fiscus.client.dto.DeliveryNoteRequestDto;
import org.efaps.esjp.electronicbilling.util.ElectronicBilling;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("c5bc9132-1f8a-44da-8780-3f63411a6607")
@EFapsApplication("eFapsApp-ElectronicBilling")
public class DeliveryNoteClient
    extends AbstractRestClient
{

    private static final Logger LOG = LoggerFactory.getLogger(DeliveryNoteClient.class);

    public Response sendUbl(final String documentType,
                            final String docName,
                            final String ubl)
        throws EFapsException
    {
        final var fileName = ERP.COMPANY_TAX.get() + "-" + documentType + "-" + docName;
        final var zipFile = zip(ubl, fileName);

        final var base64zip = getBase64Zip(zipFile);
        // {numRucEmisor}-{codCpe}-{numSerie}-{numCpe}
        final var request = getClient().target(ElectronicBilling.DELIVERYNOTE_ENDPOINTURI.get())
                        .path(fileName)
                        .request(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + getToken());
        final var dto = DeliveryNoteRequestDto.builder()
                        .withArchive(ArchiveDto.builder()
                                        .withName(fileName + ".zip")
                                        .withBase64Zip(base64zip)
                                        .withHashZip(ubl)
                                        .build())
                        .build();
        final var response = request.post(Entity.json(dto));
        if (response.getStatusInfo().equals(Status.OK)) {
            LOG.info("Response: {}", response.getEntity());
        } else {
            LOG.error("Error response: {}", response.getEntity());
        }
        return response;
    }

    protected String getHashSha256(File zipFile)
    {
        byte[] sha256 = null;
        try {
            sha256 = DigestUtils.sha256(new FileInputStream(zipFile));
        } catch (final IOException e) {
            LOG.error("Catched", e);
        }
        return new String(sha256, StandardCharsets.UTF_8);
    }

    protected String getBase64Zip(File zipFile)
    {
        byte[] encoded = null;
        try {
            encoded = Base64.encodeBase64(FileUtils.readFileToByteArray(zipFile));
        } catch (final IOException e) {
            LOG.error("Catched", e);
        }
        return new String(encoded, StandardCharsets.UTF_8);
    }

    protected File zip(String ubl,
                       String fileName)
        throws EFapsException
    {
        final var ublStream = new ByteArrayInputStream(ubl.getBytes());
        final var file = new FileUtil().getFile(fileName + ".zip");
        try {
            final var fos = new FileOutputStream(file);
            final var zipOut = new ZipOutputStream(fos);

            final ZipEntry zipEntry = new ZipEntry(fileName + ".xml");
            zipOut.putNextEntry(zipEntry);

            final byte[] bytes = new byte[1024];
            int length;
            while ((length = ublStream.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            zipOut.close();
        } catch (final IOException e) {
            throw new EFapsException("catched", e);
        }
        return file;
    }
}
