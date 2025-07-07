package uk.gov.hmcts.appregister.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/*
 * Keeping the column names the same as found in PSS for easy mapping once connected to SDRS.
 * No XSD so the following was found in the PSS Oracle DB, SYSTEM.NATIONAL_COURT_HOUSE.
 *
 * "NCH" = "National Court House"
 * "SL_NAME = "Court House Welsh name"
 * I'm not sure what HOCODE and MCC stand for, but they are used in the Libra database.
 */
@Entity
@Table(name = "courthouse")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourtHouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "court_type", nullable = false)
    private String courtType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "psa_id")
    private Long psaId;

    @Column(name = "location_code")
    private String courtLocationCode;

    @Column(name = "welsh_name")
    private String welshName;

    @Column(name = "org_id")
    private Long orgId;
}
