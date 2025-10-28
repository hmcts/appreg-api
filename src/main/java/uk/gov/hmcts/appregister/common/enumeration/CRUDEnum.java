package uk.gov.hmcts.appregister.common.enumeration;

/**
 * A programmatic means to determine the type of operation that is being performed
 */
public enum CRUDEnum {
    CREATE('C'),
    READ('R'),
    UPDATE('S'),
    DELETE('D');

    private char val;
    CRUDEnum(char  value){
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
        return this.val == 'R';
    }

    public char getValue() {
        return val;
    }

    public static CRUDEnum fromValue(char value) {
        for (CRUDEnum crud : CRUDEnum.values()) {
            if (crud.val == value) {
                return crud;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }

}
