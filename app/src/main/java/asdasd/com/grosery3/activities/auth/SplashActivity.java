package asdasd.com.grosery3.activities.auth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import asdasd.com.grosery3.R;
import asdasd.com.grosery3.activities.MainSellerActivity;
import asdasd.com.grosery3.activities.MainUserActivity;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //make fullscreen
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);

        firebaseAuth = FirebaseAuth.getInstance();

        // start login activity after 2 sec
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user==null){
                    startActivity(new Intent(SplashActivity.this, LoginActivityy.class));
                    finish();
                }
                else {
                    //user is logger in, check user type
                    checkUserType();
                }
            }
        }, 1000);
    }

    private void checkUserType() {
        //if user is seller, start seller main screen
        //if user is buyer, start user main screen
        Log.i("my_tag",firebaseAuth.getUid());

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String accountType = ""+dataSnapshot.child("accountType").getValue();
                        if (accountType.equals("Seller")){
                            //user is seller
                            startActivity(new Intent(SplashActivity.this, MainSellerActivity.class));
                            finish();
                        }
                        else {
                            //user is buyer
                            startActivity(new Intent(SplashActivity.this, MainUserActivity.class));
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

}
