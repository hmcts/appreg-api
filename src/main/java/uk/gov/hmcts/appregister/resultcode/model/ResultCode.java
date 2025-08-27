package uk.gov.hmcts.appregister.resultcode.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "resolution_codes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultCode {

    @Id
    @Column(name = "rc_id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "resolution_code", nullable = false, length = 10)
    private String resultCode;

    @Column(name = "resolution_code_title", nullable = false, length = 500)
    private String title;

    @Column(name = "resolution_code_wording", nullable = false)
    private String wording;

    @Column(name = "resolution_legislation")
    private String legislation;

    @Column(name = "rc_destination_email_address_1")
    private String destinationEmail1;

    @Column(name = "rc_destination_email_address_2")
    private String destinationEmail2;

    @Column(name = "resolution_code_start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "resolution_code_end_date")
    private LocalDate endDate;
}
