package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import uk.gov.hmcts.appregister.generated.model.ApplicationListStatus;

@StaticMetamodel(ApplicationList.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class ApplicationList_ extends uk.gov.hmcts.appregister.common.entity.base.BaseChangeableAndDeletableEntity_ {

	public static final String DATE = "date";
	public static final String DURATION_HOURS = "durationHours";
	public static final String COURT_CODE = "courtCode";
	public static final String DESCRIPTION = "description";
	public static final String UUID = "uuid";
	public static final String VERSION = "version";
	public static final String OTHER_LOCATION = "otherLocation";
	public static final String DURATION_MINUTES = "durationMinutes";
	public static final String COURT_NAME = "courtName";
	public static final String PK = "pk";
	public static final String CJA = "cja";
	public static final String TIME = "time";
	public static final String CREATED_USER = "createdUser";
	public static final String STATUS = "status";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationList#date
	 **/
	public static volatile SingularAttribute<ApplicationList, LocalDate> date;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationList#durationHours
	 **/
	public static volatile SingularAttribute<ApplicationList, Short> durationHours;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationList#courtCode
	 **/
	public static volatile SingularAttribute<ApplicationList, String> courtCode;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationList#description
	 **/
	public static volatile SingularAttribute<ApplicationList, String> description;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationList#uuid
	 **/
	public static volatile SingularAttribute<ApplicationList, UUID> uuid;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationList#version
	 **/
	public static volatile SingularAttribute<ApplicationList, Long> version;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationList#otherLocation
	 **/
	public static volatile SingularAttribute<ApplicationList, String> otherLocation;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationList#durationMinutes
	 **/
	public static volatile SingularAttribute<ApplicationList, Short> durationMinutes;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationList#courtName
	 **/
	public static volatile SingularAttribute<ApplicationList, String> courtName;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationList#pk
	 **/
	public static volatile SingularAttribute<ApplicationList, Long> pk;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationList#cja
	 **/
	public static volatile SingularAttribute<ApplicationList, CriminalJusticeArea> cja;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationList#time
	 **/
	public static volatile SingularAttribute<ApplicationList, LocalTime> time;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationList
	 **/
	public static volatile EntityType<ApplicationList> class_;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationList#createdUser
	 **/
	public static volatile SingularAttribute<ApplicationList, String> createdUser;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationList#status
	 **/
	public static volatile SingularAttribute<ApplicationList, ApplicationListStatus> status;

}

