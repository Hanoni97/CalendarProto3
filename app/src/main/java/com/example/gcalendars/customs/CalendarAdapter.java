package com.example.gcalendars.customs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gcalendars.R;

import java.util.ArrayList;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarViewHolder> {
    // 해당 월의 날짜 목록과 아이템 클릭 리스너를 저장하는 어댑터 클래스입니다.
    private final ArrayList<String> daysOfMonth;  // 해당 월의 날짜 목록
    private final OnItemListener onItemListener;  // 아이템 클릭 리스너

    // 어댑터의 생성자 메서드
    public CalendarAdapter(ArrayList<String> daysOfMonth, OnItemListener onItemListener) {
        this.daysOfMonth = daysOfMonth;
        this.onItemListener = onItemListener;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 아이템 뷰 홀더를 생성하는 메서드입니다.
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.calendar_cell, parent, false);
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = (int) (parent.getHeight() * 0.166666666);
        return new CalendarViewHolder(view, onItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        // 아이템 뷰 홀더에 데이터를 바인딩하는 메서드입니다.
        holder.dayOfMonth.setText(daysOfMonth.get(position));
    }

    @Override
    public int getItemCount() {
        // 아이템의 개수를 반환하는 메서드입니다.
        return daysOfMonth.size();
    }

    // 아이템 클릭 리스너 인터페이스
    public interface OnItemListener {
        // 아이템을 클릭했을 때 호출되는 메서드
        void onItemClick(int position, String dayText);
    }
}
