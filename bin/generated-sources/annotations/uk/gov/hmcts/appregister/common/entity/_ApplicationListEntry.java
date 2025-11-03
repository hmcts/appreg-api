package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(ApplicationListEntry.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _ApplicationListEntry extends uk.gov.hmcts.appregister.common.entity.base._BaseChangeableEntity {

	String MESSAGE_UUID = "messageUuid";
	String LODGEMENT_DATE = "lodgementDate";
	String APPLICATION_LIST = "applicationList";
	String CREATED_USER = "createdUser";
	String CASE_REFERENCE = "caseReference";
	String SEQUENCE_NUMBER = "sequenceNumber";
	String RNAMEADDRESS = "rnameaddress";
	String ENTRY_RESCHEDULED = "entryRescheduled";
	String TCEP_STATUS = "tcepStatus";
	String APPLICATION_LIST_ENTRY_WORDING = "applicationListEntryWording";
	String NUMBER_OF_BULK_RESPONDENTS = "numberOfBulkRespondents";
	String BULK_UPLOAD = "bulkUpload";
	String STANDARD_APPLICANT = "standardApplicant";
	String RETRY_COUNT = "retryCount";
	String ID = "id";
	String APPLICATION_CODE = "applicationCode";
	String NOTES = "notes";
	String VERSION = "version";
	String ANAMEDADDRESS = "anamedaddress";
	String ACCOUNT_NUMBER = "accountNumber";

	
	/**
	 * @see ApplicationListEntry#messageUuid
	 **/
	TextAttribute<ApplicationListEntry> messageUuid = new TextAttributeRecord<>(MESSAGE_UUID);
	
	/**
	 * @see ApplicationListEntry#lodgementDate
	 **/
	SortableAttribute<ApplicationListEntry> lodgementDate = new SortableAttributeRecord<>(LODGEMENT_DATE);
	
	/**
	 * @see ApplicationListEntry#applicationList
	 **/
	SortableAttribute<ApplicationListEntry> applicationList = new SortableAttributeRecord<>(APPLICATION_LIST);
	
	/**
	 * @see ApplicationListEntry#createdUser
	 **/
	TextAttribute<ApplicationListEntry> createdUser = new TextAttributeRecord<>(CREATED_USER);
	
	/**
	 * @see ApplicationListEntry#caseReference
	 **/
	TextAttribute<ApplicationListEntry> caseReference = new TextAttributeRecord<>(CASE_REFERENCE);
	
	/**
	 * @see ApplicationListEntry#sequenceNumber
	 **/
	SortableAttribute<ApplicationListEntry> sequenceNumber = new SortableAttributeRecord<>(SEQUENCE_NUMBER);
	
	/**
	 * @see ApplicationListEntry#rnameaddress
	 **/
	SortableAttribute<ApplicationListEntry> rnameaddress = new SortableAttributeRecord<>(RNAMEADDRESS);
	
	/**
	 * @see ApplicationListEntry#entryRescheduled
	 **/
	TextAttribute<ApplicationListEntry> entryRescheduled = new TextAttributeRecord<>(ENTRY_RESCHEDULED);
	
	/**
	 * @see ApplicationListEntry#tcepStatus
	 **/
	TextAttribute<ApplicationListEntry> tcepStatus = new TextAttributeRecord<>(TCEP_STATUS);
	
	/**
	 * @see ApplicationListEntry#applicationListEntryWording
	 **/
	TextAttribute<ApplicationListEntry> applicationListEntryWording = new TextAttributeRecord<>(APPLICATION_LIST_ENTRY_WORDING);
	
	/**
	 * @see ApplicationListEntry#numberOfBulkRespondents
	 **/
	SortableAttribute<ApplicationListEntry> numberOfBulkRespondents = new SortableAttributeRecord<>(NUMBER_OF_BULK_RESPONDENTS);
	
	/**
	 * @see ApplicationListEntry#bulkUpload
	 **/
	TextAttribute<ApplicationListEntry> bulkUpload = new TextAttributeRecord<>(BULK_UPLOAD);
	
	/**
	 * @see ApplicationListEntry#standardApplicant
	 **/
	SortableAttribute<ApplicationListEntry> standardApplicant = new SortableAttributeRecord<>(STANDARD_APPLICANT);
	
	/**
	 * @see ApplicationListEntry#retryCount
	 **/
	TextAttribute<ApplicationListEntry> retryCount = new TextAttributeRecord<>(RETRY_COUNT);
	
	/**
	 * @see ApplicationListEntry#id
	 **/
	SortableAttribute<ApplicationListEntry> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * @see ApplicationListEntry#applicationCode
	 **/
	SortableAttribute<ApplicationListEntry> applicationCode = new SortableAttributeRecord<>(APPLICATION_CODE);
	
	/**
	 * @see ApplicationListEntry#notes
	 **/
	TextAttribute<ApplicationListEntry> notes = new TextAttributeRecord<>(NOTES);
	
	/**
	 * @see ApplicationListEntry#version
	 **/
	SortableAttribute<ApplicationListEntry> version = new SortableAttributeRecord<>(VERSION);
	
	/**
	 * @see ApplicationListEntry#anamedaddress
	 **/
	SortableAttribute<ApplicationListEntry> anamedaddress = new SortableAttributeRecord<>(ANAMEDADDRESS);
	
	/**
	 * @see ApplicationListEntry#accountNumber
	 **/
	TextAttribute<ApplicationListEntry> accountNumber = new TextAttributeRecord<>(ACCOUNT_NUMBER);

}

