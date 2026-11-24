package com.cindyvalencia.latihanfirebase;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    EditText edKode, edNama, edSatuan, edHarga;
    Button btnSimpan;
    RecyclerView recyclerView;
    List<Item> itemList = new ArrayList<>();
    ItemAdapter adapter;

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private Button btnLogout;
    private ValueEventListener eventListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        FirebaseApp.initializeApp(this);

        String databaseUrl = "https://latihan01-ed5b8-default-rtdb.asia-southeast1.firebasedatabase.app/";
        FirebaseDatabase database = FirebaseDatabase.getInstance(databaseUrl);
        String userId = currentUser.getUid();
        dbRef = database.getReference("items").child(userId);

        edKode = findViewById(R.id.edKode);
        edNama = findViewById(R.id.edNama);
        edSatuan = findViewById(R.id.edSatuan);
        edHarga = findViewById(R.id.edHarga);
        btnSimpan = findViewById(R.id.btnSimpan);
        recyclerView = findViewById(R.id.recyclerView);
        btnLogout = findViewById(R.id.btnLogout);

        adapter = new ItemAdapter(itemList, dbRef);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnSimpan.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();

            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String kode = edKode.getText().toString();
                    String nama = edNama.getText().toString();
                    String satuan = edSatuan.getText().toString();
                    int harga = Integer.parseInt(edHarga.getText().toString());

                    Item item = new Item(kode, nama, satuan, harga);

                    dbRef.child(kode).setValue(item)
                            .addOnSuccessListener(aVoid ->{
                                Toast.makeText(MainActivity.this, "Item berhasil ditambahkan", Toast.LENGTH_SHORT).show();
                                edKode.setText("");
                                edNama.setText("");
                                edSatuan.setText("");
                                edHarga.setText("");
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(MainActivity.this, "Gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                } else {
                    Toast.makeText(this, "Akun sudah terhapus, silakan login kembali", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build();
                    GoogleSignIn.getClient(MainActivity.this, gso).signOut().addOnCompleteListener(signOutTask -> {
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    });
                }
            });
        });

        eventListener = dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                itemList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Item item = data.getValue(Item.class);
                    itemList.add(item);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                if (mAuth.getCurrentUser() != null) {
                    Toast.makeText(MainActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnLogout.setOnClickListener(v -> {
            if (dbRef != null && eventListener != null) {
                dbRef.removeEventListener(eventListener);
            }
            mAuth.signOut();
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                            .requestEmail().build();
            GoogleSignIn.getClient(MainActivity.this, gso).signOut();
            Toast.makeText(this, "Berhasil Logout!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }
}