package com.example.gcalendars;


import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
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

        buttonAddCalendar.setOnClickListener(v -> {
            // 다이얼로그 띄우기
            showAddCalendarDialog();
        });
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
        // 기존 버튼을 모두 제거
        calendarButtonsLayout.removeAllViews();

        GridLayout gridLayout = new GridLayout(this);
        gridLayout.setRowCount(10);
        gridLayout.setColumnCount(2);
        GridLayout.Spec rowSpec;
        GridLayout.Spec colSpec;
        GridLayout.LayoutParams params;

        for (int i = 0; i < userCalendars.size(); i++) {
            UserCalendar calendarInfo = userCalendars.get(i);

            LinearLayout calendarLayout = new LinearLayout(this);
            TextView calendarTextView = new TextView(this);
            calendarTextView.setText(calendarInfo.getCalendarName());

            // Drawable 파일 이름을 적절히 변경해야 함
            calendarTextView.setBackgroundResource(R.drawable.rounded_box);

            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, // 여기서 원하는 너비 설정
                    ViewGroup.LayoutParams.MATCH_PARENT  // 여기서 원하는 높이 설정
            );
            calendarLayout.setLayoutParams(layoutParams);

            // 행과 열을 설정
            rowSpec = GridLayout.spec(i / 2);  // 행
            colSpec = GridLayout.spec(i % 2);  // 열

            params = new GridLayout.LayoutParams(rowSpec, colSpec);

            // 외부 간격(margin) 설정
            int marginInPixels = 16; // 원하는 간격 크기 (픽셀)
            params.setMargins(marginInPixels, marginInPixels, marginInPixels, marginInPixels);

            // gravity를 사용하여 화면에 반반 나눠지도록 설정
            params.setGravity(Gravity.FILL_VERTICAL | Gravity.FILL_HORIZONTAL);

            // calendarButton가 아니라 calendarLayout을 사용하도록 수정
            calendarLayout.setLayoutParams(params);
            calendarLayout.setBackgroundResource(R.drawable.rounded_box_color);

            // 캘린더 삭제 기능 추가
            calendarLayout.setOnLongClickListener(view -> {
                showDeleteDialog(calendarInfo.getCalendarId(), calendarInfo.getCalendarName());
                return true;
            });

            calendarLayout.setOnClickListener(view -> {
                // 캘린더 버튼 클릭 시 커스텀 캘린더 클래스로 이동하고
                // 캘린더 아이디와 컬렉션명을 전달
                openCustomCalendar(calendarInfo.getCalendarId(), calendarInfo.getCalendarName());
            });

            // calendarButton 대신 calendarLayout을 추가
            calendarLayout.addView(calendarTextView);
            calendarButtonsLayout.addView(calendarLayout);
        }
    }

    private void showDeleteDialog(String calendarId, String calendarName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("캘린더 삭제");
        builder.setMessage(calendarName + " 캘린더를 삭제하시겠습니까?");

        builder.setPositiveButton("삭제", (dialog, which) -> {
            // 캘린더 삭제 함수 호출
            String userID = user.getUid();
            deleteCalendar(calendarId,userID);
        });

        builder.setNegativeButton("취소", (dialog, which) -> {
            // 사용자가 취소한 경우 아무 작업도 수행하지 않음
        });

        // 다이얼로그 표시
        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(dialogInterface -> {
            // 다이얼로그 표시 후 버튼 색상 변경
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.blue));
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.blue));
        });
        // 다이얼로그 표시
        alertDialog.show();
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