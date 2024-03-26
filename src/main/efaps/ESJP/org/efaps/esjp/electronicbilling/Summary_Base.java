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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Checkin;
import org.efaps.db.Checkout;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIEBilling;
import org.efaps.esjp.common.file.FileUtil;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.electronicbilling.util.ElectronicBilling;
import org.efaps.ubl.SummaryService;
import org.efaps.util.EFapsException;
import org.efaps.util.UUIDUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("73b8d286-4563-47a9-ba00-7b1492cec4b4")
@EFapsApplication("eFapsApp-ElectronicBilling")
public abstract class Summary_Base
{

    private static final Logger LOG = LoggerFactory.getLogger(Summary.class);

    public Return create(Parameter parameter)
        throws EFapsException
    {
        return new Create()
        {

            @Override
            protected void add2basicInsert(Parameter _parameter,
                                           Insert _insert)
                throws EFapsException
            {
                final var sequence = ElectronicBilling.SUMMARY_SEQ.get();
                NumberGenerator numberGenerator;
                if (UUIDUtil.isUUID(sequence)) {
                    numberGenerator = NumberGenerator.get(UUID.fromString(sequence));
                } else {
                    numberGenerator = NumberGenerator.get(sequence);
                }
                _insert.add(CIEBilling.Summary.Name, numberGenerator.getNextVal());
            }
        }.execute(parameter);
    }

    public Return ceateUBL(final Parameter _parameter)
        throws EFapsException
    {
        final var ret = new Return();
        final var instance = _parameter.getInstance();
        final var eval = EQL.builder().print()
                        .query(CIEBilling.Summary2Document)
                        .where()
                        .attribute(CIEBilling.Summary2Document.FromLink).eq(instance)
                        .select()
                        .linkto(CIEBilling.Summary2Document.ToLink).instance().as("docInst")
                        .evaluate();
        final var ubls = new ArrayList<String>();
        while (eval.next()) {
            final Instance docInst = eval.get("docInst");
            final var ubl = getUBL(docInst);
            if (ubl != null) {
                ubls.add(ubl);
            }
        }
        if (CollectionUtils.isNotEmpty(ubls)) {
            final var file = ceateSummary(instance, ubls.toArray(new String[0]));
            final var checkin = new Checkin(instance);
            try {
                checkin.execute("filename", new FileInputStream(file), Long.valueOf(file.length()).intValue());
            } catch (EFapsException | FileNotFoundException e) {
                LOG.error("Catched", e);
            }
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    public String getUBL(Instance docInstance)
        throws EFapsException
    {
        String ubl = null;
        final var eval2 = EQL.builder().print()
                        .query(CIEBilling.UBLFileAbstract)
                        .where()
                        .attribute(CIEBilling.UBLFileAbstract.DocumentLinkAbstract).eq(docInstance)
                        .select()
                        .instance()
                        .evaluate();

        if (eval2.next()) {
            final Instance fileInst = eval2.inst();
            final var checkout = new Checkout(fileInst);
            final var inputStream = checkout.execute();
            try {
                ubl = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            } catch (final IOException e) {
                LOG.error("Catched", e);
            }
        }
        return ubl;
    }

    public File ceateSummary(final Instance summaryInstance, String[] ubls)
        throws EFapsException
    {
        File file = null;
        final var ublService = new UBLService();
        final var summaryService = new SummaryService();
        final var summary = summaryService.createSummary(ubls);
        summary.setNumber("RC-20230228-1001")
                        .setReferenceDate(LocalDate.of(2023, 02, 28))
                        .setIssueDate(LocalDate.of(2023, 02, 28))
                        .setSupplier(ublService.getSupplier());
        final var ublXml = summary.getUBLXml();
        LOG.info("UBL: {}", ublXml);
        final var signResponse = ublService.sign(ublXml);
        LOG.info("signResponse: Hash {}\n UBL {}", signResponse.getHash(), signResponse.getUbl());
        try {
            file = new FileUtil().getFile("demo", "xml");
            FileUtils.writeStringToFile(file, signResponse.getUbl(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            LOG.error("Catched", e);
        }
        return file;
    }
}
