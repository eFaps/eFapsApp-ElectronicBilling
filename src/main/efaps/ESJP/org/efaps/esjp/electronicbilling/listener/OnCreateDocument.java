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
package org.efaps.esjp.electronicbilling.listener;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsListener;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.listener.IOnCreateDocument;
import org.efaps.util.EFapsException;

@EFapsUUID("42065bfe-e1a9-4a7d-90ac-e85d41bc2353")
@EFapsApplication("eFapsApp-ElectronicBilling")
@EFapsListener
public class OnCreateDocument
    implements IOnCreateDocument
{

    @Override
    public int getWeight()
    {
        return 10;
    }

    @Override
    public void afterCreate(final Parameter parameter,
                            final Instance instance)
        throws EFapsException
    {
        if (InstanceUtils.isValid(instance)) {
            final var status = EQL.builder().print(instance)
                            .status().as("status")
                            .evaluate()
                            .get("status");
            if (status != null) {
                new OnStatusChange().afterSetStatus(parameter, instance, (Status) status);
            }
        }
    }
}
