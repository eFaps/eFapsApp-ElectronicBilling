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
package org.efaps.esjp.electronicbilling.export;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang3.StringUtils;
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
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.file.FileUtil;
import org.efaps.esjp.data.columns.export.FrmtDateTimeColumn;
import org.efaps.esjp.data.columns.export.FrmtNumberColumn;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.electronicbilling.util.ElectronicBilling;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.sales.tax.xml.TaxEntry;
import org.efaps.esjp.sales.tax.xml.Taxes;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.DateTimeUtil;
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
        final var fromDate = DateTimeUtil.toDate(parameter.getParameterValue("from"));
        final var toDate = DateTimeUtil.toDate(parameter.getParameterValue("to"));
        final CsvExportOptions options = new CsvExportOptions();
        options.setLineSeparator(LineSeparatorType.UNIX);
        options.setPrintHeaders(true);
        try {
            final var file = new FileUtil().getFile("SaleRecord.csv");
            final var writer = new FileWriterWithEncoding(file, "UTF-8");
            final var exporter = new CsvExporter(options, writer);
            addColumns(exporter);
            fill(exporter, fromDate, toDate);
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
        exporter.addColumns(new StringColumn("ledger", "SUB", 3));
        exporter.addColumns(new StringColumn("costCenter", "COSTO", 3));
        exporter.addColumns(new StringColumn("account", "CTACBLE", 3));
        exporter.addColumns(new StringColumn("condition", "GLOSA", 3));
        exporter.addColumns(new StringColumn("refType", "TDOC REF", 3));
        exporter.addColumns(new StringColumn("refName", "NUMERO REF", 3));
        exporter.addColumns(new FrmtDateTimeColumn("refDate", "FECHA REF", 10, "dd/MM/yyyy"));
        exporter.addColumns(new StringColumn("refVAT", "IGV REF", 3));
        exporter.addColumns(new StringColumn("refCrossTotal", "BASE IMP REF", 3));
    }

    protected void fill(final DataExporter exporter, LocalDate fromDate, LocalDate toDate)
        throws EFapsException
    {
        final List<DataBean> beans = new ArrayList<>();
        final var print = EQL.builder().print().query(CIEBilling.Invoice, CIEBilling.Receipt, CIEBilling.CreditNote)
                        .where()
                            .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                                .attribute(CISales.DocumentAbstract.Date).greaterOrEq(fromDate.toString())
                        .and()
                            .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                                .attribute(CISales.DocumentAbstract.Date).lessOrEq(toDate.toString())
                        .select()
                        .status().key().as("statusKey")
                        .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract).instance().as("docInst")
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
                            .attribute(CISales.ChannelConditionAbstract.Name).first().as("condition");
        //sysconf
        if (true) {
            print.linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                .linkfrom(CIHumanResource.Department2DocumentAbstract.ToAbstractLink)
                .linkto(CIHumanResource.Department2DocumentAbstract.FromAbstractLink)
                .instance().first().as("depInst");
        }

        final var eval = print.evaluate();
        while (eval.next()) {
            final boolean isCanceled = "Canceled".equals(eval.get("statusKey"));

            final var docInst = eval.<Instance>get("docInst");
            final var eDocInst = eval.inst();
            final var currencyInst = CurrencyInst.get(eval.<Long>get("rateCurrencyId"));
            var doi = eval.<String>get("taxNumber");
            if (StringUtils.isEmpty(doi)) {
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

            evalUnnamedClient(dataBean);

            if (isCanceled) {
                dataBean.setDoi("0001")
                    .setClient("FACTURAS ANULADAS")
                    .setNetTotal(null)
                    .setVat(null)
                    .setCrossTotal(null)
                    .setCondition("ANULADO");
            }

            if (true) {
                final var depInst = eval.<Instance>get("depInst");
                if (InstanceUtils.isType(depInst, CIHumanResource.Department)) {
                    evalDepartment(dataBean, depInst);
                }
            }

            if (InstanceUtils.isType(eDocInst, CIEBilling.CreditNote)) {
                final var refEval = EQL.builder().print().query(CISales.CreditNote2Invoice, CISales.CreditNote2Receipt)
                                .where().attribute(CISales.Document2DocumentAbstract.FromAbstractLink).eq(docInst)
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
                    final var refVat = refRateTaxes.getEntries().stream().map(TaxEntry::getAmount)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    dataBean.setRefType(evalType(refIns))
                        .setRefDate(refEval.get("date"))
                        .setRefName(refEval.get("name"))
                        .setRefVAT(refVat)
                        .setRefCrossTotal(refEval.get("rateCrossTotal"));
                }
                if (Sales.CREDITNOTE_REASON.get()) {
                    final var reasonEval = EQL.builder()
                        .print(docInst)
                            .linkto(CISales.CreditNote.CreditReason)
                                .attribute(CISales.AttributeDefinitionCreditReason.Description)
                        .evaluate();
                    reasonEval.next();
                    dataBean.setCondition(reasonEval.get(1));
                }
            }
            beans.add(dataBean);
        }
        final ComparatorChain<DataBean> chain = new ComparatorChain<>();
        chain.addComparator((_o1, _o2) -> _o1.getType().compareTo(_o2.getType()));
        chain.addComparator((_o1, _o2) -> _o1.getName().compareTo(_o2.getName()));
        Collections.sort(beans, chain);
        beans.forEach(dataBean -> {exporter.addBeanRows(dataBean);});
    }

    protected void evalDepartment(final DataBean dataBean,
                                  final Instance depInst)
        throws EFapsException
    {
        final var properties = ElectronicBilling.EXPORT_SALERECORD.get();
        final var ledger = properties.getProperty(depInst.getOid() + ".ledger",
                        "Missing config: " + depInst.getOid() + ".ledger");
        final var costCenter = properties.getProperty(depInst.getOid() + ".costCenter",
                        "Missing config: " + depInst.getOid() + ".costCenter");
        final var account = properties.getProperty(depInst.getOid() + ".account",
                        "Missing config: " + depInst.getOid() + ".account");

        dataBean.setLedger(ledger).setCostCenter(costCenter).setAccount(account);
    }

    protected void evalUnnamedClient(final DataBean dataBean)
        throws EFapsException
    {
        final var properties = ElectronicBilling.EXPORT_SALERECORD.get();
        final var regex = properties.getProperty("UnnamedClientRegex", "(cliente.*varios)|(varios.*cliente)");
        final var value = properties.getProperty("UnnamedClientValue", "0000");
        final Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        final var matcher = pattern.matcher(dataBean.getClient());
        if (matcher.matches()) {
            dataBean.setDoi(value);
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
        private String ledger;
        private String costCenter;
        private String account;

        public String getLedger()
        {
            return ledger;
        }

        public DataBean setLedger(String ledger)
        {
            this.ledger = ledger;
            return this;
        }

        public String getCostCenter()
        {
            return costCenter;
        }

        public DataBean setCostCenter(String costCenter)
        {
            this.costCenter = costCenter;
            return this;
        }

        public String getAccount()
        {
            return account;
        }

        public DataBean setAccount(String account)
        {
            this.account = account;
            return this;
        }

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
