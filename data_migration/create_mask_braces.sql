-- Initial Creation script for creating the mask_braces fuction
--
-- This function takes a clob and replaces any { } sections with a masked version

-- Version Control
-- V1.0  	Matthew Harman  09/07/2026	Initial Version

CREATE OR REPLACE FUNCTION APPREGISTER.MASK_BRACES(p_clob CLOB)
    RETURN CLOB
    IS 
        l_result    CLOB;
        l_pos       PLS_INTEGER := 1;
        l_start     PLS_INTEGER;
        l_end       PLS_INTEGER;
        l_len       PLS_INTEGER;
        l_inner_len PLS_INTEGER;
        l_chunk     VARCHAR2(32767);
    BEGIN   
        IF p_clob IS NULL THEN
            RETURN NULL;
        END IF;

        DBMS_LOB.CREATETEMPORARY(l_result, TRUE);

        LOOP
            l_start := DBMS_LOB.INSTR(p_clob, '{', l_pos);

            IF l_start = 0 THEN
                l_chunk := DBMS_LOB.SUBSTR(p_clob, 32767, l_pos);
                IF l_chunk IS NOT NULL THEN
                    DBMS_LOB.WRITEAPPEND(l_result, LENGTH(l_chunk),l_chunk);
                END IF;
                EXIT;
            END IF;

            -- Copy text before the brace
            IF l_start > l_pos THEN
                l_chunk := DBMS_LOB.SUBSTR(p_clob, l_start - l_pos, l_pos);
                IF l_chunk IS NOT NULL THEN
                    DBMS_LOB.WRITEAPPEND(l_result, LENGTH(l_chunk), l_chunk);
                END IF;
            END IF; 

            l_end := DBMS_LOB.INSTR(p_clob, '}', l_start + 1);

            IF l_end = 0 THEN
                l_chunk := DBMS_LOB.SUBSTR(p_clob, 32767, l_start);
                IF l_chunk IS NOT NULL THEN
                    DBMS_LOB.WRITEAPPEND(l_result, LENGTH(l_chunk), l_chunk);
                END IF;
                EXIT;
            END IF;

            -- Length of text inside the braces, excluding { and }
            l_inner_len := l_end - l_start - 1;

            -- Replace with random uppercase letters of the same length
            l_chunk := '{' || DBMS_RANDOM.STRING('u', l_inner_len) || '}';
            DBMS_LOB.WRITEAPPEND(l_result, LENGTH(l_chunk), l_chunk);

            l_pos := l_end + 1;
        END LOOP;

        RETURN l_result;
    END;
/

