package org.efaps.esjp.electronicbilling.util;

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
                    .addDefaultValue(CISales.DeliveryNote.getType().getName(),
                                    CIEBilling.DeliveryNote.getType().getName())
                    .addDefaultValue(CIEBilling.DeliveryNote.getType().getName() + ".CreateStatus",
                                    CIEBilling.DeliveryNoteStatus.Pending.key);

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
                    .defaultValue(CISales.DeliveryNoteStatus.Closed.key);

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
    public static final PropertiesSysConfAttribute DELIVERYNOTE_VERIFY= new PropertiesSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "DeliveryNote.Verification")
                    .description("Properties that permit to define when an Electronic DeliveryNote should be aborted.");

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute DELIVERYNOTE_ENDPOINTURI = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "DeliveryNote.SoapEndpointURI")
                    .defaultValue("https://e-beta.sunat.gob.pe/ol-ti-itemision-guia-gem-beta/billService")
                    .description("URI of the SOAP to send to.");

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute DELIVERYNOTE_SOAPUSER = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "DeliveryNote.SoapUser")
                    .defaultValue("20100066603MODDATOS")
                    .description("Username for SOAP Service");

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute DELIVERYNOTE_SOAPPWD = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "DeliveryNote.SoapPassword")
                    .defaultValue("moddatos")
                    .description("Password for SOAP Service");

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
                    .description("Mapping of Serial to Establecimiento\n"
                                    + "Uses StartsWith as comparision.\n"
                                    + "F001=14\n"
                                    + "FC003=14");

    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute TAXMAPPING = new PropertiesSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "TaxMapping")
                    .description("Tax Mapping")
                    .addDefaultValue("tax.06e40be6-40d8-44f4-9d8f-585f2f97ce63.id", "VAT")
                    .addDefaultValue("tax.06e40be6-40d8-44f4-9d8f-585f2f97ce63.nombre", "IGV")
                    .addDefaultValue("tax.06e40be6-40d8-44f4-9d8f-585f2f97ce63.sunat-id", "1000")
                    .addDefaultValue("tax.06e40be6-40d8-44f4-9d8f-585f2f97ce63.afectacion-igv", "10");

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
    public static final StringSysConfAttribute UBL_FILETYPE = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "ubl.FileType")
                    .description("FileType");

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute UBL_RESPONSE_FILETYPE = new StringSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "ubl.ResponseFileType")
                    .description("ResponseFileType");

    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute EXPORT_SALERECORD = new PropertiesSysConfAttribute()
                    .sysConfUUID(ElectronicBilling.SYSCONFUUID)
                    .key(ElectronicBilling.BASE + "export.SaleRecord")
                    .addDefaultValue("UnnamedClientRegex", "(cliente.*various)|(various.*cliente)")
                    .addDefaultValue("UnnamedClientValue", "0000")
                    .description("Configuration for export SaleRecord\n"
                                + "Regex4UnnamedClient:  Case insensitive Regex to mark client as 'Cliente various'");




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
