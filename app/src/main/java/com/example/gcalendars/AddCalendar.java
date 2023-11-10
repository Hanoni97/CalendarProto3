package com.example.gcalendars;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

class AddCalendarDialog extends Dialog {

    private EditText calendarNameEditText;

    private DatabaseReference databaseReference;
    private DatabaseReference groupDatabaseRef;

    public AddCalendarDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_calendar);

        databaseReference = FirebaseDatabase.getInstance().getReference();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        calendarNameEditText = findViewById(R.id.editTextCalendarName);
        Button createButton = findViewById(R.id.buttonCreate);
        Button cancelButton = findViewById(R.id.buttonCancel);
        Button createGroupButton = findViewById(R.id.createGroupButton);

        String userId = Objects.requireNonNull(user).getUid();

        groupDatabaseRef = FirebaseDatabase.getInstance().getReference().child("users").child(userId).child("group-calendar");


        // 개인캘린더 생성
        createButton.setOnClickListener(view -> {
            String calendarName = calendarNameEditText.getText().toString().trim();
            if (!calendarName.isEmpty()) {
                // 사용자의 UID를 사용하여 해당 사용자의 루트 노드 아래에 "calendars" 노드를 만들고 그 아래에 캘린더 정보를 저장합니다.
                DatabaseReference userCalendarsRef = databaseReference.child("users").child(userId).child("calendars");
                String calendarId = userCalendarsRef.push().getKey();
                assert calendarId != null;
                userCalendarsRef.child(calendarId).child("calendarName").setValue(calendarName);

                Toast.makeText(getContext(), "캘린더가 생성되었습니다.", Toast.LENGTH_SHORT).show();
                dismiss();
            }
        });

        //그룹 캘린더 생성 버튼 작용함수
        createGroupButton.setOnClickListener(v -> createGroup());
        // 취소 버튼
        cancelButton.setOnClickListener(view -> dismiss());
    }

    private void createGroup() {
        String groupName = calendarNameEditText.getText().toString().trim();
        if (groupName.isEmpty()) {
            calendarNameEditText.setError("그룹 이름을 입력하세요.");
            return;
        }

        final String groupId = groupDatabaseRef.push().getKey(); // 새로운 그룹 ID 생성

        // 그룹 정보를 Realtime Database에 저장
        DatabaseReference groupRef = groupDatabaseRef.child(Objects.requireNonNull(groupId));
        groupRef.child("groupId").setValue(groupId); // 그룹 ID를 "groupId" 속성에 저장
        groupRef.child("group-calendarName").setValue(groupName); // 그룹 이름 저장
        dismiss();
    }
}
