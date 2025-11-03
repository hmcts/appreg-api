package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;

@StaticMetamodel(LinkCommunicationMedia.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class LinkCommunicationMedia_ extends uk.gov.hmcts.appregister.common.entity.base.BaseUnmanagedChangeableEntity_ {

	public static final String BU_ID = "buId";
	public static final String END_DATE = "endDate";
	public static final String ER_ID = "erId";
	public static final String LCM_TYPE = "lcmType";
	public static final String COMM_ID = "commId";
	public static final String ID = "id";
	public static final String VERSION = "version";
	public static final String START_DATE = "startDate";
	public static final String LOC_ID = "locId";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.LinkCommunicationMedia#buId
	 **/
	public static volatile SingularAttribute<LinkCommunicationMedia, Long> buId;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.LinkCommunicationMedia#endDate
	 **/
	public static volatile SingularAttribute<LinkCommunicationMedia, LocalDate> endDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.LinkCommunicationMedia#erId
	 **/
	public static volatile SingularAttribute<LinkCommunicationMedia, Long> erId;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.LinkCommunicationMedia#lcmType
	 **/
	public static volatile SingularAttribute<LinkCommunicationMedia, String> lcmType;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.LinkCommunicationMedia#commId
	 **/
	public static volatile SingularAttribute<LinkCommunicationMedia, CommunicationMedia> commId;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.LinkCommunicationMedia#id
	 **/
	public static volatile SingularAttribute<LinkCommunicationMedia, Long> id;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.LinkCommunicationMedia
	 **/
	public static volatile EntityType<LinkCommunicationMedia> class_;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.LinkCommunicationMedia#version
	 **/
	public static volatile SingularAttribute<LinkCommunicationMedia, Long> version;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.LinkCommunicationMedia#startDate
	 **/
	public static volatile SingularAttribute<LinkCommunicationMedia, LocalDate> startDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.LinkCommunicationMedia#locId
	 **/
	public static volatile SingularAttribute<LinkCommunicationMedia, Long> locId;

}

