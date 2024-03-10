package com.example.android_firebase_todo_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class CreateTodoActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener {

    Button btnCrearTodo, btnVerLista;
    EditText txtTitle, txtContent;
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_todo);

        btnCrearTodo = (Button) findViewById(R.id.btnCrearTodo);
        btnVerLista = (Button) findViewById(R.id.btnVerLista);
        txtTitle = (EditText) findViewById(R.id.editTextTituloTodo);
        txtContent = (EditText) findViewById(R.id.editTextContenidoTodo);

        btnCrearTodo.setOnClickListener(this::createTodo);
        btnVerLista.setOnClickListener(this::listaTodos);

        String userId = getStringPref(CreateTodoActivity.this, "uid");


        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);


        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                User user = dataSnapshot.getValue(User.class);


                if (user != null) {
                    TextView userName = findViewById(R.id.txtName);
                    userName.setText("Welcome! "+user.name);
                    TextView userPhone = findViewById(R.id.txtNo);
                    userPhone.setText(user.phone);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

    }

    public void createTodo(View v) {

        if (TextUtils.isEmpty(txtTitle.getText().toString()) || TextUtils.isEmpty(txtContent.getText().toString())) {
            Snackbar.make(v, "You must enter text in the title and content.", Snackbar.LENGTH_LONG).show();
            return;
        }


        String user = FirebaseAuth.getInstance().getCurrentUser().getUid();

        TODO todoDocument = new TODO(txtTitle.getText().toString(), txtContent.getText().toString(), false, Timestamp.now(), Timestamp.now(), user);

        firestore.collection("todos").add(todoDocument).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Snackbar.make(v, "TO-DO created successfully!", Snackbar.LENGTH_LONG).show();
                Toast.makeText(this,"TO-DO created successfully!",Toast.LENGTH_SHORT).show();
                txtTitle.getText().clear();
                txtContent.getText().clear();
                txtTitle.requestFocus();
            } else {
                Snackbar.make(v, "There was a problem creating the TO-DO!", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startSignUpLoginActivity();
            return;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseAuth.getInstance().removeAuthStateListener(this);
    }

    public void startSignUpLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    public void listaTodos(View v) {


        Intent i = new Intent(this, VerTodosActivity.class);
        startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {

            savePref(CreateTodoActivity.this, "uid", "");
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            CreateTodoActivity.this.finish();

        }
        return true;
    }

    public String getStringPref(Context context, String key){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString(key, "");
    }

    public void savePref(Context context, String key, String value){
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(key, value);
        editor.apply();
    }
}