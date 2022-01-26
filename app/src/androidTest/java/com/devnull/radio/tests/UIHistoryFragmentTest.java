package com.devnull.radio.tests;

import android.content.pm.ActivityInfo;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import com.devnull.radio.tests.utils.RecyclerDragAndDropAction;
import com.devnull.radio.tests.utils.RecyclerRecyclingMatcher;
import com.devnull.radio.tests.utils.RecyclerViewMatcher;
import com.devnull.radio.tests.utils.ScrollToRecyclerItemAction;
import com.devnull.radio.tests.utils.TestUtils;
import com.devnull.radio.tests.utils.conditionwatcher.ViewMatchWaiter;
import com.devnull.radio.ActivityMain;
import com.devnull.radio.HistoryManager;
import com.devnull.radio.R;
import com.devnull.radio.RadioDroidApp;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;

@LargeTest
@RunWith(Parameterized.class)
public class UIHistoryFragmentTest {

    @Parameterized.Parameter(value = 0)
    public int orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

    @Parameterized.Parameters(name = "orientation={0}")
    public static Iterable<Object[]> initParameters() {
        return Arrays.asList(new Object[][]{
                {ActivityInfo.SCREEN_ORIENTATION_PORTRAIT},
                {ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}
        });
    }

    @Rule
    public ActivityTestRule<ActivityMain> activityRule
            = new ActivityTestRule<ActivityMain>(ActivityMain.class) {
        @Override
        protected void afterActivityLaunched() {
            getActivity().setRequestedOrientation(orientation);
            super.afterActivityLaunched();
        }
    };

    private static final int STATIONS_COUNT = 20;

    private HistoryManager historyManager;

    @Before
    public void setUp() {
        TestUtils.populateHistory(ApplicationProvider.getApplicationContext(), STATIONS_COUNT);

        RadioDroidApp app = ApplicationProvider.getApplicationContext();
        historyManager = app.getHistoryManager();
    }

    @Test
    public void stationsRecyclerHistory_ShouldRecycleItems() {
        onView(ViewMatchers.withId(R.id.nav_item_starred)).perform(ViewActions.click());

        onView(withId(R.id.recyclerViewStations)).check(matches(RecyclerRecyclingMatcher.recyclerRecycles()));
    }

    @Test
    public void stationsInHistory_ShouldNotBeReordered_WithDragAndDrop() {
        onView(ViewMatchers.withId(R.id.nav_item_history)).perform(ViewActions.click());

        onView(withId(R.id.recyclerViewStations)).perform(ScrollToRecyclerItemAction.scrollToRecyclerItem(0));
        onView(withId(R.id.recyclerViewStations)).perform(RecyclerDragAndDropAction.recyclerDragAndDrop(1, 0));

        for (int i = 0; i < 5; i++) {
            onView(withId(R.id.recyclerViewStations)).perform(ScrollToRecyclerItemAction.scrollToRecyclerItem(i));
            onView(RecyclerViewMatcher.withRecyclerView(R.id.recyclerViewStations).atPosition(i))
                    .check(matches(hasDescendant(ViewMatchers.withText(TestUtils.getFakeRadioStationName(STATIONS_COUNT - i - 1)))));
            assertEquals(historyManager.getList().get(i).Name, TestUtils.getFakeRadioStationName(STATIONS_COUNT - i - 1));
        }
    }

    @Test
    public void stationInHistory_ShouldBeDeleted_WithSwipeRight() {
        onView(withId(R.id.nav_item_history)).perform(ViewActions.click());

        onView(withId(R.id.recyclerViewStations)).perform(ScrollToRecyclerItemAction.scrollToRecyclerItem(0));
        onView(RecyclerViewMatcher.withRecyclerView(R.id.recyclerViewStations).atPosition(0)).perform(ViewActions.swipeRight());
        onView(RecyclerViewMatcher.withRecyclerView(R.id.recyclerViewStations).atPosition(0))
                .check(matches(hasDescendant(ViewMatchers.withText(TestUtils.getFakeRadioStationName(STATIONS_COUNT - 2)))));
        assertEquals(STATIONS_COUNT - 1, historyManager.getList().size());

        onView(withId(R.id.recyclerViewStations)).perform(ScrollToRecyclerItemAction.scrollToRecyclerItem(1));
        onView(RecyclerViewMatcher.withRecyclerView(R.id.recyclerViewStations).atPosition(1)).perform(ViewActions.swipeRight());
        onView(RecyclerViewMatcher.withRecyclerView(R.id.recyclerViewStations).atPosition(1))
                .check(matches(hasDescendant(ViewMatchers.withText(TestUtils.getFakeRadioStationName(STATIONS_COUNT - 4)))));
        assertEquals(STATIONS_COUNT - 2, historyManager.getList().size());

        onView(withId(R.id.recyclerViewStations)).perform(ScrollToRecyclerItemAction.scrollToRecyclerItem(2));
        onView(RecyclerViewMatcher.withRecyclerView(R.id.recyclerViewStations).atPosition(2)).perform(ViewActions.swipeRight());
        onView(RecyclerViewMatcher.withRecyclerView(R.id.recyclerViewStations).atPosition(2))
                .check(matches(hasDescendant(ViewMatchers.withText(TestUtils.getFakeRadioStationName(STATIONS_COUNT - 6)))));
        assertEquals(STATIONS_COUNT - 3, historyManager.getList().size());

        // Snackbar with undo action
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
         // for whatever reason this often does not work on API 21 emulators
         ViewMatchWaiter.waitForView(withId(com.google.android.material.R.id.snackbar_action))
                    .toMatch(
                            allOf(withText(R.string.action_station_removed_from_list_undo), isDisplayed()));
            onView(withId(com.google.android.material.R.id.snackbar_action)).perform(ViewActions.click());

            assertEquals(STATIONS_COUNT - 2, historyManager.getList().size());
            onView(withId(R.id.recyclerViewStations)).perform(ScrollToRecyclerItemAction.scrollToRecyclerItem(2));
            onView(RecyclerViewMatcher.withRecyclerView(R.id.recyclerViewStations).atPosition(2))
                    .check(matches(hasDescendant(ViewMatchers.withText(TestUtils.getFakeRadioStationName(STATIONS_COUNT - 5)))));
        }
    }
}
