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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.db.SelectBuilder;
import org.efaps.eql.IEsjpSelect;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.contacts.Contacts;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.esjp.sales.tax.xml.TaxEntry;
import org.efaps.esjp.sales.tax.xml.Taxes;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("eacf78ee-a1e8-4838-85e0-074394b19083")
@EFapsApplication("eFapsApp-ElectronicBilling")
public abstract class QRCodeSelect_Base
    implements IEsjpSelect
{

    private static final Logger LOG = LoggerFactory.getLogger(QRCodeSelect.class);
    private static final Pattern SERIALPATTERN = Pattern.compile("^.*(?=-)");
    private static final Pattern NUMBERPATTERN = Pattern.compile("(?<=-).*");

    /**
     * RUC | TIPO DE DOCUMENTO | SERIE | NUMERO | MTO TOTAL IGV | MTO TOTAL DEL COMPROBANTE | FECHA DE EMISION |TIPO DE
     * DOCUMENTO ADQUIRENTE | NUMERO DE DOCUMENTO ADQUIRENTE
     *
     *
     * (non-Javadoc)
     *
     * @see org.efaps.eql.IEsjpSelect#getValue(org.efaps.db.Instance)
     */

    @Override
    public Object getValue(final Instance _docInstance)
        throws EFapsException
    {
        final List<String> values = new ArrayList<>();
        final String ruc = ERP.COMPANY_TAX.get();
        values.add(ruc);

        final PrintQuery print = new PrintQuery(_docInstance);
        final SelectBuilder selDocType = SelectBuilder.get().linkfrom(CISales.Document2DocumentType.DocumentLink)
                        .linkto(CISales.Document2DocumentType.DocumentTypeLink).attribute(CIERP.DocumentType.Name);
        final SelectBuilder selContactInst = SelectBuilder.get()
                        .linkto(CISales.DocumentSumAbstract.Contact).instance();
        print.addSelect(selDocType, selContactInst);
        print.addAttribute(CIERP.DocumentAbstract.Date, CIERP.DocumentAbstract.Name,
                        CISales.DocumentSumAbstract.RateTaxes,
                        CISales.DocumentSumAbstract.RateCrossTotal, CISales.DocumentSumAbstract.CrossTotal);
        print.executeWithoutAccessCheck();

        final String docTypeTmp = print.getSelect(selDocType);
        final String docType = docTypeTmp == null ? getDocumentType4Document(_docInstance) : docTypeTmp;
        values.add(docType);
        values.add(getSerial(print.getAttribute(CIERP.DocumentAbstract.Name)));
        values.add(getNumber(print.getAttribute(CIERP.DocumentAbstract.Name)));

        final Taxes taxes = print.getAttribute(CISales.DocumentSumAbstract.RateTaxes);
        final Optional<TaxEntry> optEntry = taxes.getEntries().stream()
                        .filter(entry -> entry.getUUID().toString()
                                        .equals("06e40be6-40d8-44f4-9d8f-585f2f97ce63"))
                        .findFirst();
        if (optEntry.isPresent()) {
            values.add(optEntry.get().getAmount().setScale(2, RoundingMode.HALF_UP).toString());
        } else {
            values.add("0");
        }
        values.add(print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal)
                        .setScale(2, RoundingMode.HALF_UP).toString());
        values.add(print.<DateTime>getAttribute(CIERP.DocumentAbstract.Date).toString("yyyy-MM-dd"));

        final Instance contactInst = print.getSelect(selContactInst);
        final BigDecimal localCrossTotal = print.getAttribute(CISales.DocumentSumAbstract.CrossTotal);
        final ContactInfo contactInfo = getContactInfo4ContactInst(contactInst,
                        localCrossTotal.compareTo(BigDecimal.valueOf(700)) < 0, docType);
        values.add(contactInfo.getTipoDocumento());
        values.add(contactInfo.getNumeroDocumento());
        return StringUtils.join(values, "|");
    }

    protected String getDocumentType4Document(final Instance _docInst)
    {
        String ret;
        if (_docInst.getType().isCIType(CISales.Invoice)) {
            ret = "01";
        } else if (_docInst.getType().isCIType(CISales.Receipt)) {
            ret = "03";
        } else if (_docInst.getType().isCIType(CISales.CreditNote)) {
            ret = "07";
        } else if (_docInst.getType().isCIType(CISales.Reminder)) {
            ret = "08";
        } else {
            ret = "UNKOWN";
        }
        return ret;
    }

    protected String getSerial(final String _name)
    {
        String ret = "";
        final Matcher m = SERIALPATTERN.matcher(_name);
        if (m.find()) {
            ret = m.group();
        }
        return ret;
    }

    protected String getNumber(final String _name)
    {
        String ret = "";
        final Matcher m = NUMBERPATTERN.matcher(_name);
        if (m.find()) {
            ret = m.group();
        }
        return ret;
    }

    public ContactInfo getContactInfo4ContactInst(final Instance _contactInst,
                                                  final boolean _allowAnonymous,
                                                  final String _docType)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_contactInst);
        final SelectBuilder selTaxNumber = SelectBuilder.get().clazz(CIContacts.ClassOrganisation)
                        .attribute(CIContacts.ClassOrganisation.TaxNumber);
        final SelectBuilder selIDCard = SelectBuilder.get().clazz(CIContacts.ClassPerson)
                        .attribute(CIContacts.ClassPerson.IdentityCard);
        final SelectBuilder selIDCardType = SelectBuilder.get().clazz(CIContacts.ClassPerson)
                        .linkto(CIContacts.ClassPerson.DOITypeLink)
                        .attribute(CIContacts.AttributeDefinitionDOIType.MappingKey);

        print.addSelect(selTaxNumber, selIDCard, selIDCardType);
        print.executeWithoutAccessCheck();
        final String taxNumber = print.getSelect(selTaxNumber);
        final String idCard = print.getSelect(selIDCard);
        final String idCardType = print.getSelect(selIDCardType);

        ContactInfo ret = new ContactInfo();
        if (StringUtils.isNotEmpty(taxNumber)) {
            ret = new ContactInfo()
                            .setNumeroDocumento(taxNumber)
                            .setTipoDocumento("6");
        } else if (StringUtils.isNotEmpty(idCard) && StringUtils.isNotEmpty(idCardType)) {
            ret = new ContactInfo()
                            .setNumeroDocumento(idCard)
                            .setTipoDocumento(idCardType);
        } else if (_allowAnonymous && ("03".equals(_docType) || "07".equals(_docType))) {
            ret = new ContactInfo()
                            .setNumeroDocumento("0")
                            .setTipoDocumento("-");
        }
        if (Contacts.isForeign(ParameterUtil.instance(), _contactInst)) {
            ret = new ContactInfo()
                            .setNumeroDocumento("0")
                            .setTipoDocumento("-");
        }
        return ret;
    }

    @Override
    public void initialize(final List<Instance> _instances,
                           final String... _arguments)
        throws EFapsException
    {
        LOG.debug("initialize");
    }

    public static class ContactInfo
    {

        public String getTipoDocumento()
        {
            return tipoDocumento;
        }

        public String getNumeroDocumento()
        {
            return numeroDocumento;
        }

        private String tipoDocumento;

        private String numeroDocumento;

        public ContactInfo setTipoDocumento(final String _tipoDocumento)
        {
            tipoDocumento = _tipoDocumento;
            return this;
        }

        public ContactInfo setNumeroDocumento(final String _numeroDocumento)
        {
            numeroDocumento = _numeroDocumento;
            return this;
        }

    }

}
