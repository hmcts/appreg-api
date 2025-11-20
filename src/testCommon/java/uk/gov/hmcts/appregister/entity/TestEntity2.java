package uk.gov.hmcts.appregister.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.appregister.audit.listener.diff.Audit;
import uk.gov.hmcts.appregister.common.entity.base.Keyable;
import uk.gov.hmcts.appregister.common.enumeration.CrudEnum;

@Getter
@Setter
@Table(name = "random_list")
public class TestEntity2 implements Keyable {
    @Id
    @Column(name = "lst_adr_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "adr_gen")
    @SequenceGenerator(name = "adr_gen", sequenceName = "adr_id", allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "lst_entry", nullable = false)
    @Audit(action = CrudEnum.DELETE)
    private String name;

    // test recursion
    @Column(name = "lst_entity", nullable = false)
    private TestEntityAuditable entity;
}
