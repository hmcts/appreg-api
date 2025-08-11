  CREATE OR REPLACE TRIGGER "LIBRA"."LA_MCC_TRG"
                AFTER INSERT ON link_addresses
                REFERENCING OLD AS OLD NEW AS NEW
                FOR EACH ROW
DECLARE
BEGIN
        IF(:NEW.er_er_ID IS NOT NULL and :NEW.adr_adr_id is NOT NULL) THEN
                update addresses adr set adr.mcc_mcc_id = (select mcc_mcc_id from entity_roles
 er where er.er_id = :NEW.er_er_id) where adr.adr_id = :NEW.adr_adr_id;
        END IF;
END;
ALTER TRIGGER "LIBRA"."LA_MCC_TRG" ENABLE
