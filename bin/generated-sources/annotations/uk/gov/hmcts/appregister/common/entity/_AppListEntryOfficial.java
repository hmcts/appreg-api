package uk.gov.hmcts.appregister.common.entity;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(AppListEntryOfficial.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _AppListEntryOfficial extends uk.gov.hmcts.appregister.common.entity.base._BaseChangeableEntity {

	String SURNAME = "surname";
	String OFFICIAL_TYPE = "officialType";
	String TITLE = "title";
	String ID = "id";
	String APP_LIST_ENTRY = "appListEntry";
	String FORENAME = "forename";
	String CREATED_USER = "createdUser";

	
	/**
	 * @see AppListEntryOfficial#surname
	 **/
	TextAttribute<AppListEntryOfficial> surname = new TextAttributeRecord<>(SURNAME);
	
	/**
	 * @see AppListEntryOfficial#officialType
	 **/
	TextAttribute<AppListEntryOfficial> officialType = new TextAttributeRecord<>(OFFICIAL_TYPE);
	
	/**
	 * @see AppListEntryOfficial#title
	 **/
	TextAttribute<AppListEntryOfficial> title = new TextAttributeRecord<>(TITLE);
	
	/**
	 * @see AppListEntryOfficial#id
	 **/
	SortableAttribute<AppListEntryOfficial> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * @see AppListEntryOfficial#appListEntry
	 **/
	SortableAttribute<AppListEntryOfficial> appListEntry = new SortableAttributeRecord<>(APP_LIST_ENTRY);
	
	/**
	 * @see AppListEntryOfficial#forename
	 **/
	TextAttribute<AppListEntryOfficial> forename = new TextAttributeRecord<>(FORENAME);
	
	/**
	 * @see AppListEntryOfficial#createdUser
	 **/
	TextAttribute<AppListEntryOfficial> createdUser = new TextAttributeRecord<>(CREATED_USER);

}

