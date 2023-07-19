/*
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
package org.efaps.esjp.electronicbilling.sync;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIEBilling;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.electronicbilling.soap.SoapClient;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.CacheReloadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("ac1ddc41-d60b-49ae-8efd-3bbccf6d4afc")
@EFapsApplication("eFapsApp-ElectronicBilling")
public class Synchronizer
{

    private static final Logger LOG = LoggerFactory.getLogger(Synchronizer.class);

    public void syncIssued(Parameter parameter)
        throws EFapsException
    {
        LOG.info("Syncing issued DeliveryNotes");
        final var eval = EQL.builder().print().query(CIEBilling.DeliveryNote)
                        .where().attribute(CIEBilling.DeliveryNote.Status)
                        .eq(Status.find(CIEBilling.DeliveryNoteStatus.Issued))
                        .select()
                        .linkto(CIEBilling.DeliveryNote.DeliveryNoteLink)
                        .attribute(CISales.DeliveryNote.Name).as("DocName")
                        .evaluate();

        final var client = new SoapClient();
        while (eval.next()) {
            final String docName = eval.get("DocName");
            LOG.info("Syncing: {}", docName);
            client.getStatus("09", docName);
        }
    }

    public void syncPending(Parameter parameter) throws CacheReloadException, EFapsException
    {
        LOG.info("Syncing issued DeliveryNotes");
        final var eval = EQL.builder().print().query(CIEBilling.DeliveryNote)
                        .where().attribute(CIEBilling.DeliveryNote.Status)
                        .eq(Status.find(CIEBilling.DeliveryNoteStatus.Pending))
                        .select()
                        .linkto(CIEBilling.DeliveryNote.DeliveryNoteLink)
                            .attribute(CISales.DeliveryNote.Name).as("DocName")
                        .evaluate();

        while (eval.next()) {
            final String docName = eval.get("DocName");
            LOG.info("Syncing: {}", docName);
        }

    }

}
