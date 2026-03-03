package uk.gov.hmcts.appregister.common.enumeration;

/**
 * A programmatic means to determine the type of operation that is being performed.
 */
public enum CrudEnum {
    CREATE('I'),
    READ('R'),
    UPDATE('S'),
    DELETE('D');

    private char val;

    CrudEnum(char value) {
        this.val = value;
    }

    public boolean isDelete() {
        return this == DELETE;
    }

    public boolean isCreate() {
        return this == CREATE;
    }

    public boolean isUpdate() {
        return this == UPDATE;
    }

    public boolean isRead() {
        return this == READ;
    }

    public char getValue() {
        return val;
    }

    public static CrudEnum fromValue(char value) {
        for (CrudEnum crud : CrudEnum.values()) {
            if (crud.val == value) {
                return crud;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }
}
