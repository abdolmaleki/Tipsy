package ir.candoo.tipsy.utils;

public class Constants {

    public static final String FIREBASE_URL = "https://tipsy-2a356.firebaseio.com";
    public static final String FIREBASE_LOCATION_USERS = "users";
    public static final String FIREBASE_URL_USERS = FIREBASE_URL + "/" + FIREBASE_LOCATION_USERS;
    public static final String FIREBASE_PROPERTY_TIMESTAMP = "timestamp";
    public static final String FIREBASE_WEB_CLIENT = "464077003640-80clndq49aoktjq8r997p0phnk3g7n45.apps.googleusercontent.com";

    public static class LoginMethod {
        public static final int LOGIN_FACEBOOK = 2100;
        public static final int LOGIN_GOOGLE = 2101;

    }

    public static class Key {
        public static final String LOGIN_METHOD = "login.method";

    }

}
