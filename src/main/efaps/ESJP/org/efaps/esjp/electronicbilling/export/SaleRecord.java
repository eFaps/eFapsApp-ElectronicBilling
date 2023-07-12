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
package org.efaps.esjp.electronicbilling.export;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.dataexporter.DataExporter;
import org.efaps.dataexporter.LineSeparatorType;
import org.efaps.dataexporter.model.LineNumberColumn;
import org.efaps.dataexporter.model.StringColumn;
import org.efaps.dataexporter.output.csv.CsvExportOptions;
import org.efaps.dataexporter.output.csv.CsvExporter;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIEBilling;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.file.FileUtil;
import org.efaps.esjp.data.columns.export.FrmtDateTimeColumn;
import org.efaps.esjp.data.columns.export.FrmtNumberColumn;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.sales.tax.xml.TaxEntry;
import org.efaps.esjp.sales.tax.xml.Taxes;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("773ed3f2-3df1-4b0e-b65d-5f5a3706e7a4")
@EFapsApplication("eFapsApp-ElectronicBilling")
public class SaleRecord
{

    private static final Logger LOG = LoggerFactory.getLogger(SaleRecord.class);

    public Return export(final Parameter parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final CsvExportOptions options = new CsvExportOptions();
        options.setLineSeparator(LineSeparatorType.UNIX);
        options.setPrintHeaders(true);
        try {
            final var file = new FileUtil().getFile("SaleRecord.csv");
            final var writer = new FileWriterWithEncoding(file, "UTF-8");
            final var exporter = new CsvExporter(options, writer);
            addColumns(exporter);
            fill(exporter);
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        } catch (final IOException | EFapsException e) {
            LOG.error("Catched", e);
        }
        return ret;
    }

    protected void addColumns(final DataExporter exporter)
    {
        exporter.addColumns(new LineNumberColumn("item", "ITEM", 3));
        exporter.addColumns(new FrmtDateTimeColumn("date", "EMISIÓN", 10, "dd/MM/yyyy"));
        exporter.addColumns(new FrmtDateTimeColumn("dueDate", "VENCIMIENTO", 10, "dd/MM/yyyy"));
        exporter.addColumns(new StringColumn("type", "TIPO", 3));
        exporter.addColumns(new StringColumn("name", "NUMERO", 20));
        exporter.addColumns(new StringColumn("currency", "MONEDA", 4));
        exporter.addColumns(new StringColumn("doi", "CODIGO|RUC|DNI", 11));
        exporter.addColumns(new StringColumn("client", "CLIENTE", 256));
        exporter.addColumns(new FrmtNumberColumn("netTotal", 10, 2).withTitle("VALOR"));
        exporter.addColumns(new StringColumn("empty", "VENTA BOLSA", 3));
        exporter.addColumns(new StringColumn("empty", "EXONERADO", 3));
        exporter.addColumns(new FrmtNumberColumn("vat", 10, 2).withTitle("IGV"));
        exporter.addColumns(new StringColumn("empty", "ICBPER", 3));
        exporter.addColumns(new StringColumn("empty", "PERCEPCIÓN", 3));
        exporter.addColumns(new FrmtNumberColumn("crossTotal", 10, 2).withTitle("TOTAL"));
        exporter.addColumns(new StringColumn("empty", "SUB", 3));
        exporter.addColumns(new StringColumn("empty", "COSTO", 3));
        exporter.addColumns(new StringColumn("empty", "CTACBLE", 3));
        exporter.addColumns(new StringColumn("condition", "GLOSA", 3));

        exporter.addColumns(new StringColumn("refType", "TDOC REF", 3));
        exporter.addColumns(new StringColumn("refName", "NUMERO REF", 3));
        exporter.addColumns(new StringColumn("refDate", "FECHA REF", 3));
        exporter.addColumns(new StringColumn("refVAT", "IGV REF", 3));
        exporter.addColumns(new StringColumn("refCrossTotal", "BASE IMP REF", 3));
    }

