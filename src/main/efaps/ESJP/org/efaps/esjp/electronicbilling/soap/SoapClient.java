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
package org.efaps.esjp.electronicbilling.soap;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIEBilling;
import org.efaps.esjp.common.file.FileUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.electronicbilling.UBLService;
import org.efaps.esjp.electronicbilling.soap.dto.ResponseEnvelope;
import org.efaps.esjp.electronicbilling.soap.dto.SendBillResponse;
import org.efaps.esjp.electronicbilling.soap.dto.SendBillResponseBody;
import org.efaps.esjp.electronicbilling.util.ElectronicBilling;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;


@EFapsUUID("a6992c5b-4a26-4758-b91e-79b9fc951546")
@EFapsApplication("eFapsApp-ElectronicBilling")
public class SoapClient
{

    private static final Logger LOG = LoggerFactory.getLogger(SoapClient.class);
    private static final String prefixUri = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-";
    private static final String uri = prefixUri + "wssecurity-secext-1.0.xsd";
    private static final String uta = prefixUri + "wssecurity-utility-1.0.xsd";
    private static final String ta = prefixUri + "username-token-profile-1.0#PasswordText";

    public void sendBill(final Instance eDocInst,
                         final String documentType,
                         final String docName,
                         final String ubl)
        throws EFapsException
    {
        final var fileName = ERP.COMPANY_TAX.get() + "-" + documentType + "-" + docName;
        final var requestFile = zip(ubl, fileName);

        final var response = sendBill(eDocInst, requestFile);
        if (response != null) {
            final var crdFile = unzip(response.getApplicationResponse());
            final var service = new UBLService();
            service.checkInApplicationResponse(eDocInst, crdFile);
            service.evalApplicationResponse(eDocInst, crdFile, CIEBilling.LogResponse);
        }
    }

    protected File unzip(final String content)
        throws EFapsException
    {
        File file = null;
        try {
            final var responseZipfile = new FileUtil().getFile("response.zip");
            FileUtils.writeByteArrayToFile(responseZipfile, Base64.decodeBase64(content.getBytes()));
            final byte[] buffer = new byte[1024];
            final ZipInputStream zis = new ZipInputStream(new FileInputStream(responseZipfile));
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                file = new FileUtil().getFile(zipEntry.getName());
                final FileOutputStream fos = new FileOutputStream(file);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                zipEntry = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();

        } catch (final IOException e) {
            throw new EFapsException("catched", e);
        }
        return file;
    }

    protected File zip(final String ubl,
                       final String fileName)
        throws EFapsException
    {
        final var ublStream = new ByteArrayInputStream(ubl.getBytes());
        final var file = new FileUtil().getFile(fileName + ".ZIP");
        try {
            final var fos = new FileOutputStream(file);
            final var zipOut = new ZipOutputStream(fos);

            final ZipEntry zipEntry = new ZipEntry(fileName + ".XML");
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

    public SendBillResponse sendBill(final Instance eDocInst,
                                     final File zipFile)
        throws EFapsException
    {
        SendBillResponse ret = null;
        try {
            String endpoint;
            if (InstanceUtils.isType(eDocInst, CIEBilling.DeliveryNote)) {
                endpoint =  ElectronicBilling.DELIVERYNOTE_ENDPOINTURI.get();
            } else {
                endpoint = "TODO";
            }
            final var msg = sendBillMessage(zipFile);
            final var body = callSOAPAction(endpoint, msg,
                            new TypeReference<ResponseEnvelope<SendBillResponseBody>>()
                            {
                            });
            if (body.getFault() != null) {
                EQL.builder().insert(CIEBilling.Log)
                                .set(CIEBilling.Log.DocumentLinkAbstract, eDocInst)
                                .set(CIEBilling.Log.Content, body.getFault().toString())
                                .stmt()
                                .execute();
            } else {
                ret = body.getSendBillResponse();
            }
        } catch (final SOAPException | UnsupportedOperationException | IOException e) {
            LOG.error("Catched", e);
        }
        return ret;
    }

    public SOAPMessage sendBillMessage(final File zipFile)
        throws SOAPException, EFapsException, IOException
    {
        final var messageFactory = MessageFactory.newInstance();
        final var soapMessage = messageFactory.createMessage();
        final var soapPart = soapMessage.getSOAPPart();

        final var ns = "tns";
        final var myNamespaceURI = "http://service.sunat.gob.pe";

        final SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration(ns, myNamespaceURI);
        addAuthHeader(envelope);

        final SOAPBody soapBody = envelope.getBody();
        final SOAPElement bodyRootElement = soapBody.addChildElement("sendBill", ns);
        bodyRootElement.addChildElement("fileName").addTextNode(zipFile.getName());
        final byte[] encoded = Base64.encodeBase64(FileUtils.readFileToByteArray(zipFile));
        bodyRootElement.addChildElement("contentFile").addTextNode(new String(encoded, StandardCharsets.UTF_8));
        return soapMessage;
    }

    protected void addAuthHeader(SOAPEnvelope envelope)
        throws SOAPException, EFapsException
    {

        final var userName = ElectronicBilling.DELIVERYNOTE_SOAPUSER.get();
        final var pwd = ElectronicBilling.DELIVERYNOTE_SOAPPWD.get();

        final SOAPFactory factory = SOAPFactory.newInstance();
        final String prefix = "wsse";
        final SOAPElement securityElem = factory.createElement("Security", prefix, uri);
        final SOAPElement tokenElem = factory.createElement("UsernameToken", prefix, uri);
        tokenElem.addAttribute(QName.valueOf("wsu:Id"), "UsernameToken-2");
        tokenElem.addAttribute(QName.valueOf("xmlns:wsu"), uta);
        final SOAPElement userElem = factory.createElement("Username", prefix, uri);
        userElem.addTextNode(userName);
        final SOAPElement pwdElem = factory.createElement("Password", prefix, uri);
        pwdElem.addTextNode(pwd);
        pwdElem.addAttribute(QName.valueOf("Type"), ta);
        tokenElem.addChildElement(userElem);
        tokenElem.addChildElement(pwdElem);
        securityElem.addChildElement(tokenElem);
        final SOAPHeader header = envelope.getHeader();
        header.addChildElement(securityElem);
    }

    public <T> T callSOAPAction(final String endpointUrl,
                                final SOAPMessage _message,
                                final TypeReference<ResponseEnvelope<T>> type)
        throws UnsupportedOperationException, SOAPException, EFapsException, IOException
    {
        logMessage("Request SOAP Message:", _message);
        final var connectionFactory = SOAPConnectionFactory.newInstance();
        final var soapConnection = connectionFactory.createConnection();
        final var soapResponse = soapConnection.call(_message, endpointUrl);
        logMessage("Response SOAP Message:", soapResponse);

        final var out = new ByteArrayOutputStream();
        soapResponse.writeTo(out);

        final XmlMapper mapper = new XmlMapper();
        final var responseEnvelope = mapper.readValue(out.toByteArray(), type);
        return responseEnvelope.getBody();
    }

    public void logMessage(final String title,
                           final SOAPMessage _message)
        throws SOAPException, IOException
    {
        LOG.info(title);
        final var out = new ByteArrayOutputStream();
        _message.writeTo(out);
        LOG.info(out.toString(StandardCharsets.UTF_8));
    }

    public void getStatus(final String documentType, final String docName)
    {
        // TODO Auto-generated method stub

    }
}
