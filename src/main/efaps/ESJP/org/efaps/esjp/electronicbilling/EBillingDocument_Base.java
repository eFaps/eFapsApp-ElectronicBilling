/*
 * Copyright 2003 - 2016 The eFaps Team
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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIEBilling;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.electronicbilling.listener.IOnDocument;
import org.efaps.esjp.electronicbilling.util.ElectronicBilling;
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
        final Properties docProps = ElectronicBilling.DOCMAPPING.get();
        final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, props);

        final QueryBuilder attrQueryBldr = new QueryBuilder(CIEBilling.DocumentAbstract);
        queryBldr.addWhereAttrNotInQuery(CIERP.DocumentAbstract.ID, attrQueryBldr.getAttributeQuery(
                        CIEBilling.DocumentAbstract.DocumentLinkAbstract));
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        final List<Instance> instances = new ArrayList<>();
        while (query.next()) {
            final String typeName = query.getCurrentValue().getType().getName();
            final String typeUUID = query.getCurrentValue().getType().getUUID().toString();
            final String edoc = docProps.getProperty(typeName, docProps.getProperty(typeUUID));
            if (edoc != null) {
                final Type eType = isUUID(edoc) ? Type.get(UUID.fromString(edoc)) : Type.get(edoc);
                final String eTypeName = eType.getName();
                final String eTypeUUID = eType.getUUID().toString();
                final String edocStatusKey = docProps.getProperty(eTypeName + ".CreateStatus", docProps.getProperty(
                                eTypeUUID + ".CreateStatus"));
                if (edocStatusKey != null) {
                    final Status status = Status.find(eType.getStatusAttribute().getLink().getUUID(), edocStatusKey);
                    if (status != null) {
                        final Insert insert = new Insert(eType);
                        insert.add(CIEBilling.DocumentAbstract.DocumentLinkAbstract, query.getCurrentValue());
                        insert.add(CIEBilling.DocumentAbstract.StatusAbstract, status);
                        insert.executeWithoutAccessCheck();
                        instances.add(insert.getInstance());
                    }
                }
            }
        }
        for (final IOnDocument listener : Listener.get().<IOnDocument>invoke(IOnDocument.class)) {
            listener.afterCreate(_parameter, instances.toArray(new Instance[instances.size()]));
        }
        return new Return();
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
        getEmails(_parameter, _parameter.getInstance());
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
        if (InstanceUtils.isKindOf(_contactInstance, CIContacts.Contact)) {
            final PrintQuery print = new PrintQuery(_contactInstance);
            final SelectBuilder selEmails = SelectBuilder.get().clazz(CIContacts.Class)
                            .attributeset(CIContacts.Class.EmailSet, "attribute[ElectronicBilling]==true")
                            .attribute("Email");
            print.addSelect(selEmails);
            if (print.execute()) {
                final Object obj = print.getSelect(selEmails);
                if (obj instanceof List) {
                    ret.addAll((List<String>) obj);
                } else {
                    ret.add((String) obj);
                }
            }
        }
        return ret;
    }
}