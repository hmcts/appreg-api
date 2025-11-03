package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(CriminalJusticeArea.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _CriminalJusticeArea {

	String CODE = "code";
	String ID = "id";
	String DESCRIPTION = "description";

	
	/**
	 * @see CriminalJusticeArea#code
	 **/
	TextAttribute<CriminalJusticeArea> code = new TextAttributeRecord<>(CODE);
	
	/**
	 * @see CriminalJusticeArea#id
	 **/
	SortableAttribute<CriminalJusticeArea> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * @see CriminalJusticeArea#description
	 **/
	TextAttribute<CriminalJusticeArea> description = new TextAttributeRecord<>(DESCRIPTION);

}

