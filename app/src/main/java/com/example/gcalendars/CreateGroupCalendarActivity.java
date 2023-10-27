package com.example.gcalendars;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class CreateGroupCalendarActivity extends AppCompatActivity {

    private EditText groupNameEditText;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference groupDatabaseRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group_calendar);

        groupNameEditText = findViewById(R.id.groupNameEditText);
        Button createGroupButton = findViewById(R.id.createGroupButton);

        firebaseAuth = FirebaseAuth.getInstance();
        userId = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();

        groupDatabaseRef = FirebaseDatabase.getInstance().getReference().child("group-calendar");

        createGroupButton.setOnClickListener(v -> createGroup());
    }

    private void createGroup() {
        String groupName = groupNameEditText.getText().toString().trim();
        if (groupName.isEmpty()) {
            groupNameEditText.setError("그룹 이름을 입력하세요.");
            return;
        }

        final String groupId = groupDatabaseRef.push().getKey(); // 새로운 그룹 ID 생성

        // 그룹 정보를 Realtime Database에 저장
        DatabaseReference groupRef = groupDatabaseRef.child(Objects.requireNonNull(groupId));
        groupRef.child("calendarName").setValue(groupName); // 그룹 이름 저장

        // 사용자의 그룹 목록에 그룹 ID 추가
        userId = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();
        DatabaseReference userGroupRef;
        userGroupRef = FirebaseDatabase.getInstance().getReference().child("users").child(userId).child("group-calendar");
        userGroupRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userGroupRef.child(groupId).setValue(true);
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // 오류 처리
            }
        });
    }
}
