package uk.gov.hmcts.appregister.common.entity.base;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.MappedSuperclassType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.OffsetDateTime;

@StaticMetamodel(BaseChangeableEntity.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class BaseChangeableEntity_ {

	public static final String CHANGED_DATE = "changedDate";
	public static final String CHANGED_BY = "changedBy";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.base.BaseChangeableEntity#changedDate
	 **/
	public static volatile SingularAttribute<BaseChangeableEntity, OffsetDateTime> changedDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.base.BaseChangeableEntity#changedBy
	 **/
	public static volatile SingularAttribute<BaseChangeableEntity, String> changedBy;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.base.BaseChangeableEntity
	 **/
	public static volatile MappedSuperclassType<BaseChangeableEntity> class_;

}

