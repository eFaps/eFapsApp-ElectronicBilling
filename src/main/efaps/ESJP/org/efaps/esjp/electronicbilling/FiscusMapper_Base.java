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
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.contacts.Contacts;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.electronicbilling.util.ElectronicBilling;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.esjp.products.ProductFamily;
import org.efaps.esjp.products.util.Products;
import org.efaps.esjp.sales.tax.Tax;
import org.efaps.util.EFapsException;

@EFapsUUID("a1379109-a71a-4beb-af8b-16146339998f")
@EFapsApplication("eFapsApp-ElectronicBilling")
public abstract class FiscusMapper_Base
{

    /**
     * El Peruano N° 244-2019/SUNAT Catálogo No.51 Código de tipo de operación
     * <table>
     * <tr><th>Codigo</th><th>Descripcion</th><th>Tipo de Comprobante asociado</th> </tr>
     * <tr><td>0101</td><td>Venta Interna</td><td>Factura, Boletas</td></tr>
     * <tr><td>0112</td><td>Venta Interna - Sustenta Gastos Deducibles Persona Natural</td><td>Factura</td></tr>
     * <tr><td>0113</td><td>Venta Interna-NRUS</td><td>Boleta</td></tr>
     * <tr><td>0200</td><td>Exportación de Bienes</td><td>Factura, Boletas</td></tr>
     * <tr><td>0201</td><td>Exportación de Servicios - Prestación servicios realizados íntegramente en el país</td><td>Factura, Boletas</td></tr>
     * <tr><td>0202</td><td>Exportación de Servicios - Prestación de servicios de hospedaje No Domiciliado</td><td>Factura, Boletas</td></tr>
     * <tr><td>0203</td><td>Exportación de Servicios - Transporte de navieras</td><td>Factura, Boletas</td></tr>
     * <tr><td>0204</td><td>Exportación de Servicios - Servicios a naves y aeronaves de bandera extranjera</td><td>Factura, Boletas</td></tr>
     * <tr><td>0205</td><td>Exportación de Servicios - Servicios que conformen un paquete turístico</td><td>Factura, Boletas</td></tr>
     * <tr><td>0206</td><td>Exportación de Servicios - Servicios complementarios al transporte de carga</td><td>Factura, Boletas</td></tr>
     * <tr><td>0207</td><td>Exportación de Servicios - Suministro de energía eléctrica a favor de sujetos domiciliados en ZED</td><td>Factura, Boletas</td></tr>
     * <tr><td>0208</td><td>Exportación de Servicios - Prestación servicios realizados parcialmente en el extranjero</td><td>Factura, Boletas</td></tr>
     * <tr><td>0301</td><td>Operaciones con Carta de porte aéreo (emitidas en el ámbito nacional)</td><td>Factura, Boletas</td></tr>
     * <tr><td>0302</td><td>Operaciones de Transporte ferroviario de pasajeros</td><td>Factura, Boletas</td></tr>
     * <tr><td>0401</td><td>Ventas no domiciliados que no califican como exportación</td><td>Factura, Boletas</td></tr>
     * <tr><td>0501</td><td>Compra interna</td><td>Liquidación de compra</td></tr>
     * <tr><td>0502</td><td>Anticipos</td><td>Liquidación de compra</td></tr>
     * <tr><td>0503</td><td>Compra de oro</td><td>Liquidación de compra</td></tr>
     * <tr><td>1001</td><td>Operación Sujeta a Detracción</td><td>Factura, Boletas</td></tr>
     * <tr><td>1002</td><td>Operación Sujeta a Detracción - Recursos Hidrobiológicos</td><td>Factura, Boletas</td></tr>
     * <tr><td>1003</td><td>Operación Sujeta a Detracción - Servicios de Transporte Pasajeros</td><td>Factura, Boletas</td></tr>
     * <tr><td>1004</td><td>Operación Sujeta a Detracción - Servicios de Transporte Carga</td><td>Factura, Boletas</td></tr>
     * <tr><td>2001</td><td>Operación Sujeta a Percepción</td><td>Factura, Boletas</td></tr>
     * <tr><td>2100</td><td>Créditos a empresas</td><td>Factura, Boletas</td></tr>
     * <tr><td>2101</td><td>Créditos de consumo revolvente</td><td>Factura, Boletas</td></tr>
     * <tr><td>2102</td><td>Créditos de consumo no revolvente</td><td>Factura, Boletas</td></tr>
     * <tr><td>2103</td><td>Otras operaciones no gravadas - Empresas del sistema financiero ycooperativas de ahorro y crédito no autorizadas a captar recursos del público</td><td>Factura, Boletas</td></tr>
     * <tr><td>2104</td><td>Otras operaciones no gravadas - Empresas del sistema de seguros</td><td>Factura, Boletas
     *</table>
     * @param _parameter Parameter
     * @param _docInst instance of the document
     * @param _contactInst instance of the contact
     * @throws EFapsException on error
     *
     */
    protected String getTipoOperacion(final Parameter _parameter, final Instance _docInst, final Instance _contactInst)
        throws EFapsException
    {
        String ret = null;
        if (InstanceUtils.isType(_docInst, CISales.Invoice) || InstanceUtils.isType(_docInst, CISales.Receipt)) {
            // if it is an "export"
            if (Contacts.isForeign(_parameter, _contactInst)) {
                // check the type of product that the invoice / receipt contains (only the first)
                final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionAbstract);
                queryBldr.addWhereAttrEqValue(CISales.PositionAbstract.DocumentAbstractLink, _docInst);
                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selProdInstant = SelectBuilder.get()
                                .linkto(CISales.PositionAbstract.Product).instance();
                multi.addSelect(selProdInstant);
                multi.executeWithoutAccessCheck();
                boolean service = false;
                if (multi.next()) {
                   final Instance prodInst =  multi.getSelect(selProdInstant);
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
     * <li>13: Gravado - Retiro </li>
     * <li>14: Gravado - Retiro por publicidad</li>
     * <li>15: Gravado - Bonificaciones</li>
     * <li>16: Gravado - Retiro por entrega a trabajadores</li>
     * <li>17: Gravado - IVAP</li>
     * <li>20: Exonerado - Operación Onerosa</li>
     * <li>21: Exonerado - Transferencia Gratuita</li>
     * <li>30: Inafecto - Operación Onerosa </li>
     * <li>31: Inafecto - Retiro por Bonificación</li>
     * <li>32: Inafecto - Retiro</li>
     * <li>33: Inafecto - Retiro por Muestras Médicas</li>
     * <li>34: Inafecto - Retiro por Convenio Colectivo</li>
     * <li>35: Inafecto - Retiro por premio</li>
     * <li>36: Inafecto - Retiro por publicidad</li>
     * <li>40: Exportación</li>
     * </ul>
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
     * <tr><th>Código</th><th>Descripción</th><th>UN/ECE 5153-Duty or tax or fee type name code</th></tr>
     * <tr><td>1000</td><td>IGV IMPUESTO GENERAL A LAS VENTAS</td><td>VAT</td></tr>
     * <tr><td>2000</td><td>ISC IMPUESTO SELECTIVO AL CONSUMO</td><td>EXC</td></tr>
     * <tr><td>9995</td><td>EXPORTACIÓN</td><td>FRE</td></tr>
     * <tr><td>9996</td><td>GRATUITO</td><td>FRE</td></tr>
     * <tr><td>9997</td><td>EXONERADO</td><td>VAT</td></tr>
     * <tr><td>9998</td><td>INAFECTO</td><td>FRE</td></tr>
     * <tr><td>9999</td><td>OTROS CONCEPTOS DE PAGO</td><td>OTH</td></tr>
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
            ret = new String[] { "VAT", "9997", "EXONERADO" };
        }
        return ret;
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
        return ElectronicBilling.TAXMAPPING.get().getProperty("tax." + _tax.getUUID().toString() + "." + _key);
    }

}
