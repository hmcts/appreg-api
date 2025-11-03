package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.OffsetDateTime;

@StaticMetamodel(ApplicationListEntry.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class ApplicationListEntry_ extends uk.gov.hmcts.appregister.common.entity.base.BaseChangeableEntity_ {

	public static final String ENTRY_RESCHEDULED = "entryRescheduled";
	public static final String NOTES = "notes";
	public static final String CASE_REFERENCE = "caseReference";
	public static final String ANAMEDADDRESS = "anamedaddress";
	public static final String RESOLUTIONS = "resolutions";
	public static final String NUMBER_OF_BULK_RESPONDENTS = "numberOfBulkRespondents";
	public static final String RNAMEADDRESS = "rnameaddress";
	public static final String MESSAGE_UUID = "messageUuid";
	public static final String LODGEMENT_DATE = "lodgementDate";
	public static final String OFFICIALS = "officials";
	public static final String ID = "id";
	public static final String CREATED_USER = "createdUser";
	public static final String SEQUENCE_NUMBER = "sequenceNumber";
	public static final String RETRY_COUNT = "retryCount";
	public static final String APPLICATION_LIST = "applicationList";
	public static final String ACCOUNT_NUMBER = "accountNumber";
	public static final String VERSION = "version";
	public static final String TCEP_STATUS = "tcepStatus";
	public static final String ENTRY_FEE_STATUSES = "entryFeeStatuses";
	public static final String ENTRY_FEE_IDS = "entryFeeIds";
	public static final String BULK_UPLOAD = "bulkUpload";
	public static final String STANDARD_APPLICANT = "standardApplicant";
	public static final String APPLICATION_LIST_ENTRY_WORDING = "applicationListEntryWording";
	public static final String APPLICATION_CODE = "applicationCode";

	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#entryRescheduled
	 **/
	public static volatile SingularAttribute<ApplicationListEntry, String> entryRescheduled;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#notes
	 **/
	public static volatile SingularAttribute<ApplicationListEntry, String> notes;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#caseReference
	 **/
	public static volatile SingularAttribute<ApplicationListEntry, String> caseReference;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#anamedaddress
	 **/
	public static volatile SingularAttribute<ApplicationListEntry, NameAddress> anamedaddress;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#resolutions
	 **/
	public static volatile ListAttribute<ApplicationListEntry, AppListEntryResolution> resolutions;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#numberOfBulkRespondents
	 **/
	public static volatile SingularAttribute<ApplicationListEntry, Short> numberOfBulkRespondents;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#rnameaddress
	 **/
	public static volatile SingularAttribute<ApplicationListEntry, NameAddress> rnameaddress;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#messageUuid
	 **/
	public static volatile SingularAttribute<ApplicationListEntry, String> messageUuid;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#lodgementDate
	 **/
	public static volatile SingularAttribute<ApplicationListEntry, OffsetDateTime> lodgementDate;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#officials
	 **/
	public static volatile ListAttribute<ApplicationListEntry, AppListEntryOfficial> officials;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#id
	 **/
	public static volatile SingularAttribute<ApplicationListEntry, Long> id;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry
	 **/
	public static volatile EntityType<ApplicationListEntry> class_;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#createdUser
	 **/
	public static volatile SingularAttribute<ApplicationListEntry, String> createdUser;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#sequenceNumber
	 **/
	public static volatile SingularAttribute<ApplicationListEntry, Short> sequenceNumber;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#retryCount
	 **/
	public static volatile SingularAttribute<ApplicationListEntry, String> retryCount;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#applicationList
	 **/
	public static volatile SingularAttribute<ApplicationListEntry, ApplicationList> applicationList;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#accountNumber
	 **/
	public static volatile SingularAttribute<ApplicationListEntry, String> accountNumber;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#version
	 **/
	public static volatile SingularAttribute<ApplicationListEntry, Long> version;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#tcepStatus
	 **/
	public static volatile SingularAttribute<ApplicationListEntry, String> tcepStatus;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#entryFeeStatuses
	 **/
	public static volatile ListAttribute<ApplicationListEntry, AppListEntryFeeStatus> entryFeeStatuses;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#entryFeeIds
	 **/
	public static volatile ListAttribute<ApplicationListEntry, AppListEntryFeeId> entryFeeIds;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#bulkUpload
	 **/
	public static volatile SingularAttribute<ApplicationListEntry, String> bulkUpload;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#standardApplicant
	 **/
	public static volatile SingularAttribute<ApplicationListEntry, StandardApplicant> standardApplicant;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#applicationListEntryWording
	 **/
	public static volatile SingularAttribute<ApplicationListEntry, String> applicationListEntryWording;
	
	/**
	 * @see uk.gov.hmcts.appregister.common.entity.ApplicationListEntry#applicationCode
	 **/
	public static volatile SingularAttribute<ApplicationListEntry, ApplicationCode> applicationCode;

}

