package pl.rafman.scrollcalendar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;

import pl.rafman.scrollcalendar.adapter.ResProvider;
import pl.rafman.scrollcalendar.adapter.ScrollCalendarAdapter;
import pl.rafman.scrollcalendar.adapter.example.DefaultDateScrollCalendarAdapter;
import pl.rafman.scrollcalendar.adapter.example.DefaultRangeScrollCalendarAdapter;
import pl.rafman.scrollcalendar.contract.DateWatcher;
import pl.rafman.scrollcalendar.contract.MonthScrollListener;
import pl.rafman.scrollcalendar.contract.OnDateClickListener;
import pl.rafman.scrollcalendar.style.DayResProviderImpl;
import pl.rafman.scrollcalendar.style.MonthResProviderImpl;
import pl.rafman.scrollcalendar.values.Defaults;
import pl.rafman.scrollcalendar.values.Keys;

/**
 * Created by rafal.manka on 10/09/2017
 */
public class ScrollCalendar extends LinearLayoutCompat implements ResProvider, CalendarProvider {

    @Nullable
    private String customFont;
    @Nullable
    private ScrollCalendarAdapter adapter;
    @StyleRes
    private int monthTitleStyle;
    @StyleRes
    private int legendItemStyle;
    @StyleRes
    private int legendSeparatorStyle;
    @StyleRes
    private int currentDayStyle;
    @StyleRes
    private int selectedItemStyle;
    @StyleRes
    private int selectedBeginningDayStyle;
    @StyleRes
    private int selectedMiddleDayStyle;
    @StyleRes
    private int selectedEndDayStyle;
    @StyleRes
    private int disabledItemStyle;
    @StyleRes
    private int unavailableItemStyle;
    @StyleRes
    private int dayStyle;

    private int defaultAdapter;
    private int firstDayOfWeek;
    private boolean showYearAlways;
    private boolean softLineBreaks;
    private boolean addCurrentMonth;
    private boolean useShortWeekdayNames;

    private final LegendItem[] legend = new LegendItem[7];

    public ScrollCalendar(Context context) {
        super(context);
        init(context);
    }

