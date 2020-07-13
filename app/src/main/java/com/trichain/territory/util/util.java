package com.trichain.territory.util;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.model.LatLng;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.trichain.territory.R;

import java.util.List;

public class util {

    public static final String CHALLENGE_WAITING = "awaiting opponent";
    public static final String CHALLENGE_ACCEPTED = "accepted";
    public static final String CHALLENGE_COMPLETE = "complete";
    public static final String CHALLENGE_IN_PROGRESS = "in progress";
    public static final String CHALLENGE_REJECTED = "rejected";

    public static final String DB_CHALLENGES = "challenges";
    public static final String DB_PLAYERS = "players";
    public static final String DB_GAMES = "games";

    private static Activity activityRequestingImage = null;
    private static boolean mIsSquarePicture;
    public static final int REQUEST_IMAGE = 103;
    private static final String TAG = "util";


    public static void hideView(View... v) {
        for (int i = 0; i < v.length; i++)
            if (v[i].getVisibility() == View.VISIBLE) {
                int finalI = i;
                v[i].animate()
                        .alpha(0f)
                        .setDuration(350)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                v[finalI].setVisibility(View.GONE);
                            }
                        });
            }
    }

    public static void showView(View v) {
        if (v.getVisibility() == View.GONE || v.getVisibility() == View.INVISIBLE) {
            v.setAlpha(0f);
            v.setVisibility(View.VISIBLE);
            v.animate()
                    .alpha(1f)
                    .setDuration(350)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            v.setVisibility(View.VISIBLE);
                        }
                    });
        }
    }

    public static void loadImage(ImageView v, Object url, boolean isLandscape) {
        try {
            Glide.with(v)
                    .load(url)
                    .placeholder(isLandscape ? R.drawable.ic_placeholder_game : R.drawable.ic_placeholder_game)
                    .into(v);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ;
    }

    public static void pickImg(Activity activity, boolean isSquarePicture) {
        mIsSquarePicture = isSquarePicture;
        activityRequestingImage = activity;
        Dexter.withActivity(activityRequestingImage)
                .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            showImagePickerOptions();
                        }

                        if (report.isAnyPermissionPermanentlyDenied()) {
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    public static void showImagePickerOptions() {
        ImageCrop.showImagePickerOptions(activityRequestingImage, () -> launchGalleryIntent());
    }

    private static void launchGalleryIntent() {
        Intent intent = new Intent(activityRequestingImage, ImageCrop.class);
        intent.putExtra(ImageCrop.INTENT_IMAGE_PICKER_OPTION, ImageCrop.REQUEST_GALLERY_IMAGE);

        // setting aspect ratio
        intent.putExtra(ImageCrop.INTENT_LOCK_ASPECT_RATIO, true);
        intent.putExtra(ImageCrop.INTENT_ASPECT_RATIO_X, mIsSquarePicture ? 1 : 16); // 16x9, 1x1, 3:4, 3:2
        intent.putExtra(ImageCrop.INTENT_ASPECT_RATIO_Y, mIsSquarePicture ? 1 : 9);

        activityRequestingImage.startActivityForResult(intent, REQUEST_IMAGE);
    }

    /**
     * Showing Alert Dialog with Settings option
     * Navigates user to app settings
     * NOTE: Keep proper title and message depending on your app
     */
    private static void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activityRequestingImage);
        builder.setTitle("Grant Permissions");
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.");
        builder.setPositiveButton("GOTO SETTINGS", (dialog, which) -> {
            dialog.cancel();
            openSettings();
        });
        builder.setNegativeButton(activityRequestingImage.getString(android.R.string.cancel), (dialog, which) -> dialog.cancel());
        builder.show();

    }

    // navigating user to app settings
    private static void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activityRequestingImage.getPackageName(), null);
        intent.setData(uri);
        activityRequestingImage.startActivityForResult(intent, 101);
    }

    public static boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, 0);
            Log.e("PKG", "isPackageInstalled: Package is installed");
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("PKG", "isPackageInstalled: Package is NOT installed");
            return false;
        }
    }

    //https://play.google.com/store/apps/details?id=com.rockstargames.gtasa
    public static String extractPackageName(String playStoreURL) {
        String packageName = "";
        String[] splitted = playStoreURL.split("\\?");
        String lastHalf = splitted[1];
        Log.e(TAG, "extractPackageName: lastHalf " + lastHalf);
        if (lastHalf.startsWith("id=")) {
            packageName = lastHalf.substring(lastHalf.indexOf("=") + 1, lastHalf.contains("&") ? lastHalf.lastIndexOf("&") : lastHalf.length());
            System.out.println("Package: " + packageName);
        } else {
            Log.e(TAG, "extractPackageName: Not a valid URL");
        }
        return packageName;
    }

    public static double calculateDistance(LatLng myLatLng, LatLng rivalLatLng) {
        double lon1 = myLatLng.longitude;
        double lat1 = myLatLng.latitude;
        double lon2 = rivalLatLng.longitude;
        double lat2 = rivalLatLng.latitude;

        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private static double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    public static Bitmap getCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        // canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        //Bitmap _bmp = Bitmap.createScaledBitmap(output, 60, 60, false);
        //return _bmp;
        return output;
    }

}
