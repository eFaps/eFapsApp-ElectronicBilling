package org.efaps.esjp.electronicbilling.util;

import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.api.annotation.EFapsSysConfAttribute;
import org.efaps.api.annotation.EFapsSystemConfiguration;
import org.efaps.esjp.admin.common.systemconfiguration.BooleanSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.PropertiesSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.StringSysConfAttribute;
import org.efaps.esjp.ci.CIEBilling;
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
                                    CIEBilling.ReminderStatus.Pending.key);

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
