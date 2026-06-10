package uk.gov.hmcts.appregister.common.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.appregister.common.enumeration.Status;

class ApplicationListSerializationTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void givenBidirectionalListAndEntry_whenSerializeList_thenDoesNotTraverseEntries() {
        ApplicationList list = applicationListWithEntryGraph();

        String json = Assertions.assertDoesNotThrow(() -> objectMapper.writeValueAsString(list));

        assertThat(json).doesNotContain("\"entries\"");
    }

    @Test
    void givenBidirectionalListAndEntry_whenSerializeEntry_thenDoesNotTraverseApplicationList() {
        ApplicationList list = applicationListWithEntryGraph();
        ApplicationListEntry entry = list.getEntries().iterator().next();

        String json = Assertions.assertDoesNotThrow(() -> objectMapper.writeValueAsString(entry));

        assertThat(json).doesNotContain("\"applicationList\"");
    }

    private ApplicationList applicationListWithEntryGraph() {
        ApplicationList list = new ApplicationList();
        list.setId(1L);
        list.setUuid(UUID.randomUUID());
        list.setStatus(Status.OPEN);
        list.setDescription("Serialization regression list");
        list.setDate(LocalDate.of(2026, 6, 2));
        list.setTime(LocalTime.of(10, 30));

        ApplicationListEntry entry = new ApplicationListEntry();
        entry.setId(2L);
        entry.setUuid(UUID.randomUUID());
        entry.setApplicationList(list);
        entry.setApplicationListEntryWording("Serialization regression entry");
        entry.setEntryRescheduled("N");
        entry.setSequenceNumber((short) 1);
        entry.setLodgementDate(LocalDate.of(2026, 6, 2));

        list.setEntries(Set.of(entry));

        return list;
    }
}
