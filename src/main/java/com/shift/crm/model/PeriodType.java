package com.shift.crm.model;

import java.time.LocalDateTime;
import java.time.temporal.IsoFields;

public enum PeriodType {
    DAY {
        @Override
        public LocalDateTime startOf(LocalDateTime reference) {
            return reference.toLocalDate().atStartOfDay();
        }
    },

    MONTH {
        @Override
        public LocalDateTime startOf(LocalDateTime reference) {
            return reference.toLocalDate().withDayOfMonth(1).atStartOfDay();
        }
    },

    QUARTER {
        @Override
        public LocalDateTime startOf(LocalDateTime reference) {
            int quarter = reference.get(IsoFields.QUARTER_OF_YEAR);
            int firstMonth = (quarter - 1) * 3 + 1;
            return reference.toLocalDate()
                    .withMonth(firstMonth)
                    .withDayOfMonth(1)
                    .atStartOfDay();
        }
    },

    YEAR {
        @Override
        public LocalDateTime startOf(LocalDateTime reference) {
            return reference.toLocalDate().withDayOfYear(1).atStartOfDay();
        }
    };

    public abstract LocalDateTime startOf(LocalDateTime reference);

}
