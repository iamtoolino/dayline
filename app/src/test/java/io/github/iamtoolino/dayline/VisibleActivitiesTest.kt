package io.github.iamtoolino.dayline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VisibleActivitiesTest {
    @Test fun cancelledLauncherGestureRestoresUnderlyingAppWithoutAnotherResume() {
        val activities = VisibleActivities()
        activities.resume("selected", "Main")
        repeat(5) {
            activities.resume("launcher", "Home")
            assertEquals("launcher", activities.foreground)
            activities.stop("launcher", "Home")
            assertEquals("selected", activities.foreground)
        }
    }
    @Test fun completingHomeGestureDoesNotRestoreAStoppedApp() {
        val activities = VisibleActivities()
        activities.resume("selected", "Main")
        activities.resume("launcher", "Home")
        activities.stop("selected", "Main")
        assertEquals("launcher", activities.foreground)
        activities.stop("launcher", "Home")
        assertNull(activities.foreground)
    }
    @Test fun oldActivityStopDoesNotEraseNewActivityInSameApp() {
        val activities = VisibleActivities()
        activities.resume("selected", "First")
        activities.resume("selected", "Second")
        activities.stop("selected", "First")
        assertEquals("selected", activities.foreground)
        activities.stop("selected", "Second")
        assertNull(activities.foreground)
    }
    @Test fun unrelatedAppTakesPriorityAndStoppedAppsDoNotComeBack() {
        val activities = VisibleActivities()
        activities.resume("selected", "Main")
        activities.resume("unselected", "Main")
        activities.stop("selected", "Main")
        assertEquals("unselected", activities.foreground)
        activities.stop("unselected", "Main")
        assertNull(activities.foreground)
    }
    @Test fun resumingAnAlreadyVisibleActivityMovesItToTheFront() {
        val activities = VisibleActivities()
        activities.resume("first", "Main")
        activities.resume("second", "Main")
        activities.resume("first", "Main")
        assertEquals("first", activities.foreground)
        activities.stop("first", "Main")
        assertEquals("second", activities.foreground)
    }
    @Test fun shutdownClearsAllRememberedActivities() {
        val activities = VisibleActivities()
        activities.resume("selected", "Main")
        activities.resume("launcher", "Home")
        activities.clear()
        activities.stop("launcher", "Home")
        assertNull(activities.foreground)
    }
}
