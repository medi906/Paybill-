package com.example.paybills;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.util.HashMap;
import java.util.Map;
import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private CircleImageView ivProfilePic;
    private ImageButton btnBack;
    private TextInputEditText etFullName, etEmail;
    private MaterialButton btnChangePhoto, btnSaveProfile;
    private SwitchCompat switchNotifications, switchDarkMode;
    private Uri imageUri;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        ivProfilePic = findViewById(R.id.ivProfilePic);
        btnBack = findViewById(R.id.btnBack);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        switchNotifications = findViewById(R.id.switchNotifications);
        switchDarkMode = findViewById(R.id.switchDarkMode);

        loadUserData();

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnChangePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePicker();
            }
        });

        btnSaveProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfile();
            }
        });

        switchNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SettingsActivity.this,
                        "Notifications " + (switchNotifications.isChecked() ? "ON" : "OFF"),
                        Toast.LENGTH_SHORT).show();
            }
        });

        switchDarkMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchDarkMode.isChecked()) {
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                    Toast.makeText(SettingsActivity.this, "Dark Mode ON", Toast.LENGTH_SHORT).show();
                } else {
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                    Toast.makeText(SettingsActivity.this, "Dark Mode OFF", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadUserData() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            String name = task.getResult().getString("fullName");
                            String email = task.getResult().getString("email");
                            String profilePic = task.getResult().getString("profilePic");

                            etFullName.setText(name);
                            etEmail.setText(email);

                            if (profilePic != null && !profilePic.isEmpty()) {
                                Glide.with(SettingsActivity.this)
                                        .load(profilePic)
                                        .placeholder(R.drawable.ic_person_white)
                                        .into(ivProfilePic);
                            }
                        }
                    }
                });
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            ivProfilePic.setImageURI(imageUri);
            uploadProfilePic();
        }
    }

    private void uploadProfilePic() {
        if (imageUri == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        StorageReference ref = storage.getReference().child("profile_pics/" + userId + ".jpg");

        ref.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String downloadUrl = uri.toString();
                                db.collection("users").document(userId)
                                        .update("profilePic", downloadUrl);
                                Toast.makeText(SettingsActivity.this,
                                        "Profile photo updated!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
    }

    private void saveProfile() {
        String fullName = etFullName.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Name is required");
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);

        db.collection("users").document(userId).update(updates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(SettingsActivity.this,
                                    "Profile updated!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SettingsActivity.this,
                                    "Failed to update", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}