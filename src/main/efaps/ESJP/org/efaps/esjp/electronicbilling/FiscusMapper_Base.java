/*
 * Copyright 2003 - 2020 The eFaps Team
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.CachedPrintQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.contacts.Contacts;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.electronicbilling.PaymentMethod.Installment;
import org.efaps.esjp.electronicbilling.util.ElectronicBilling;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.esjp.products.ProductFamily;
import org.efaps.esjp.products.util.Products;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.tax.Tax;
import org.efaps.esjp.sales.util.Sales.TaxRetention;
import org.efaps.number2words.Converter;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

@EFapsUUID("a1379109-a71a-4beb-af8b-16146339998f")
@EFapsApplication("eFapsApp-ElectronicBilling")
public abstract class FiscusMapper_Base
{

    /**
     * El Peruano N° 244-2019/SUNAT Catálogo No.51 Código de tipo de operación
     * <table>
     * <tr>
     * <th>Codigo</th>
     * <th>Descripcion</th>
     * <th>Tipo de Comprobante asociado</th>
     * </tr>
     * <tr>
     * <td>0101</td>
     * <td>Venta Interna</td>
     * <td>Factura, Boletas</td>
     * </tr>
     * <tr>
     * <td>0112</td>
     * <td>Venta Interna - Sustenta Gastos Deducibles Persona Natural</td>
     * <td>Factura</td>
     * </tr>
     * <tr>
     * <td>0113</td>
     * <td>Venta Interna-NRUS</td>
     * <td>Boleta</td>
     * </tr>
     * <tr>
     * <td>0200</td>
     * <td>Exportación de Bienes</td>
     * <td>Factura, Boletas</td>
     * </tr>
     * <tr>
     * <td>0201</td>
     * <td>Exportación de Servicios - Prestación servicios realizados
     * íntegramente en el país</td>
     * <td>Factura, Boletas</td>
     * </tr>
     * <tr>
     * <td>0202</td>
     * <td>Exportación de Servicios - Prestación de servicios de hospedaje No
     * Domiciliado</td>
     * <td>Factura, Boletas</td>
     * </tr>
     * <tr>
     * <td>0203</td>
     * <td>Exportación de Servicios - Transporte de navieras</td>
     * <td>Factura, Boletas</td>
     * </tr>
     * <tr>
     * <td>0204</td>
     * <td>Exportación de Servicios - Servicios a naves y aeronaves de bandera
     * extranjera</td>
     * <td>Factura, Boletas</td>
     * </tr>
     * <tr>
     * <td>0205</td>
     * <td>Exportación de Servicios - Servicios que conformen un paquete
     * turístico</td>
     * <td>Factura, Boletas</td>
     * </tr>
     * <tr>
     * <td>0206</td>
     * <td>Exportación de Servicios - Servicios complementarios al transporte de
     * carga</td>
     * <td>Factura, Boletas</td>
     * </tr>
     * <tr>
     * <td>0207</td>
     * <td>Exportación de Servicios - Suministro de energía eléctrica a favor de
     * sujetos domiciliados en ZED</td>
     * <td>Factura, Boletas</td>
     * </tr>
     * <tr>
     * <td>0208</td>
     * <td>Exportación de Servicios - Prestación servicios realizados
     * parcialmente en el extranjero</td>
     * <td>Factura, Boletas</td>
     * </tr>
     * <tr>
     * <td>0301</td>
     * <td>Operaciones con Carta de porte aéreo (emitidas en el ámbito
     * nacional)</td>
     * <td>Factura, Boletas</td>
     * </tr>
     * <tr>
     * <td>0302</td>
     * <td>Operaciones de Transporte ferroviario de pasajeros</td>
     * <td>Factura, Boletas</td>
     * </tr>
     * <tr>
     * <td>0401</td>
     * <td>Ventas no domiciliados que no califican como exportación</td>
     * <td>Factura, Boletas</td>
     * </tr>
     * <tr>
     * <td>0501</td>
     * <td>Compra interna</td>
     * <td>Liquidación de compra</td>
     * </tr>
     * <tr>
     * <td>0502</td>
     * <td>Anticipos</td>
     * <td>Liquidación de compra</td>
     * </tr>
     * <tr>
     * <td>0503</td>
     * <td>Compra de oro</td>
     * <td>Liquidación de compra</td>
     * </tr>
     * <tr>
     * <td>1001</td>
     * <td>Operación Sujeta a Detracción</td>
     * <td>Factura, Boletas</td>
     * </tr>
     * <tr>
     * <td>1002</td>
     * <td>Operación Sujeta a Detracción - Recursos Hidrobiológicos</td>
     * <td>Factura, Boletas</td>
     * </tr>
     * <tr>
     * <td>1003</td>
     * <td>Operación Sujeta a Detracción - Servicios de Transporte
     * Pasajeros</td>
     * <td>Factura, Boletas</td>
     * </tr>
     * <tr>
     * <td>1004</td>
     * <td>Operación Sujeta a Detracción - Servicios de Transporte Carga</td>
     * <td>Factura, Boletas</td>
     * </tr>
     * <tr>
     * <td>2001</td>
     * <td>Operación Sujeta a Percepción</td>
     * <td>Factura, Boletas</td>
     * </tr>
     * <tr>
     * <td>2100</td>
     * <td>Créditos a empresas</td>
     * <td>Factura, Boletas</td>
     * </tr>
     * <tr>
     * <td>2101</td>
     * <td>Créditos de consumo revolvente</td>
     * <td>Factura, Boletas</td>
     * </tr>
     * <tr>
     * <td>2102</td>
     * <td>Créditos de consumo no revolvente</td>
     * <td>Factura, Boletas</td>
     * </tr>
     * <tr>
     * <td>2103</td>
     * <td>Otras operaciones no gravadas - Empresas del sistema financiero
     * ycooperativas de ahorro y crédito no autorizadas a captar recursos del
     * público</td>
     * <td>Factura, Boletas</td>
     * </tr>
     * <tr>
     * <td>2104</td>
     * <td>Otras operaciones no gravadas - Empresas del sistema de seguros</td>
     * <td>Factura, Boletas
     * </table>
     *
     * @param _parameter Parameter
     * @param _docInst instance of the document
     * @param _contactInst instance of the contact
     * @throws EFapsException on error
     *
     */
    protected String getOperationType(final Parameter _parameter, final Instance _docInst, final Instance _contactInst)
        throws EFapsException
    {
        String ret = null;
        if (InstanceUtils.isType(_docInst, CISales.Invoice) || InstanceUtils.isType(_docInst, CISales.Receipt)) {
            // if it is an "export"
            if (Contacts.isForeign(_parameter, _contactInst)) {
                // check the type of product that the invoice / receipt contains
                // (only the first)
                final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionAbstract);
                queryBldr.addWhereAttrEqValue(CISales.PositionAbstract.DocumentAbstractLink, _docInst);
                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selProdInstant = SelectBuilder.get()
                                .linkto(CISales.PositionAbstract.Product).instance();
                multi.addSelect(selProdInstant);
                multi.executeWithoutAccessCheck();
                boolean service = false;
                if (multi.next()) {
                    final Instance prodInst = multi.getSelect(selProdInstant);
                    service = InstanceUtils.isKindOf(prodInst, CIProducts.UnstoreableProductAbstract);
                }
                if (service) {
                    ret = "0201";
                } else {
                    ret = "0200";
                }
            } else {
                ret = "0101";
            }
        }
        return ret;
    }

    protected String evalUNSPSC(final Instance _productInstance)
        throws EFapsException
    {
        String ret = null;
        if (Products.FAMILY_ACTIVATE_UNSPSC.get()) {
            final PrintQuery print = CachedPrintQuery.get4Request(_productInstance);
            final SelectBuilder selFamInts = SelectBuilder.get().linkto(CIProducts.ProductAbstract.ProductFamilyLink)
                            .instance();
            print.addSelect(selFamInts);
            print.executeWithoutAccessCheck();
            Instance inst = print.getSelect(selFamInts);
            while (StringUtils.isEmpty(ret) && InstanceUtils.isValid(inst)) {
                final PrintQuery famPrint = new CachedPrintQuery(inst, ProductFamily.CACHEKEY);
                final SelectBuilder selParentInst = SelectBuilder.get().linkto(
                                CIProducts.ProductFamilyStandart.ParentLink).instance();
                famPrint.addSelect(selParentInst);
                famPrint.addAttribute(CIProducts.ProductFamilyAbstract.UNSPSC);
                famPrint.execute();
                inst = famPrint.getSelect(selParentInst);
                ret = famPrint.<String>getAttribute(CIProducts.ProductFamilyAbstract.UNSPSC);
            }
        }
        return ret;
    }

    /**
     * Anexo VII-117-2017
     *
     * Catálogo No. 07: Códigos de Tipo de Afectación del IGV
     * <ul>
     * <li>10: Gravado - Operacion Onerosa</li>
     * <li>11: Gravado - Retiro por premio</li>
     * <li>12: Gravado - Retiro por donación</li>
     * <li>13: Gravado - Retiro</li>
     * <li>14: Gravado - Retiro por publicidad</li>
     * <li>15: Gravado - Bonificaciones</li>
     * <li>16: Gravado - Retiro por entrega a trabajadores</li>
     * <li>17: Gravado - IVAP</li>
     * <li>20: Exonerado - Operación Onerosa</li>
     * <li>21: Exonerado - Transferencia Gratuita</li>
     * <li>30: Inafecto - Operación Onerosa</li>
     * <li>31: Inafecto - Retiro por Bonificación</li>
     * <li>32: Inafecto - Retiro</li>
     * <li>33: Inafecto - Retiro por Muestras Médicas</li>
     * <li>34: Inafecto - Retiro por Convenio Colectivo</li>
     * <li>35: Inafecto - Retiro por premio</li>
     * <li>36: Inafecto - Retiro por publicidad</li>
     * <li>40: Exportación</li>
     * </ul>
     *
     * @return key
     * @throws EFapsException
     */
    protected String evalTaxAffectation4TaxFree(final Parameter _parameter, final Instance _contactInst)
        throws EFapsException
    {
        String ret;
        // if the contact is foreign we assume exportation
        // else taxfree operations
        if (Contacts.isForeign(_parameter, _contactInst)) {
            ret = "40";
        } else {
            ret = "20";
        }
        return ret;
    }

    /**
     * Anexo VII-117-2017
     *
     * <table>
     * <tr>
     * <th>Código</th>
     * <th>Descripción</th>
     * <th>UN/ECE 5153-Duty or tax or fee type name code</th>
     * </tr>
     * <tr>
     * <td>1000</td>
     * <td>IGV IMPUESTO GENERAL A LAS VENTAS</td>
     * <td>VAT</td>
     * </tr>
     * <tr>
     * <td>2000</td>
     * <td>ISC IMPUESTO SELECTIVO AL CONSUMO</td>
     * <td>EXC</td>
     * </tr>
     * <tr>
     * <td>9995</td>
     * <td>EXPORTACIÓN</td>
     * <td>FRE</td>
     * </tr>
     * <tr>
     * <td>9996</td>
     * <td>GRATUITO</td>
     * <td>FRE</td>
     * </tr>
     * <tr>
     * <td>9997</td>
     * <td>EXONERADO</td>
     * <td>VAT</td>
     * </tr>
     * <tr>
     * <td>9998</td>
     * <td>INAFECTO</td>
     * <td>FRE</td>
     * </tr>
     * <tr>
     * <td>9999</td>
     * <td>OTROS CONCEPTOS DE PAGO</td>
     * <td>OTH</td>
     * </tr>
     * </table>
     *
     * @param _parameter
     * @param _contactInst
     * @return
     * @throws EFapsException
     */
    protected String[] evalTaxCode4TaxFree(final Parameter _parameter, final Instance _contactInst)
        throws EFapsException
    {
        String[] ret;
        if (Contacts.isForeign(_parameter, _contactInst)) {
            ret = new String[] { "FRE", "9995", "EXP" };
        } else {
            // Email from Factus 2020-10-30 -> EXONERADO = EXO
            ret = new String[] { "VAT", "9997", "EXO" };
        }
        return ret;
    }

    protected String[] evalTaxCode4Gratis(final Parameter _parameter)
        throws EFapsException
    {
        return new String[] { "FRE", "9996", "GRA" };
    }

    protected String evalTaxAffectation4Gratis(final Parameter _parameter)
        throws EFapsException
    {

        return "11";
    }

    protected String getDocumentType4Document(final Instance _docInst)
    {
        String ret;
        if (_docInst.getType().isCIType(CISales.Invoice)) {
            ret = "01";
        } else if (_docInst.getType().isCIType(CISales.Receipt)) {
            ret = "03";
        } else if (_docInst.getType().isCIType(CISales.CreditNote)) {
            ret = "07";
        } else if (_docInst.getType().isCIType(CISales.Reminder)) {
            ret = "08";
        } else {
            ret = "UNKOWN";
        }
        return ret;
    }

    protected String evalPremisesCode(final Parameter _parameter, final Instance _docInst)
        throws EFapsException
    {
        String ret = null;
        if (ElectronicBilling.PREMISESCODE_BY_SERIAL.exists()) {
            final PrintQuery print = new PrintQuery(_docInst);
            print.addAttribute(CIERP.DocumentAbstract.Name);
            print.executeWithoutAccessCheck();
            final String docName = print.getAttribute(CIERP.DocumentAbstract.Name);

            for (final String key : ElectronicBilling.PREMISESCODE_BY_SERIAL.get().stringPropertyNames()) {
                if (docName.startsWith(key)) {
                    ret = ElectronicBilling.PREMISESCODE_BY_SERIAL.get().getProperty(key);
                    break;
                }
            }
        }
        return StringUtils.isEmpty(ret) ? ERP.COMPANY_ESTABLECIMIENTO.get() : ret;
    }

    protected String getTaxProperty(final Tax _tax, final String _key)
        throws EFapsException
    {
        return getTaxProperty(_tax.getUUID(), _key);
    }

    protected String getTaxProperty(final UUID uuid, final String _key)
        throws EFapsException
    {
        return ElectronicBilling.TAXMAPPING.get().getProperty("tax." + uuid.toString() + "." + _key);
    }

    protected String number2words(final BigDecimal _amount)
        throws EFapsException
    {
        return new StringBuilder().append(Converter.getMaleConverter(
                        new Locale("es")).convert(_amount.longValue())).append(" y ")
                        .append(_amount.setScale(2, RoundingMode.HALF_UP).toPlainString().replaceAll("^.*\\.", ""))
                        .append("/100 ").toString().toUpperCase();
    }

    protected boolean isGratis(final Calculator calculator)
    {
        return calculator.getDiscount().compareTo(new BigDecimal(100)) == 0;
    }

    protected boolean isFreeOfCharge(final Instance docInstance)
        throws EFapsException
    {
        final var eval = EQL.builder().print()
                        .query(CISales.FreeOfChargeTag)
                        .where()
                        .attribute(CISales.FreeOfChargeTag.ObjectID).eq(docInstance)
                        .select()
                        .attribute(CISales.FreeOfChargeTag.ID)
                        .evaluate();
        return eval.next();
    }

    // Rudolf 2021-09-07 El monto neto pendiente de pago no incluye las
    // retenciones del IGV, el monto del
    // deposito que deba efectuar el adquiriente o usuario, seg7un el Sistema de
    // Pago de Obligaciones Tributarios
    // regulado por el texto unico ordenado de Decreto Legislativo N940
    protected PaymentMethod getPaymentMethod(final Instance _docInst)
        throws EFapsException
    {
        final PaymentMethod ret = new PaymentMethod();
        if (InstanceUtils.isType(_docInst, CISales.CreditNote)) {
            // Factus 2021-09-19
            // NC con motivo 01 no debe llevar forma de pago.
            // En notas de credito solo se aplica la forma de pago al credito
            // cuando es motivo 13
            // y el documento relacionado ha sido pago al Credito y el cliente
            // requiere modificar la fecha o monto de las cuotas;
            // del resto no se aplica la forma de pago en NC.
            ret.setSkip(true);
        } else if (InstanceUtils.isType(_docInst, CISales.Invoice) || InstanceUtils.isType(_docInst, CISales.Receipt)) {
            final PrintQuery print = new PrintQuery(_docInst);
            final SelectBuilder selContactInst = SelectBuilder.get().linkto(CISales.DocumentAbstract.Contact)
                            .instance();
            print.addSelect(selContactInst);
            print.addAttribute(CISales.DocumentAbstract.DueDate, CISales.DocumentAbstract.Date,
                            CISales.DocumentSumAbstract.RateCrossTotal);
            print.executeWithoutAccessCheck();
            final DateTime date = print.getAttribute(CISales.DocumentAbstract.Date);
            final DateTime dueDate = print.getAttribute(CISales.DocumentAbstract.DueDate);
            final BigDecimal crossTotal = print.getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);

            if (dueDate != null && dueDate.isAfter(date)) {
                boolean add = true;
                if (ElectronicBilling.PAYMENTMETHODREGEX.exists()) {
                    final QueryBuilder queryBldr = new QueryBuilder(CISales.ChannelSalesCondition2DocumentAbstract);
                    queryBldr.addWhereAttrEqValue(CISales.ChannelSalesCondition2DocumentAbstract.ToAbstractLink,
                                    _docInst);
                    final MultiPrintQuery multi = queryBldr.getPrint();
                    final SelectBuilder selName = SelectBuilder.get()
                                    .linkto(CISales.ChannelSalesCondition2DocumentAbstract.FromAbstractLink)
                                    .attribute(CISales.ChannelConditionAbstract.Name)
                                    .instance();
                    multi.addSelect(selName);
                    multi.executeWithoutAccessCheck();
                    if (multi.next()) {
                        final String name = multi.getSelect(selName);
                        add = name.matches(ElectronicBilling.PAYMENTMETHODREGEX.get());
                    }
                }
                if (add) {
                    BigDecimal installmentAmount = crossTotal;
                    // check if we are "Agente de Retencion"
                    if (ElectronicBilling.RETENTION_ISAGENT.get()) {
                        final Instance contactInst = print.getSelect(selContactInst);
                        final PrintQuery retPrint = new PrintQuery(contactInst);
                        final SelectBuilder selRetention = SelectBuilder.get()
                                        .clazz(CISales.Contacts_ClassTaxinfo)
                                        .attribute(CISales.Contacts_ClassTaxinfo.Retention);
                        retPrint.addSelect(selRetention);
                        retPrint.executeWithoutAccessCheck();
                        final TaxRetention retention = retPrint.getSelect(selRetention);
                        // check that the client is also "Agente de Retencion"
                        if (retention != null && TaxRetention.AGENT.equals(retention)) {
                            installmentAmount = crossTotal.multiply(new BigDecimal("100")
                                            .subtract(new BigDecimal(ElectronicBilling.RETENTION_PERCENTAGE.get())))
                                            .divide(new BigDecimal("100"), RoundingMode.HALF_DOWN)
                                            .setScale(2, RoundingMode.HALF_DOWN);
                        }
                    }
                    ret.getInstallments().add(new Installment().setAmount(installmentAmount).setDueDate(LocalDate
                                    .of(dueDate.getYear(), dueDate.getMonthOfYear(), dueDate.getDayOfMonth())));
                }
            }
        }
        return ret;
    }

}
