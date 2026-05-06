package com.example.paybills;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.*;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView tvUserName, tvTotalPaid, tvTransactionCount;
    private ImageButton btnLogout, btnSettings;
    private RecyclerView rvRecentTransactions;
    private RecentTransactionsAdapter adapter;
    private List<String> recentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        recentList = new ArrayList<>();

        tvUserName = findViewById(R.id.tvUserName);
        tvTotalPaid = findViewById(R.id.tvTotalPaid);
        tvTransactionCount = findViewById(R.id.tvTransactionCount);
        btnLogout = findViewById(R.id.btnLogout);
        btnSettings = findViewById(R.id.btnSettings);
        rvRecentTransactions = findViewById(R.id.rvRecentTransactions);

        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecentTransactionsAdapter(recentList);
        rvRecentTransactions.setAdapter(adapter);

        loadUserData();
        loadDashboardStats();
        setupBillCategories();
        loadRecentTransactions();

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserData() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        String name = task.getResult().getString("fullName");
                        tvUserName.setText(name != null ? name : "User");
                    } else {
                        tvUserName.setText("User");
                    }
                });
    }

    private void loadDashboardStats() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    int total = 0;
                    int count = 0;
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Long amount = doc.getLong("amount");
                            if (amount != null) {
                                total += amount;
                                count++;
                            }
                        }
                    }
                    tvTotalPaid.setText("UGX " + String.format("%,d", total));
                    tvTransactionCount.setText(String.valueOf(count));
                });
    }

    private void setupBillCategories() {
        setupCategory(R.id.categoryElectricity, R.drawable.ic_electricity, "Electricity (Umeme)");
        setupCategory(R.id.categoryWater, R.drawable.ic_water, "Water (NWSC)");
        setupCategory(R.id.categoryPayTV, R.drawable.ic_tv, "Pay TV (DStv/GOtv)");
        setupCategory(R.id.categoryInternet, R.drawable.ic_internet, "Internet");
        setupCategory(R.id.categoryWaste, R.drawable.ic_rubbish, "Waste (KCCA)");
        setupCategory(R.id.categoryAirtime, R.drawable.ic_mobile, "Airtime/MoMo");
        setupCategory(R.id.categoryRent, R.drawable.ic_rent, "Rent");
        setupCategory(R.id.categorySchoolFees, R.drawable.ic_school, "School Fees");
        setupCategory(R.id.categorySolar, R.drawable.ic_solar, "Solar");
    }

    private void setupCategory(int viewId, int iconRes, String categoryName) {
        View view = findViewById(viewId);
        if (view == null) return;

        ImageView icon = view.findViewById(R.id.ivCategoryIcon);
        TextView name = view.findViewById(R.id.tvCategoryName);

        if (icon != null) icon.setImageResource(iconRes);
        if (name != null) name.setText(categoryName);

        view.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, PaymentActivity.class);
            intent.putExtra("categoryName", categoryName);
            intent.putExtra("categoryIcon", iconRes);
            startActivity(intent);
        });
    }

    private void loadRecentTransactions() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnCompleteListener(task -> {
                    recentList.clear();
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String category = doc.getString("category");
                            Long amount = doc.getLong("amount");
                            String paymentMethod = doc.getString("paymentMethod");
                            Long timestamp = doc.getLong("timestamp");
                            if (category != null && amount != null) {
                                String display = category + "|UGX " + String.format("%,d", amount) + "|" +
                                        (paymentMethod != null ? paymentMethod : "Mobile Money") + "|" +
                                        (timestamp != null ? getFormattedDate(timestamp) : "");
                                recentList.add(display);
                            }
                        }
                    }
                    if (recentList.isEmpty()) {
                        recentList.add("No transactions yet|||");
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private String getFormattedDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardStats();
        loadRecentTransactions();
    }

    private class RecentTransactionsAdapter extends RecyclerView.Adapter<RecentTransactionsAdapter.ViewHolder> {
        private List<String> items;
        private int[] colors = {0xFF1A237E, 0xFF283593, 0xFF303F9F, 0xFF3949AB, 0xFF3F51B5};
        private int[] lightColors = {0xFFE8EAF6, 0xFFE3F2FD, 0xFFE0F2F1, 0xFFE8F5E9, 0xFFF3E5F5};

        RecentTransactionsAdapter(List<String> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String item = items.get(position);
            String[] parts = item.split("\\|");

            if (parts.length >= 2 && !item.equals("No transactions yet|||")) {
                holder.tvCategory.setText(parts[0]);
                holder.tvAmount.setText(parts[1]);
                if (parts.length >= 3) holder.tvMethod.setText(parts[2]);
                if (parts.length >= 4) holder.tvDate.setText(parts[3]);

                int colorIndex = position % colors.length;
                holder.ivCategoryIcon.setBackgroundColor(colors[colorIndex]);
                holder.cardView.setCardBackgroundColor(lightColors[colorIndex]);

                String category = holder.tvCategory.getText().toString();
                if (!category.isEmpty()) {
                    holder.tvIconLetter.setText(category.substring(0, 1).toUpperCase());
                }
            } else {
                holder.tvCategory.setText(item);
                holder.tvAmount.setVisibility(View.GONE);
                holder.tvMethod.setVisibility(View.GONE);
                holder.tvDate.setVisibility(View.GONE);
                holder.ivCategoryIcon.setVisibility(View.GONE);
                holder.tvIconLetter.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCategory, tvAmount, tvMethod, tvDate, tvIconLetter;
            ImageView ivCategoryIcon;
            CardView cardView;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvCategory = itemView.findViewById(R.id.tvCategory);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                tvMethod = itemView.findViewById(R.id.tvMethod);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvIconLetter = itemView.findViewById(R.id.tvIconLetter);
                ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
                cardView = itemView.findViewById(R.id.cardTransaction);
            }
        }
    }
}