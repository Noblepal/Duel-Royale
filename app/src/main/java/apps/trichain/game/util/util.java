package apps.trichain.game.util;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.List;

import apps.trichain.game.R;

public class util {

    private static Activity activityRequestingImage = null;
    private static boolean mIsSquarePicture;
    public static final int REQUEST_IMAGE = 103;

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
        Glide.with(v)
                .load(url)
                .placeholder(isLandscape ? R.drawable.ic_placeholder_game : R.drawable.ic_placeholder_game)
                .into(v);
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
}
