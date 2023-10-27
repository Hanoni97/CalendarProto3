package com.example.gcalendars.custom;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.gcalendars.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditEventDialog extends Dialog {
    private final String title;
    private final String selectedDate;
    private final List<String> dates;
    private final List<String> content;
    private String privacy;
    private final String collectionName;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private EditText titleEditText;
    private EditText startDateEditText;
    private EditText endDateEditText;
    private EditText contentEditText;

    public EditEventDialog(Context context, String title, String selectedDate, List<String> dates, List<String> content, String privacy, String collectionName) {
        super(context);
        this.title = title;
        this.selectedDate = selectedDate;
        this.dates = dates;
        this.content = content;
        this.privacy = privacy;
        this.collectionName = collectionName;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_event_dialog);

        titleEditText = findViewById(R.id.editTextEventTitle);
        startDateEditText = findViewById(R.id.editTextEventStartDate);
        endDateEditText = findViewById(R.id.editTextEventEndDate);
        contentEditText = findViewById(R.id.editTextEventContent);
        RadioGroup privacyRadioGroup = findViewById(R.id.privacyRadioGroup);

        startDateEditText.setOnClickListener(v -> showStartDateDialog());
        endDateEditText.setOnClickListener(v -> showEndDateDialog());

        titleEditText.setText(title);
        startDateEditText.setText(dates.get(0));
        endDateEditText.setText(dates.get(dates.size() - 1)); // 마지막 날짜로 설정
        contentEditText.setText(TextUtils.join("\n", content));

        if (privacy.equals("public")) {
            privacyRadioGroup.check(R.id.radioPublic);
        } else {
            privacyRadioGroup.check(R.id.radioPrivate);
        }

        privacyRadioGroup.setOnCheckedChangeListener((group, checkedId) -> privacy = (checkedId == R.id.radioPublic) ? "public" : "private");

        Button updateButton = findViewById(R.id.updateButton);
        Button cancelButton = findViewById(R.id.cancelButton);

        updateButton.setOnClickListener(v -> {
            String updatedTitle = titleEditText.getText().toString();
            String updatedStartDate = startDateEditText.getText().toString();
            String updatedEndDate = endDateEditText.getText().toString();
            List<String> updatedContent = new ArrayList<>(Arrays.asList(contentEditText.getText().toString().split("\n")));

            if (!updatedTitle.isEmpty() && !updatedStartDate.isEmpty() && !updatedEndDate.isEmpty()) {
                updateEventInFirestore(updatedTitle, updatedStartDate, updatedEndDate, updatedContent, privacy);
            } else {
                Toast.makeText(getContext(), "일정 제목, 시작 날짜, 종료 날짜를 입력해 주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(v -> dismiss());
    }

    private void updateEventInFirestore(String updatedTitle, String updatedStartDate, String updatedEndDate, List<String> updatedContent, String updatedPrivacy) {
        db.collection(collectionName)
                .whereArrayContains("dates", selectedDate)
                .whereEqualTo("title", title)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean found = false;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        found = true;
                        List<String> dates = getDatesBetween(updatedStartDate, updatedEndDate);

                        Map<String, Object> data = new HashMap<>();
                        data.put("title", updatedTitle);
                        data.put("dates", dates);
                        data.put("content", updatedContent);
                        data.put("privacy", updatedPrivacy);

                        db.collection(collectionName).document(document.getId())
                                .set(data)
                                .addOnSuccessListener(aVoid -> {
                                    dismiss();
                                    Toast.makeText(getContext(), "일정이 업데이트되었습니다.", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "일정 업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show());
                        break;
                    }

                    if (!found) {
                        Toast.makeText(getContext(), "해당 일정을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "일정 업데이트 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show());
    }

    private void showStartDateDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (view, year1, month1, dayOfMonth1) -> {
            String selectedDate = year1 + "-" + (month1 + 1) + "-" + dayOfMonth1;
            startDateEditText.setText(selectedDate);
        }, year, month, dayOfMonth);

        datePickerDialog.show();
        datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(Color.BLUE);
        datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(Color.BLUE);
    }

    private void showEndDateDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (view, year1, month1, dayOfMonth1) -> {
            String selectedDate = year1 + "-" + (month1 + 1) + "-" + dayOfMonth1;
            endDateEditText.setText(selectedDate);
        }, year, month, dayOfMonth);

        datePickerDialog.show();
        datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(Color.BLUE);
        datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(Color.BLUE);
    }

    private List<String> getDatesBetween(String startDate, String endDate) {
        List<String> dateList = new ArrayList<>();
        try {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date start = sdf.parse(startDate);
            Date end = sdf.parse(endDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(start);
            while (!calendar.getTime().after(end)) {
                dateList.add(sdf.format(calendar.getTime()));
                calendar.add(Calendar.DATE, 1);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return dateList;
    }
}