package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;

@StaticMetamodel(ApplicationCode.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class ApplicationCode_ extends uk.gov.hmcts.appregister.common.entity.base.BaseUnmanagedChangeableEntity_ {

	public static final String FEE_DUE = "feeDue";
	public static final String DESTINATION_EMAIL2 = "destinationEmail2";
	public static final String CODE = "code";
	public static final String DESTINATION_EMAIL1 = "destinationEmail1";
	public static final String END_DATE = "endDate";
	public static final String TITLE = "title";
	public static final String USER_NAME = "userName";
	public static final String FEE_REFERENCE = "feeReference";
	public static final String VERSION = "version";
	public static final String REQUIRES_RESPONDENT = "requiresRespondent";
	public static final String BULK_RESPONDENT_ALLOWED = "bulkRespondentAllowed";
	public static final String ID = "id";
	public static final String WORDING = "wording";
	public static final String START_DATE = "startDate";
	public static final String LEGISLATION = "legislation";
	public static final String APPLICATION_LIST_ENTRY_LIST = "applicationListEntryList";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationCode#feeDue
	 **/
	public static volatile SingularAttribute<ApplicationCode, YesOrNo> feeDue;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationCode#destinationEmail2
	 **/
	public static volatile SingularAttribute<ApplicationCode, String> destinationEmail2;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationCode#code
	 **/
	public static volatile SingularAttribute<ApplicationCode, String> code;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationCode#destinationEmail1
	 **/
	public static volatile SingularAttribute<ApplicationCode, String> destinationEmail1;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationCode#endDate
	 **/
	public static volatile SingularAttribute<ApplicationCode, LocalDate> endDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationCode#title
	 **/
	public static volatile SingularAttribute<ApplicationCode, String> title;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationCode#userName
	 **/
	public static volatile SingularAttribute<ApplicationCode, String> userName;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationCode#feeReference
	 **/
	public static volatile SingularAttribute<ApplicationCode, String> feeReference;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationCode#version
	 **/
	public static volatile SingularAttribute<ApplicationCode, Long> version;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationCode#requiresRespondent
	 **/
	public static volatile SingularAttribute<ApplicationCode, YesOrNo> requiresRespondent;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationCode#bulkRespondentAllowed
	 **/
	public static volatile SingularAttribute<ApplicationCode, YesOrNo> bulkRespondentAllowed;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationCode#id
	 **/
	public static volatile SingularAttribute<ApplicationCode, Long> id;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationCode#wording
	 **/
	public static volatile SingularAttribute<ApplicationCode, String> wording;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationCode
	 **/
	public static volatile EntityType<ApplicationCode> class_;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationCode#startDate
	 **/
	public static volatile SingularAttribute<ApplicationCode, LocalDate> startDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationCode#legislation
	 **/
	public static volatile SingularAttribute<ApplicationCode, String> legislation;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationCode#applicationListEntryList
	 **/
	public static volatile ListAttribute<ApplicationCode, ApplicationListEntry> applicationListEntryList;

}

