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

import java.util.List;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.common.listener.ITypedClass;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.electronicbilling.EBillingDocument;
import org.efaps.esjp.electronicbilling.util.ElectronicBilling;
import org.efaps.esjp.erp.AbstractWarning;
import org.efaps.esjp.erp.IWarning;
import org.efaps.esjp.sales.document.Invoice;
import org.efaps.esjp.sales.listener.IOnValidation;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("fac6772e-f600-43a5-b0ab-7827efec421b")
@EFapsApplication("eFapsApp-ElectronicBilling")
public abstract class OnValidation_Base
    implements IOnValidation
{

    @Override
    public void validate(final Parameter _parameter,
                         final ITypedClass _doc,
                         final List<IWarning> _warnings)
        throws EFapsException
    {
        if (ElectronicBilling.ACTIVATEMAIL.get()) {
           if (_doc instanceof Invoice) {
               final Instance contactInst = Instance.get(_parameter.getParameterValue("contact"));
               if (InstanceUtils.isKindOf(contactInst, CIContacts.ContactAbstract)) {
                   final List<String> mails = new EBillingDocument().getEmails(_parameter, contactInst);
                   if (mails.isEmpty()) {
                       _warnings.add(new MissingEmail4EBillingWarning());
                   }
               }
           }
        }
    }

    @Override
    public int getWeight()
    {
        return 0;
    }

    /**
     * Warning that a product must be individual.
     */
    public static class MissingEmail4EBillingWarning
        extends AbstractWarning
    {
        /**
         * Constructor.
         */
        public MissingEmail4EBillingWarning()
        {
        }
    }
}
