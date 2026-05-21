package com.example.paybills;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class PaymentActivity extends AppCompatActivity {

    private EditText etAmount, etAccountNumber, etMeterNumber;
    private Spinner spinnerPaymentMethod;
    private Button btnMakePayment;
    private TextView tvCategoryName, tvEmailDisplay;
    private String categoryName, userEmail;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private LinearLayout llElectricityFields;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get data from intent
        categoryName = getIntent().getStringExtra("categoryName");
        userEmail = getIntent().getStringExtra("userEmail");

        // Initialize views
        tvCategoryName = findViewById(R.id.tvCategoryName);
        tvEmailDisplay = findViewById(R.id.tvEmailDisplay);
        etAmount = findViewById(R.id.etAmount);
        etAccountNumber = findViewById(R.id.etAccountNumber);
        etMeterNumber = findViewById(R.id.etMeterNumber);
        spinnerPaymentMethod = findViewById(R.id.spinnerPaymentMethod);
        btnMakePayment = findViewById(R.id.btnMakePayment);
        llElectricityFields = findViewById(R.id.llElectricityFields);

        // Set category name
        tvCategoryName.setText(categoryName);

        // Display user email
        if (userEmail != null && !userEmail.isEmpty()) {
            tvEmailDisplay.setText("Paying as: " + userEmail);
        } else if (mAuth.getCurrentUser() != null) {
            tvEmailDisplay.setText("Paying as: " + mAuth.getCurrentUser().getEmail());
        }

        // Setup payment method spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.payment_methods, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPaymentMethod.setAdapter(adapter);

        // Show/hide specific fields based on category
        setupCategoryFields();

        // Make payment button click
        btnMakePayment.setOnClickListener(v -> makePayment());
    }

    private void setupCategoryFields() {
        // Hide electricity fields by default
        if (llElectricityFields != null) {
            llElectricityFields.setVisibility(View.GONE);
        }

        // Show electricity fields only for Electricity category
        if (categoryName != null && categoryName.contains("Electricity")) {
            if (llElectricityFields != null) {
                llElectricityFields.setVisibility(View.VISIBLE);
            }
        }
    }

    private void makePayment() {
        String amountStr = etAmount.getText().toString().trim();
        String accountNumber = etAccountNumber.getText().toString().trim();
        String paymentMethod = spinnerPaymentMethod.getSelectedItem().toString();

        // Validate amount
        if (amountStr.isEmpty()) {
            etAmount.setError("Please enter amount");
            etAmount.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                etAmount.setError("Please enter valid amount");
                etAmount.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etAmount.setError("Invalid amount");
            etAmount.requestFocus();
            return;
        }

        // Validate account number for electricity
        if (categoryName != null && categoryName.contains("Electricity")) {
            if (accountNumber.isEmpty()) {
                etAccountNumber.setError("Please enter account number");
                etAccountNumber.requestFocus();
                return;
            }
        }

        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Processing payment...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Save transaction to Firestore
        String userId = mAuth.getCurrentUser().getUid();
        String transactionId = db.collection("transactions").document().getId();

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("transactionId", transactionId);
        transaction.put("userId", userId);
        transaction.put("userEmail", userEmail != null ? userEmail : mAuth.getCurrentUser().getEmail());
        transaction.put("category", categoryName);
        transaction.put("amount", amount);
        transaction.put("accountNumber", accountNumber);
        transaction.put("paymentMethod", paymentMethod);
        transaction.put("timestamp", System.currentTimeMillis());
        transaction.put("status", "SUCCESS");

        db.collection("transactions").document(transactionId)
                .set(transaction)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();

                    // Show success message with email
                    String receiverEmail = userEmail != null ? userEmail : mAuth.getCurrentUser().getEmail();
                    String successMessage = "✅ Payment Successful!\n\n" +
                            "Amount: UGX " + String.format("%,.0f", amount) + "\n" +
                            "Category: " + categoryName + "\n" +
                            "Method: " + paymentMethod + "\n" +
                            "Receipt sent to: " + receiverEmail + "\n\n" +
                            "Thank you for using PayBills!";

                    new androidx.appcompat.app.AlertDialog.Builder(PaymentActivity.this)
                            .setTitle("Payment Success")
                            .setMessage(successMessage)
                            .setPositiveButton("OK", (dialog, which) -> {
                                // Navigate back to dashboard
                                Intent intent = new Intent(PaymentActivity.this, DashboardActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .setCancelable(false)
                            .show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(PaymentActivity.this, "Payment failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}