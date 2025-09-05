package uk.gov.hmcts.appregister.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.appregister.common.entity.base.Accountable;
import uk.gov.hmcts.appregister.common.entity.base.BaseChangeableEntity;

/**
 * Represents an official associated with an application list entry, mapped to the
 * "app_list_entry_official" table in the database.
 */
@Entity
@Table(name = "app_list_entry_official")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AppListEntryOfficial extends BaseChangeableEntity implements Accountable {
    @Id
    @Column(name = "aleo_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = " aleo_gen")
    @SequenceGenerator(name = " aleo_gen", sequenceName = " aleo_seq", allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ale_ale_id", nullable = false)
    private ApplicationListEntry appListEntry;

    @Column(name = "title")
    @Size(max = 100)
    private String title;

    @Column(name = "forename")
    @Size(max = 100)
    private String forename;

    @Column(name = "surname")
    @Size(max = 100)
    private String surname;

    @Column(name = "official_type", nullable = false)
    private String officialType;

    @Column(name = "user_name", nullable = false)
    private String createdUser;
}
