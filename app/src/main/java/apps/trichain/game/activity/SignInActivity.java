package apps.trichain.game.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import apps.trichain.game.R;
import apps.trichain.game.databinding.ActivitySigninBinding;
import apps.trichain.game.model.Player;
import apps.trichain.game.util.SharedPrefsManager;
import apps.trichain.game.util.util;

public class SignInActivity extends AppCompatActivity {

    private GoogleSignInClient mGoogleSignInClient;
    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 1313;
    private FirebaseAuth mAuth;
    private SharedPrefsManager sharedPrefsManager;
    private ActivitySigninBinding b;
    private DatabaseReference dbReference;
    private ValueEventListener userListener;
    private List<Player> playerList = new ArrayList<>();
    private boolean isPlayerExists = false;
    private Player player;
    private boolean hasSignedIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = DataBindingUtil.setContentView(this, R.layout.activity_signin);

        sharedPrefsManager = SharedPrefsManager.getInstance(this);

        if (sharedPrefsManager.checkLoggedIn()) {
            player = sharedPrefsManager.getSavedPlayer();
            startMainActivity();
            return;
        }

        dbReference = FirebaseDatabase.getInstance().getReference();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        sharedPrefsManager = SharedPrefsManager.getInstance(this);
        mAuth = FirebaseAuth.getInstance();

        b.btnSignin.setOnClickListener(v -> signIn());

    }

    private void signIn() {
        util.showView(b.signInLoadingView);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {

            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                // ...
            }
        }
    }


    private void updateUI(FirebaseUser currentUser) {
        if (currentUser != null) {
            b.tvSignInMessage.setText("Signing in..");
            //Find player
            userListener = dbReference.child("players").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                        try {
                            Log.i(TAG, "validatePlayer: onDataChange: Adding to list: " + messageSnapshot.getValue(Player.class).getId());
                            playerList.add(messageSnapshot.getValue(Player.class));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (!hasSignedIn)
                        validatePlayer(currentUser);
                    else
                        try {
                            dbReference.removeEventListener(userListener);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "onCancelled");
                    Toast.makeText(SignInActivity.this, "An error has occurred. Please try again", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show();
        }
    }

    private void validatePlayer(FirebaseUser currentUser) {
        b.tvSignInMessage.setText("Validating credentials...");
        for (Player p : playerList) {
            Log.i(TAG, "validatePlayer: compare: " + p.getId() + " with: " + currentUser.getUid());
            try {
                if (p.getId().equals(currentUser.getUid())) {
                    Log.i(TAG, "validatePlayer: Found player! Proceed to login");
                    player = p;
                    isPlayerExists = true;
                    break;
                } else {
                    Log.i(TAG, "validatePlayer: Not this player! Move on to next...");
                    isPlayerExists = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "validatePlayer: Player is invalid");
            }
        }

        if (!isPlayerExists) {
            b.tvSignInMessage.setText("Creating account...");
            player = new Player(
                    currentUser.getUid(),
                    currentUser.getDisplayName(),
                    0,
                    String.valueOf(currentUser.getPhotoUrl()),
                    0.0,
                    0.0,
                    0.0f,
                    true,
                    0
            );
            saveToFireBase(player);
            isPlayerExists = true;
        } else {
            b.tvSignInMessage.setText("Signing in...");
        }

        sharedPrefsManager.savePlayerData(player.jsonify());
        util.hideView(b.signInLoadingView);
        startMainActivity();
    }

    private void startMainActivity() {
        hasSignedIn = true;
        try {
            dbReference.removeEventListener(userListener);
            dbReference = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        sharedPrefsManager.isLoggedIn(true);
        Toast.makeText(this, "Logged in as " + player.getPlayerName(), Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void saveToFireBase(Player player) {
        dbReference.child("players").child(player.getId()).setValue(player);
    }


    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        b.tvSignInMessage.setText("Authenticating...");
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            util.hideView(b.signInLoadingView);
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            util.hideView(b.signInLoadingView);
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());

                            updateUI(null);
                        }
                        // ...
                    }
                });
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            dbReference.removeEventListener(userListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
