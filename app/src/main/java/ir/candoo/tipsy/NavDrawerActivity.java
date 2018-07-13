package ir.candoo.tipsy;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.support.design.widget.NavigationView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;
import ir.candoo.tipsy.utils.Constants;
import ir.candoo.tipsy.utils.SharedPrefManager;

public class NavDrawerActivity extends BaseActivity implements
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "NavDrawerActivity ===>";
    NavDrawerActivity mContext = this;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private TextView mFullNameTextView, mEmailTextView;
    private CircleImageView mProfileImageView;
    private String mUsername, mEmail;
    private int mLoginMethod;

    SharedPrefManager sharedPrefManager;
    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mAuth;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav_drawer);
        if (!isAuthorized()) {
            goToLoginPage();
        }
        loadData();
        initFireBase();
        initView();
        configureSignIn();
    }

    private boolean isAuthorized() {
        boolean isAuthorize = false;
        if (AccessToken.getCurrentAccessToken() != null) {
            isAuthorize = true;
        }


        return isAuthorize;
    }

    private void loadData() {
        mLoginMethod = getIntent().getExtras().getInt(Constants.Key.LOGIN_METHOD);
    }

    private void initView() {
        initNavigationDrawer();
        View header = mNavigationView.getHeaderView(0);

        mFullNameTextView = header.findViewById(R.id.fullName);
        mEmailTextView = header.findViewById(R.id.email);
        mProfileImageView = header.findViewById(R.id.profileImage);

        sharedPrefManager = new SharedPrefManager(mContext);
        mUsername = sharedPrefManager.getName();
        mEmail = sharedPrefManager.getUserEmail();
        String uri = sharedPrefManager.getPhoto();
        Uri mPhotoUri = Uri.parse(uri);
        mFullNameTextView.setText(mUsername);
        mEmailTextView.setText(mEmail);

        Picasso.with(mContext)
                .load(mPhotoUri)
                .placeholder(android.R.drawable.sym_def_app_icon)
                .error(android.R.drawable.sym_def_app_icon)
                .into(mProfileImageView);
    }

    private void initFireBase() {
        mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public void initNavigationDrawer() {

        mNavigationView = findViewById(R.id.navigation_view);
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {

                int id = item.getItemId();

                switch (id) {
                    case R.id.freebie:
                        Toast.makeText(getApplicationContext(), "Home", Toast.LENGTH_SHORT).show();
                        mDrawerLayout.closeDrawers();
                        break;
                    case R.id.payment:
                        Toast.makeText(getApplicationContext(), "Settings", Toast.LENGTH_SHORT).show();
                        mDrawerLayout.closeDrawers();
                        break;
                    case R.id.trip:
                        Toast.makeText(getApplicationContext(), "Trash", Toast.LENGTH_SHORT).show();
                        mDrawerLayout.closeDrawers();
                        break;
                    case R.id.logout:
                        signOut();
                        mDrawerLayout.closeDrawers();
                        break;
                    case R.id.tips:
                        Toast.makeText(getApplicationContext(), "Trash", Toast.LENGTH_SHORT).show();
                        mDrawerLayout.closeDrawers();
                        break;
                }
                return false;
            }
        });

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        android.support.v7.app.ActionBarDrawerToggle actionBarDrawerToggle = new android.support.v7.app.ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };
        mDrawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    public void configureSignIn() {
        try {
            GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestIdToken(Constants.FIREBASE_WEB_CLIENT)
                    .build();

            mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                    .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, options)
                    .build();

            mGoogleApiClient.connect();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }

    private void signOut() {
        new SharedPrefManager(mContext).clear();
        switch (mLoginMethod) {
            case Constants.LoginMethod.LOGIN_FACEBOOK:
                LoginManager.getInstance().logOut();
                goToLoginPage();
                break;
            case Constants.LoginMethod.LOGIN_GOOGLE:
                mAuth.signOut();
                Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                goToLoginPage();
                            }
                        }
                );
                break;
        }
    }
    private void goToLoginPage() {
        Intent intent = new Intent(NavDrawerActivity.this, MainActivity.class);
        startActivity(intent);
    }
}