    protected void fill(final DataExporter exporter)
        throws EFapsException
    {
        final var eval = EQL.builder().print().query(CIEBilling.Invoice, CIEBilling.Receipt, CIEBilling.CreditNote)
                        .select()
                        .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                            .attribute(CISales.DocumentSumAbstract.Date).as("date")
                        .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                            .attribute(CISales.DocumentSumAbstract.DueDate).as("dueDate")
                        .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                            .attribute(CISales.DocumentSumAbstract.Name).as("name")
                        .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                            .attribute(CISales.DocumentSumAbstract.RateCurrencyId).as("rateCurrencyId")
                        .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                            .linkto(CISales.DocumentSumAbstract.Contact)
                            .clazz(CIContacts.ClassOrganisation)
                            .attribute(CIContacts.ClassOrganisation.TaxNumber).as("taxNumber")
                        .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                            .linkto(CISales.DocumentSumAbstract.Contact)
                            .clazz(CIContacts.ClassPerson)
                            .attribute(CIContacts.ClassPerson.IdentityCard).as("identityCard")
                        .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                            .linkto(CISales.DocumentSumAbstract.Contact)
                            .attribute(CIContacts.Contact.Name).as("contactName")
                        .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                            .attribute(CISales.DocumentSumAbstract.RateNetTotal).as("rateNetTotal")
                        .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                            .attribute(CISales.DocumentSumAbstract.RateTaxes).as("rateTaxes")
                        .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                            .attribute(CISales.DocumentSumAbstract.RateCrossTotal).as("rateCrossTotal")
                        .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                            .linkfrom(CISales.ChannelSalesCondition2DocumentAbstract.ToAbstractLink)
                            .linkto(CISales.ChannelSalesCondition2DocumentAbstract.FromAbstractLink)
                            .attribute(CISales.ChannelConditionAbstract.Name).first().as("condition")
                        .evaluate();

        while (eval.next()) {
            final var eDocInst = eval.inst();
            final var currencyInst = CurrencyInst.get(eval.<Long>get("rateCurrencyId"));
            var doi = eval.<String>get("taxNumber");
            if (doi == null) {
                doi = eval.get("identityCard");
            }
            final var dueDate = eval.<LocalDate>get("dueDate");

            final Taxes rateTaxes = eval.get("rateTaxes");
            final var vat = rateTaxes.getEntries().stream().map(TaxEntry::getAmount).reduce(BigDecimal.ZERO,
                            BigDecimal::add);
            final var dataBean = new DataBean()
                            .setDate(eval.get("date"))
                            .setDueDate(dueDate == null ? null : dueDate)
                            .setType(evalType(eDocInst))
                            .setName(eval.get("name"))
                            .setCurrency(currencyInst.getSymbol())
                            .setDoi(doi)
                            .setClient(eval.get("contactName"))
                            .setNetTotal(eval.get("rateNetTotal"))
                            .setVat(vat)
                            .setCrossTotal(eval.get("rateCrossTotal"))
                            .setCondition(eval.get("condition"));
            exporter.addBeanRows(dataBean);

            if (InstanceUtils.isType(eDocInst, CIEBilling.CreditNote)) {
                final var refEval = EQL.builder().print().query(CISales.CreditNote2Invoice, CISales.CreditNote2Receipt)
                                .where().attribute(CISales.Document2DocumentAbstract.FromAbstractLink).eq(eDocInst)
                                .select()
                                .linkto(CISales.Document2DocumentAbstract.ToAbstractLink).instance().as("refInst")
                                .linkto(CISales.Document2DocumentAbstract.ToAbstractLink)
                                .attribute(CISales.DocumentAbstract.Name).as("name")
                                .linkto(CISales.Document2DocumentAbstract.ToAbstractLink)
                                .attribute(CISales.DocumentAbstract.Date).as("date")
                                .linkto(CISales.Document2DocumentAbstract.ToAbstractLink)
                                .attribute(CISales.DocumentSumAbstract.RateCrossTotal).as("rateCrossTotal")
                                .linkto(CISales.Document2DocumentAbstract.ToAbstractLink)
                                .attribute(CISales.DocumentSumAbstract.RateTaxes).as("rateTaxes")
                                .evaluate();
                refEval.next();
                final Instance refIns = refEval.get("refInst");
                if (InstanceUtils.isValid(refIns)) {
                    final Taxes refRateTaxes = refEval.get("rateTaxes");
                    final var refVat = refRateTaxes.getEntries().stream().map(TaxEntry::getAmount).reduce(BigDecimal.ZERO,
                                    BigDecimal::add);
                    dataBean.setRefType(evalType(refIns))
                        .setRefDate(refEval.get("date"))
                        .setRefName(refEval.get("name"))
                        .setRefVAT(refVat)
                        .setRefCrossTotal(refEval.get("rateCrossTotal"));
                }
            }
        }
    }

