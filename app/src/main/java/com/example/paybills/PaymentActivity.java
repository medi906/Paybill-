package com.example.paybills;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaymentActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ImageButton btnBack;
    private ImageView ivPaymentIcon;
    private TextView tvPaymentTitle;
    private RecyclerView rvPackages;
    private TextInputEditText etCustomAmount;
    private MaterialButton btnPayCustom;
    private PackageAdapter adapter;
    private List<BillPackage> packageList;
    private String categoryName;
    private int categoryIcon;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        packageList = new ArrayList<>();

        btnBack = findViewById(R.id.btnBack);
        ivPaymentIcon = findViewById(R.id.ivPaymentIcon);
        tvPaymentTitle = findViewById(R.id.tvPaymentTitle);
        rvPackages = findViewById(R.id.rvPackages);
        etCustomAmount = findViewById(R.id.etCustomAmount);
        btnPayCustom = findViewById(R.id.btnPayCustom);

        categoryName = getIntent().getStringExtra("categoryName");
        categoryIcon = getIntent().getIntExtra("categoryIcon", R.drawable.ic_electricity);

        ivPaymentIcon.setImageResource(categoryIcon);
        tvPaymentTitle.setText(categoryName);

        rvPackages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PackageAdapter();
        rvPackages.setAdapter(adapter);

        loadPackages();

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnPayCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amountStr = etCustomAmount.getText().toString().trim();
                if (TextUtils.isEmpty(amountStr)) {
                    etCustomAmount.setError("Enter amount");
                    return;
                }
                try {
                    int amount = Integer.parseInt(amountStr);
                    if (amount < 500) {
                        etCustomAmount.setError("Minimum UGX 500");
                        return;
                    }
                    showPaymentMethodDialog(amount);
                } catch (NumberFormatException e) {
                    etCustomAmount.setError("Invalid amount");
                }
            }
        });
    }

    private void loadPackages() {
        packageList.clear();
        switch (categoryName) {
            case "Pay TV (DStv/GOtv)":
                packageList.add(new BillPackage("DStv Access", "50+ channels", 35000));
                packageList.add(new BillPackage("DStv Family", "80+ channels", 65000));
                packageList.add(new BillPackage("DStv Compact", "120+ channels", 105000));
                packageList.add(new BillPackage("DStv Compact Plus", "140+ channels", 165000));
                packageList.add(new BillPackage("DStv Premium", "175+ channels", 275000));
                packageList.add(new BillPackage("GOtv Lite", "25+ channels", 16500));
                packageList.add(new BillPackage("GOtv Max", "45+ channels", 32000));
                packageList.add(new BillPackage("StarTimes Basic", "30+ channels", 25000));
                break;
            case "Electricity (Umeme)":
                packageList.add(new BillPackage("Yaka 20 Units", "~20 kWh", 20000));
                packageList.add(new BillPackage("Yaka 50 Units", "~50 kWh", 50000));
                packageList.add(new BillPackage("Yaka 100 Units", "~100 kWh", 100000));
                packageList.add(new BillPackage("Yaka 200 Units", "~200 kWh", 200000));
                break;
            case "Water (NWSC)":
                packageList.add(new BillPackage("Basic (5,000L)", "Small household", 15000));
                packageList.add(new BillPackage("Standard (10,000L)", "Medium household", 30000));
                packageList.add(new BillPackage("Premium (20,000L)", "Large household", 60000));
                break;
            case "Internet":
                packageList.add(new BillPackage("MTN 5GB", "30 days", 25000));
                packageList.add(new BillPackage("MTN 10GB", "30 days", 50000));
                packageList.add(new BillPackage("Airtel 10GB", "30 days", 45000));
                packageList.add(new BillPackage("Airtel 15GB", "30 days", 65000));
                packageList.add(new BillPackage("Roke Telkom Unlimited", "30 days", 150000));
                break;
            case "Airtime/MoMo":
                packageList.add(new BillPackage("MTN Airtime", "Instant load", 5000));
                packageList.add(new BillPackage("MTN Airtime", "Instant load", 10000));
                packageList.add(new BillPackage("MTN Airtime", "Instant load", 20000));
                packageList.add(new BillPackage("Airtel Airtime", "Instant load", 5000));
                packageList.add(new BillPackage("Airtel Airtime", "Instant load", 10000));
                packageList.add(new BillPackage("Airtel Airtime", "Instant load", 20000));
                break;
            case "Rent":
                packageList.add(new BillPackage("Monthly Rent", "Standard", 300000));
                packageList.add(new BillPackage("Monthly Rent", "Premium", 500000));
                break;
            case "School Fees":
                packageList.add(new BillPackage("Day School", "Per term", 300000));
                packageList.add(new BillPackage("Boarding School", "Per term", 800000));
                packageList.add(new BillPackage("University", "Per semester", 1500000));
                break;
            case "Solar":
                packageList.add(new BillPackage("Fenix ReadyPay", "Monthly", 50000));
                packageList.add(new BillPackage("M-KOPA Solar", "Monthly", 45000));
                packageList.add(new BillPackage("d.light", "Monthly", 40000));
                break;
            case "Waste (KCCA)":
                packageList.add(new BillPackage("Residential", "Monthly", 10000));
                packageList.add(new BillPackage("Commercial", "Monthly", 30000));
                break;
            default:
                packageList.add(new BillPackage("Standard", "Basic payment", 0));
                break;
        }
        adapter.notifyDataSetChanged();
    }

    private void showPaymentMethodDialog(final int amount) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_payment_method, null);
        builder.setView(view);

        final RadioGroup rgMethod = view.findViewById(R.id.rgPaymentMethod);

        builder.setTitle("Select Payment Method")
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int selectedId = rgMethod.getCheckedRadioButtonId();
                        String method;
                        if (selectedId == R.id.rbMtn) method = "MTN Mobile Money";
                        else if (selectedId == R.id.rbAirtel) method = "Airtel Money";
                        else method = "Cash";
                        saveTransaction(amount, method);
                    }
                })
                .setNegativeButton("Cancel", null);

        builder.create().show();
    }

    private void saveTransaction(int amount, String paymentMethod) {
        String userId = mAuth.getCurrentUser().getUid();
        String transactionId = db.collection("transactions").document().getId();

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("transactionId", transactionId);
        transaction.put("userId", userId);
        transaction.put("category", categoryName);
        transaction.put("amount", amount);
        transaction.put("paymentMethod", paymentMethod);
        transaction.put("timestamp", System.currentTimeMillis());
        transaction.put("status", "completed");

        db.collection("transactions").document(transactionId).set(transaction)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(PaymentActivity.this,
                                    "Paid UGX " + String.format("%,d", amount) + " via " + paymentMethod,
                                    Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(PaymentActivity.this, "Failed. Try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    class BillPackage {
        String name, description;
        int price;
        BillPackage(String name, String description, int price) {
            this.name = name;
            this.description = description;
            this.price = price;
        }
    }

    class PackageAdapter extends RecyclerView.Adapter<PackageAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_package, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BillPackage pkg = packageList.get(position);
            holder.tvName.setText(pkg.name);
            holder.tvDesc.setText(pkg.description);
            holder.tvPrice.setText("UGX " + String.format("%,d", pkg.price));
            final int price = pkg.price;
            holder.btnSelect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPaymentMethodDialog(price);
                }
            });
        }

        @Override
        public int getItemCount() {
            return packageList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDesc, tvPrice;
            View btnSelect;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvPackageName);
                tvDesc = itemView.findViewById(R.id.tvPackageDesc);
                tvPrice = itemView.findViewById(R.id.tvPackagePrice);
                btnSelect = itemView.findViewById(R.id.btnSelect);
            }
        }
    }
}