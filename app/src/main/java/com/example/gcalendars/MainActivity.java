package com.example.gcalendars;


import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.gcalendars.LogIn.UserCalendar;
import com.example.gcalendars.customs.CustomCalendar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private FirebaseUser user;
    private GridLayout calendarButtonsLayout;
    private DatabaseReference databaseReference; // 추가: 데이터베이스 레퍼런스

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        databaseReference = FirebaseDatabase.getInstance().getReference();

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        calendarButtonsLayout = findViewById(R.id.calendarButtonsLayout);

        if (user != null) {
            loadUserCalendars();
            checkAndRespondToGroupCalendarRequests(user.getUid()); // 수정: user.getUid()로 사용자 ID 가져오기
        }

        Button buttonAddCalendar = findViewById(R.id.addButton);
        buttonAddCalendar.setOnClickListener(v -> showAddCalendarDialog());
    }

    private void showAddCalendarDialog() {
        AddCalendarDialog addCalendarDialog = new AddCalendarDialog(this);
        addCalendarDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.nav_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_profile_settings) {
            // "개인정보 설정" 메뉴를 눌렀을 때의 동작
            startActivity(new Intent(this, personalSettings.class)); // 개인정보 설정 화면으로 이동
            return true;
        } else if (id == R.id.menu_share_calendar) {
            // "친구 관리" 버튼을 눌렀을 때의 동작
            startActivity(new Intent(this, FriendsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadUserCalendars() {
        String userUid = user.getUid();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userUid);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<UserCalendar> userCalendars = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.child("calendars").getChildren()) {
                    String calendarId = dataSnapshot.getKey();
                    String calendarName = dataSnapshot.child("calendarName").getValue(String.class);
                    userCalendars.add(new UserCalendar(calendarId, calendarName));
                }

                // 사용자의 개인 캘린더 정보를 로드한 후 그룹 캘린더 정보를 추가
                for (DataSnapshot dataSnapshot : snapshot.child("group-calendar").getChildren()) {
                    String groupCalendarId = dataSnapshot.child("groupId").getValue(String.class); // "groupId" 필드 사용
                    String groupCalendarName = dataSnapshot.child("group-calendarName").getValue(String.class); // "group-calendarName" 필드 사용
                    userCalendars.add(new UserCalendar(groupCalendarId, groupCalendarName));
                }

                // 캘린더 정보를 로드한 후 버튼을 생성
                createCalendarLayouts(userCalendars);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // 처리 중에 오류가 발생한 경우 처리할 내용 추가
            }
        });
    }

    private void createCalendarLayouts(List<UserCalendar> userCalendars) {
        calendarButtonsLayout.removeAllViews();

        for (int i = 0; i < userCalendars.size(); i++) {
            UserCalendar calendarInfo = userCalendars.get(i);
            LinearLayout calendarLayout = createCalendarLayout(calendarInfo);

            calendarLayout.setOnLongClickListener(view -> {
                showDeleteDialog(calendarInfo.getCalendarId(), calendarInfo.getCalendarName());
                return true; // Return true to indicate that the long click event was consumed
            });

            // 캘린더 클릭 시 이벤트 추가
            calendarLayout.setOnClickListener(view ->
                    openCustomCalendar(calendarInfo.getCalendarId(), calendarInfo.getCalendarName()));

            calendarButtonsLayout.addView(calendarLayout);
        }
    }

    private LinearLayout createCalendarLayout(UserCalendar calendarInfo) {
        LinearLayout calendarLayout = new LinearLayout(this);
        calendarLayout.setOrientation(LinearLayout.VERTICAL);

        TextView calendarTextView = new TextView(this);
        calendarTextView.setText(calendarInfo.getCalendarName());
        calendarTextView.setGravity(Gravity.CENTER);
        calendarTextView.setBackgroundResource(R.drawable.rounded_box);

        List<String> nearestEvents = getNearestEvents(calendarInfo.getCalendarId());
        for (String event : nearestEvents) {
            TextView eventTextView = createEventTextView(event);
            calendarLayout.addView(eventTextView);
        }

        setLayoutParamsAndBackground(calendarLayout);

        return calendarLayout;
    }

    private TextView createEventTextView(String event) {
        TextView eventTextView = new TextView(this);
        eventTextView.setText(event);
        eventTextView.setGravity(Gravity.CENTER);
        return eventTextView;
    }

    private void setLayoutParamsAndBackground(LinearLayout calendarLayout) {
        // 레이아웃 파라미터 설정
        GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, colSpec);
        int marginInPixels = 16;
        params.setMargins(marginInPixels, marginInPixels, marginInPixels, marginInPixels);
        params.setGravity(Gravity.FILL_VERTICAL | Gravity.FILL_HORIZONTAL);
        calendarLayout.setLayoutParams(params);

        // 배경 설정
        calendarLayout.setBackgroundResource(R.drawable.rounded_box_color);
    }

    private List<String> getNearestEvents(String calendarId) {
        List<String> events = new ArrayList<>();

        // 현재 날짜를 구합니다.
        LocalDate currentDate = LocalDate.now();

        // Firestore에서 해당 캘린더의 일정을 가져오기
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(calendarId)
                .whereArrayContains("dates", currentDate.format(DateTimeFormatter.ofPattern("yyyy MM dd")))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // 각 문서의 dates 배열에서 현재 날짜를 포함하거나 앞으로 가까운 일정을 가져옴
                            List<String> dates = (List<String>) document.get("dates");
                            if (dates != null && !dates.isEmpty()) {
                                int currentIndex = dates.indexOf(currentDate.format(DateTimeFormatter.ofPattern("yyyy MM dd")));
                                if (currentIndex != -1) {
                                    // 현재 날짜를 포함하는 경우
                                    int endIndex = Math.min(currentIndex + 3, dates.size());
                                    for (int i = currentIndex; i < endIndex; i++) {
                                        events.add(document.getString("title"));
                                    }
                                } else {
                                    // 현재 날짜를 포함하지 않는 경우
                                    int endIndex = Math.min(3, dates.size());
                                    for (int i = 0; i < endIndex; i++) {
                                        events.add(document.getString("title"));
                                    }
                                }
                            }
                        }
                        // UI에 표시하기 위해 호출
                        updateUIWithEvents(events);

                    } else {
                        Log.e(TAG, "Error getting documents: " + task.getException(), task.getException());
                    }
                });

        return events;
    }

    private void updateUIWithEvents(List<String> events) {
        calendarButtonsLayout.removeAllViews();

        for (String event : events) {
            TextView eventTextView = createEventTextView(event);
            calendarButtonsLayout.addView(eventTextView);
        }
    }


    private void showDeleteDialog(String calendarId, String calendarName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("캘린더 삭제");
        builder.setMessage(calendarName + " 캘린더를 삭제하시겠습니까?");

        builder.setPositiveButton("삭제", (dialog, which) ->
                deleteCalendar(calendarId, user.getUid()));

        builder.setNegativeButton("취소", (dialog, which) -> {
            // 사용자가 취소한 경우 아무 작업도 수행하지 않음
        });

        AlertDialog alertDialog = builder.create();
        setDialogButtonColors(alertDialog);
        alertDialog.show();
    }

    private void setDialogButtonColors(AlertDialog alertDialog) {
        alertDialog.setOnShowListener(dialogInterface -> {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.blue));
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.blue));
        });
    }

    private void deleteCalendar(String calendarId, String userID) {
        DatabaseReference calendarsRef = databaseReference.child("users").child(userID).child("calendars").child(calendarId);
        DatabaseReference groupCalendarsRef = databaseReference.child("users").child(userID).child("group-calendar").child(calendarId);

        // calendars 경로에서 삭제
        calendarsRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    // 캘린더 삭제 성공
                    Toast.makeText(MainActivity.this, "캘린더가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    // 필요하다면 UI 업데이트 등 추가 작업 수행
                })
                .addOnFailureListener(e -> {
                    // 캘린더 삭제 실패
                    Toast.makeText(MainActivity.this, "캘린더 삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });

        // group-calendar 경로에서 삭제
        groupCalendarsRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    // 캘린더 삭제 성공
                    Toast.makeText(MainActivity.this, "캘린더가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    // 필요하다면 UI 업데이트 등 추가 작업 수행
                })
                .addOnFailureListener(e -> {
                    // 캘린더 삭제 실패
                    Toast.makeText(MainActivity.this, "캘린더 삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
    }


    private void openCustomCalendar(String calendarId, String calendarName) {
        // 커스텀 캘린더 클래스로 이동하고 정보 전달
        Intent intent = new Intent(this, CustomCalendar.class);
        intent.putExtra("calendarId", calendarId);
        intent.putExtra("calendarName", calendarName);
        startActivity(intent);
    }

    private void checkAndRespondToGroupCalendarRequests(String userID) {
        DatabaseReference userRef = databaseReference.child("users").child(userID).child("group-calendar-requests");

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot requestSnapshot : dataSnapshot.getChildren()) {
                    String groupID = requestSnapshot.child("groupID").getValue(String.class);
                    String groupCalendarName = requestSnapshot.child("groupCalendarName").getValue(String.class);
                    String requestStatus = requestSnapshot.child("status").getValue(String.class);
                    String userName = requestSnapshot.child("username").getValue(String.class);
                    if ("pending".equals(requestStatus)) {
                        FriendsActivity.showGroupCalendarRequestDialog(MainActivity.this, userName, groupID, groupCalendarName);
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "그룹 캘린더 공유 요청을 확인하는 동안 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

}