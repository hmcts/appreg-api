package uk.gov.hmcts.appregister.common.entity.base;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(BaseChangeableAndDeletableEntity.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _BaseChangeableAndDeletableEntity {

	String CHANGED_BY = "changedBy";
	String DELETED = "deleted";
	String DELETED_DATE = "deletedDate";
	String CHANGED_DATE = "changedDate";
	String DELETED_BY = "deletedBy";

	
	/**
	 * @see BaseChangeableAndDeletableEntity#changedBy
	 **/
	TextAttribute<BaseChangeableAndDeletableEntity> changedBy = new TextAttributeRecord<>(CHANGED_BY);
	
	/**
	 * @see BaseChangeableAndDeletableEntity#deleted
	 **/
	SortableAttribute<BaseChangeableAndDeletableEntity> deleted = new SortableAttributeRecord<>(DELETED);
	
	/**
	 * @see BaseChangeableAndDeletableEntity#deletedDate
	 **/
	SortableAttribute<BaseChangeableAndDeletableEntity> deletedDate = new SortableAttributeRecord<>(DELETED_DATE);
	
	/**
	 * @see BaseChangeableAndDeletableEntity#changedDate
	 **/
	SortableAttribute<BaseChangeableAndDeletableEntity> changedDate = new SortableAttributeRecord<>(CHANGED_DATE);
	
	/**
	 * @see BaseChangeableAndDeletableEntity#deletedBy
	 **/
	TextAttribute<BaseChangeableAndDeletableEntity> deletedBy = new TextAttributeRecord<>(DELETED_BY);

}

