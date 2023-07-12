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
        final CsvExportOptions options = new CsvExportOptions();
        options.setLineSeparator(LineSeparatorType.UNIX);
        options.setPrintHeaders(true);
        try {
            final var file = new FileUtil().getFile("SaleRecord.csv");
            final var writer = new FileWriterWithEncoding(file, "UTF-8");
            final var exporter = new CsvExporter(options, writer);
            addColumns(exporter);
            fill(exporter);
        } catch (final IOException | EFapsException e) {
            LOG.error("Catched", e);
        }
        final Return ret = new Return();
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
        exporter.addColumns(new FrmtNumberColumn("igv", 10, 2).withTitle("IGV"));
        exporter.addColumns(new StringColumn("empty", "ICBPER", 3));
        exporter.addColumns(new StringColumn("empty", "PERCEPCIÓN", 3));
        exporter.addColumns(new FrmtNumberColumn("crossTotal", 10, 2).withTitle("TOTAL"));

        exporter.addColumns(new StringColumn("empty", "SUB", 3));
        exporter.addColumns(new StringColumn("empty", "COSTO", 3));
        exporter.addColumns(new StringColumn("empty", "CTACBLE", 3));
        exporter.addColumns(new StringColumn("empty", "GLOSA", 3));
        exporter.addColumns(new StringColumn("empty", "TDOC REF", 3));
        exporter.addColumns(new StringColumn("empty", "NUMERO REF", 3));
        exporter.addColumns(new StringColumn("empty", "FECHA REF", 3));
        exporter.addColumns(new StringColumn("empty", "IGV REF", 3));
        exporter.addColumns(new StringColumn("empty", "BASE IMP REF", 3));
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
                        .clazz(CIContacts.ClassOrganisation).attribute(CIContacts.ClassOrganisation.TaxNumber)
                        .as("taxNumber")
                        .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                        .linkto(CISales.DocumentSumAbstract.Contact)
                        .clazz(CIContacts.ClassPerson).attribute(CIContacts.ClassPerson.IdentityCard)
                        .as("identityCard")
                        .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                        .linkto(CISales.DocumentSumAbstract.Contact)
                        .attribute(CIContacts.Contact.Name).as("contactName")
                        .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                        .attribute(CISales.DocumentSumAbstract.RateNetTotal).as("rateNetTotal")
                        .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                        .attribute(CISales.DocumentSumAbstract.RateTaxes).as("rateTaxes")
                        .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                        .attribute(CISales.DocumentSumAbstract.RateCrossTotal).as("rateCrossTotal")
                        .evaluate();

        while (eval.next()) {
            final var currencyInst = CurrencyInst.get(eval.<Long>get("rateCurrencyId"));
            var doi = eval.<String>get("taxNumber");
            if (doi == null) {
                doi = eval.get("identityCard");
            }
            final var dueDate = eval.<LocalDate>get("dueDate");

            final Taxes rateTaxes = eval.get("rateTaxes");
            final var igv = rateTaxes.getEntries().stream().map(TaxEntry::getAmount).reduce(BigDecimal.ZERO,
                            BigDecimal::add);
            final var dataBean = new DataBean()
                            .setDate(eval.get("date"))
                            .setDueDate(dueDate == null ? null : dueDate)
                            .setType(evalType(eval.inst()))
                            .setName(eval.get("name"))
                            .setCurrency(currencyInst.getSymbol())
                            .setDoi(doi)
                            .setClient(eval.get("contactName"))
                            .setNetTotal(eval.get("rateNetTotal"))
                            .setIgv(igv)
                            .setCrossTotal(eval.get("rateCrossTotal"));
            exporter.addBeanRows(dataBean);
        }
    }

    protected String evalType(final Instance eDocIns) {
        String ret = "01";
        if (InstanceUtils.isType(eDocIns, CIEBilling.Receipt)) {
            ret = "03";
        } else if (InstanceUtils.isType(eDocIns, CIEBilling.CreditNote)) {
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
        private BigDecimal igv;

        private BigDecimal crossTotal;

        private String empty;

        public BigDecimal getIgv()
        {
            return igv;
        }

        public DataBean setIgv(BigDecimal igv)
        {
            this.igv = igv;
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
