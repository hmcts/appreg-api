package uk.gov.hmcts.appregister.common.mapper;

import static uk.gov.hmcts.appregister.common.exception.CommonAppError.SORT_DIRECTION_NOT_SUITABLE;
import static uk.gov.hmcts.appregister.common.exception.CommonAppError.SORT_NOT_SUITABLE;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.api.SortableField;
import uk.gov.hmcts.appregister.common.exception.AppRegistryException;

public class SortMapperTest {
    private SortMapper sortMapper = new SortMapper();

    @Test
    public void testMapperSuccess() {
        String[] sortString = {"title", "code,desc"};
        List<String> sortStringLst =
                sortMapper.map(
                        SortableField.of(sortString),
                        (field) -> {
                            for (TestSortableOperationEnum testEnum :
                                    TestSortableOperationEnum.values()) {
                                if (testEnum.getApiValue().equals(field)) {
                                    return testEnum;
                                }
                            }
                            return null;
                        });

        Assertions.assertEquals(2, sortStringLst.size());
        Assertions.assertEquals("destinationEmail1", sortStringLst.get(0));
        Assertions.assertEquals("requiresRespondent,desc", sortStringLst.get(1));
    }

    @Test
    public void testMapperSuccess2() {
        String[] sortString = {"title"};
        List<String> sortStringLst =
                sortMapper.map(
                        SortableField.of(sortString),
                        (field) -> {
                            for (TestSortableOperationEnum testEnum :
                                    TestSortableOperationEnum.values()) {
                                if (testEnum.getApiValue().equals(field)) {
                                    return testEnum;
                                }
                            }
                            return null;
                        });

        Assertions.assertEquals(1, sortStringLst.size());
        Assertions.assertEquals("destinationEmail1", sortStringLst.get(0));
    }

    @Test
    public void testMapperFailDirection() {
        String[] sortString = {"title", "code,desc1"};
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                sortMapper.map(
                                        SortableField.of(sortString),
                                        (field) -> {
                                            for (TestSortableOperationEnum testEnum :
                                                    TestSortableOperationEnum.values()) {
                                                if (testEnum.getApiValue().equals(field)) {
                                                    return testEnum;
                                                }
                                            }
                                            return null;
                                        }));

        Assertions.assertEquals(SORT_DIRECTION_NOT_SUITABLE, appRegistryException.getCode());
    }

    @Test
    public void testMapperFieldNotSuitable() {
        String[] sortString = {"title1", "code,desc"};
        AppRegistryException appRegistryException =
                Assertions.assertThrows(
                        AppRegistryException.class,
                        () ->
                                sortMapper.map(
                                        SortableField.of(sortString),
                                        (field) -> {
                                            for (TestSortableOperationEnum testEnum :
                                                    TestSortableOperationEnum.values()) {
                                                if (testEnum.getApiValue().equals(field)) {
                                                    return testEnum;
                                                }
                                            }
                                            return null;
                                        }));

        Assertions.assertEquals(SORT_NOT_SUITABLE, appRegistryException.getCode());
    }
}
