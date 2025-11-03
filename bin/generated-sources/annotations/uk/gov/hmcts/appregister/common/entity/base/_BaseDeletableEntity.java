package uk.gov.hmcts.appregister.common.entity.base;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(BaseDeletableEntity.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _BaseDeletableEntity {

	String DELETED = "deleted";
	String DELETED_DATE = "deletedDate";
	String DELETED_BY = "deletedBy";

	
	/**
	 * @see BaseDeletableEntity#getDeleted
	 **/
	SortableAttribute<BaseDeletableEntity> deleted = new SortableAttributeRecord<>(DELETED);
	
	/**
	 * @see BaseDeletableEntity#getDeletedDate
	 **/
	SortableAttribute<BaseDeletableEntity> deletedDate = new SortableAttributeRecord<>(DELETED_DATE);
	
	/**
	 * @see BaseDeletableEntity#getDeletedBy
	 **/
	TextAttribute<BaseDeletableEntity> deletedBy = new TextAttributeRecord<>(DELETED_BY);

}

