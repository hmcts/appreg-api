package uk.gov.hmcts.appregister.common.entity.base;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(BaseChangeableEntity.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _BaseChangeableEntity {

	String CHANGED_BY = "changedBy";
	String CHANGED_DATE = "changedDate";

	
	/**
	 * @see BaseChangeableEntity#changedBy
	 **/
	TextAttribute<BaseChangeableEntity> changedBy = new TextAttributeRecord<>(CHANGED_BY);
	
	/**
	 * @see BaseChangeableEntity#changedDate
	 **/
	SortableAttribute<BaseChangeableEntity> changedDate = new SortableAttributeRecord<>(CHANGED_DATE);

}

