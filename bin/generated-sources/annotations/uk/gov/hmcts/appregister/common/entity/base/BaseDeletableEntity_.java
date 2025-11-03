package uk.gov.hmcts.appregister.common.entity.base;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.MappedSuperclassType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.OffsetDateTime;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;

@StaticMetamodel(BaseDeletableEntity.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class BaseDeletableEntity_ {

	public static final String DELETED = "deleted";
	public static final String DELETED_DATE = "deletedDate";
	public static final String DELETED_BY = "deletedBy";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.base.BaseDeletableEntity#getDeleted
	 **/
	public static volatile SingularAttribute<BaseDeletableEntity, YesOrNo> deleted;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.base.BaseDeletableEntity#getDeletedDate
	 **/
	public static volatile SingularAttribute<BaseDeletableEntity, OffsetDateTime> deletedDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.base.BaseDeletableEntity
	 **/
	public static volatile MappedSuperclassType<BaseDeletableEntity> class_;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.base.BaseDeletableEntity#getDeletedBy
	 **/
	public static volatile SingularAttribute<BaseDeletableEntity, String> deletedBy;

}

