package uk.gov.hmcts.appregister.common.entity.base;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.impl.SortableAttributeRecord;

@StaticMetamodel(BaseUnmanagedChangeableEntity.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _BaseUnmanagedChangeableEntity {

	String CHANGED_BY = "changedBy";
	String CHANGED_DATE = "changedDate";

	
	/**
	 * @see BaseUnmanagedChangeableEntity#changedBy
	 **/
	SortableAttribute<BaseUnmanagedChangeableEntity> changedBy = new SortableAttributeRecord<>(CHANGED_BY);
	
	/**
	 * @see BaseUnmanagedChangeableEntity#changedDate
	 **/
	SortableAttribute<BaseUnmanagedChangeableEntity> changedDate = new SortableAttributeRecord<>(CHANGED_DATE);

}

