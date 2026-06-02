package com.example

import com.example.util.ScheduleUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ScheduleUtilsTest {

  @Test
  fun timeToMinutes_parsesTwelveHourClock() {
    assertEquals(0, ScheduleUtils.timeToMinutes("12:00 AM"))
    assertEquals(8 * 60, ScheduleUtils.timeToMinutes("8:00 AM"))
    assertEquals(11 * 60 + 30, ScheduleUtils.timeToMinutes("11:30 AM"))
    assertEquals(12 * 60, ScheduleUtils.timeToMinutes("12:00 PM"))
    assertEquals(20 * 60, ScheduleUtils.timeToMinutes("8:00 PM"))
  }

  @Test
  fun timeToMinutes_ordersChronologicallyNotLexicographically() {
    // The original SQL string sort placed "11:30 AM" before "8:00 AM". Verify the fix.
    val times = listOf("8:00 PM", "11:30 AM", "8:00 AM")
    val sorted = times.sortedBy { ScheduleUtils.timeToMinutes(it) }
    assertEquals(listOf("8:00 AM", "11:30 AM", "8:00 PM"), sorted)
  }

  @Test
  fun timeToMinutes_unparseableSortsLast() {
    assertEquals(Int.MAX_VALUE, ScheduleUtils.timeToMinutes(""))
    assertEquals(Int.MAX_VALUE, ScheduleUtils.timeToMinutes("whenever"))
  }

  @Test
  fun isToday_distinguishesDays() {
    val now = System.currentTimeMillis()
    val yesterday = Calendar.getInstance().apply {
      timeInMillis = now
      add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis

    assertTrue(ScheduleUtils.isToday(now, now))
    assertFalse(ScheduleUtils.isToday(yesterday, now))
  }
}
