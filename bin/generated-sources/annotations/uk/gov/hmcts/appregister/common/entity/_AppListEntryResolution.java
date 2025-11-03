package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(AppListEntryResolution.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _AppListEntryResolution extends uk.gov.hmcts.appregister.common.entity.base._BaseChangeableEntity {

	String RESOLUTION_CODE = "resolutionCode";
	String RESOLUTION_OFFICER = "resolutionOfficer";
	String APPLICATION_LIST = "applicationList";
	String ID = "id";
	String VERSION = "version";
	String CREATED_USER = "createdUser";
	String RESOLUTION_WORDING = "resolutionWording";

	
	/**
	 * @see AppListEntryResolution#resolutionCode
	 **/
	SortableAttribute<AppListEntryResolution> resolutionCode = new SortableAttributeRecord<>(RESOLUTION_CODE);
	
	/**
	 * @see AppListEntryResolution#resolutionOfficer
	 **/
	TextAttribute<AppListEntryResolution> resolutionOfficer = new TextAttributeRecord<>(RESOLUTION_OFFICER);
	
	/**
	 * @see AppListEntryResolution#applicationList
	 **/
	SortableAttribute<AppListEntryResolution> applicationList = new SortableAttributeRecord<>(APPLICATION_LIST);
	
	/**
	 * @see AppListEntryResolution#id
	 **/
	SortableAttribute<AppListEntryResolution> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * @see AppListEntryResolution#version
	 **/
	SortableAttribute<AppListEntryResolution> version = new SortableAttributeRecord<>(VERSION);
	
	/**
	 * @see AppListEntryResolution#createdUser
	 **/
	TextAttribute<AppListEntryResolution> createdUser = new TextAttributeRecord<>(CREATED_USER);
	
	/**
	 * @see AppListEntryResolution#resolutionWording
	 **/
	TextAttribute<AppListEntryResolution> resolutionWording = new TextAttributeRecord<>(RESOLUTION_WORDING);

}

