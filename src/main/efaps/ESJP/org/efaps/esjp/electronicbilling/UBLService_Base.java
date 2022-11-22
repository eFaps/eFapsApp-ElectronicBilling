/*
 * Copyright 2003 - 2022 The eFaps Team
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
package org.efaps.esjp.electronicbilling;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Checkout;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIEBilling;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.electronicbilling.entities.AllowanceEntry;
import org.efaps.esjp.electronicbilling.entities.ChargeEntry;
import org.efaps.esjp.electronicbilling.util.ElectronicBilling;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.esjp.sales.tax.Tax_Base;
import org.efaps.esjp.sales.tax.xml.Taxes;
import org.efaps.ubl.Signing;
import org.efaps.ubl.documents.AbstractDocument;
import org.efaps.ubl.documents.Customer;
import org.efaps.ubl.documents.IAllowanceChargeEntry;
import org.efaps.ubl.documents.ICustomer;
import org.efaps.ubl.documents.IInstallment;
import org.efaps.ubl.documents.ILine;
import org.efaps.ubl.documents.IPaymentTerms;
import org.efaps.ubl.documents.ITaxEntry;
import org.efaps.ubl.documents.Invoice;
import org.efaps.ubl.documents.Line;
import org.efaps.ubl.documents.Supplier;
import org.efaps.ubl.dto.SignResponseDto;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("6c3fda6b-7d54-41ca-bd7e-5a8531d2383e")
@EFapsApplication("eFapsApp-ElectronicBilling")
public abstract class UBLService_Base extends FiscusMapper
{

    private static final Logger LOG = LoggerFactory.getLogger(UBLService.class);

    public Return ceateUBL(final Parameter _parameter) throws EFapsException
    {
        final var instance = _parameter.getInstance();
        final var eval = EQL.builder().print(instance)
                        .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract).instance().as("docInstance")
                        .evaluate();
        final Instance docInstance = eval.get("docInstance");
        if (InstanceUtils.isType(docInstance, CISales.Invoice)) {
            ceateInvoice(docInstance);
        }
        return new Return();
    }

    public Invoice ceateInvoice(final Instance instance)
        throws EFapsException
    {
        final var ublInvoice = new Invoice();
        final var ubl = fill(instance, ublInvoice);
        final var ublXml = ubl.getUBLXml();
        LOG.info("UBL: {}", ublXml);
        final var signResponse = sign(ublXml);
        LOG.info("signResponse: Hash {}\n UBL {}", signResponse.getHash(), signResponse.getUbl());
        return ublInvoice;
    }

    protected AbstractDocument<?> fill(final Instance docInstance,
                                       final AbstractDocument<?> ubl)
        throws EFapsException
    {
        final var eval = EQL.builder().print(docInstance)
                        .attribute(CISales.DocumentSumAbstract.Name, CISales.DocumentSumAbstract.Taxes,
                                   CISales.DocumentSumAbstract.RateCurrencyId, CISales.DocumentSumAbstract.Date,
                                   CISales.DocumentSumAbstract.RateCrossTotal, CISales.DocumentSumAbstract.RateNetTotal)
                        .linkto(CISales.DocumentSumAbstract.Contact).instance().as("contactInstance")
                        .evaluate();

        final var taxes = eval.<Taxes>get(CISales.DocumentSumAbstract.Taxes);

        final var allowancesCharges = getCharges(taxes, false);
        allowancesCharges.addAll(getAllowances(docInstance));

        final var currencyInst = CurrencyInst.get(eval.<Long>get(CISales.DocumentSumAbstract.RateCurrencyId));
        final LocalDate date = eval.get(CISales.DocumentSumAbstract.Date);
        final BigDecimal crossTotal = eval.get(CISales.DocumentSumAbstract.RateCrossTotal);
        final var paymentMethod = getPaymentMethod(docInstance);

        ubl.withNumber(eval.get(CISales.DocumentSumAbstract.Name))
            .withCurrency(currencyInst.getISOCode())
            .withDate(date)
            .withCrossTotal(crossTotal)
            .withNetTotal(eval.get(CISales.DocumentSumAbstract.RateNetTotal))
            .withSupplier(getSupplier())
            .withCustomer(getCustomer(eval.get("contactInstance")))
            .withAllowancesCharges(allowancesCharges)
            .withLines(getLines(docInstance))
            .withPaymentTerms(new IPaymentTerms()
            {

                @Override
                public boolean isCredit()
                {
                    return !paymentMethod.isCash();
                }

                @Override
                public BigDecimal getTotal()
                {
                    return crossTotal;
                }

                @Override
                public List<IInstallment> getInstallments()
                {
                    // TODO
                    return null;
                }
            });
        return ubl;
    }

    protected List<IAllowanceChargeEntry> getCharges(final Taxes taxes,
                                                     final boolean isItem)
        throws EFapsException
    {
        final var ret = new ArrayList<IAllowanceChargeEntry>();
        final var taxproperties = ElectronicBilling.TAXMAPPING.get();
        for (final var entry : taxes.getEntries()) {
            final var taxKey = entry.getUUID();
            if (taxproperties.containsKey("charge." + taxKey + ".id")) {
                final var id = taxproperties.getProperty("charge." + taxKey + ".id");
                final var isGlobal = "true".equalsIgnoreCase(taxproperties.getProperty("charge." + taxKey + ".global"));
                if (!(isGlobal && isItem)) {
                    final var tax = Tax_Base.get(entry.getCatUUID(), entry.getUUID());
                    ret.add(ChargeEntry.builder()
                                    .withAmount(entry.getAmount())
                                    .withBaseAmount(entry.getBase())
                                    .withReason(id)
                                    .withFactor(tax.getFactor())
                                    .build());
                }
            }
        }
        return ret;
    }

    // discounts are added as a line --> convert that into a global discount
    protected List<IAllowanceChargeEntry> getAllowances(final Instance docInstance)
        throws EFapsException
    {
        final var ret = new ArrayList<IAllowanceChargeEntry>();
        final var eval = EQL.builder()
                        .print()
                        .query(CISales.PositionSumAbstract)
                        .where()
                        .attribute(CISales.PositionSumAbstract.DocumentAbstractLink).eq(docInstance)
                        .select()
                        .attribute(CISales.PositionSumAbstract.RateCrossPrice, CISales.PositionSumAbstract.RateNetPrice)
                        .evaluate();

        var total = BigDecimal.ZERO;
        var discount = BigDecimal.ZERO;
        while (eval.next()) {
            final BigDecimal crossPrice = eval.get(CISales.PositionSumAbstract.RateCrossPrice);
            final BigDecimal netPrice = eval.get(CISales.PositionSumAbstract.RateNetPrice);

            if (crossPrice.compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(netPrice);
            } else {
                discount = discount.add(netPrice.abs());
            }
        }
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            ret.add(AllowanceEntry.builder()
                            .withAmount(discount)
                            .withBaseAmount(total)
                            // Catalogo 53
                            // Descuentos globales que afectan la base imponible
                            // del IGV/IVAP
                            .withReason("02")
                            .withFactor(discount.divide(total, RoundingMode.HALF_UP))
                            .build());
        }
        return ret;
    }


    protected Supplier getSupplier() throws EFapsException
    {
        final var ret = new Supplier();
        ret.setDoiType("6");
        ret.setDOI(ERP.COMPANY_TAX.get());
        ret.setName(ERP.COMPANY_NAME.get());
        ret.setStreetName(ERP.COMPANY_STREET.get());
        ret.setUbigeo(ERP.COMPANY_UBIGEO.get());
        ret.setCountry(ERP.COMPANY_COUNTRY.get());
        ret.setAnexo(ERP.COMPANY_ESTABLECIMIENTO.get());
        ret.setDistrict(ERP.COMPANY_DISTRICT.get());
        return ret;
    }

    protected ICustomer getCustomer(final Instance contanctInstance)
        throws EFapsException
    {
        final var eval = EQL.builder().print(contanctInstance)
                        .attribute(CIContacts.ContactAbstract.Name)
                        .clazz(CIContacts.ClassOrganisation).attribute(CIContacts.ClassOrganisation.TaxNumber)
                        .as("taxNumber")
                        .clazz(CIContacts.ClassPerson).linkto(CIContacts.ClassPerson.DOITypeLink)
                        .attribute(CIContacts.AttributeDefinitionDOIType.MappingKey).as("doiType")
                        .clazz(CIContacts.ClassPerson).attribute(CIContacts.ClassPerson.IdentityCard).as("identityCard")
                        .evaluate();
        final var taxNumber = eval.<String>get("taxNumber");
        final var identityCard = eval.<String>get("identityCard");
        String doiType;
        if (taxNumber != null) {
            doiType = "6";
        } else {
            doiType = eval.<String>get("doiType");
        }
        final Customer ret = new Customer();
        ret.setDOI(taxNumber == null ? identityCard : taxNumber);
        ret.setDoiType(doiType);
        ret.setName(eval.get(CIContacts.ContactAbstract.Name));
        return ret;
    }


    protected ArrayList<ILine> getLines(final Instance docInstance)
        throws EFapsException
    {
        final var ret = new ArrayList<ILine>();

        final var eval = EQL.builder()
                        .print()
                        .query(CISales.PositionSumAbstract)
                        .where()
                        .attribute(CISales.PositionSumAbstract.DocumentAbstractLink).eq(docInstance)
                        .select()
                        .attribute(CISales.PositionSumAbstract.Quantity, CISales.PositionSumAbstract.ProductDesc,
                                        CISales.PositionSumAbstract.RateNetUnitPrice,
                                        CISales.PositionSumAbstract.RateCrossUnitPrice,
                                        CISales.PositionSumAbstract.RateCrossPrice,
                                        CISales.PositionSumAbstract.RateNetPrice,
                                        CISales.PositionSumAbstract.PositionNumber, CISales.PositionSumAbstract.UoM,
                                        CISales.PositionSumAbstract.RateTaxes)
                        .linkto(CISales.PositionSumAbstract.Product).attribute(CIProducts.ProductAbstract.Name)
                        .as("prodName")
                        .orderBy(CISales.PositionSumAbstract.PositionNumber)
                        .evaluate();

        while (eval.next()) {
            final var uomId = eval.<Long>get(CISales.PositionSumAbstract.UoM);
            final var taxes = eval.<Taxes>get(CISales.PositionSumAbstract.RateTaxes);
            ret.add(Line.builder()
                            .withQuantity(eval.get(CISales.PositionSumAbstract.Quantity))
                            .withSku(eval.get("prodName"))
                            .withDescription(eval.get(CISales.PositionSumAbstract.ProductDesc))
                            .withNetUnitPrice(eval.get(CISales.PositionSumAbstract.RateNetUnitPrice))
                            .withCrossUnitPrice(eval.get(CISales.PositionSumAbstract.RateCrossUnitPrice))
                            .withNetPrice(eval.get(CISales.PositionSumAbstract.RateNetPrice))
                            .withCrossPrice(eval.get(CISales.PositionSumAbstract.RateCrossPrice))
                            .withUoMCode(Dimension.getUoM(uomId).getCommonCode())
                            .withTaxEntries(getTaxes(taxes))
                            .withAllowancesCharges(getCharges(taxes, true))
                            .build());
        }
        return ret;
    }

    protected List<ITaxEntry> getTaxes(final Taxes taxes)
        throws EFapsException
    {
        final var ret = new ArrayList<ITaxEntry>();
        for (final var entry : taxes.getEntries()) {
            if (getTaxProperty(entry.getUUID(), ".id") != null) {
                final var code = getTaxProperty(entry.getUUID(), "id");
                final var name = getTaxProperty(entry.getUUID(), "nombre");
                final var id = getTaxProperty(entry.getUUID(), "sunat-id");
                final var taxExemptionReasonCode = getTaxProperty(entry.getUUID(), "afectacion-igv");
                org.efaps.ubl.documents.TaxType taxType;
                final var tax = Tax_Base.get(entry.getCatUUID(), entry.getUUID());

                switch (tax.getTaxType()) {
                    case PERUNIT:
                        taxType = org.efaps.ubl.documents.TaxType.PERUNIT;
                        break;
                    case ADVALOREM:
                    default:
                        taxType = org.efaps.ubl.documents.TaxType.ADVALOREM;
                        break;
                }
                ret.add(org.efaps.esjp.electronicbilling.entities.TaxEntry.builder()
                                .withTaxType(taxType)
                                .withTaxExemptionReasonCode(taxExemptionReasonCode)
                                .withAmount(entry.getAmount())
                                .withTaxableAmount(entry.getBase())
                                .withPercent(tax.getFactor().multiply(new BigDecimal(100)))
                                .withName(name)
                                .withCode(code)
                                .withId(id)
                                .build());
            }
        }
        return ret;
    }

    public SignResponseDto sign(final String ublXml)
        throws EFapsException
    {
        return new UBlSigning()
                        .withKeyAlias(ElectronicBilling.KEYSTORE_ALIAS.get())
                        .withKeyStorePwd(ElectronicBilling.KEYSTORE_PWD.get())
                        .withKeyPwd(ElectronicBilling.KEYSTORE_KEYPWD.get())
                        .signDocument(ublXml);
    }

    public static class UBlSigning
        extends Signing
    {

        @Override
        protected KeyStore.PrivateKeyEntry getKeyEntry()
        {
            KeyStore.PrivateKeyEntry ret = null;
            try {
                final var checkout = new Checkout(ElectronicBilling.KEYSTORE.get());
                final var inputStream = checkout.execute();
                final KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(inputStream, getKeyStorePwd().toCharArray());
                ret = (KeyStore.PrivateKeyEntry) ks.getEntry(getKeyAlias(),
                                new KeyStore.PasswordProtection(getKeyPwd().toCharArray()));
            } catch (KeyStoreException | NoSuchAlgorithmException | java.security.cert.CertificateException
                            | java.security.UnrecoverableEntryException | java.io.IOException | EFapsException e) {
                LOG.error("Catched", e);
            }
            return ret;
        }
    }

}
