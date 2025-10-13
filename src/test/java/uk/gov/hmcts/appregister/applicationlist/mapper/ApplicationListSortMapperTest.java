package uk.gov.hmcts.appregister.applicationlist.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.hmcts.appregister.applicationlist.api.ApplicationListApiFields;
import uk.gov.hmcts.appregister.applicationlist.validator.ApplicationListSortValidator;
import uk.gov.hmcts.appregister.common.entity.ApplicationList_;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

/**
 * Unit tests for {@link ApplicationListSortMapper}.
 */
public class ApplicationListSortMapperTest {

    private ApplicationListSortValidator sortValidator;
    private ApplicationListSortMapper mapper;

    @BeforeEach
    void setUp() {
        sortValidator = Mockito.mock(ApplicationListSortValidator.class);
        mapper = new ApplicationListSortMapper(sortValidator);
    }

    @Test
    void mapAndValidate_nullSorts_returnsEmptyList() {
        List<String> result = mapper.mapAndValidate(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void mapAndValidate_emptySorts_returnsEmptyList() {
        List<String> result = mapper.mapAndValidate(List.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void mapAndValidate_validSingleFieldWithoutDirection_returnsMappedField() {
        List<String> result = mapper.mapAndValidate(List.of(ApplicationListApiFields.DATE));
        assertEquals(1, result.size());
        assertEquals(ApplicationList_.DATE, result.getFirst());
        verify(sortValidator).validate(ApplicationList_.DATE);
    }

    @Test
    void mapAndValidate_validSingleFieldWithAscDirection_returnsMappedFieldWithAsc() {
        List<String> result =
                mapper.mapAndValidate(List.of(ApplicationListApiFields.STATUS + ",asc"));
        assertEquals(1, result.size());
        assertEquals(ApplicationList_.STATUS + ",asc", result.getFirst());
        verify(sortValidator).validate(ApplicationList_.STATUS);
    }

    @Test
    void mapAndValidate_validSingleFieldWithDescDirection_returnsMappedFieldWithDesc() {
        List<String> result =
                mapper.mapAndValidate(List.of(ApplicationListApiFields.CJA + ",desc"));
        assertEquals(1, result.size());
        assertEquals(ApplicationList_.CJA + ",desc", result.getFirst());
        verify(sortValidator).validate(ApplicationList_.CJA);
    }

    @Test
    void mapAndValidate_blankSortValue_throwsException() {
        AppRegistryException ex =
                assertThrows(AppRegistryException.class, () -> mapper.mapAndValidate(List.of(" ")));
        assertTrue(ex.getMessage().contains("blank"));
    }

    @Test
    void mapAndValidate_unknownField_throwsException() {
        AppRegistryException ex =
                assertThrows(
                        AppRegistryException.class,
                        () -> mapper.mapAndValidate(List.of("unknownField,asc")));
        assertTrue(ex.getMessage().contains("not allowed"));
    }

    @Test
    void mapAndValidate_invalidDirection_throwsException() {
        AppRegistryException ex =
                assertThrows(
                        AppRegistryException.class,
                        () ->
                                mapper.mapAndValidate(
                                        List.of(ApplicationListApiFields.DATE + ",up")));
        assertTrue(ex.getMessage().contains("not valid"));
    }

    @Test
    void mapAndValidate_multipleValidFields_returnsAllMapped() {
        List<String> sorts =
                List.of(
                        ApplicationListApiFields.DATE + ",desc",
                        ApplicationListApiFields.STATUS + ",asc");
        List<String> result = mapper.mapAndValidate(sorts);
        assertEquals(2, result.size());
        assertEquals(ApplicationList_.DATE + ",desc", result.get(0));
        assertEquals(ApplicationList_.STATUS + ",asc", result.get(1));
        verify(sortValidator).validate(ApplicationList_.DATE);
        verify(sortValidator).validate(ApplicationList_.STATUS);
    }
}
