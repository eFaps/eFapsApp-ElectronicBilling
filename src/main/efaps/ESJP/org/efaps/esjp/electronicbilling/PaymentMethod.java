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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ubl.documents.interfaces.IInstallment;

@EFapsUUID("02ba44f6-3ad9-4d55-b94b-8c51262cd085")
@EFapsApplication("eFapsApp-ElectronicBilling")
public class PaymentMethod
{

    private boolean skip;
    private final List<Installment> installments = new ArrayList<>();

    public boolean isSkip()
    {
        return skip;
    }

    public void setSkip(final boolean skip)
    {
        this.skip = skip;
    }

    public List<Installment> getInstallments()
    {
        return installments;
    }

    public boolean isCash()
    {
        return CollectionUtils.isEmpty(installments);
    }

    public BigDecimal getPendingAmount()
    {
        return installments.stream().map(Installment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static class Installment
        implements IInstallment
    {

        private LocalDate dueDate;
        private BigDecimal amount;

        @Override
        public LocalDate getDueDate()
        {
            return dueDate;
        }

        public Installment setDueDate(final LocalDate dueDate)
        {
            this.dueDate = dueDate;
            return this;
        }

        @Override
        public BigDecimal getAmount()
        {
            return amount;
        }

        public Installment setAmount(final BigDecimal amount)
        {
            this.amount = amount;
            return this;
        }
    }
}
