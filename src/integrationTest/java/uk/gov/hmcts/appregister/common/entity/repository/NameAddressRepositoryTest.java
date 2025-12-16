package uk.gov.hmcts.appregister.common.entity.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.appregister.common.entity.NameAddress;
import uk.gov.hmcts.appregister.common.security.UserProvider;
import uk.gov.hmcts.appregister.data.NameAddressTestData;
import uk.gov.hmcts.appregister.testutils.BaseRepositoryTest;

@Slf4j
public class NameAddressRepositoryTest extends BaseRepositoryTest {
    @Autowired private NameAddressRepository namedAddressRepository;

    @Autowired private UserProvider loggedInUser;

    private static final int BASELINE_TEST_COUNT = 5;

    @Test
    public void testBasicInsertionUpdate() throws Exception {
        // assert that the save has occurred
        long count = namedAddressRepository.count();
        Assertions.assertEquals(BASELINE_TEST_COUNT, count);

        // test save
        NameAddress nameAddress = persistance.save(new NameAddressTestData().someComplete());

        // test get
        Optional<NameAddress> nameAddressToAssertAgainst =
                namedAddressRepository.findById(nameAddress.getId());

        // assert that the data that has been retrieved aligns with the data that we have stored
        expectAllCommonEntityFields(nameAddress, nameAddressToAssertAgainst.get());
        assertTrue(nameAddressToAssertAgainst.isPresent());
        assertEquals(nameAddress.getAddress1(), nameAddressToAssertAgainst.get().getAddress1());
        assertEquals(nameAddress.getAddress2(), nameAddressToAssertAgainst.get().getAddress2());
        assertEquals(nameAddress.getAddress3(), nameAddressToAssertAgainst.get().getAddress3());
        assertEquals(nameAddress.getAddress4(), nameAddressToAssertAgainst.get().getAddress4());
        assertEquals(nameAddress.getName(), nameAddressToAssertAgainst.get().getName());
        assertEquals(nameAddress.getCode(), nameAddressToAssertAgainst.get().getCode());
        assertEquals(nameAddress.getDmsId(), nameAddressToAssertAgainst.get().getDmsId());
        assertEquals(nameAddress.getDmsId(), nameAddressToAssertAgainst.get().getDmsId());
        assertEquals(
                nameAddress.getDateOfBirth().toLocalDate(),
                nameAddressToAssertAgainst.get().getDateOfBirth().toLocalDate());
        assertEquals(
                nameAddress.getEmailAddress(), nameAddressToAssertAgainst.get().getEmailAddress());
        assertEquals(nameAddress.getForename1(), nameAddressToAssertAgainst.get().getForename1());
        assertEquals(nameAddress.getForename2(), nameAddressToAssertAgainst.get().getForename2());
        assertEquals(nameAddress.getForename3(), nameAddressToAssertAgainst.get().getForename3());
        assertEquals(nameAddress.getSurname(), nameAddressToAssertAgainst.get().getSurname());
        assertEquals(
                nameAddress.getTelephoneNumber(),
                nameAddressToAssertAgainst.get().getTelephoneNumber());
        assertEquals(nameAddress.getTitle(), nameAddressToAssertAgainst.get().getTitle());
    }
}
