package com.example.gcalendars.customs;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gcalendars.R;

public class CalendarViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
{
    public final TextView dayOfMonth;                               // 날짜를 표시하는 텍스트 뷰
    private final CalendarAdapter.OnItemListener onItemListener;    // 아이템 클릭 리스너
    public CalendarViewHolder(@NonNull View itemView, CalendarAdapter.OnItemListener onItemListener)
    {
        super(itemView);
        dayOfMonth = itemView.findViewById(R.id.cellDayText);   // 레이아웃에서 날짜 텍스트 뷰를 찾아 초기화
        this.onItemListener = onItemListener;                   // 아이템 클릭 리스너 초기화
        itemView.setOnClickListener(this);                      // 아이템 뷰에 클릭 리스너를 등록
    }

    @Override
    public void onClick(View view)
    {
        onItemListener.onItemClick(getAdapterPosition(), (String) dayOfMonth.getText());
        // 아이템이 클릭되면, 클릭 이벤트를 처리하기 위해 CalendarAdapter의 onItemClick 메서드를 호출
        // getAdapterPosition()을 사용하여 클릭된 아이템의 위치를 전달
        // 클릭된 날짜를 문자열로 변환하여 전달.
    }
}