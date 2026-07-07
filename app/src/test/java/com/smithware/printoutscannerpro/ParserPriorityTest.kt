package com.smithware.printoutscannerpro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ParserPriorityTest {
    @Test
    fun trainingParserExtractsRowsAndDates() {
        val parser = TrainingReportParser(today = LocalDate.of(2026, 7, 7))

        val parsed = parser.parse(
            """
            Associate | Training | Due Date | Status
            Jordan M. | Food Safety | Due: Jul 18 | Incomplete
            Mike R. | Cleaning | 07/16/2026 | Overdue
            """.trimIndent()
        )

        assertEquals(2, parsed.rows.size)
        assertEquals("Jordan M.", parsed.rows[0].associate)
        assertEquals("Food Safety", parsed.rows[0].training)
        assertTrue(parsed.rows[0].dueDateMillis != null)
        assertEquals(TrainingStatus.OVERDUE, parsed.rows[1].status)
    }

    @Test
    fun priorityEngineRanksOverdueWorkingItemsHigh() {
        val today = LocalDate.of(2026, 7, 7)
        val overdue = today.minusDays(3).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val result = TrainingPriorityEngine.score(
            dueDate = overdue,
            status = TrainingStatus.OVERDUE,
            isWorkingToday = true,
            shiftEnd = "4:00 PM",
            openTrainingItemsForAssociate = 2,
            dueSoonWindow = 3,
            nowMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

        assertEquals("Critical", result.label)
        assertTrue(result.score >= 75)
        assertTrue(result.reason.contains("Overdue"))
    }
}
