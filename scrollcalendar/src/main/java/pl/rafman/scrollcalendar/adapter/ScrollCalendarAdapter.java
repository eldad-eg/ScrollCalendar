package pl.rafman.scrollcalendar.adapter;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import pl.rafman.scrollcalendar.CalendarProvider;
import pl.rafman.scrollcalendar.contract.ClickCallback;
import pl.rafman.scrollcalendar.contract.DateWatcher;
import pl.rafman.scrollcalendar.contract.MonthScrollListener;
import pl.rafman.scrollcalendar.contract.OnDateClickListener;
import pl.rafman.scrollcalendar.contract.State;
import pl.rafman.scrollcalendar.data.CalendarDay;
import pl.rafman.scrollcalendar.data.CalendarMonth;
import pl.rafman.scrollcalendar.style.DayResProvider;
import pl.rafman.scrollcalendar.style.MonthResProvider;


/**
 * Created by rafal.manka on 10/09/2017
 */
public class ScrollCalendarAdapter extends RecyclerView.Adapter<MonthViewHolder> implements ClickCallback {

    @NonNull
    private final List<CalendarMonth> months = new ArrayList<>();

    @Nullable
    private RecyclerView recyclerView;

    @Nullable
    private MonthScrollListener monthScrollListener;
    @Nullable
    private OnDateClickListener onDateClickListener;
    @Nullable
    private DateWatcher dateWatcher;
    protected MonthResProvider monthResProvider;
    protected DayResProvider dayResProvider;
    protected CalendarProvider calendarProvider;

    @Nullable
    private Calendar firstDate;
    @Nullable
    private Calendar lastDate;

    public ScrollCalendarAdapter(@NonNull MonthResProvider monthResProvider, @NonNull DayResProvider dayResProvider, CalendarProvider calendarProvider) {
        this.monthResProvider = monthResProvider;
        this.dayResProvider = dayResProvider;
        this.calendarProvider = calendarProvider;
        months.add(CalendarMonth.now(calendarProvider));
    }

    public ScrollCalendarAdapter(@NonNull MonthResProvider monthResProvider, @NonNull DayResProvider dayResProvider, CalendarProvider calendarProvider, boolean addCurrentMonth) {
        this.monthResProvider = monthResProvider;
        this.dayResProvider = dayResProvider;
        this.calendarProvider = calendarProvider;
        if (addCurrentMonth) {
            months.add(CalendarMonth.now(calendarProvider));
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public MonthViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return MonthViewHolder.create(parent, this, monthResProvider, dayResProvider, calendarProvider);
    }

    @Override
    public void onBindViewHolder(MonthViewHolder holder, int position) {
        CalendarMonth month = getItem(position);
        prepare(month);
        holder.bind(month);
        afterBindViewHolder(position);
    }

    private void afterBindViewHolder(int position) {
        if (recyclerView != null) {
            if (shouldAddPreviousMonth(position)) {
                recyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        prependCalendarMonth();
                    }
                });
            }
            if (shouldAddNextMonth(position)) {
                recyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        appendCalendarMonth();
                    }
                });
            }
        }

    }

    private boolean shouldAddPreviousMonth(int position) {
        return isNearTop(position) && isAllowedToAddPreviousMonth();
    }

    protected boolean isAllowedToAddPreviousMonth() {
        if (firstDate != null) {
            CalendarMonth item = getFirstItem();
            return item.getYear() > firstDate.get(Calendar.YEAR)
                    || (item.getYear() == firstDate.get(Calendar.YEAR) && item.getMonth() > firstDate.get(Calendar.MONTH));
        }
        if (monthScrollListener == null) {
            return false;
        }
        CalendarMonth item = getFirstItem();
        return monthScrollListener.shouldAddPreviousMonth(item.getYear(), item.getMonth());
    }

    private boolean shouldAddNextMonth(int position) {
        return isNearBottom(position) && isAllowedToAddNextMonth();
    }

    protected boolean isAllowedToAddNextMonth() {
        if (lastDate != null) {
            CalendarMonth item = getLastItem();
            return item.getYear() < lastDate.get(Calendar.YEAR)
                    || (item.getYear() == lastDate.get(Calendar.YEAR) && item.getMonth() < lastDate.get(Calendar.MONTH));
        }
        if (monthScrollListener == null) {
            return true;
        }
        CalendarMonth item = getLastItem();
        return monthScrollListener.shouldAddNextMonth(item.getYear(), item.getMonth());
    }

    private void prepare(CalendarMonth month) {
        for (CalendarDay calendarDay : month.getDays()) {
            calendarDay.setState(makeState(month, calendarDay));
        }
    }

    @State
    private int makeState(CalendarMonth month, CalendarDay calendarDay) {
        int year = month.getYear();
        int monthInt = month.getMonth();
        int day = calendarDay.getDay();
        return getStateForDate(year, monthInt, day);
    }

    @State
    protected int getStateForDate(int year, int month, int day) {
        if (dateWatcher == null) {
            return CalendarDay.DEFAULT;
        }
        return dateWatcher.getStateForDate(year, month, day);
    }


    private void prependCalendarMonth() {
        if (recyclerView != null) {
            months.add(0, getFirstItem().previous());
            notifyItemInserted(0);
        }
    }

    private void appendCalendarMonth() {
        months.add(getLastItem().next());
        notifyItemInserted(months.size() - 1);
    }

    private CalendarMonth getFirstItem() {
        return months.get(0);
    }

    private CalendarMonth getLastItem() {
        return months.get(months.size() - 1);
    }

    private CalendarMonth getItem(int position) {
        return months.get(position);
    }

    private boolean isNearTop(int position) {
        return position == 0;
    }

    private boolean isNearBottom(int position) {
        return months.size() - 1 <= position;
    }

    @Override
    public int getItemCount() {
        return months.size();
    }

    public void setOnDateClickListener(@Nullable OnDateClickListener onDateClickListener) {
        this.onDateClickListener = onDateClickListener;
    }

    public void setMonthScrollListener(@Nullable MonthScrollListener monthScrollListener) {
        this.monthScrollListener = monthScrollListener;
    }

    public void setDateWatcher(@Nullable DateWatcher dateWatcher) {
        this.dateWatcher = dateWatcher;
    }

    public void setDateRange(@Nullable Calendar firstDate, @Nullable Calendar lastDate, boolean truncateFirstAndLastMonth) {
        this.firstDate = firstDate;
        this.lastDate = lastDate;
        if (truncateFirstAndLastMonth) {
            CalendarMonth.setDateRange(firstDate, lastDate);
        }
        if (months.size() != 0) { // should already be empty - scrollcalendar:addCurrentMonth="false"
            months.clear();
            notifyDataSetChanged();
        }
        months.add(new CalendarMonth(calendarProvider, firstDate.get(Calendar.YEAR), firstDate.get(Calendar.MONTH)));
        notifyItemInserted(0);
    }

    @Override
    public void onCalendarDayClicked(@NonNull CalendarMonth calendarMonth, @NonNull CalendarDay calendarDay) {
        int year = calendarMonth.getYear();
        int month = calendarMonth.getMonth();
        int day = calendarDay.getDay();
        onCalendarDayClicked(year, month, day);
        notifyDataSetChanged();
    }

    protected void onCalendarDayClicked(int year, int month, int day) {
        if (onDateClickListener != null) {
            onDateClickListener.onCalendarDayClicked(year, month, day);
        }
    }

}
