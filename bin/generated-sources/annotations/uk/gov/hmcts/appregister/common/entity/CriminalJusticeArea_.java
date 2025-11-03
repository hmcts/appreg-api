package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(CriminalJusticeArea.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class CriminalJusticeArea_ {

	public static final String CODE = "code";
	public static final String DESCRIPTION = "description";
	public static final String ID = "id";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea#code
	 **/
	public static volatile SingularAttribute<CriminalJusticeArea, String> code;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea#description
	 **/
	public static volatile SingularAttribute<CriminalJusticeArea, String> description;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea#id
	 **/
	public static volatile SingularAttribute<CriminalJusticeArea, Long> id;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.CriminalJusticeArea
	 **/
	public static volatile EntityType<CriminalJusticeArea> class_;

}

