package uk.gov.hmcts.appregister.common.entity.base;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.MappedSuperclassType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.OffsetDateTime;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;

@StaticMetamodel(BaseChangeableAndDeletableEntity.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class BaseChangeableAndDeletableEntity_ {

	public static final String CHANGED_DATE = "changedDate";
	public static final String DELETED = "deleted";
	public static final String DELETED_DATE = "deletedDate";
	public static final String CHANGED_BY = "changedBy";
	public static final String DELETED_BY = "deletedBy";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.base.BaseChangeableAndDeletableEntity#changedDate
	 **/
	public static volatile SingularAttribute<BaseChangeableAndDeletableEntity, OffsetDateTime> changedDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.base.BaseChangeableAndDeletableEntity#deleted
	 **/
	public static volatile SingularAttribute<BaseChangeableAndDeletableEntity, YesOrNo> deleted;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.base.BaseChangeableAndDeletableEntity#deletedDate
	 **/
	public static volatile SingularAttribute<BaseChangeableAndDeletableEntity, OffsetDateTime> deletedDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.base.BaseChangeableAndDeletableEntity#changedBy
	 **/
	public static volatile SingularAttribute<BaseChangeableAndDeletableEntity, String> changedBy;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.base.BaseChangeableAndDeletableEntity
	 **/
	public static volatile MappedSuperclassType<BaseChangeableAndDeletableEntity> class_;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.base.BaseChangeableAndDeletableEntity#deletedBy
	 **/
	public static volatile SingularAttribute<BaseChangeableAndDeletableEntity, String> deletedBy;

}

