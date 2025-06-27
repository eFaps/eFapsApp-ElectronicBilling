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
package org.efaps.esjp.electronicbilling.util;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.api.annotation.EFapsSysConfAttribute;
import org.efaps.api.annotation.EFapsSysConfLink;
import org.efaps.api.annotation.EFapsSystemConfiguration;
import org.efaps.esjp.admin.common.systemconfiguration.BooleanSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.IntegerSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.PropertiesSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.StringSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.SysConfLink;
import org.efaps.esjp.ci.CIEBilling;
import org.efaps.esjp.ci.CINumGenEBilling;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.cache.CacheReloadException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("8ed79746-51c8-4590-b9bc-1716e1c0a2d3")
@EFapsApplication("eFapsApp-ElectronicBilling")
@EFapsSystemConfiguration("451e21b9-27ff-4378-adfa-a578a9ba0b51")
public final class ElectronicBilling
{

    /** The base. */
    public static final String BASE = "org.efaps.electronicbilling.";

    /** Sales-Configuration. */
    public static final UUID SYSCONFUUID = UUID.fromString("451e21b9-27ff-4378-adfa-a578a9ba0b51");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute DOCMAPPING = new PropertiesSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "DocumentMapping")
                    .description("Config for Document to EBilling Document relation")
                    .addDefaultValue(CISales.Invoice.getType().getName(), CIEBilling.Invoice.getType().getName())
                    .addDefaultValue(CIEBilling.Invoice.getType().getName() + ".CreateStatus",
                                    CIEBilling.InvoiceStatus.Pending.key)
                    .addDefaultValue(CISales.Receipt.getType().getName(), CIEBilling.Receipt.getType().getName())
                    .addDefaultValue(CIEBilling.Receipt.getType().getName() + ".CreateStatus",
                                    CIEBilling.ReceiptStatus.Pending.key)
                    .addDefaultValue(CISales.CreditNote.getType().getName(), CIEBilling.CreditNote.getType().getName())
                    .addDefaultValue(CIEBilling.CreditNote.getType().getName() + ".CreateStatus",
                                    CIEBilling.CreditNoteStatus.Pending.key)
                    .addDefaultValue(CISales.Reminder.getType().getName(), CIEBilling.Reminder.getType().getName())
                    .addDefaultValue(CIEBilling.Reminder.getType().getName() + ".CreateStatus",
                                    CIEBilling.ReminderStatus.Pending.key)
                    .addDefaultValue(CISales.RetentionCertificate.getType().getName(),
                                    CIEBilling.RetentionCertificate.getType().getName())
                    .addDefaultValue(CIEBilling.RetentionCertificate.getType().getName() + ".CreateStatus",
                                    CIEBilling.RetentionCertificateStatus.Pending.key)
                    .addDefaultValue(CISales.DeliveryNote.getType().getName(),
                                    CIEBilling.DeliveryNote.getType().getName())
                    .addDefaultValue(CIEBilling.DeliveryNote.getType().getName() + ".CreateStatus",
                                    CIEBilling.DeliveryNoteStatus.Pending.key)
                    .addDefaultValue(CISales.DeliveryNote.getType().getName() + ".NameRegexMatch", "^T.*");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute QUERYBLDR4DOCSCAN = new PropertiesSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "QueryBldr4DocumentScanner")
                    .description("QueryBuilder for Include Scanner")
                    .addDefaultValue("Type01", CISales.Invoice.getType().getName())
                    .addDefaultValue("StatusGroup01", CISales.InvoiceStatus.getType().getName())
                    .addDefaultValue("Status01", CISales.InvoiceStatus.Open.key)
                    .addDefaultValue("StatusGroup02", CISales.InvoiceStatus.getType().getName())
                    .addDefaultValue("Status02", CISales.InvoiceStatus.Paid.key)
                    .addDefaultValue("Type02", CISales.Receipt.getType().getName())
                    .addDefaultValue("StatusGroup03", CISales.ReceiptStatus.getType().getName())
                    .addDefaultValue("Status03", CISales.ReceiptStatus.Open.key)
                    .addDefaultValue("StatusGroup04", CISales.ReceiptStatus.getType().getName())
                    .addDefaultValue("Status04", CISales.ReceiptStatus.Paid.key)
                    .addDefaultValue("Type03", CISales.CreditNote.getType().getName())
                    .addDefaultValue("StatusGroup05", CISales.CreditNoteStatus.getType().getName())
                    .addDefaultValue("Status05", CISales.CreditNoteStatus.Open.key)
                    .addDefaultValue("StatusGroup06", CISales.CreditNoteStatus.getType().getName())
                    .addDefaultValue("Status06", CISales.CreditNoteStatus.Paid.key)
                    .addDefaultValue("Type04", CISales.Reminder.getType().getName())
                    .addDefaultValue("StatusGroup07", CISales.ReminderStatus.getType().getName())
                    .addDefaultValue("Status07", CISales.ReminderStatus.Open.key)
                    .addDefaultValue("StatusGroup08", CISales.ReminderStatus.getType().getName())
                    .addDefaultValue("Status08", CISales.ReminderStatus.Paid.key);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATEMAIL = new BooleanSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "ActivateMailing")
                    .description("Activate the evaluation for mails.")
                    .defaultValue(false);

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute DELIVERYNOTE_CREATEONSTATUS = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "DeliveryNote.CreateOnStatusChange")
                    .description("Activate the mechanism to create the electronic billing document on status change")
                    .defaultValue(CISales.DeliveryNoteStatus.Open.key);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute DELIVERYNOTE_ACTIVE = new BooleanSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "DeliveryNote.Activate")
                    .description("Activate DeliveryNote")
                    .defaultValue(true);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute DELIVERYNOTE_CREATEREPORT= new BooleanSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "DeliveryNote.CreateReport")
                    .description("Activate the creation of the Report for DeliveryNote")
                    .defaultValue(true);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute DELIVERYNOTE_CREATEUBL= new BooleanSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "DeliveryNote.CreateUBL")
                    .description("Activate the creation of the UBL for DeliveryNote")
                    .defaultValue(true);

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute DELIVERYNOTE_VERIFY= new PropertiesSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "DeliveryNote.Verification")
                    .description("Properties that permit to define when an Electronic DeliveryNote should not be created.");

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute DELIVERYNOTE_ENDPOINTURI = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "DeliveryNote.EndpointURI")
                    .defaultValue("https://api-cpe.sunat.gob.pe/v1/contribuyente/gem/comprobantes")
                    .description("URI of the DeliveryNote endpoint.");

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute DELIVERYNOTE_STATUSENDPOINTURI = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "DeliveryNote.StatusEndpointURI")
                    .defaultValue("https://api-cpe.sunat.gob.pe/v1/contribuyente/gem/comprobantes/envios")
                    .description("URI of the state DeliveryNote endpoint.");

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute FISCUS_SSO_ENDPOINTURI = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Fiscus.sso.EndpointURI")
                    .defaultValue("https://api-seguridad.sunat.gob.pe/v1/clientessol")
                    .description("URI of the Fiscus SSO.");

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute FISCUS_SSO_CLIENTID = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Fiscus.sso.ClientId")
                    .defaultValue("MISSING CONFIG")
                    .description("Fiscus SSO CLientId");

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute FISCUS_SSO_CLIENTSECRET = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Fiscus.sso.ClientSecret")
                    .defaultValue("MISSING CONFIG")
                    .description("Fiscus SSO CLientId");

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute FISCUS_SSO_USERNAME = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Fiscus.sso.UserName")
                    .defaultValue("MISSING CONFIG")
                    .description("Fiscus SSO CLientId");

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute FISCUS_SSO_PWD = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Fiscus.sso.Password")
                    .defaultValue("MISSING CONFIG")
                    .description("Fiscus SSO CLientId");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INVOICE_CREATEONSTATUS = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Invoice.CreateOnStatusChange")
                    .description("Activate the mechanism to create the electronic billing document on statsu change")
                    .defaultValue(CISales.InvoiceStatus.Open.key);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INVOICE_ACTIVE = new BooleanSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Invoice.Activate")
                    .description("Activate Invoice")
                    .defaultValue(true);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INVOICE_CREATEUBL= new BooleanSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Invoice.CreateUBL")
                    .description("Activate the creation of the UBL for Invoice")
                    .defaultValue(true);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INVOICE_CREATEREPORT= new BooleanSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Invoice.CreateReport")
                    .description("Activate the creation of the Report for Invoice")
                    .defaultValue(true);

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INVOICE_VERIFY= new PropertiesSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Invoice.Verification")
                    .description("Properties that permit to define when an Electronic Invoice should be aborted.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute RECEIPT_CREATEONSTATUS = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Receipt.CreateOnStatusChange")
                    .description("Activate the mechanism to create the electronic billing document on statsu change")
                    .defaultValue(CISales.ReceiptStatus.Open.key);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute RECEIPT_ACTIVE = new BooleanSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Receipt.Activate")
                    .description("Activate Receipt")
                    .defaultValue(true);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute RECEIPT_CREATEUBL= new BooleanSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Receipt.CreateUBL")
                    .description("Activate the creation of the UBL for Receipt")
                    .defaultValue(true);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute RECEIPT_CREATEREPORT= new BooleanSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Receipt.CreateReport")
                    .description("Activate the creation of the Report for Receipt")
                    .defaultValue(true);

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute RECEIPT_VERIFY = new PropertiesSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Receipt.Verification")
                    .description("Properties that permit to define when an Electronic Receipt should be aborted.");


    /** See description. */
    @EFapsSysConfAttribute
    public static final IntegerSysConfAttribute RECEIPT_ANONYMOUSTHRESHOLD = new IntegerSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Receipt.AnonymousThreshold")
                    .defaultValue(700)
                    .description("Amount in BaseCurrency that requires Named receipts and not anonymous");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute REMINDER_CREATEONSTATUS = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Reminder.CreateOnStatusChange")
                    .description("Activate the mechanism to create the electronic billing document on statsu change")
                    .defaultValue(CISales.ReminderStatus.Open.key);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REMINDER_ACTIVE = new BooleanSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Reminder.Activate")
                    .description("Activate Reminder")
                    .defaultValue(true);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REMINDER_CREATEREPORT= new BooleanSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Reminder.CreateReport")
                    .description("Activate the creation of the Report for Reminder")
                    .defaultValue(true);

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REMINDER_VERIFY= new PropertiesSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Reminder.Verification")
                    .description("Properties that permit to define when an Electronic Reminder should be aborted.");


    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute CREDITNOTE_CREATEONSTATUS = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "CreditNote.CreateOnStatusChange")
                    .description("Activate the mechanism to create the electronic billing document on statsu change")
                    .defaultValue(CISales.CreditNoteStatus.Open.key);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute CREDITNOTE_ACTIVE = new BooleanSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "CreditNote.Activate")
                    .description("Activate CreditNote")
                    .defaultValue(true);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute CREDITNOTE_TRYDETAILED = new BooleanSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "CreditNote.TryDetailed")
                    .description("Should the CreditNote include the detailed (if possible)")
                    .defaultValue(false);

    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute CREDITNOTE_CREATEUBL= new BooleanSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "CreditNote.CreateUBL")
                    .description("Activate the creation of the UBL for CreditNote")
                    .defaultValue(true);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute CREDITNOTE_CREATEREPORT= new BooleanSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "CreditNote.CreateReport")
                    .description("Activate the creation of the Report for CreditNote")
                    .defaultValue(true);

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute CREDITNOTE_VERIFY= new PropertiesSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "CreditNote.Verification")
                    .description("Properties that permit to define when an Electronic CreditNote should be aborted.");

    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PREMISESCODE_BY_SERIAL = new PropertiesSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "PremisesCode.BySerial")
                    .description("""
                        Mapping of Serial to Establecimiento
                        Uses StartsWith as comparision.
                        F001=14
                        FC003=14""");

    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute TAXMAPPING = new PropertiesSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "TaxMapping")
                    .description("Tax Mapping")
                    .addDefaultValue("tax.06e40be6-40d8-44f4-9d8f-585f2f97ce63.id", "VAT")
                    .addDefaultValue("tax.06e40be6-40d8-44f4-9d8f-585f2f97ce63.nombre", "IGV")
                    .addDefaultValue("tax.06e40be6-40d8-44f4-9d8f-585f2f97ce63.sunat-id", "1000")
                    .addDefaultValue("tax.06e40be6-40d8-44f4-9d8f-585f2f97ce63.afectacion-igv", "10")
                    .addDefaultValue("tax.e391b264-ad67-40f4-9e68-26556e28062f.id","FRE")
                    .addDefaultValue("tax.e391b264-ad67-40f4-9e68-26556e28062f.sunat-id","9996")
                    .addDefaultValue("tax.e391b264-ad67-40f4-9e68-26556e28062f.nombre","GRA")
                    .addDefaultValue("tax.e391b264-ad67-40f4-9e68-26556e28062f.freeOfCharge","true");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute ISSUER_EMAIL = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Issuer.Email")
                    .description("defines the issuer email")
                    .defaultValue("info@synercom.pe");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENTMETHODREGEX = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "PaymentMethod.Regex")
                    .description("Name of ChannelSalesCondition will be matched against this to be included");

    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute RETENTION_ISAGENT = new BooleanSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "retention.IsAgent")
                    .defaultValue(false)
                    .description("Is the Current Company \"Agente de Retencion\"");

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute RETENTION_PERCENTAGE = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "retention.Percentage")
                    .defaultValue("3")
                    .description("Is the Current Company \"Agente de Retencion\"");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute RETENTIONCERTIFICATE_ACTIVE = new BooleanSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "RetentionCertificate.Activate")
                    .description("Activate RetentionCertificate")
                    .defaultValue(true);

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute RETENTIONCERTIFICATE_CREATEONSTATUS = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "RetentionCertificate.CreateOnStatusChange")
                    .description("Activate the mechanism to create the electronic billing document on status change")
                    .defaultValue(CISales.RetentionCertificateStatus.Open.key);

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute KEYSTORE_ALIAS = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Keystore.Alias");

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute KEYSTORE_PWD = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Keystore.Password");

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute KEYSTORE_KEYPWD = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Keystore.KeyPassword");

    @EFapsSysConfLink
    public static final SysConfLink KEYSTORE = new SysConfLink()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "KeyStore")
                    .description("Keystore containing the cert to sign the UBL");

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute SUMMARY_SEQ = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "Summary.Sequence")
                    .defaultValue(CINumGenEBilling.SummarySequence.uuid.toString());

    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute UBL_ACTIVE = new BooleanSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "ubl.Active")
                    .defaultValue(false);

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute UBL_ENCODING = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "ubl.Encoding")
                    .defaultValue(StandardCharsets.UTF_8.name())
                    .description("ResponseFileType");

    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute EXPORT_SALERECORD = new PropertiesSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "export.SaleRecord")
                    .addDefaultValue("UnnamedClientRegex", "(cliente.*various)|(various.*cliente)")
                    .addDefaultValue("UnnamedClientValue", "0000")
                    .description("Configuration for export SaleRecord\n"
                                + "Regex4UnnamedClient:  Case insensitive Regex to mark client as 'Cliente various'");

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute RECORDMGTM_SSO_ENDPOINTURI = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "RecordManagement.sso.EndpointURI")
                    .defaultValue("https://sso.synercom.pe/auth/realms/SynerCOM/protocol/openid-connect/token")
                    .description("URI of the RecordManagement SSO.");

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute RECORDMGTM_SSO_CLIENTID = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "RecordManagement.sso.ClientId")
                    .defaultValue("MISSING CONFIG")
                    .description("Fiscus RecordManagement CLientId");

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute RECORDMGTM_SSO_CLIENTSECRET = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "RecordManagement.sso.ClientSecret")
                    .defaultValue("MISSING CONFIG")
                    .description("Fiscus RecordManagement CLientId");

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute RECORDMGTM_SSO_USERNAME = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "RecordManagement.sso.UserName")
                    .defaultValue("MISSING CONFIG")
                    .description("Fiscus RecordManagement CLientId");

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute RECORDMGTM_SSO_PWD = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "RecordManagement.sso.Password")
                    .defaultValue("MISSING CONFIG")
                    .description("Fiscus RecordManagement CLientId");
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute RECORDMGTM_ENDPOINTURI = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "RecordManagement.EndpointURI")
                    .defaultValue("https://record-management.synercom.pe/api/service/records")
                    .description("URI of RecordManagement.");

    /**
     * @return the SystemConfigruation for Sales
     * @throws CacheReloadException on error
     */
    public static SystemConfiguration getSysConfig()
        throws CacheReloadException
    {
        return SystemConfiguration.get(ElectronicBilling.SYSCONFUUID);
    }
}
