package uk.gov.hmcts.appregister.common.enumeration;

/**
 * A programmatic means to determine the type of operation that is being performed.
 */
public enum CrudEnum {
    CREATE('C'),
    READ('G'),
    UPDATE('S'),
    DELETE('D');

    private char val;

    CrudEnum(char value) {
        this.val = value;
    }

    public boolean isDelete() {
        return this.val == 'D';
    }

    public boolean isCreate() {
        return this.val == 'C';
    }

    public boolean isUpdate() {
        return this.val == 'S';
    }

    public boolean isRead() {
        return this.val == 'G';
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
