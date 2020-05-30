package apps.trichain.game.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

import apps.trichain.game.R;
import apps.trichain.game.databinding.ActivityAddGameBinding;
import apps.trichain.game.model.Game;
import apps.trichain.game.util.util;

import static apps.trichain.game.util.util.REQUEST_IMAGE;

public class AddGameActivity extends AppCompatActivity {

    private ActivityAddGameBinding b;
    private Uri filePath;

    // instance for firebase storage and StorageReference
    FirebaseStorage storage;
    StorageReference storageReference;
    DatabaseReference dbReference;

    private String DATABASE_PATH = "games";
    private String STORAGE_PATH = "images/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = DataBindingUtil.setContentView(this, R.layout.activity_add_game);

        // get the Firebase  storage reference
        storageReference = FirebaseStorage.getInstance().getReference();
        dbReference = FirebaseDatabase.getInstance().getReference(DATABASE_PATH);

        b.btnChoose.setOnClickListener(v -> pickImage());

        b.btnUpload.setOnClickListener(v -> {
            String gameName = b.edtGameName.getText().toString();
            String gameUrl = b.edtGameUrl.getText().toString();

            if (TextUtils.isEmpty(gameName) || TextUtils.isEmpty(gameUrl)) {
                Toast.makeText(this, "Game name and URL are required", Toast.LENGTH_SHORT).show();
            } else {
                uploadImage(filePath, gameName, gameUrl);
            }
        });

    }

    // Select Image method
    private void pickImage() {
        util.pickImg(this, false);
    }

    // Override onActivityResult method
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    filePath = data.getParcelableExtra("path");

                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                    b.imgView.setImageBitmap(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // UploadImage method
    private void uploadImage(Uri filePath, String gameName, String gameUrl) {
        util.showView(b.llProgress);
        if (filePath != null) {
            File file = new File(String.valueOf(filePath));
            String IMAGES = STORAGE_PATH + System.currentTimeMillis() + "." + getFileExtension(file);
            StorageReference ref = storageReference.child(IMAGES);

            ref.putFile(filePath).addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                b.pbSavingGame.setProgress((int) Math.ceil(progress));
            }).continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                // Continue with the task to get the download URL
                return ref.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Uri downloadUrl = task.getResult();
                    util.hideView(b.llProgress);
                    Toast.makeText(AddGameActivity.this, "Image Uploaded!", Toast.LENGTH_SHORT).show();

                    Game gameInfo = new Game(gameName, downloadUrl.toString(), 0, gameUrl);

                    // Getting image upload ID.
                    String ImageUploadId = dbReference.push().getKey();

                    dbReference.child(ImageUploadId).setValue(gameInfo);
                } else {
                    util.hideView(b.llProgress);
                    // Error, Image not uploaded
                    Toast.makeText(AddGameActivity.this, "Unsuccessful", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                util.hideView(b.llProgress);
                // Error, Image not uploaded
                Toast.makeText(AddGameActivity.this, "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });

            /*
            ref.putFile(filePath).addOnSuccessListener(taskSnapshot -> {
                util.hideView(b.llProgress);
                Toast.makeText(AddGameActivity.this, "Image Uploaded!", Toast.LENGTH_SHORT).show();

                Task<Uri> downloadUrl = taskSnapshot.getMetadata().getReference().getDownloadUrl();
                Game gameInfo = new Game(gameName, downloadUrl.toString(), 0, gameUrl);

                // Getting image upload ID.
                String ImageUploadId = dbReference.push().getKey();

                dbReference.child(ImageUploadId).setValue(gameInfo);
            }).addOnFailureListener(e -> {

            }).addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                b.pbSavingGame.setProgress((int) Math.ceil(progress));
            });*/
        } else {
            Toast.makeText(this, "Image is null", Toast.LENGTH_SHORT).show();
        }
    }

    public String getFileExtension(File file) {
        Log.e("FILE", "getFileExtension: ext " + MimeTypeMap.getFileExtensionFromUrl(file.toString()));
        return MimeTypeMap.getFileExtensionFromUrl(file.toString());
    }
}
