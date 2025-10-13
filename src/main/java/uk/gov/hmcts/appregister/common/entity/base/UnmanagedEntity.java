package uk.gov.hmcts.appregister.common.entity.base;

/**
 * A marker interface to indicate that an entity is not managed by the application. An unmanaged
 * entity is an entity that is not tracked for changes, meaning it does not implement auditing
 * features
 */
public interface UnmanagedEntity {}
