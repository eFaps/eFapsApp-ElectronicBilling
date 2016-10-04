package org.efaps.esjp.electronicbilling.util;

import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.api.annotation.EFapsSysConfAttribute;
import org.efaps.api.annotation.EFapsSystemConfiguration;
import org.efaps.esjp.admin.common.systemconfiguration.PropertiesSysConfAttribute;
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
    public static final PropertiesSysConfAttribute QUERYBLDR4DOCSCAN = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "QueryBldr4DocumentScanner")
                    .description("QueryBuilder for Include Scanner")
                    .addDefaultValue("Type01", CISales.Invoice.getType().getName())
                    .addDefaultValue("StatusGroup01", CISales.InvoiceStatus.getType().getName())
                    .addDefaultValue("Status01", CISales.InvoiceStatus.Open.key)
                    .addDefaultValue("StatusGroup02", CISales.InvoiceStatus.getType().getName())
                    .addDefaultValue("Status02", CISales.InvoiceStatus.Paid.key);

    /**
     * @return the SystemConfigruation for Sales
     * @throws CacheReloadException on error
     */
    public static SystemConfiguration getSysConfig()
        throws CacheReloadException
    {
        return SystemConfiguration.get(SYSCONFUUID);
    }
}
