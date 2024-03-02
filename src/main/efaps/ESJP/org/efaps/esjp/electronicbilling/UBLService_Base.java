/**
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
package org.efaps.esjp.electronicbilling;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Checkin;
import org.efaps.db.Checkout;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.stmt.selection.Evaluator;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIEBilling;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.ci.CIUbicaciones;
import org.efaps.esjp.common.file.FileUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.electronicbilling.entities.AllowanceEntry;
import org.efaps.esjp.electronicbilling.entities.ChargeEntry;
import org.efaps.esjp.electronicbilling.util.ElectronicBilling;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.esjp.products.Conversion;
import org.efaps.esjp.products.util.ConversionType;
import org.efaps.esjp.products.util.Products;
import org.efaps.esjp.sales.tax.Tax_Base;
import org.efaps.esjp.sales.tax.xml.Taxes;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.Sales.TaxRetention;
import org.efaps.ubl.Signing;
import org.efaps.ubl.documents.AbstractDocument;
import org.efaps.ubl.documents.CreditNote;
import org.efaps.ubl.documents.DeliveryNote;
import org.efaps.ubl.documents.Invoice;
import org.efaps.ubl.documents.Receipt;
import org.efaps.ubl.documents.elements.AdditionalItemProperty;
import org.efaps.ubl.documents.elements.Carrier;
import org.efaps.ubl.documents.elements.Customer;
import org.efaps.ubl.documents.elements.Delivery;
import org.efaps.ubl.documents.elements.Driver;
import org.efaps.ubl.documents.elements.Equipment;
import org.efaps.ubl.documents.elements.Line;
import org.efaps.ubl.documents.elements.Reference;
import org.efaps.ubl.documents.elements.Shipment;
import org.efaps.ubl.documents.elements.Stage;
import org.efaps.ubl.documents.elements.Supplier;
import org.efaps.ubl.documents.elements.Transport;
import org.efaps.ubl.documents.interfaces.IAddress;
import org.efaps.ubl.documents.interfaces.IAllowanceChargeEntry;
import org.efaps.ubl.documents.interfaces.ICarrier;
import org.efaps.ubl.documents.interfaces.ICustomer;
import org.efaps.ubl.documents.interfaces.IInstallment;
import org.efaps.ubl.documents.interfaces.ILine;
import org.efaps.ubl.documents.interfaces.IPaymentTerms;
import org.efaps.ubl.documents.interfaces.ITaxEntry;
import org.efaps.ubl.documents.values.CreditNoteTypeCode;
import org.efaps.ubl.documents.values.DeliveryNoteInstruction;
import org.efaps.ubl.dto.SignResponseDto;
import org.efaps.ubl.reader.ApplicationResponseReader;
import org.efaps.util.EFapsException;
import org.efaps.util.UUIDUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.MonetaryTotalType;
import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType;

@EFapsUUID("6c3fda6b-7d54-41ca-bd7e-5a8531d2383e")
@EFapsApplication("eFapsApp-ElectronicBilling")
public abstract class UBLService_Base
    extends FiscusMapper
{

    private static final Logger LOG = LoggerFactory.getLogger(UBLService.class);

    public void checkInApplicationResponse(final Instance eDocInst,
                                           final File file)
        throws EFapsException
    {
        final var fileTypeStr = ElectronicBilling.UBL_RESPONSE_FILETYPE.get();
        if (StringUtils.isNotEmpty(fileTypeStr)) {
            Type fileType;
            if (UUIDUtil.isUUID(fileTypeStr)) {
                fileType = Type.get(UUID.fromString(fileTypeStr));
            } else {
                fileType = Type.get(fileTypeStr);
            }
            final var fileInst = EQL.builder()
                            .insert(fileType)
                            .set(CIEBilling.ResponseFileAbstract.DocumentLinkAbstract, eDocInst)
                            .stmt()
                            .execute();
            try {
                final var is = new FileInputStream(file);
                final var checkin = new Checkin(fileInst);
                checkin.execute(file.getName(), is, is.available());
            } catch (final IOException e) {
                LOG.error("Catched", e);
            }
        }
    }

    public void evalApplicationResponse(final Instance eDocInst,
                                        final File file,
                                        final CIType logType)
        throws EFapsException
    {
        final var reader = new ApplicationResponseReader();
        final var appResponse = reader.read(file);
        final var response = appResponse.getDocumentResponseAtIndex(0).getResponse();
        final var responseCode = response.getResponseCodeValue();

        if ("0".equals(responseCode)) {
            final var status = Status.find(eDocInst.getType().getStatusAttribute().getLink().getUUID(), "Successful");
            EQL.builder().update(eDocInst)
                            .set(CIEBilling.DocumentAbstract.StatusAbstract, status.getId())
                            .stmt()
                            .execute();
        }

        if (logType != null) {
            final var description = response.getDescriptionAtIndex(0);

            final var content = new StringBuilder()
                            .append("ResponseCode: ").append(responseCode).append("\n")
                            .append("Description: ").append(description.getValue()).append("\n");

            response.getStatus().forEach(status -> {
                content.append("Status: ")
                                .append(status.getStatusReasonCodeValue())
                                .append(" ").append(status.getStatusReasonAtIndex(0).getValue())
                                .append("\n");
            });

            EQL.builder()
                            .insert(logType)
                            .set(CIEBilling.LogAbstract.DocumentLinkAbstract, eDocInst)
                            .set(CIEBilling.LogAbstract.Content, content)
                            .stmt()
                            .execute();
        }
    }

    public Return ceateUBL(final Parameter _parameter)
        throws EFapsException
    {
        final var ret = new Return();
        final var instance = _parameter.getInstance();
        final var eval = EQL.builder().print(instance)
                        .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract).instance().as("docInstance")
                        .evaluate();
        final Instance docInstance = eval.get("docInstance");
        LOG.info("instance {}", docInstance);
        ImmutablePair<String, File> ublTuplet = null;
        if (InstanceUtils.isType(docInstance, CISales.Invoice)) {
            ublTuplet = createInvoice(docInstance);
        } else if (InstanceUtils.isType(docInstance, CISales.CreditNote)) {
            ublTuplet = createCreditNote(docInstance);
        } else if (InstanceUtils.isType(docInstance, CISales.DeliveryNote)) {
            ublTuplet = createDeliveryNote(docInstance);
        } else if (InstanceUtils.isType(docInstance, CISales.Receipt)) {
            ublTuplet = createReceipt(docInstance);
        }
        if (ublTuplet != null) {
            checkInUBLFile(_parameter, instance, ublTuplet.getRight(), ublTuplet.left);
            ret.put(ReturnValues.VALUES, ublTuplet.getRight());
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    protected void checkInUBLFile(final Parameter _parameter,
                                  final Instance eDocInst,
                                  final File file,
                                  final String ublHash)
        throws EFapsException
    {
        if (getUBLFileType() != null) {
            final var fileInst = EQL.builder()
                            .insert(getUBLFileType())
                            .set(CIEBilling.UBLFileAbstract.DocumentLinkAbstract, eDocInst)
                            .set(CIEBilling.UBLFileAbstract.UBLHash, ublHash)
                            .stmt()
                            .execute();
            try {
                final var is = new FileInputStream(file);
                final var checkin = new Checkin(fileInst);
                checkin.execute(file.getName(), is, is.available());
            } catch (final IOException e) {
                LOG.error("Catched", e);
            }
        }
    }

    protected CIType getUBLFileType()
    {
        return CIEBilling.UBLFile;
    }

    public ImmutablePair<String, File> createReceipt(final Instance docInstance)
        throws EFapsException
    {
        ImmutablePair<String, File> response = null;
        final var ublReceipt = new Receipt().withEncoding(Charset.forName(ElectronicBilling.UBL_ENCODING.get()));
        final var ubl = fill(docInstance, ublReceipt, false);
        final var ublXml = ubl.getUBLXml();
        LOG.info("UBL: {}", ublXml);
        final var signResponse = sign(ublXml);
        LOG.info("signResponse: Hash {}\n UBL {}", signResponse.getHash(), signResponse.getUbl());
        try {
            final var file = new FileUtil().getFile(ubl.getNumber(), "xml");
            FileUtils.writeStringToFile(file, signResponse.getUbl(), StandardCharsets.UTF_8);
            response = ImmutablePair.of(signResponse.getHash(), file);
        } catch (final IOException e) {
            LOG.error("Catched", e);
        }
        return response;
    }

    public ImmutablePair<String, File> createInvoice(final Instance docInstance)
        throws EFapsException
    {
        ImmutablePair<String, File> response = null;
        final boolean freeOfCharge = isFreeOfCharge(docInstance);
        final var ublInvoice = new Invoice()
        {

            @Override
            protected MonetaryTotalType getMonetaryTotal(final InvoiceType invoice)
            {
                final var total = super.getMonetaryTotal(invoice);
                if (freeOfCharge) {
                    total.setLineExtensionAmount(BigDecimal.ZERO);
                }
                return total;
            }
        }.withEncoding(Charset.forName(ElectronicBilling.UBL_ENCODING.get()));
        final var ubl = fill(docInstance, ublInvoice, freeOfCharge);
        final var ublXml = ubl.getUBLXml();
        LOG.info("UBL: {}", ublXml);
        final var signResponse = sign(ublXml);
        LOG.info("signResponse: Hash {}\n UBL {}", signResponse.getHash(), signResponse.getUbl());
        try {
            final var file = new FileUtil().getFile(ubl.getNumber(), "xml");
            FileUtils.writeStringToFile(file, signResponse.getUbl(), StandardCharsets.UTF_8);
            response = ImmutablePair.of(signResponse.getHash(), file);
        } catch (final IOException e) {
            LOG.error("Catched", e);
        }
        return response;
    }

    public ImmutablePair<String, File> createCreditNote(final Instance docInstance)
        throws EFapsException
    {
        ImmutablePair<String, File> response = null;
        var ublCreditNote = new CreditNote().withEncoding(Charset.forName(ElectronicBilling.UBL_ENCODING.get()));
        if (ElectronicBilling.CREDITNOTE_TRYDETAILED.get()) {
            ublCreditNote = (CreditNote) fill(docInstance, ublCreditNote, false);
        } else {
            ublCreditNote = fillCreditNote(docInstance, ublCreditNote);
        }
        final var ublXml = ublCreditNote.getUBLXml();
        LOG.info("UBL: {}", ublXml);
        final var signResponse = sign(ublXml);
        LOG.info("signResponse: Hash {}\n UBL {}", signResponse.getHash(), signResponse.getUbl());
        try {
            final var file = new FileUtil().getFile(ublCreditNote.getNumber(), "xml");
            FileUtils.writeStringToFile(file, signResponse.getUbl(), StandardCharsets.UTF_8);
            response = ImmutablePair.of(signResponse.getHash(), file);
        } catch (final IOException e) {
            LOG.error("Catched", e);
        }
        return response;
    }

    public ImmutablePair<String, File> createDeliveryNote(final Instance docInstance)
        throws EFapsException
    {
        ImmutablePair<String, File> response = null;
        final var ublDeliveryNote = new DeliveryNote()
                        .withEncoding(Charset.forName(ElectronicBilling.UBL_ENCODING.get()));
        final var ubl = fillDeliveryNote(docInstance, ublDeliveryNote);
        final var ublXml = ubl.getUBLXml();
        LOG.info("UBL: {}", ublXml);
        final var signResponse = sign(ublXml);
        LOG.info("signResponse: Hash {}\n UBL {}", signResponse.getHash(), signResponse.getUbl());
        try {
            final var file = new FileUtil().getFile(ubl.getNumber(), "xml");
            FileUtils.writeStringToFile(file, signResponse.getUbl(), StandardCharsets.UTF_8);
            response = ImmutablePair.of(signResponse.getHash(), file);
        } catch (final IOException e) {
            LOG.error("Catched", e);
        }
        return response;
    }

    protected CreditNote fillCreditNote(final Instance docInstance,
                                        final CreditNote ubl)
        throws EFapsException
    {
        final var eval = EQL.builder().print(docInstance)
                        .attribute(CISales.CreditNote.Name, CISales.CreditNote.Taxes,
                                        CISales.CreditNote.RateCurrencyId, CISales.CreditNote.Date,
                                        CISales.CreditNote.RateCrossTotal,
                                        CISales.CreditNote.RateNetTotal,
                                        CISales.CreditNote.CrossTotal)
                        .linkto(CISales.CreditNote.Contact).instance().as("contactInstance")
                        .linkto(CISales.CreditNote.CreditReason)
                        .attribute(CISales.AttributeDefinitionCreditReason.Value).as("creditReason")
                        .evaluate();

        final var taxes = eval.<Taxes>get(CISales.DocumentSumAbstract.Taxes);
        final Instance contactInstance = eval.get("contactInstance");
        final BigDecimal crossTotal = eval.get(CISales.DocumentSumAbstract.RateCrossTotal);
        final var currencyInst = CurrencyInst.get(eval.<Long>get(CISales.DocumentSumAbstract.RateCurrencyId));
        final LocalDate date = eval.get(CISales.DocumentSumAbstract.Date);

        final String creditReason = eval.get("creditReason");
        final CreditNoteTypeCode typeCode = switch (creditReason) {
            case "01" -> CreditNoteTypeCode.C01;
            case "02" -> CreditNoteTypeCode.C02;
            case "03" -> CreditNoteTypeCode.C03;
            case "04" -> CreditNoteTypeCode.C04;
            case "05" -> CreditNoteTypeCode.C05;
            case "06" -> CreditNoteTypeCode.C06;
            case "07" -> CreditNoteTypeCode.C07;
            case "08" -> CreditNoteTypeCode.C08;
            case "09" -> CreditNoteTypeCode.C09;
            case "10" -> CreditNoteTypeCode.C10;
            case "11" -> CreditNoteTypeCode.C11;
            case "12" -> CreditNoteTypeCode.C12;
            case "13" -> CreditNoteTypeCode.C13;
            default -> CreditNoteTypeCode.C01;
        };
        ubl.setCreditNoteTypeCode(typeCode);

        ubl.withNumber(eval.get(CISales.DocumentSumAbstract.Name))
                        .withCurrency(currencyInst.getISOCode())
                        .withDate(date)
                        .withCrossTotal(crossTotal)
                        .withNetTotal(eval.get(CISales.DocumentSumAbstract.RateNetTotal))
                        .withSupplier(getSupplier())
                        .withCustomer(getCustomer(contactInstance))
                        .withTaxes(getTaxes(taxes, false, false));

        final var line = Line.builder()
                        .withQuantity(BigDecimal.ONE)
                        .withSku("01")
                        .withDescription(typeCode.getDescription())
                        .withNetUnitPrice(ubl.getNetTotal())
                        .withCrossUnitPrice(ubl.getCrossTotal())
                        .withNetPrice(ubl.getNetTotal())
                        .withCrossPrice(ubl.getCrossTotal())
                        .withUoMCode("NIU")
                        .withTaxEntries(ubl.getTaxes())
                        .withPriceType("01")
                        .build();

        ubl.withLines(Collections.singletonList(line));
        final var refEval = EQL.builder().print().query(CISales.CreditNote2Invoice, CISales.CreditNote2Receipt)
                        .where().attribute(CISales.Document2DocumentAbstract.FromAbstractLink).eq(docInstance)
                        .select()
                        .linkto(CISales.Document2DocumentAbstract.ToAbstractLink).instance().as("refInst")
                        .linkto(CISales.Document2DocumentAbstract.ToAbstractLink)
                        .attribute(CISales.DocumentAbstract.Name).as("name")
                        .linkto(CISales.Document2DocumentAbstract.ToAbstractLink)
                        .attribute(CISales.DocumentAbstract.Date).as("date")
                        .evaluate();
        refEval.next();
        final Instance refIns = refEval.get("refInst");
        final var reference = new Reference()
                        .setDocType(InstanceUtils.isKindOf(refIns, CISales.Invoice) ? "01" : "03")
                        .setNumber(refEval.get("name"))
                        .setDate(refEval.get("date"));
        ubl.withReference(reference);
        return ubl;
    }

    protected DeliveryNote fillDeliveryNote(final Instance docInstance,
                                            final DeliveryNote ubl)
        throws EFapsException
    {
        LOG.info("starting filling {}", docInstance);
        final var eval = EQL.builder().print(docInstance)
                        .attribute(CISales.DeliveryNote.Name, CISales.DeliveryNote.Date, CISales.DeliveryNote.DueDate,
                                        CISales.DeliveryNote.Created, CISales.DeliveryNote.DriverLink,
                                        CISales.DeliveryNote.VehicleLink, CISales.DeliveryNote.CrossWeight)
                        .linkto(CISales.DocumentSumAbstract.Contact).instance().as("contactInstance")
                        .linkto(CISales.DeliveryNote.TransferReason)
                        .attribute(CISales.AttributeDefinitionTransferReason.MappingKey).as("transferReason")
                        .linkto(CISales.DeliveryNote.TransferReason)
                        .attribute(CISales.AttributeDefinitionTransferReason.Description).as("transferReasonDescr")
                        .linkto(CISales.DeliveryNote.CarrierLink).instance().as("carrierInst")
                        .linkto(CISales.DeliveryNote.DeparturePointLink).instance().as("departurePointInst")
                        .linkto(CISales.DeliveryNote.ArrivalPointLink).instance().as("arrivalPointInst")
                        .evaluate();
        final Instance contactInstance = eval.get("contactInstance");
        final LocalDate date = eval.get(CISales.DeliveryNote.Date);
        eval.get(CISales.DeliveryNote.DueDate);
        final OffsetDateTime created = eval.get(CISales.DeliveryNote.Created);

        final boolean thirdParty = !ERP.COMPANY_CONTACT.get().equals(eval.get("carrierInst"));

        ubl.withNumber(eval.get(CISales.DeliveryNote.Name))
                        .withDate(date)
                        .withTime(evalTime(date, created))
                        .withSupplier(getSupplier())
                        .withCustomer(getCustomer(contactInstance))
                        .withLines(getDeliveryNoteLines(docInstance))
                        .withShipment(getShipment(docInstance, thirdParty, eval));
        return ubl;
    }

    protected Shipment getShipment(final Instance docInstance,
                                   final boolean thirdParty,
                                   final Evaluator eval)
        throws EFapsException
    {
        final var ret = new Shipment();
        final var stage = new Stage()
                        .withMode(thirdParty ? "01" : "02")
                        // carrier only if thirdParty
                        .withCarrier(thirdParty ? getCarrier(eval.get("carrierInst")) : null)
                        .withStartDate(thirdParty ? eval.get(CISales.DeliveryNote.DueDate)
                                        : eval.get(CISales.DeliveryNote.Date));
        ret.withHandlingCode(eval.get("transferReason"))
                        .withHandlingInstructions(eval.get("transferReasonDescr"))
                        .withDelivery(getDelivery(eval))
                        .addTransportUnit(getTransport(ret, thirdParty, eval))
                        .addStage(stage);

        evalWeight(docInstance, ret, eval);
        stage.withDriver(getDriver(eval.get(CISales.DeliveryNote.DriverLink)));
        return ret;
    }

    protected void evalWeight(final Instance docInstance,
                              final Shipment shipment,
                              final Evaluator eval)
        throws EFapsException
    {
        BigDecimal crossWeight = BigDecimal.ZERO;
        UoM uoM = null;
        final BigDecimal weight = eval.get(CISales.DeliveryNote.CrossWeight);
        if (weight != null) {
            crossWeight = weight;
        } else if (Products.STANDART_CONV.exists()) {
            final var posEval = EQL.builder()
                            .print()
                            .query(CISales.DeliveryNotePosition)
                            .where()
                            .attribute(CISales.DeliveryNotePosition.DeliveryNote).eq(docInstance)
                            .select()
                            .attribute(CISales.DeliveryNotePosition.Quantity, CISales.DeliveryNotePosition.UoM)
                            .linkto(CISales.DeliveryNotePosition.Product).instance()
                            .as("prodInst")
                            .evaluate();

            while (posEval.next()) {
                final Instance prodInst = posEval.get("prodInst");
                final var qty = posEval.<BigDecimal>get(CISales.DeliveryNotePosition.Quantity);
                final var uomId = posEval.<Long>get(CISales.DeliveryNotePosition.UoM);
                final var uom = Dimension.getUoM(uomId);
                final var conversion = Conversion.convert(ConversionType.TRANSPORTWEIGHT, prodInst, qty, uom);
                crossWeight = crossWeight.add(conversion.getValue());
                if (uoM == null) {
                    uoM = conversion.getUoM();
                }
            }
        }
        if (crossWeight.scale() > 3) {
            crossWeight = crossWeight.setScale(3, RoundingMode.HALF_UP);
        }
        shipment.withCrossWeight(crossWeight)
                        .withCrossWeightUoM(uoM == null ? "KGM" : uoM.getCommonCode());
    }

    protected Transport getTransport(final Shipment shipment,
                                     final boolean thirdParty,
                                     final Evaluator eval)
        throws EFapsException
    {
        final var ret = new Transport()
                        .addEquipment(getEquipment(shipment, thirdParty, eval.get(CISales.DeliveryNote.VehicleLink)));
        return ret;
    }

    protected Equipment getEquipment(final Shipment shipment,
                                     final boolean thirdParty,
                                     final Long vehicleId)
        throws EFapsException
    {
        if (vehicleId == null) {
            LOG.error("No Vehicle for DeliveryNote found");
        }
        // Contacts_ClassCarrier/CarrierSet
        final var eval = EQL.builder()
                        .print(Instance.get(UUID.fromString("d6d49ad1-dd0f-400b-a250-cd58315234fb"), vehicleId))
                        .attribute(CIContacts.AttributeAbstractClassCarrier.Registration,
                                        CIContacts.AttributeAbstractClassCarrier.Certificate,
                                        CIContacts.AttributeAbstractClassCarrier.PublicRegistryNo,
                                        CIContacts.AttributeAbstractClassCarrier.MinorVehicle)
                        .evaluate();
        final var equipment = new Equipment()
                        .withLicensePlate(eval.get(CIContacts.AttributeAbstractClassCarrier.Registration));

        if (thirdParty) {
            equipment.withCertificate(eval.get(CIContacts.AttributeAbstractClassCarrier.Certificate));
            final Boolean isMinor = eval.get(CIContacts.AttributeAbstractClassCarrier.MinorVehicle);
            if (isMinor != null && isMinor) {
                shipment.addInstruction(DeliveryNoteInstruction.VEHICLE_MINOR.getName());
            } else {
                shipment.addInstruction(DeliveryNoteInstruction.VEHICLE_CONDUCTOR.getName());
            }
        }
        return equipment;
    }

    protected Delivery getDelivery(final Evaluator eval)
        throws EFapsException
    {
        final var ret = new Delivery()
                        .withDeliveryAddress(getAddress(eval, "arrivalPointInst"))
                        .withDespatchAddress(getAddress(eval, "departurePointInst"));
        return ret;
    }

    protected IAddress getAddress(final Evaluator eval,
                                  final String key)
        throws EFapsException
    {
        final Instance contactInstance = eval.get(key);
        String address = null;
        String geoLocationId = null;
        if (InstanceUtils.isKindOf(contactInstance, CIContacts.Contact)) {
            final var locEval = EQL.builder().print().query(CIContacts.ClassLocation)
                            .where().attribute(CIContacts.ClassLocation.ContactLink).eq(contactInstance)
                            .select()
                            .linkto(CIContacts.ClassLocation.DistrictLocationLink)
                            .attribute(CIUbicaciones.UbicacionStandardRoot.Code).as("ubigeo")
                            .attribute(CIContacts.ClassLocation.LocationAdressStreet,
                                            CIContacts.ClassLocation.LocationAdressCity)
                            .evaluate();
            if (locEval.next()) {
                address = locEval.get(CIContacts.ClassLocation.LocationAdressStreet) + " - "
                                + locEval.get(CIContacts.ClassLocation.LocationAdressCity);
                geoLocationId = locEval.get("ubigeo");
            }
        } else {
            final var locEval = EQL.builder().print().query(CIContacts.SubContactClassLocation)
                            .where().attribute(CIContacts.SubContactClassLocation.SubContactLink).eq(contactInstance)
                            .select()
                            .linkto(CIContacts.SubContactClassLocation.DistrictLocationLink)
                            .attribute(CIUbicaciones.UbicacionStandardRoot.Code).as("ubigeo")
                            .attribute(CIContacts.SubContactClassLocation.LocationAdressStreet,
                                            CIContacts.SubContactClassLocation.LocationAdressCity)
                            .evaluate();
            if (locEval.next()) {
                address = locEval.get(CIContacts.SubContactClassLocation.LocationAdressStreet) + " - "
                                + locEval.get(CIContacts.SubContactClassLocation.LocationAdressCity);
                geoLocationId = locEval.get("ubigeo");
            }
        }
        final var addressLine = address == null ? "NO address" : address;
        final var geoLocation = geoLocationId;
        return new IAddress()
        {

            @Override
            public String getAddressLine()
            {
                return addressLine;
            }

            @Override
            public String getGeoLocationId()
            {
                return geoLocation;
            }
        };
    }

    protected Driver getDriver(final Long driverId)
        throws EFapsException
    {
        if (driverId == null) {
            LOG.error("No driver for DeliveryNote found");
        }
        // Contacts_ClassCarrier/DriverSet
        final var eval = EQL.builder()
                        .print(Instance.get(UUID.fromString("09ee80a0-e8c0-41d2-b272-c30a32733fea"), driverId))
                        .linkto(CIContacts.AttributeAbstractClassCarrierDriver.DOITypeLink)
                        .attribute(CIContacts.AttributeDefinitionDOIType.MappingKey).as("doiType")
                        .attribute(CIContacts.AttributeAbstractClassCarrierDriver.Name,
                                        CIContacts.AttributeAbstractClassCarrierDriver.LastName,
                                        CIContacts.AttributeAbstractClassCarrierDriver.License,
                                        CIContacts.AttributeAbstractClassCarrierDriver.DocumentOfIdentity)
                        .evaluate();
        final var driver = new Driver()
                        .withDoiType(eval.get("doiType"))
                        .withDOI(eval.get(CIContacts.AttributeAbstractClassCarrierDriver.DocumentOfIdentity))
                        .withFirstName(eval.get(CIContacts.AttributeAbstractClassCarrierDriver.Name))
                        .withFamilyName(eval.get(CIContacts.AttributeAbstractClassCarrierDriver.LastName))
                        .withLicense(eval.get(CIContacts.AttributeAbstractClassCarrierDriver.License))
                        .withJobTitle("Principal");
        return driver;
    }

    protected List<ILine> getDeliveryNoteLines(final Instance docInstance)
        throws EFapsException
    {
        final var ret = new ArrayList<ILine>();
        final var eval = EQL.builder()
                        .print()
                        .query(CISales.DeliveryNotePosition)
                        .where()
                        .attribute(CISales.DeliveryNotePosition.DeliveryNote).eq(docInstance)
                        .select()
                        .attribute(CISales.DeliveryNotePosition.Quantity, CISales.DeliveryNotePosition.ProductDesc,
                                        CISales.DeliveryNotePosition.PositionNumber, CISales.DeliveryNotePosition.UoM)
                        .linkto(CISales.DeliveryNotePosition.Product).attribute(CIProducts.ProductAbstract.Name)
                        .as("prodName")
                        .orderBy(CISales.DeliveryNotePosition.PositionNumber)
                        .evaluate();

        while (eval.next()) {
            final var uomId = eval.<Long>get(CISales.DeliveryNotePosition.UoM);
            ret.add(Line.builder()
                            .withQuantity(eval.get(CISales.PositionSumAbstract.Quantity))
                            .withSku(eval.get("prodName"))
                            .withDescription(eval.get(CISales.PositionSumAbstract.ProductDesc))
                            .withUoMCode(Dimension.getUoM(uomId).getCommonCode())
                            .withAdditionalItemProperties(Collections.singletonList(
                                            new AdditionalItemProperty()))
                            .build());
        }
        return ret;
    }

    protected LocalTime evalTime(final LocalDate date,
                                 final OffsetDateTime created)
        throws EFapsException
    {
        LocalTime ret;
        final var zoneId = Context.getThreadContext().getZoneId();
        final var currenDate = LocalDate.now(zoneId);
        if (currenDate.equals(date)) {
            ret = LocalTime.now(zoneId).withNano(0);
        } else {
            ret = created.atZoneSameInstant(zoneId).toLocalTime().withNano(0);
        }
        return ret;
    }

    protected AbstractDocument<?> fill(final Instance docInstance,
                                       final AbstractDocument<?> ubl,
                                       final boolean freeOfCharge)
        throws EFapsException
    {
        final var eval = EQL.builder().print(docInstance)
                        .attribute(CISales.DocumentSumAbstract.Name, CISales.DocumentSumAbstract.Taxes,
                                        CISales.DocumentSumAbstract.RateCurrencyId, CISales.DocumentSumAbstract.Date,
                                        CISales.DocumentSumAbstract.RateCrossTotal,
                                        CISales.DocumentSumAbstract.RateNetTotal,
                                        CISales.DocumentSumAbstract.CrossTotal)
                        .linkto(CISales.DocumentSumAbstract.Contact).instance().as("contactInstance")
                        .evaluate();

        final var taxes = eval.<Taxes>get(CISales.DocumentSumAbstract.Taxes);
        final Instance contactInstance = eval.get("contactInstance");
        final BigDecimal crossTotal = eval.get(CISales.DocumentSumAbstract.RateCrossTotal);
        final BigDecimal baseCrossTotal = eval.get(CISales.DocumentSumAbstract.CrossTotal);

        final var allowancesCharges = getCharges(taxes, false);
        evalRetention(allowancesCharges, contactInstance, crossTotal, baseCrossTotal);
        allowancesCharges.addAll(getAllowances(docInstance));

        final var currencyInst = CurrencyInst.get(eval.<Long>get(CISales.DocumentSumAbstract.RateCurrencyId));
        final LocalDate date = eval.get(CISales.DocumentSumAbstract.Date);

        final var paymentMethod = getPaymentMethod(docInstance);

        ubl.withNumber(eval.get(CISales.DocumentSumAbstract.Name))
                        .withCurrency(currencyInst.getISOCode())
                        .withDate(date)
                        .withCrossTotal(crossTotal)
                        .withNetTotal(eval.get(CISales.DocumentSumAbstract.RateNetTotal))
                        .withSupplier(getSupplier())
                        .withCustomer(getCustomer(contactInstance))
                        .withAllowancesCharges(allowancesCharges)
                        .withLines(getLines(docInstance, freeOfCharge))
                        .withTaxes(getTaxes(taxes, false, freeOfCharge))
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
                                return paymentMethod.getInstallments().stream()
                                                .map(installment -> ((IInstallment) installment))
                                                .collect(Collectors.toList());
                            }
                        });

        if (ubl instanceof CreditNote) {
            final var refEval = EQL.builder().print().query(CISales.CreditNote2Invoice, CISales.CreditNote2Receipt)
                            .where().attribute(CISales.Document2DocumentAbstract.FromAbstractLink).eq(docInstance)
                            .select()
                            .linkto(CISales.Document2DocumentAbstract.ToAbstractLink).instance().as("refInst")
                            .linkto(CISales.Document2DocumentAbstract.ToAbstractLink)
                            .attribute(CISales.DocumentAbstract.Name).as("name")
                            .linkto(CISales.Document2DocumentAbstract.ToAbstractLink)
                            .attribute(CISales.DocumentAbstract.Date).as("date")
                            .evaluate();
            refEval.next();
            final Instance refIns = refEval.get("refInst");
            final var reference = new Reference()
                            .setDocType(InstanceUtils.isKindOf(refIns, CISales.Invoice) ? "01" : "03")
                            .setNumber(refEval.get("name"))
                            .setDate(refEval.get("date"));
            ((CreditNote) ubl).withReference(reference);
            ubl.withPaymentTerms(null);
        }
        return ubl;
    }

    protected void evalRetention(final List<IAllowanceChargeEntry> allowancesCharges,
                                 final Instance contactInstance,
                                 final BigDecimal rateCrossTotal,
                                 final BigDecimal baseCrossTotal)
        throws EFapsException
    {
        if (Sales.CLASSTAXINFOACTIVATE.get() && baseCrossTotal.compareTo(new BigDecimal("700")) > 0) {
            final var eval = EQL.builder().print(contactInstance)
                            .clazz(CISales.Contacts_ClassTaxinfo)
                            .attribute(CISales.Contacts_ClassTaxinfo.Retention).as("retention")
                            .evaluate();
            if (eval.next()) {
                final TaxRetention retention = eval.get("retention");
                if (retention != null && retention.equals(TaxRetention.AGENT)) {
                    allowancesCharges.add(AllowanceEntry.builder()
                                    .withAmount(new BigDecimal("0.03").multiply(rateCrossTotal))
                                    .withBaseAmount(rateCrossTotal)
                                    // (Código de motivo de cargo/ descuento:
                                    // Retención del IGV)
                                    .withReason("62")
                                    .withFactor(new BigDecimal("0.03"))
                                    .build());
                }
            }
        }
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

    public Supplier getSupplier()
        throws EFapsException
    {
        final var ret = new Supplier();
        ret.setDoiType("6");
        ret.setDOI(ERP.COMPANY_TAX.get());
        ret.setName(ERP.COMPANY_NAME.get());
        ret.setStreetName(ERP.COMPANY_STREET.get());
        ret.withGeoLocationId(ERP.COMPANY_UBIGEO.get());
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

    protected ICarrier getCarrier(final Instance contactInstance)
        throws EFapsException
    {
        final var eval = EQL.builder().print(contactInstance)
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
        final Carrier ret = new Carrier();
        ret.setDOI(taxNumber == null ? identityCard : taxNumber);
        ret.setDoiType(doiType);
        ret.setName(eval.get(CIContacts.ContactAbstract.Name));
        return ret;
    }

    protected ArrayList<ILine> getLines(final Instance docInstance,
                                        final boolean freeOfCharge)
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
                                        CISales.PositionSumAbstract.RateDiscountNetUnitPrice,
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
                            .withNetUnitPrice(freeOfCharge ? BigDecimal.ZERO
                                            : eval.get(CISales.PositionSumAbstract.RateDiscountNetUnitPrice))
                            .withCrossUnitPrice(eval.get(CISales.PositionSumAbstract.RateCrossUnitPrice))
                            .withNetPrice(eval.get(CISales.PositionSumAbstract.RateNetPrice))
                            .withCrossPrice(eval.get(CISales.PositionSumAbstract.RateCrossPrice))
                            .withUoMCode(Dimension.getUoM(uomId).getCommonCode())
                            .withTaxEntries(getTaxes(taxes, true, freeOfCharge))
                            .withAllowancesCharges(getCharges(taxes, true))
                            // CATALOGO Nr.16:
                            // 01 - Precio Unitario (incluye IGV),
                            // 02 - Valor referencial unitario en operaciones no
                            // onerosas
                            .withPriceType(freeOfCharge ? "02" : "01")
                            .build());
        }
        return ret;
    }

    protected List<ITaxEntry> getTaxes(final Taxes taxes,
                                       final boolean isLine,
                                       final boolean freeOfCharge)
        throws EFapsException
    {
        final var ret = new ArrayList<ITaxEntry>();
        for (final var entry : taxes.getEntries()) {
            if (freeOfCharge) {
                final var tax = Tax_Base.get(entry.getCatUUID(), entry.getUUID());
                ret.add(org.efaps.esjp.electronicbilling.entities.TaxEntry.builder()
                                .withTaxType(org.efaps.ubl.documents.values.TaxType.ADVALOREM)
                                .withAmount(entry.getAmount())
                                .withTaxableAmount(entry.getBase())
                                .withTaxExemptionReasonCode(isLine ? "11" : null)
                                .withPercent(tax.getFactor().multiply(new BigDecimal(100)))
                                .withName("GRA")
                                .withCode("FRE")
                                .withId("9996")
                                .withFreeOfCharge(true)
                                .build());

            } else if (getTaxProperty(entry.getUUID(), "id") != null) {
                final var code = getTaxProperty(entry.getUUID(), "id");
                final var name = getTaxProperty(entry.getUUID(), "nombre");
                final var id = getTaxProperty(entry.getUUID(), "sunat-id");
                final var taxExemptionReasonCode = getTaxProperty(entry.getUUID(), "afectacion-igv");
                org.efaps.ubl.documents.values.TaxType taxType;
                final var tax = Tax_Base.get(entry.getCatUUID(), entry.getUUID());

                taxType = switch (tax.getTaxType()) {
                    case PERUNIT -> org.efaps.ubl.documents.values.TaxType.PERUNIT;
                    case ADVALOREM -> org.efaps.ubl.documents.values.TaxType.ADVALOREM;
                    default -> org.efaps.ubl.documents.values.TaxType.ADVALOREM;
                };
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
                        .signDocument(ublXml, Charset.forName(ElectronicBilling.UBL_ENCODING.get()));
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
