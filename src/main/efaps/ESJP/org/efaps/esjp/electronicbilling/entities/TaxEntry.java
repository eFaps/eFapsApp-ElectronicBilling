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
package org.efaps.esjp.electronicbilling.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ubl.documents.interfaces.ITaxEntry;
import org.efaps.ubl.documents.values.TaxType;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@EFapsUUID("9f662f85-d464-49cc-9aac-484e4ad166cb")
@EFapsApplication("eFapsApp-ElectronicBilling")
@JsonDeserialize(builder = TaxEntry.Builder.class)
public class TaxEntry
    implements ITaxEntry
{

    private BigDecimal amount;

    private BigDecimal taxableAmount;

    private BigDecimal percent;

    private String id;

    private String name;

    private String code;

    private String taxExemptionReasonCode;

    private TaxType taxType;

    private boolean freeOfCharge;

    private TaxEntry(final Builder builder)
    {
        amount = builder.amount.setScale(2, RoundingMode.HALF_UP);
        taxableAmount = builder.taxableAmount.setScale(2, RoundingMode.HALF_UP);
        percent = builder.percent.setScale(2, RoundingMode.HALF_UP);
        id = builder.id;
        name = builder.name;
        code = builder.code;
        taxExemptionReasonCode = builder.taxExemptionReasonCode;
        freeOfCharge = builder.freeOfCharge;
        setTaxType(builder.taxType);
    }

    @Override
    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAmount(final BigDecimal amount)
    {
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getTaxableAmount()
    {
        return taxableAmount;
    }

    public void setTaxableAmount(final BigDecimal taxableAmount)
    {
        this.taxableAmount = taxableAmount.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getPercent()
    {
        return percent;
    }

    public void setPercent(final BigDecimal percent)
    {
        this.percent = percent.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getId()
    {
        return id;
    }

    public void setId(final String id)
    {
        this.id = id;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public void setName(final String name)
    {
        this.name = name;
    }

    @Override
    public String getCode()
    {
        return code;
    }

    public void setCode(final String code)
    {
        this.code = code;
    }

    @Override
    public String getTaxExemptionReasonCode()
    {
        return taxExemptionReasonCode;
    }

    public void setTaxExemptionReasonCode(final String taxExemptionReasonCode)
    {
        this.taxExemptionReasonCode = taxExemptionReasonCode;
    }

    @Override
    public TaxType getTaxType()
    {
        return taxType;
    }

    public void setTaxType(final TaxType taxType)
    {
        this.taxType = taxType;
    }

    @Override
    public boolean isFreeOfCharge()
    {
        return freeOfCharge;
    }

    public void setFreeOfCharge(final boolean freeOfCharge)
    {
        this.freeOfCharge = freeOfCharge;
    }

    /**
     * Creates builder to build {@link TaxEntry}.
     *
     * @return created builder
     */
    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * Builder to build {@link TaxEntry}.
     */
    public static final class Builder
    {

        private BigDecimal amount;
        private BigDecimal taxableAmount;
        private BigDecimal percent;
        private String id;
        private String name;
        private String code;
        private String taxExemptionReasonCode;
        private TaxType taxType;
        private boolean freeOfCharge;

        private Builder()
        {
        }

        public Builder withAmount(final BigDecimal amount)
        {
            this.amount = amount;
            return this;
        }

        public Builder withTaxableAmount(final BigDecimal taxableAmount)
        {
            this.taxableAmount = taxableAmount;
            return this;
        }

        public Builder withPercent(final BigDecimal percent)
        {
            this.percent = percent;
            return this;
        }

        public Builder withId(final String id)
        {
            this.id = id;
            return this;
        }

        public Builder withName(final String name)
        {
            this.name = name;
            return this;
        }

        public Builder withCode(final String code)
        {
            this.code = code;
            return this;
        }

        public Builder withTaxExemptionReasonCode(final String taxExemptionReasonCode)
        {
            this.taxExemptionReasonCode = taxExemptionReasonCode;
            return this;
        }

        public Builder withTaxType(final TaxType taxType)
        {
            this.taxType = taxType;
            return this;
        }

        public Builder withFreeOfCharge(final boolean freeOfCharge) {
            this.freeOfCharge = freeOfCharge;
            return this;
        }

        public TaxEntry build()
        {
            return new TaxEntry(this);
        }
    }

}
