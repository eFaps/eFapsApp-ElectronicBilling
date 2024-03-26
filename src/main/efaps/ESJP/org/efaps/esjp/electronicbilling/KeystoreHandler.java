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
package org.efaps.esjp.electronicbilling;

/*
 * Copyright 2003 - 2022 The eFaps Team
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
import java.io.IOException;
import java.util.Map;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Checkin;
import org.efaps.db.Context;
import org.efaps.db.Context.FileParameter;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIEBilling;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("75bf966d-9e35-4683-96d1-f3db0926e094")
@EFapsApplication("eFapsApp-ElectronicBilling")
public class KeystoreHandler
{

    private static final Logger LOG = LoggerFactory.getLogger(KeystoreHandler.class);

    public Return upload(final Parameter _parameter)
        throws EFapsException, IOException
    {
        LOG.info("===Upload of Keystore===");

        final Map<String, FileParameter> fileParameters = Context.getThreadContext().getFileParameters();
        if (fileParameters.containsKey("keystore")) {
            final var fileItem = fileParameters.get("keystore");
            final var inst = EQL.builder().insert(CIEBilling.Keystore)
                .set(CIEBilling.Keystore.Name,_parameter.getParameterValue("name"))
                .stmt()
                .execute();
            final var checkin = new Checkin(inst);
            try {
                checkin.execute(fileItem.getName(), fileItem.getInputStream(), (int) fileItem.getSize());
            } catch (final IOException e) {
                throw new EFapsException(this.getClass(), "execute", e, _parameter);
            }
        }
        return new Return();
    }
}
