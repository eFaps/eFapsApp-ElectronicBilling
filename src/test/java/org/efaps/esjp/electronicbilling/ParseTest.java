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
package org.efaps.esjp.electronicbilling;

import org.efaps.ubl.reader.ApplicationResponseReader;
import org.testng.annotations.Test;

public class ParseTest
{
    @Test
    public void parseApplicationResponse() {
        final var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ApplicationResponse xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:ApplicationResponse-2\" xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\" xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\" xmlns:ccts=\"urn:un:unece:uncefact:documentation:2\" xmlns:ext=\"urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2\" xmlns:ns5=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n"
                        + "    <ext:UBLExtensions>\n"
                        + "        <ext:UBLExtension>\n"
                        + "            <ext:ExtensionContent><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" Id=\"signatureKG\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/><ds:Reference URI=\"\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>SOAyHGt/n6NHierK8V9aAA763eg=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>R1CELgrKoW7pTi3FXm9qyDJ8hk8rhmbewFDbFl7G8c9paK9Jo3NJf3bERhhAYtO8EmhR8GbxBpN4NYZVfNjUMKwT8BUXTkCAnlRXGw1cR7Z+7diWtOIkFl8V0Ro/QWN4Rz1EBtVDoNVUfpSAvayAELpQNiVFbBMgSSogVs7fZo3+vD51LIMCU4oCbf0D4zg2A43R324D+w3AwQdnrxcWF25VdKBWZnRjdnljFECQJ6QzolzULHmgys8sWz6m2Up9GEvwkMn4iuvYoMmvzo4Somb2pywEToVVSudU9L73MSFwZ9nU7zhTIH9w9LWIT8Iu3MDOgcegxO9YUiQSQPLKfQ==</ds:SignatureValue><ds:KeyInfo><ds:X509Data><ds:X509Certificate>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</ds:X509Certificate></ds:X509Data></ds:KeyInfo></ds:Signature></ext:ExtensionContent>\n"
                        + "        </ext:UBLExtension>\n"
                        + "    </ext:UBLExtensions>\n"
                        + "    <cbc:UBLVersionID>2.1</cbc:UBLVersionID>\n"
                        + "    <cbc:CustomizationID>1.0</cbc:CustomizationID>\n"
                        + "    <cbc:ID>9179f882-2b5c-47d8-815f-b681fa67479d</cbc:ID>\n"
                        + "    <cbc:IssueDate>2023-06-02</cbc:IssueDate>\n"
                        + "    <cbc:IssueTime>14:21:49.00063</cbc:IssueTime>\n"
                        + "    <cbc:ResponseDate>2023-06-02</cbc:ResponseDate>\n"
                        + "    <cbc:ResponseTime>14:21:49.00796</cbc:ResponseTime>\n"
                        + "    <cac:SenderParty>\n"
                        + "        <cac:PartyLegalEntity>\n"
                        + "            <cbc:CompanyID schemeAgencyName=\"PE:SUNAT\" schemeID=\"6\" schemeURI=\"urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo6\">20546153372</cbc:CompanyID>\n"
                        + "        </cac:PartyLegalEntity>\n"
                        + "    </cac:SenderParty>\n"
                        + "    <cac:ReceiverParty>\n"
                        + "        <cac:PartyLegalEntity>\n"
                        + "            <cbc:CompanyID schemeAgencyName=\"PE:SUNAT\" schemeID=\"6\" schemeURI=\"urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo6\">20478005017</cbc:CompanyID>\n"
                        + "        </cac:PartyLegalEntity>\n"
                        + "    </cac:ReceiverParty>\n"
                        + "    <cac:DocumentResponse>\n"
                        + "        <cac:Response>\n"
                        + "            <cbc:ResponseCode listAgencyName=\"PE:SUNAT\">0</cbc:ResponseCode>\n"
                        + "            <cbc:Description>El comprobante numero F999-000271, ha sido aceptada</cbc:Description>\n"
                        + "            <cac:Status>\n"
                        + "                <cbc:StatusReasonCode listURI=\"urn:pe:gob:sunat:cpe:see:gem:codigos:codigoretorno\">4252</cbc:StatusReasonCode>\n"
                        + "                <cbc:StatusReason>El dato ingresado como atributo @listName es incorrecto. - Comprobantes: [[line ID - 1]]</cbc:StatusReason>\n"
                        + "            </cac:Status>\n"
                        + "            <cac:Status>\n"
                        + "                <cbc:StatusReasonCode listURI=\"urn:pe:gob:sunat:cpe:see:gem:codigos:codigoretorno\">4255</cbc:StatusReasonCode>\n"
                        + "                <cbc:StatusReason>El dato ingresado como atributo @schemeName es incorrecto. - Comprobantes: [[line ID - 1]]</cbc:StatusReason>\n"
                        + "            </cac:Status>\n"
                        + "            <cac:Status>\n"
                        + "                <cbc:StatusReasonCode listURI=\"urn:pe:gob:sunat:cpe:see:gem:codigos:codigoretorno\">4094</cbc:StatusReasonCode>\n"
                        + "                <cbc:StatusReason>La dirección completa y detallada del domicilio fiscal del emisor no cumple con el formato establecido</cbc:StatusReason>\n"
                        + "            </cac:Status>\n"
                        + "        </cac:Response>\n"
                        + "        <cac:DocumentReference>\n"
                        + "            <cbc:ID>F999-000271</cbc:ID>\n"
                        + "            <cbc:IssueDate>2023-06-01</cbc:IssueDate>\n"
                        + "            <cbc:IssueTime>00:00:00.00000</cbc:IssueTime>\n"
                        + "            <cbc:DocumentTypeCode>01</cbc:DocumentTypeCode>\n"
                        + "            <cac:Attachment>\n"
                        + "                <cac:ExternalReference>\n"
                        + "                    <cbc:DocumentHash>AYWUZ2oymSq0jeBSSPONoepmuwQ=</cbc:DocumentHash>\n"
                        + "                </cac:ExternalReference>\n"
                        + "            </cac:Attachment>\n"
                        + "        </cac:DocumentReference>\n"
                        + "        <cac:IssuerParty>\n"
                        + "            <cac:PartyLegalEntity>\n"
                        + "                <cbc:CompanyID schemeID=\"6\">20546153372</cbc:CompanyID>\n"
                        + "            </cac:PartyLegalEntity>\n"
                        + "        </cac:IssuerParty>\n"
                        + "        <cac:RecipientParty>\n"
                        + "            <cac:PartyLegalEntity>\n"
                        + "                <cbc:CompanyID schemeID=\"6\">20501946398</cbc:CompanyID>\n"
                        + "            </cac:PartyLegalEntity>\n"
                        + "        </cac:RecipientParty>\n"
                        + "    </cac:DocumentResponse>\n"
                        + "</ApplicationResponse>";
        final var reader = new ApplicationResponseReader();
        final var appResponse = reader.read(xml);
        System.out.println(appResponse);

    }

}
