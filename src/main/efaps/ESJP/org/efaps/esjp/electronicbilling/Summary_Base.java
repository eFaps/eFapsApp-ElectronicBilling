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
package org.efaps.esjp.electronicbilling;

import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.esjp.ci.CIEBilling;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.electronicbilling.util.ElectronicBilling;
import org.efaps.util.EFapsException;
import org.efaps.util.UUIDUtil;

@EFapsUUID("73b8d286-4563-47a9-ba00-7b1492cec4b4")
@EFapsApplication("eFapsApp-ElectronicBilling")
public abstract class Summary_Base
{

    public Return create(Parameter parameter)
        throws EFapsException
    {
        return new Create() {
            @Override
            protected void add2basicInsert(Parameter _parameter, Insert _insert) throws EFapsException {
                final var sequence = ElectronicBilling.SUMMARY_SEQ.get();
                NumberGenerator numberGenerator;
                if (UUIDUtil.isUUID(sequence)) {
                    numberGenerator = NumberGenerator.get(UUID.fromString(sequence));
                } else {
                    numberGenerator =  NumberGenerator.get(sequence);
                }
                _insert.add(CIEBilling.Summary.Name, numberGenerator.getNextVal());
            }
        }.execute(parameter);
    }
}
