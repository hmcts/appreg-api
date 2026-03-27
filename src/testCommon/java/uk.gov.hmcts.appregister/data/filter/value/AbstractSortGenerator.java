package uk.gov.hmcts.appregister.data.filter.value;

import uk.gov.hmcts.appregister.data.filter.OrderEnum;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public abstract class AbstractSortGenerator<T> implements GenerateAccordingToSort<T> {
    protected static String PREFIX_START = "AAAAA";

    protected static String PREFIX_END = "ZZZZZZ";

    public String getString(OrderEnum orderEnum) {
        UUID uuid = UUID.randomUUID();
        if (orderEnum == OrderEnum.START) {
            return PREFIX_START + uuid;
        } else {
            return PREFIX_END + uuid;
        }
    }

    public Boolean getBoolean(OrderEnum orderEnum) {
        if (orderEnum == OrderEnum.START) {
            return true;
        } else {
            return false;
        }
    }

    public Boolean getLong(OrderEnum orderEnum) {
        if (orderEnum == OrderEnum.START) {
            return true;
        } else {
            return false;
        }
    }

    public LocalDate getDate(OrderEnum orderEnum) {
        UUID uuid = UUID.randomUUID();
        if (orderEnum == OrderEnum.START) {
            return LocalDate.now();
        } else {
            return LocalDate.now().plusDays(2);
        }
    }

    public LocalTime getTime(OrderEnum orderEnum) {
        if (orderEnum == OrderEnum.START) {
            return LocalTime.now();
        } else {
            return LocalTime.now().plusHours(2);
        }
    }
}
