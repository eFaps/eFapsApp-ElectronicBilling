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
package org.efaps.esjp.electronicbilling.recordmanagement;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.user.Company;
import org.efaps.db.Context;
import org.efaps.util.EFapsException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("df3eb032-655b-4388-b2e8-d0f24aeee5b6")
@EFapsApplication("eFapsApp-ElectronicBilling")
public class PublischJob
    implements Job
{

    private static final Logger LOG = LoggerFactory.getLogger(PublischJob.class);

    @Override
    public void execute(JobExecutionContext context)
        throws JobExecutionException
    {
        try {

            for (final Long companyId : Context.getThreadContext().getPerson().getCompanies()) {
                final Company company = Company.get(companyId);
                Context.getThreadContext().setCompany(company);
                new Publish().scan4Documents();
            }
            // remove the company to be sure
            Context.getThreadContext().setCompany(null);
        } catch (final EFapsException e) {
            LOG.error("Catched error", e);
        }

    }

}
