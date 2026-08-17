package uk.gov.hmcts.appregister.common.entity.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import lombok.val;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.jdbc.ReturningWork;
import org.junit.jupiter.api.Test;

/**
 * Covers concurrent block allocation, schema qualification, and incomplete block handling.
 */
class DataAuditIdGeneratorTest {
    @Test
    void givenTwoHundredAndFiveIds_whenGenerated_thenFetchThreeSequenceBlocks() throws Exception {
        val session = mock(SharedSessionContractImplementor.class);
        val connection = mock(Connection.class);
        val statement = mock(PreparedStatement.class);
        val nextBlockStart = new AtomicLong(1);
        doAnswer(
                        invocation -> {
                            ReturningWork<?> work = invocation.getArgument(0);
                            return work.execute(connection);
                        })
                .when(session)
                .doReturningWork(any());
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery())
                .thenAnswer(
                        invocation ->
                                resultSet(
                                        nextBlockStart.getAndAdd(
                                                DataAuditIdGenerator.PREFETCH_SIZE)));

        val generator = new DataAuditIdGenerator("testschema.add_dataaudit_event");
        try (val executor = Executors.newFixedThreadPool(5)) {
            val generatedIdTasks =
                    IntStream.range(0, 205)
                            .mapToObj(
                                    ignored ->
                                            executor.submit(
                                                    () ->
                                                            generator.generate(
                                                                    session, new Object())))
                            .toList();
            val generatedIds = new ArrayList<Long>(generatedIdTasks.size());
            for (val task : generatedIdTasks) {
                generatedIds.add(task.get());
            }

            assertThat(generatedIds)
                    .containsExactlyInAnyOrderElementsOf(
                            LongStream.rangeClosed(1, 205).boxed().toList());
        }
        verify(session, times(3)).doReturningWork(any());
        verify(statement, times(3)).setString(1, "testschema.add_dataaudit_event");
        verify(statement, times(3)).setInt(2, DataAuditIdGenerator.PREFETCH_SIZE);
        verify(statement, times(3)).executeQuery();
    }

    @Test
    void givenHibernateSchema_whenCreated_thenUseQualifiedSequenceName() {
        val creationContext = mock(GeneratorCreationContext.class);
        val sqlContext = mock(SqlStringGenerationContext.class);
        val schema = Identifier.toIdentifier("testschema");
        val sequence = Identifier.toIdentifier("add_dataaudit_event");
        when(creationContext.getSqlStringGenerationContext()).thenReturn(sqlContext);
        when(sqlContext.getDefaultSchema()).thenReturn(schema);
        when(sqlContext.toIdentifier("add_dataaudit_event")).thenReturn(sequence);
        when(sqlContext.format(any(QualifiedSequenceName.class)))
                .thenReturn("testschema.add_dataaudit_event");

        new DataAuditIdGenerator(mock(DataAuditGeneratedId.class), creationContext);

        verify(sqlContext).format(new QualifiedSequenceName(null, schema, sequence));
    }

    @Test
    void givenIncompleteSequenceBlock_whenGenerated_thenFail() throws SQLException {
        val session = mock(SharedSessionContractImplementor.class);
        val connection = mock(Connection.class);
        val statement = mock(PreparedStatement.class);
        val resultSet = mock(ResultSet.class);
        doAnswer(
                        invocation -> {
                            ReturningWork<?> work = invocation.getArgument(0);
                            return work.execute(connection);
                        })
                .when(session)
                .doReturningWork(any());
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        val generator = new DataAuditIdGenerator("testschema.add_dataaudit_event");

        assertThatThrownBy(() -> generator.generate(session, new Object()))
                .isInstanceOf(SQLException.class)
                .hasMessage("Expected 100 audit sequence values but received 0");
    }

    private ResultSet resultSet(long firstId) throws SQLException {
        val resultSet = mock(ResultSet.class);
        val offset = new AtomicInteger();
        when(resultSet.next())
                .thenAnswer(invocation -> offset.get() < DataAuditIdGenerator.PREFETCH_SIZE);
        when(resultSet.getLong(1)).thenAnswer(invocation -> firstId + offset.getAndIncrement());
        return resultSet;
    }
}