    protected String evalType(final Instance eDocIns)
    {
        String ret = "01";
        if (InstanceUtils.isType(eDocIns, CIEBilling.Receipt) || InstanceUtils.isType(eDocIns, CISales.Receipt)) {
            ret = "03";
        } else if (InstanceUtils.isType(eDocIns, CIEBilling.CreditNote)
                        || InstanceUtils.isType(eDocIns, CISales.CreditNote)) {
            ret = "07";
        }
        return ret;
    }

    public static class DataBean
    {

        private LocalDate date;
        private LocalDate dueDate;
        private String type;
        private String name;
        private String currency;
        private String doi;
        private String client;
        private BigDecimal netTotal;
        private BigDecimal vat;
        private BigDecimal crossTotal;
        private String refType;
        private String refName;
        private LocalDate refDate;
        private BigDecimal refVAT;
        private BigDecimal refCrossTotal;
        private String condition;
        private String empty;

        public String getCondition()
        {
            return condition;
        }

        public DataBean setCondition(String condition)
        {
            this.condition = condition;
            return this;
        }

        public String getRefType()
        {
            return refType;
        }

        public DataBean setRefType(String refType)
        {
            this.refType = refType;
            return this;
        }

        public String getRefName()
        {
            return refName;
        }

        public DataBean setRefName(String refName)
        {
            this.refName = refName;
            return this;
        }

        public LocalDate getRefDate()
        {
            return refDate;
        }

        public DataBean setRefDate(LocalDate refDate)
        {
            this.refDate = refDate;
            return this;
        }

        public BigDecimal getRefVAT()
        {
            return refVAT;
        }

        public DataBean setRefVAT(BigDecimal refVAT)
        {
            this.refVAT = refVAT;
            return this;
        }

        public BigDecimal getRefCrossTotal()
        {
            return refCrossTotal;
        }

        public DataBean setRefCrossTotal(BigDecimal refCrossTotal)
        {
            this.refCrossTotal = refCrossTotal;
            return this;
        }

        public BigDecimal getVat()
        {
            return vat;
        }

        public DataBean setVat(BigDecimal vat)
        {
            this.vat = vat;
            return this;
        }

        public BigDecimal getCrossTotal()
        {
            return crossTotal;
        }

        public DataBean setCrossTotal(BigDecimal crossTotal)
        {
            this.crossTotal = crossTotal;
            return this;
        }

        public void setEmpty(String empty)
        {
            this.empty = empty;
        }

        public String getEmpty()
        {
            return empty;
        }

        public BigDecimal getNetTotal()
        {
            return netTotal;
        }

        public DataBean setNetTotal(BigDecimal netTotal)
        {
            this.netTotal = netTotal;
            return this;
        }

        public LocalDate getDueDate()
        {
            return dueDate;
        }

        public DataBean setDueDate(LocalDate dueDate)
        {
            this.dueDate = dueDate;
            return this;
        }

        public String getType()
        {
            return type;
        }

        public DataBean setType(String type)
        {
            this.type = type;
            return this;
        }

        public String getName()
        {
            return name;
        }

        public DataBean setName(String name)
        {
            this.name = name;
            return this;
        }

        public String getCurrency()
        {
            return currency;
        }

        public DataBean setCurrency(String currency)
        {
            this.currency = currency;
            return this;
        }

        public String getDoi()
        {
            return doi;
        }

        public DataBean setDoi(String doi)
        {
            this.doi = doi;
            return this;
        }

        public String getClient()
        {
            return client;
        }

        public DataBean setClient(String client)
        {
            this.client = client;
            return this;
        }

        public LocalDate getDate()
        {
            return date;
        }

        public DataBean setDate(LocalDate date)
        {
            this.date = date;
            return this;
        }
    }
}
