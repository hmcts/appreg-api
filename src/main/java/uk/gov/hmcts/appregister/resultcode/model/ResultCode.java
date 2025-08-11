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
@Table(name = "result_code")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultCode {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "result_code", nullable = false, length = 10)
    private String resultCode;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "wording", nullable = false)
    private String wording;

    @Column(name = "legislation")
    private String legislation;

    @Column(name = "destination_email_address_1")
    private String destinationEmail1;

    @Column(name = "destination_email_address_2")
    private String destinationEmail2;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;
}
