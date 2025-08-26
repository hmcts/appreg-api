package uk.gov.hmcts.appregister.courtlocation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * Keeping the column names the same as found in PSS for easy mapping once connected to SDRS.
 * No XSD so the following was found in the PSS Oracle DB, SYSTEM.NATIONAL_COURT_HOUSE.
 *
 * "NCH" = "National Court House"
 * "SL_NAME = "Court House Welsh name"
 * I'm not sure what HOCODE and MCC stand for, but they are used in the Libra database.
 */
@Entity
@Table(name = "national_court_houses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourtHouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nch_id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "courthouse_name", nullable = false)
    private String name;

    @Column(name = "court_type", nullable = false)
    private String courtType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "loc_loc_id")
    private Long locationId;

    @Column(name = "psa_psa_id")
    private Long psaId;

    @Column(name = "court_location_code")
    private String courtLocationCode;

    @Column(name = "sl_courthouse_name")
    private String welshName;

    @Column(name = "norg_id")
    private Long orgId;
}
