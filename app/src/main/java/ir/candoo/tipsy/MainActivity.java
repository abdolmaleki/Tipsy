package ir.candoo.tipsy;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.ServerValue;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import ir.candoo.tipsy.model.User;
import ir.candoo.tipsy.utils.Constants;
import ir.candoo.tipsy.utils.SharedPrefManager;
import ir.candoo.tipsy.utils.Utils;

public class MainActivity extends BaseActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    //////////////////////////////////////////////////////////////////////
    /////////// Google Login Fields
    //////////////////////////////////////////////////////////////////////

    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "MainActivity =====>";
    private GoogleApiClient mGoogleApiClient;
    private SharedPrefManager mSharedPrefManager;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private String mGoogleIdToken;
    private final Context mContext = this;
    private String name, email;
    private String photo;

    //////////////////////////////////////////////////////////////////////
    /////////// Facebook Login Fields
    //////////////////////////////////////////////////////////////////////

    private LoginButton mFacebookLoginButton;
    private CallbackManager mFacebookCallbackManager;
    private String mFacebookIdToken;
    private String mFacebookUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initializeFirebase();
        initializeFacebookSignIn();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        mFacebookCallbackManager.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                try {
                    GoogleSignInAccount account = result.getSignInAccount();
                    mGoogleIdToken = account.getIdToken();
                    name = account.getDisplayName();
                    email = account.getEmail();
                    Uri photoUri = account.getPhotoUrl();
                    photo = photoUri.toString();
                    mSharedPrefManager = new SharedPrefManager(mContext);
                    mSharedPrefManager.saveIsLoggedIn(mContext, true);
                    mSharedPrefManager.saveEmail(mContext, email);
                    mSharedPrefManager.saveName(mContext, name);
                    mSharedPrefManager.savePhoto(mContext, photo);
                    mSharedPrefManager.saveToken(mContext, mGoogleIdToken);
                    mSharedPrefManager.saveIsLoggedIn(mContext, true);
                    AuthCredential credential = GoogleAuthProvider.getCredential(mGoogleIdToken, null);
                    firebaseAuthWithGoogle(credential);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            } else {
                Toast.makeText(this, "Login Unsuccessful", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    @Override
    public void onClick(View view) {

        Utils utils = new Utils(this);
        int id = view.getId();

        if (id == R.id.btn_login_with_google) {
            if (utils.isNetworkAvailable()) {
                signIn();
            } else {
                Toast.makeText(MainActivity.this, "Oops! no internet connection!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initView() {
        SignInButton signInButton = findViewById(R.id.btn_login_with_google);
        signInButton.setSize(SignInButton.SIZE_WIDE);
        signInButton.setOnClickListener(this);
        mFacebookLoginButton = findViewById(R.id.btn_login_with_facebook);
    }

    private void goToHomePage(int loginMethod) {
        Intent intent = new Intent(MainActivity.this, NavDrawerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(Constants.Key.LOGIN_METHOD, loginMethod);
        startActivity(intent);
        finish();
    }

    //
    //   ____                   _        ____  _               ___
    //  / ___| ___   ___   __ _| | ___  / ___|(_) __ _ _ __   |_ _|_ __
    // | |  _ / _ \ / _ \ / _` | |/ _ \ \___ \| |/ _` | '_ \   | || '_ \
    // | |_| | (_) | (_) | (_| | |  __/  ___) | | (_| | | | |  | || | | |
    //  \____|\___/ \___/ \__, |_|\___| |____/|_|\__, |_| |_| |___|_| |_|
    //                    |___/                  |___/

    private void initializeFirebase() {
        configureSignIn();
        try {
            mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
            mAuthListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        createUserInFirebaseHelper();
                        Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    } else {
                        Log.d(TAG, "onAuthStateChanged:signed_out");
                    }
                }
            };
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }

    private void createUserInFirebaseHelper() {

        final String encodedEmail = Utils.encodeEmail(email.toLowerCase());
        final Firebase userLocation = new Firebase(Constants.FIREBASE_URL_USERS).child(encodedEmail);
        userLocation.addListenerForSingleValueEvent(new com.firebase.client.ValueEventListener() {
            @Override
            public void onDataChange(com.firebase.client.DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    HashMap<String, Object> timestampJoined = new HashMap<>();
                    timestampJoined.put(Constants.FIREBASE_PROPERTY_TIMESTAMP, ServerValue.TIMESTAMP);
                    User newUser = new User(name, photo, encodedEmail, timestampJoined);
                    userLocation.setValue(newUser);
                    Toast.makeText(MainActivity.this, "Account created!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, NavDrawerActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.d(TAG, getString(R.string.log_error_occurred) + firebaseError.getMessage());
                if (firebaseError.getCode() == FirebaseError.EMAIL_TAKEN) {
                } else {
                }
            }
        });
    }

    public void configureSignIn() {
        try {
            GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(Constants.FIREBASE_WEB_CLIENT)
                    .requestEmail()
                    .build();

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, options)
                    .build();

            mGoogleApiClient.connect();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }

    private void signIn() {
        try {
            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
            startActivityForResult(signInIntent, RC_SIGN_IN);
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());

        }
    }

    private void firebaseAuthWithGoogle(AuthCredential credential) {
        showProgressDialog();
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "signInWithCredential" + task.getException().getMessage());
                            task.getException().printStackTrace();
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            createUserInFirebaseHelper();
                            Toast.makeText(MainActivity.this, "Login successful",
                                    Toast.LENGTH_SHORT).show();
                            goToHomePage(Constants.LoginMethod.LOGIN_GOOGLE);
                        }
                        hideProgressDialog();
                    }
                });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    //
    //   _____              _                 _       ____  _                ___
    //  |  ___|_ _  ___ ___| |__   ___   ___ | | __  / ___|(_) __ _ _ __    |_ _|_ __
    //  | |_ / _` |/ __/ _ \ '_ \ / _ \ / _ \| |/ /  \___ \| |/ _` | '_ \    | || '_ \
    //  |  _| (_| | (_|  __/ |_) | (_) | (_) |   <    ___) | | (_| | | | |   | || | | |
    //  |_|  \__,_|\___\___|_.__/ \___/ \___/|_|\_\  |____/|_|\__, |_| |_|  |___|_| |_|
    //                                                        |___/

    private void initializeFacebookSignIn() {
        mFacebookCallbackManager = CallbackManager.Factory.create();

        mFacebookLoginButton.registerCallback(mFacebookCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                mFacebookIdToken = loginResult.getAccessToken().getToken();
                mFacebookUserId = loginResult.getAccessToken().getUserId();

                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {

                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        getFacebookData(object);
                        goToHomePage(Constants.LoginMethod.LOGIN_FACEBOOK);

                    }
                });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id, first_name, last_name, email,gender, birthday, location"); // Parámetros que pedimos a facebook
                request.setParameters(parameters);
                request.executeAsync();

            }

            @Override
            public void onCancel() {
                Toast.makeText(MainActivity.this, "Login attempt canceled.",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException e) {
                Toast.makeText(MainActivity.this, "Login attempt failed.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bundle getFacebookData(JSONObject object) {

        try {

            mSharedPrefManager = new SharedPrefManager(mContext);
            mSharedPrefManager.saveIsLoggedIn(mContext, true);
            mSharedPrefManager.saveToken(mContext, mGoogleIdToken);
            Bundle bundle = new Bundle();
            String id = object.getString("id");

            Uri uri = Uri.parse("https://graph.facebook.com/" + id + "/picture?width=200&height=150");
            mSharedPrefManager.savePhoto(mContext, uri.toString());


            bundle.putString("idFacebook", id);
            if (object.has("first_name"))
                mSharedPrefManager.saveName(mContext, object.getString("first_name"));
            if (object.has("last_name"))
                bundle.putString("last_name", object.getString("last_name"));
            if (object.has("email"))
                mSharedPrefManager.saveEmail(mContext, object.getString("email"));
            return bundle;
        } catch (JSONException e) {
            Log.d(TAG, "Error parsing JSON");
        }
        return null;
    }


}