    public ScrollCalendar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initStyle(context, attrs);
        init(context);
    }

    public ScrollCalendar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initStyle(context, attrs);
        init(context);
    }

    private void init(@NonNull Context context) {
        setOrientation(VERTICAL);
        inflate(context, R.layout.scrollcalendar_calendar, this);
        LegendItem.useShortWeekdayNames(useShortWeekdayNames);
        for (int i = 0; i < legend.length; i++) {
            legend[i] = new LegendItem(i + 1, this);
        }
    }

    private void initStyle(@NonNull Context context, @Nullable AttributeSet attrs) {
        TypedArray typedArray = context
                .obtainStyledAttributes(attrs, R.styleable.ScrollCalendar, R.attr.scrollCalendarStyleAttr, R.style.ScrollCalendarStyle);
        selectedItemStyle = typedArray.getResourceId(R.styleable.ScrollCalendar_selectedItemStyle, 0);
        defaultAdapter = typedArray.getInt(Keys.ADAPTER, Defaults.ADAPTER);
        firstDayOfWeek = typedArray.getInt(R.styleable.ScrollCalendar_firstDayOfWeek, -1);
        customFont = typedArray.getString(Keys.CUSTOM_FONT);
        monthTitleStyle = typedArray.getResourceId(R.styleable.ScrollCalendar_monthTitleStyle, 0);
        legendItemStyle = typedArray.getResourceId(R.styleable.ScrollCalendar_legendItemStyle, 0);
        currentDayStyle = typedArray.getResourceId(R.styleable.ScrollCalendar_currentDayStyle, 0);
        selectedBeginningDayStyle = typedArray.getResourceId(R.styleable.ScrollCalendar_selectedBeginningItemStyle, 0);
        legendSeparatorStyle = typedArray.getResourceId(R.styleable.ScrollCalendar_legendSeparatorStyle, 0);
        selectedMiddleDayStyle = typedArray.getResourceId(R.styleable.ScrollCalendar_selectedMiddleItemStyle, 0);
        selectedEndDayStyle = typedArray.getResourceId(R.styleable.ScrollCalendar_selectedEndItemStyle, 0);
        disabledItemStyle = typedArray.getResourceId(R.styleable.ScrollCalendar_disabledItemStyle, 0);
        unavailableItemStyle = typedArray.getResourceId(R.styleable.ScrollCalendar_unavailableItemStyle, 0);
        dayStyle = typedArray.getResourceId(R.styleable.ScrollCalendar_dayStyle, 0);
        showYearAlways = typedArray.getBoolean(R.styleable.ScrollCalendar_showYearAlways, false);
        softLineBreaks = typedArray.getBoolean(R.styleable.ScrollCalendar_roundLineBreaks, true);
        addCurrentMonth = typedArray.getBoolean(R.styleable.ScrollCalendar_addCurrentMonth, true);
        useShortWeekdayNames = typedArray.getBoolean(R.styleable.ScrollCalendar_useShortWeekdayNames, false);

        typedArray.recycle();
    }

    public void setOnDateClickListener(@Nullable final OnDateClickListener calendarCallback) {
        getAdapter().setOnDateClickListener(calendarCallback);
    }

    // make sure to style with
    // scrollcalendar:addCurrentMonth="false"
    public void setDateRange(@Nullable Calendar firstDate, @Nullable Calendar lastDate, boolean truncateFirstAndLastMonth) {
        getAdapter().setDateRange(firstDate, lastDate, truncateFirstAndLastMonth);
    }

    @SuppressWarnings("unused")
    public void refresh() {
        refreshLegend();
        getAdapter().notifyDataSetChanged();
    }

    public void setDateWatcher(@Nullable final DateWatcher dateWatcher) {
        getAdapter().setDateWatcher(dateWatcher);
    }

    public void setMonthScrollListener(@Nullable final MonthScrollListener monthScrollListener) {
        getAdapter().setMonthScrollListener(monthScrollListener);
    }

    private static final int[] attrs = {
            android.R.attr.background,
            android.R.attr.height,
    };

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setupLegend();
        setupSeparatorStyle();
        setupRecyclerView();
    }

    private void setupLegend() {
        LinearLayout legendHolder = findViewById(R.id.legend);
        for (LegendItem legendItem : legend) {
            legendHolder.addView(legendItem.layout(legendHolder, this));
        }
        refreshLegend();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(getAdapter());
    }

    private void setupSeparatorStyle() {
        // Legend
        View separator = findViewById(R.id.separator);
        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(legendSeparatorStyle, attrs);
        for (int i = 0; i < attrs.length; i++) {
            switch (attrs[i]) {
                case android.R.attr.background: {
                    int resourceId = typedArray.getResourceId(i, 0);
                    if (resourceId != 0) {
                        separator.setBackgroundResource(resourceId);
                    } else {
                        int color = typedArray.getColor(i, 0);
                        separator.setBackgroundColor(color);

                    }
                    break;
                }
                case android.R.attr.height: {
                    int resource = typedArray.getDimensionPixelSize(i, 0);
                    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, resource);
                    separator.setLayoutParams(params);
                    break;
                }
                default:
                    break;
            }
        }
        typedArray.recycle();
    }

    private void refreshLegend() {
        for (LegendItem legendItem : legend) {
            legendItem.display();
        }
    }

    @NonNull
    public ScrollCalendarAdapter getAdapter() {
        if (adapter == null) {
            adapter = createAdapter();
        }
        return adapter;
    }

    protected ScrollCalendarAdapter createAdapter() {
        MonthResProviderImpl monthResProvider = new MonthResProviderImpl(getContext(), this);
        DayResProviderImpl dayResProvider = new DayResProviderImpl(getContext(), this);

        switch (defaultAdapter) {
            case 1:
                return new DefaultDateScrollCalendarAdapter(monthResProvider, dayResProvider, this, addCurrentMonth);
            case 2:
                return new DefaultRangeScrollCalendarAdapter(monthResProvider, dayResProvider, this, addCurrentMonth);
            case 0:
            default:
                return new ScrollCalendarAdapter(monthResProvider, dayResProvider, this, addCurrentMonth);
        }
    }

    @StyleRes
    @Override
    public int getMonthTitleStyle() {
        return monthTitleStyle;
    }

    @Override
    public int getLegendItemStyle() {
        return legendItemStyle;
    }

    @StyleRes
    @Override
    public int getCurrentDayStyle() {
        return currentDayStyle;
    }

    @StyleRes
    @Override
    public int getSelectedDayStyle() {
        return selectedItemStyle;
    }

    @StyleRes
    @Override
    public int getSelectedBeginningDayStyle() {
        return selectedBeginningDayStyle;
    }

    @StyleRes
    @Override
    public int getSelectedMiddleDayStyle() {
        return selectedMiddleDayStyle;
    }

    @StyleRes
    @Override
    public int getSelectedEndDayStyle() {
        return selectedEndDayStyle;
    }

    @StyleRes
    @Override
    public int getUnavailableItemStyle() {
        return unavailableItemStyle;
    }

    @StyleRes
    @Override
    public int getDayStyle() {
        return dayStyle;
    }

    @StyleRes
    @Override
    public int getDisabledItemStyle() {
        return disabledItemStyle;
    }

    @Override
    public boolean showYearAlways() {
        return showYearAlways;
    }

    @Override
    public boolean softLineBreaks() {
        return softLineBreaks;
    }

    // Other

    @Override
    @Nullable
    public Typeface getCustomFont() {
        if (customFont == null) {
            return null;
        }
        try {
            return Typeface.createFromAsset(getContext().getAssets(), customFont);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Calendar getCalendar() {
        Calendar calendar = Calendar.getInstance();
        if(firstDayOfWeek != -1) {
            calendar.setFirstDayOfWeek(firstDayOfWeek);
        }
        return calendar;
    }

    public void setFirstDayOfWeek(int firstDayOfWeek) {
        this.firstDayOfWeek = firstDayOfWeek;
        for (int i = 0; i < legend.length; i++) {
            legend[i] = new LegendItem(i + 1, this);
        }
        LinearLayout legendHolder = findViewById(R.id.legend);
        legendHolder.removeAllViews();
        setupLegend();
        adapter.notifyDataSetChanged();
    }
}
