package uk.gov.hmcts.appregister.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "application_list")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "entries")
@Builder
public class ApplicationList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    // TODO: Should probably be time rather than String.
    @Column(name = "time", nullable = false)
    private String time;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courthouse_id", nullable = false)
    private CourtHouse courthouse;

    @Column(name = "description", nullable = false)
    private String description;

    @OneToMany(mappedBy = "applicationList", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Application> entries;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "changed_date", nullable = false)
    private LocalDate changedDate;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "user_id", nullable = false)
    private String userId;
}
