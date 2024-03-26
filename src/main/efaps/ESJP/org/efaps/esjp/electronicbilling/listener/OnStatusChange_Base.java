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
package org.efaps.esjp.electronicbilling.listener;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.esjp.admin.datamodel.ISetStatusListener;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.electronicbilling.EBillingDocument;
import org.efaps.esjp.electronicbilling.util.ElectronicBilling;
import org.efaps.util.EFapsException;

/**
 * The Class OnStatusChange_Base.
 *
 * @author The eFaps Team
 */
@EFapsUUID("7ef82ee9-b2eb-43c7-9b2b-763ebe833256")
@EFapsApplication("eFapsApp-ElectronicBilling")
public abstract class OnStatusChange_Base
    implements ISetStatusListener
{

    @Override
    public void afterSetStatus(final Parameter _parameter,
                               final Instance _instance,
                               final Status _status)
        throws EFapsException
    {
        if (InstanceUtils.isType(_instance, CISales.Invoice) && ElectronicBilling.INVOICE_CREATEONSTATUS.exists()
                        && ElectronicBilling.INVOICE_CREATEONSTATUS.get().equals(_status.getKey())) {
            this.createDocument(_parameter, _instance);
        } else if (InstanceUtils.isType(_instance, CISales.Receipt) && ElectronicBilling.RECEIPT_CREATEONSTATUS.exists()
                        && ElectronicBilling.RECEIPT_CREATEONSTATUS.get().equals(_status.getKey())) {
            this.createDocument(_parameter, _instance);
        } else if (InstanceUtils.isType(_instance, CISales.Reminder) && ElectronicBilling.REMINDER_CREATEONSTATUS
                        .exists() && ElectronicBilling.REMINDER_CREATEONSTATUS.get().equals(_status.getKey())) {
            this.createDocument(_parameter, _instance);
        } else if (InstanceUtils.isType(_instance, CISales.CreditNote) && ElectronicBilling.CREDITNOTE_CREATEONSTATUS
                        .exists() && ElectronicBilling.CREDITNOTE_CREATEONSTATUS.get().equals(_status.getKey())) {
            this.createDocument(_parameter, _instance);
        }  else if (InstanceUtils.isType(_instance, CISales.DeliveryNote) && ElectronicBilling.DELIVERYNOTE_CREATEONSTATUS
                        .exists() && ElectronicBilling.DELIVERYNOTE_CREATEONSTATUS.get().equals(_status.getKey())) {
            this.createDocument(_parameter, _instance);
        }
    }

    /**
     * Creates the document.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _instance the instance
     * @throws EFapsException on error
     */
    protected void createDocument(final Parameter _parameter,
                                  final Instance salesDocInst)
        throws EFapsException
    {
        final Instance docInst = new EBillingDocument().createDocument(_parameter, salesDocInst);
        if (InstanceUtils.isValid(docInst)) {
            for (final IOnDocument listener : Listener.get().<IOnDocument>invoke(IOnDocument.class)) {
                listener.afterCreate(_parameter, docInst);
            }
        }
        Context.save();
        new EBillingDocument().createUBL(_parameter, docInst);
        new EBillingDocument().createReport4Document(_parameter, salesDocInst);
    }


    @Override
    public int getWeight()
    {
        return 0;
    }

}
