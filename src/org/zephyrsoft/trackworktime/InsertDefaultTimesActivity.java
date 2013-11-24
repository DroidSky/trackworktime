/*
 * This file is part of TrackWorkTime (TWT).
 * 
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TWT is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with TWT. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.trackworktime;

import hirondelle.date4j.DateTime;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.WeekDayEnum;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;
import org.zephyrsoft.trackworktime.util.Logger;

/**
 * Activity for managing the events of a week.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class InsertDefaultTimesActivity extends Activity {

	private DAO dao = null;
	private TimerManager timerManager = null;

	private Button save = null;
	private Button cancel = null;
	private TextView fromWeekday = null;
	private DatePicker fromDate = null;
	private int selectedFromYear = -1;
	private int selectedFromMonth = -1;
	private int selectedFromDay = -1;
	private OnDateChangedListener fromDateListener = null;
	private boolean noFromDateChangedReaction = false;
	private boolean fromPickerIsInitialized = false;
	private TextView toWeekday = null;
	private DatePicker toDate = null;
	private int selectedToYear = -1;
	private int selectedToMonth = -1;
	private int selectedToDay = -1;
	private OnDateChangedListener toDateListener = null;
	private boolean noToDateChangedReaction = false;
	private boolean toPickerIsInitialized = false;
	private List<Task> tasks;
	private ArrayAdapter<Task> tasksAdapter;
	private Spinner task = null;
	private EditText text = null;

	@Override
	protected void onPause() {
		dao.close();
		super.onPause();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		dao = Basics.getInstance().getDao();
		timerManager = Basics.getInstance().getTimerManager();

		setContentView(R.layout.default_times);

		save = (Button) findViewById(R.id.save);
		cancel = (Button) findViewById(R.id.cancel);
		fromWeekday = (TextView) findViewById(R.id.fromWeekday);
		fromDate = (DatePicker) findViewById(R.id.fromDate);
		toWeekday = (TextView) findViewById(R.id.toWeekday);
		toDate = (DatePicker) findViewById(R.id.toDate);
		task = (Spinner) findViewById(R.id.task);
		text = (EditText) findViewById(R.id.text);

		// bind lists to spinners
		tasks = dao.getActiveTasks();
		tasksAdapter = new ArrayAdapter<Task>(this, android.R.layout.simple_list_item_1, tasks);
		tasksAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		task.setAdapter(tasksAdapter);

		fromDateListener = new OnDateChangedListener() {
			@Override
			public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
				if (noFromDateChangedReaction) {
					Logger.debug("from date not changed - infinite loop protection");
				} else {
					selectedFromYear = year;
					selectedFromMonth = monthOfYear;
					selectedFromDay = dayOfMonth;

					DateTime newFromDate = getCurrentlySetFromDate();
					DateTime newToDate = getCurrentlySetToDate();
					try {
						noFromDateChangedReaction = true;

						// correct to date if from date would be after to date
						if (newFromDate.gt(newToDate)) {
							updateToDatePicker(newFromDate);
						}
					} finally {
						noFromDateChangedReaction = false;
					}

					setFromWeekday();
					Logger.debug("from date changed to {0}-{1}-{2}", year, monthOfYear, dayOfMonth);
				}
			}
		};
		toDateListener = new OnDateChangedListener() {
			@Override
			public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
				if (noToDateChangedReaction) {
					Logger.debug("to date not changed - infinite loop protection");
				} else {
					selectedToYear = year;
					selectedToMonth = monthOfYear;
					selectedToDay = dayOfMonth;

					DateTime newFromDate = getCurrentlySetFromDate();
					DateTime newToDate = getCurrentlySetToDate();
					try {
						noToDateChangedReaction = true;

						// correct from date if to date would be before from date
						if (newToDate.lt(newFromDate)) {
							updateFromDatePicker(newToDate);
						}
					} finally {
						noToDateChangedReaction = false;
					}

					setToWeekday();
					Logger.debug("to date changed to {0}-{1}-{2}", year, monthOfYear, dayOfMonth);
				}
			}
		};

		save.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// commit all edit fields
				fromDate.clearFocus();
				text.clearFocus();
				// fetch the data
				DateTime from = getCurrentlySetFromDate();
				DateTime to = getCurrentlySetToDate();
				Task selectedTask = (Task) task.getSelectedItem();
				Integer taskId = selectedTask == null ? null : selectedTask.getId();
				String textString = text.getText().toString();

				Logger.info("inserting default times from {0} to {1} with task=\"{2}\" and text=\"{3}\"", from, to,
					taskId, textString);

				// save the resulting events
				timerManager.insertDefaultWorkTimes(from, to, taskId, textString);

				finish();
			}
		});
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Logger.debug("canceling InsertDefaultTimesActivity");
				finish();
			}
		});
	}

	@Override
	public void onBackPressed() {
		Logger.debug("canceling InsertDefaultTimesActivity (back button pressed)");
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();

		// prepare for entering dates
		DateTime now = DateTimeUtil.getCurrentDateTime();
		updateFromDatePicker(now);
		updateToDatePicker(now);
	}

	private void updateFromDatePicker(DateTime dateTime) {
		if (fromPickerIsInitialized) {
			fromDate.updateDate(dateTime.getYear(), dateTime.getMonth() - 1, dateTime.getDay());
		} else {
			fromDate.init(dateTime.getYear(), dateTime.getMonth() - 1, dateTime.getDay(), fromDateListener);
			fromPickerIsInitialized = true;
		}

		selectedFromYear = dateTime.getYear();
		selectedFromMonth = dateTime.getMonth() - 1;
		selectedFromDay = dateTime.getDay();
		setFromWeekday();
	}

	private void updateToDatePicker(DateTime dateTime) {
		if (toPickerIsInitialized) {
			toDate.updateDate(dateTime.getYear(), dateTime.getMonth() - 1, dateTime.getDay());
		} else {
			toDate.init(dateTime.getYear(), dateTime.getMonth() - 1, dateTime.getDay(), toDateListener);
			toPickerIsInitialized = true;
		}

		selectedToYear = dateTime.getYear();
		selectedToMonth = dateTime.getMonth() - 1;
		selectedToDay = dateTime.getDay();
		setToWeekday();
	}

	private void setFromWeekday() {
		DateTime currentlySelectedFrom = getCurrentlySetFromDate();
		setWeekday(currentlySelectedFrom, fromWeekday);
	}

	private void setToWeekday() {
		DateTime currentlySelectedTo = getCurrentlySetToDate();
		setWeekday(currentlySelectedTo, toWeekday);
	}

	private void setWeekday(DateTime currentlySelected, TextView weekdayView) {
		WeekDayEnum weekDay = WeekDayEnum.getByValue(currentlySelected.getWeekDay());
		switch (weekDay) {
			case MONDAY:
				weekdayView.setText(R.string.monday);
				break;
			case TUESDAY:
				weekdayView.setText(R.string.tuesday);
				break;
			case WEDNESDAY:
				weekdayView.setText(R.string.wednesday);
				break;
			case THURSDAY:
				weekdayView.setText(R.string.thursday);
				break;
			case FRIDAY:
				weekdayView.setText(R.string.friday);
				break;
			case SATURDAY:
				weekdayView.setText(R.string.saturday);
				break;
			case SUNDAY:
				weekdayView.setText(R.string.sunday);
				break;
			default:
				throw new IllegalStateException("unknown weekday");
		}
	}

	private DateTime getCurrentlySetFromDate() {
		// DON'T get the numbers directly from the date and time controls, but from the private variables!
		return getCurrectlySelectedDate(selectedFromYear, selectedFromMonth, selectedFromDay);
	}

	private DateTime getCurrentlySetToDate() {
		// DON'T get the numbers directly from the date and time controls, but from the private variables!
		return getCurrectlySelectedDate(selectedToYear, selectedToMonth, selectedToDay);
	}

	private DateTime getCurrectlySelectedDate(int year, int month, int day) {
		String datePartString = String.valueOf(year) + "-" + DateTimeUtil.padToTwoDigits(month + 1) + "-"
			+ DateTimeUtil.padToTwoDigits(day);
		DateTime dateTime = new DateTime(datePartString);
		return dateTime;
	}

}
