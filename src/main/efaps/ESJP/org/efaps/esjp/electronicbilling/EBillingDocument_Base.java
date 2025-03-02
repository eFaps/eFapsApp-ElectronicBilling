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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.db.Context;
import org.efaps.db.Delete;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.db.stmt.selection.Evaluator;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIEBilling;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.electronicbilling.listener.IOnDocument;
import org.efaps.esjp.electronicbilling.util.ElectronicBilling;
import org.efaps.esjp.sales.document.CreditNote;
import org.efaps.esjp.sales.document.DeliveryNote;
import org.efaps.esjp.sales.document.Invoice;
import org.efaps.esjp.sales.document.Receipt;
import org.efaps.esjp.sales.document.Reminder;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

/**
 * The Class AbstractEBillingDocument_Base.
 *
 * @author The eFaps Team
 */
@EFapsUUID("171bd803-81f1-445e-ad79-5b18e35878b6")
@EFapsApplication("eFapsApp-ElectronicBilling")
public abstract class EBillingDocument_Base
    extends AbstractCommon
{

    /**
     * Resend.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return cancelDocument(final Parameter _parameter)
        throws EFapsException
    {
        final List<Instance> instances = new ArrayList<>();
        instances.add(_parameter.getInstance());
        boolean cancel = true;
        for (final IOnDocument listener : Listener.get().<IOnDocument>invoke(IOnDocument.class)) {
            cancel = cancel && listener.onCancel(_parameter, instances.toArray(new Instance[instances.size()]));
        }
        return new Return();
    }

    public void createDocument(final Parameter parameter)
        throws EFapsException
    {
        final Instance salesDocInst = parameter.getCallInstance();
        if (InstanceUtils.isKindOf(salesDocInst, CISales.DocumentAbstract)) {
            final Instance docInst = createDocument(parameter, salesDocInst);
            if (InstanceUtils.isValid(docInst)) {
                for (final IOnDocument listener : Listener.get().<IOnDocument>invoke(IOnDocument.class)) {
                    listener.afterCreate(parameter, docInst);
                }
            }
            Context.save();
            createUBL(parameter, docInst);
            createReport4Document(parameter, salesDocInst);
        }
    }

    /**
     * Creates the document.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst the doc inst
     * @return the instance
     * @throws EFapsException on error
     */
    public Instance createDocument(final Parameter _parameter,
                                   final Instance _docInst)
        throws EFapsException
    {
        Instance ret = null;
        if (InstanceUtils.isType(_docInst, CISales.CreditNote) && ElectronicBilling.CREDITNOTE_ACTIVE.get()
                        || InstanceUtils.isType(_docInst, CISales.Invoice) && ElectronicBilling.INVOICE_ACTIVE.get()
                        || InstanceUtils.isType(_docInst, CISales.Receipt) && ElectronicBilling.RECEIPT_ACTIVE.get()
                        || InstanceUtils.isType(_docInst, CISales.Reminder) && ElectronicBilling.REMINDER_ACTIVE.get()
                        || InstanceUtils.isType(_docInst, CISales.DeliveryNote)
                                        && ElectronicBilling.DELIVERYNOTE_ACTIVE.get()
                        || InstanceUtils.isType(_docInst, CISales.RetentionCertificate)
                                        && ElectronicBilling.RETENTIONCERTIFICATE_ACTIVE.get()) {

            final Properties docProps = ElectronicBilling.DOCMAPPING.get();
            final String typeName = _docInst.getType().getName();
            final String typeUUID = _docInst.getType().getUUID().toString();
            final String edoc = docProps.getProperty(typeName, docProps.getProperty(typeUUID));
            if (edoc != null) {
                final QueryBuilder queryBldr = new QueryBuilder(CIEBilling.DocumentAbstract);
                queryBldr.addWhereAttrEqValue(CIEBilling.DocumentAbstract.DocumentLinkAbstract, _docInst);
                if (queryBldr.getQuery().executeWithoutAccessCheck().isEmpty()) {
                    final Evaluator eval = EQL.builder().print(_docInst).attribute(CISales.DocumentAbstract.Name).evaluate();
                    final String name = eval.<String>get(CISales.DocumentAbstract.Name);
                    final String regex = docProps.getProperty(typeName + ".NameRegexMatch", ".*");
                    if (name.matches(regex)) {
                        final Type eType = isUUID(edoc) ? Type.get(UUID.fromString(edoc)) : Type.get(edoc);
                        final String eTypeName = eType.getName();
                        final String eTypeUUID = eType.getUUID().toString();
                        final String edocStatusKey = docProps.getProperty(eTypeName + ".CreateStatus",
                                        docProps.getProperty(
                                                        eTypeUUID + ".CreateStatus"));
                        if (edocStatusKey != null) {
                            final Status status = Status.find(eType.getStatusAttribute().getLink().getUUID(),
                                            edocStatusKey);
                            if (status != null) {
                                final Insert insert = new Insert(eType);
                                insert.add(CIEBilling.DocumentAbstract.DocumentLinkAbstract, _docInst);
                                insert.add(CIEBilling.DocumentAbstract.StatusAbstract, status);
                                insert.executeWithoutAccessCheck();
                                ret = insert.getInstance();
                            }
                        }
                    }
                }
            }
            ret = verifyElecDocInst(_parameter, ret);
        }
        return ret;
    }

    public void createUBL(final Parameter parameter,
                          final Instance eDocInst)
        throws EFapsException
    {
        if (InstanceUtils.isType(eDocInst, CIEBilling.DeliveryNote) && ElectronicBilling.DELIVERYNOTE_CREATEUBL.get()
                        || InstanceUtils.isType(eDocInst, CIEBilling.Invoice)
                                        && ElectronicBilling.INVOICE_CREATEUBL.get()
                        || InstanceUtils.isType(eDocInst, CIEBilling.Receipt)
                                        && ElectronicBilling.RECEIPT_CREATEUBL.get()
                        || InstanceUtils.isType(eDocInst, CIEBilling.CreditNote)
                                        && ElectronicBilling.CREDITNOTE_CREATEUBL.get()) {
            final var parameterClone = ParameterUtil.clone(parameter, ParameterValues.INSTANCE, eDocInst);
            new UBLService().ceateUBL(parameterClone);
        }
    }

    /**
     * Creates the report for document.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _salesDocInst the sales doc inst
     * @throws EFapsException on error
     */
    public void createReport4Document(final Parameter _parameter,
                                      final Instance _salesDocInst)
        throws EFapsException
    {
        if (InstanceUtils.isType(_salesDocInst, CISales.Invoice) && ElectronicBilling.INVOICE_CREATEREPORT.get()) {
            final Parameter parameter = ParameterUtil.clone(_parameter, ParameterValues.INSTANCE, _salesDocInst);
            ParameterUtil.setProperty(parameter, "JasperConfig", Sales.getSysConfig().getUUID().toString());
            ParameterUtil.setProperty(parameter, "JasperConfigReport", Sales.INVOICE_JASPERREPORT.getKey());
            ParameterUtil.setProperty(parameter, "JasperConfigMime", Sales.INVOICE_MIME.getKey());
            ParameterUtil.setProperty(parameter, "Checkin", "true");
            new Invoice().createReport(parameter);
        } else if (InstanceUtils.isType(_salesDocInst, CISales.Receipt) && ElectronicBilling.RECEIPT_CREATEREPORT
                        .get()) {

            final Parameter parameter = ParameterUtil.clone(_parameter, ParameterValues.INSTANCE, _salesDocInst);
            ParameterUtil.setProperty(parameter, "JasperConfig", Sales.getSysConfig().getUUID().toString());
            ParameterUtil.setProperty(parameter, "JasperConfigReport", Sales.RECEIPT_JASPERREPORT.getKey());
            ParameterUtil.setProperty(parameter, "JasperConfigMime", Sales.RECEIPT_MIME.getKey());
            ParameterUtil.setProperty(parameter, "Checkin", "true");
            new Receipt().createReport(parameter);
        } else if (InstanceUtils.isType(_salesDocInst, CISales.Reminder) && ElectronicBilling.REMINDER_CREATEREPORT
                        .get()) {

            final Parameter parameter = ParameterUtil.clone(_parameter, ParameterValues.INSTANCE, _salesDocInst);
            ParameterUtil.setProperty(parameter, "JasperConfig", Sales.getSysConfig().getUUID().toString());
            ParameterUtil.setProperty(parameter, "JasperConfigReport", Sales.REMINDER_JASPERREPORT.getKey());
            ParameterUtil.setProperty(parameter, "JasperConfigMime", Sales.REMINDER_MIME.getKey());
            ParameterUtil.setProperty(parameter, "Checkin", "true");
            new Reminder().createReport(parameter);
        } else if (InstanceUtils.isType(_salesDocInst, CISales.CreditNote) && ElectronicBilling.CREDITNOTE_CREATEREPORT
                        .get()) {

            final Parameter parameter = ParameterUtil.clone(_parameter, ParameterValues.INSTANCE, _salesDocInst);
            ParameterUtil.setProperty(parameter, "JasperConfig", Sales.getSysConfig().getUUID().toString());
            ParameterUtil.setProperty(parameter, "JasperConfigReport", Sales.CREDITNOTE_JASPERREPORT.getKey());
            ParameterUtil.setProperty(parameter, "JasperConfigMime", Sales.CREDITNOTE_MIME.getKey());
            ParameterUtil.setProperty(parameter, "Checkin", "true");
            new CreditNote().createReport(parameter);
        } else if (InstanceUtils.isType(_salesDocInst, CISales.DeliveryNote)
                        && ElectronicBilling.DELIVERYNOTE_CREATEREPORT.get()) {

            final Parameter parameter = ParameterUtil.clone(_parameter, ParameterValues.INSTANCE, _salesDocInst);
            ParameterUtil.setProperty(parameter, "JasperConfig", Sales.getSysConfig().getUUID().toString());
            ParameterUtil.setProperty(parameter, "JasperConfigReport", Sales.DELIVERYNOTE_JASPERREPORT.getKey());
            ParameterUtil.setProperty(parameter, "JasperConfigMime", Sales.DELIVERYNOTE_MIME.getKey());
            ParameterUtil.setProperty(parameter, "Checkin", "true");
            new DeliveryNote().createReport(parameter);
        }
    }

    /**
     * Gets the emails.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _contactInstance the contact instance
     * @return the emails
     * @throws EFapsException on error
     */
    public Return deletePreTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final Instance docInstance = _parameter.getInstance();
        final QueryBuilder queryBldr = new QueryBuilder(CIEBilling.DocumentLogAbstract);
        queryBldr.addWhereAttrEqValue(CIEBilling.DocumentLogAbstract.DocumentLinkAbstract, docInstance);
        for (final Instance protInst : queryBldr.getQuery().execute()) {
            new Delete(protInst).execute();
        }
        final QueryBuilder queryBldr2 = new QueryBuilder(CIEBilling.FileAbstract);
        queryBldr2.addWhereAttrEqValue(CIEBilling.FileAbstract.DocumentLinkAbstract, docInstance);
        for (final Instance protInst : queryBldr2.getQuery().execute()) {
            new Delete(protInst).execute();
        }
        return new Return();
    }

    /**
     * Gets the emails.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _contactInstance the contact instance
     * @return the emails
     * @throws EFapsException on error
     */
    public Return getEmails(final Parameter _parameter)
        throws EFapsException
    {
        this.getEmails(_parameter, _parameter.getInstance());
        return new Return();
    }

    /**
     * Gets the emails.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _contactInstance the contact instance
     * @return the emails
     * @throws EFapsException on error
     */
    @SuppressWarnings("unchecked")
    public List<String> getEmails(final Parameter _parameter,
                                  final Instance _contactInstance)
        throws EFapsException
    {
        final List<String> ret = new ArrayList<>();
        if (ElectronicBilling.ACTIVATEMAIL.get() && InstanceUtils.isKindOf(_contactInstance, CIContacts.Contact)) {
            final PrintQuery print = new PrintQuery(_contactInstance);
            final SelectBuilder selEmails = SelectBuilder.get().clazz(CIContacts.Class)
                            .attributeset(CIContacts.Class.EmailSet, "attribute[ElectronicBilling]==true")
                            .attribute("Email");
            print.addSelect(selEmails);
            if (print.execute()) {
                final Object obj = print.getSelect(selEmails);
                if (obj instanceof List) {
                    ret.addAll((List<String>) obj);
                } else if (obj != null) {
                    ret.add((String) obj);
                }
            }
        }
        return ret;
    }

    /**
     * Resend.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return resend(final Parameter _parameter)
        throws EFapsException
    {
        final List<Instance> instances = new ArrayList<>();
        instances.add(_parameter.getInstance());
        for (final IOnDocument listener : Listener.get().<IOnDocument>invoke(IOnDocument.class)) {
            listener.afterCreate(_parameter, instances.toArray(new Instance[instances.size()]));
        }
        return new Return();
    }

    /**
     * Scan for documents.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return scan4Documents(final Parameter _parameter)
        throws EFapsException
    {
        final Properties props = ElectronicBilling.QUERYBLDR4DOCSCAN.get();

        final QueryBuilder queryBldr = this.getQueryBldrFromProperties(_parameter, props);

        final QueryBuilder attrQueryBldr = new QueryBuilder(CIEBilling.DocumentAbstract);
        queryBldr.addWhereAttrNotInQuery(CIERP.DocumentAbstract.ID, attrQueryBldr.getAttributeQuery(
                        CIEBilling.DocumentAbstract.DocumentLinkAbstract));
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();

        final List<Instance> instances = new ArrayList<>();
        final List<Instance> sdocInsts = new ArrayList<>();
        while (query.next()) {
            sdocInsts.add(query.getCurrentValue());
            final Instance inst = createDocument(_parameter, query.getCurrentValue());
            if (InstanceUtils.isValid(inst)) {
                instances.add(inst);
            }
        }
        for (final IOnDocument listener : Listener.get().<IOnDocument>invoke(IOnDocument.class)) {
            listener.afterCreate(_parameter, instances.toArray(new Instance[instances.size()]));
        }
        Context.save();
        for (final Instance docInst : sdocInsts) {
            createReport4Document(_parameter, docInst);
        }
        return new Return();
    }

    /**
     * Check for a verification. If verify action is not passed, set status to
     * aborted and return null.
     *
     * @param _parameter the parameter
     * @return true, if successful
     */
    protected Instance verifyElecDocInst(final Parameter _parameter,
                                         final Instance _elecDocInst)
        throws EFapsException
    {
        Instance ret = _elecDocInst;
        if (InstanceUtils.isType(_elecDocInst, CIEBilling.CreditNote) && ElectronicBilling.CREDITNOTE_VERIFY.exists()
                        || InstanceUtils.isType(_elecDocInst, CIEBilling.Invoice)
                                        && ElectronicBilling.INVOICE_VERIFY.exists()
                        || InstanceUtils.isType(_elecDocInst, CIEBilling.Receipt)
                                        && ElectronicBilling.RECEIPT_VERIFY.exists()
                        || InstanceUtils.isType(_elecDocInst, CIEBilling.Reminder) && ElectronicBilling.REMINDER_VERIFY
                                        .exists()
                        || InstanceUtils.isType(_elecDocInst, CIEBilling.DeliveryNote)
                                        && ElectronicBilling.DELIVERYNOTE_VERIFY.exists()) {
            Properties props = null;
            if (InstanceUtils.isType(_elecDocInst, CIEBilling.Invoice)) {
                props = ElectronicBilling.INVOICE_VERIFY.get();
            } else if (InstanceUtils.isType(_elecDocInst, CIEBilling.Receipt)) {
                props = ElectronicBilling.RECEIPT_VERIFY.get();
            } else if (InstanceUtils.isType(_elecDocInst, CIEBilling.CreditNote)) {
                props = ElectronicBilling.CREDITNOTE_VERIFY.get();
            } else if (InstanceUtils.isType(_elecDocInst, CIEBilling.Reminder)) {
                props = ElectronicBilling.REMINDER_VERIFY.get();
            } else if (InstanceUtils.isType(_elecDocInst, CIEBilling.DeliveryNote)) {
                props = ElectronicBilling.DELIVERYNOTE_VERIFY.get();
            }
            if (props != null) {
                boolean pass = true;
                // test must return true to go on
                if (props.containsKey("PositivTest4RegexOnName")) {
                    final PrintQuery print = new PrintQuery(_elecDocInst);
                    final SelectBuilder selDocName = SelectBuilder.get()
                                    .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                                    .attribute(CIERP.DocumentAbstract.Name);
                    print.addSelect(selDocName);
                    print.executeWithoutAccessCheck();
                    final String docName = print.getSelect(selDocName);
                    if (!docName.matches(props.getProperty("PositivTest4RegexOnName"))) {
                        pass = false;
                    }
                } else if (props.containsKey("NegativTest4RegexOnName")) {
                    final PrintQuery print = new PrintQuery(_elecDocInst);
                    final SelectBuilder selDocName = SelectBuilder.get()
                                    .linkto(CIEBilling.DocumentAbstract.DocumentLinkAbstract)
                                    .attribute(CIERP.DocumentAbstract.Name);
                    print.addSelect(selDocName);
                    print.executeWithoutAccessCheck();
                    final String docName = print.getSelect(selDocName);
                    if (docName.matches(props.getProperty("NegativTest4RegexOnName"))) {
                        pass = false;
                    }
                }

                if (!pass) {
                    final Status status = Status.find(_elecDocInst.getType().getStatusAttribute().getLink().getUUID(),
                                    "Aborted");
                    final Update update = new Update(_elecDocInst);
                    update.add(CIEBilling.DocumentAbstract.StatusAbstract, status);
                    update.executeWithoutTrigger();
                    ret = null;
                }
            }
        }
        return ret;
    }

    public String getHash(final Instance salesDocInst)
        throws EFapsException
    {
        String ret = null;
        final Evaluator eval = EQL.builder().print().query(CIEBilling.DocumentAbstract)
                        .where()
                        .attribute(CIEBilling.DocumentAbstract.DocumentLinkAbstract).eq(salesDocInst)
                        .select()
                        .linkfrom(CIEBilling.UBLFileAbstract.DocumentLinkAbstract)
                        .attribute(CIEBilling.UBLFileAbstract.UBLHash).first().as("hash")
                        .evaluate();
        if (eval.next()) {
            ret = eval.get("hash");
        }
        return ret;
    }
}
