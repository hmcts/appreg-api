package uk.gov.hmcts.appregister.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.audit.listener.diff.Audit;
import uk.gov.hmcts.appregister.audit.listener.diff.AuditEnabled;
import uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@AuditEnabled(types = {CrudEnum.DELETE, CrudEnum.CREATE})
@Table(name = "test_entity")
@Getter
@Setter
public class TestEntityAuditable implements Keyable {
    @Id
    @Column(name = "adr_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "adr_gen")
    @SequenceGenerator(name = "adr_gen", sequenceName = "adr_id", allocationSize = 1)
    @EqualsAndHashCode.Include
    @Audit(action = CrudEnum.CREATE)
    private Long id;

    @Column(name = "line1")
    @Size(max = 35)
    @Audit(action = CrudEnum.CREATE)
    private CriminalJusticeArea criminalJusticeArea;

    @Column(name = "al_entry_resolution_wording", nullable = false)
    private String resolutionWording;

    @Column(name = "myname", nullable = false)
    @Audit(action = { CrudEnum.DELETE, CrudEnum.CREATE} )
    private String name;

    @Column(name = "entry", nullable = false)
    @Audit(action = CrudEnum.DELETE)
    private List<TestEntity2> entry = new ArrayList<>();

    @Column(name = "entry2", nullable = false)
    private List<String> entryStrings = new ArrayList<>();
}
