package com.example.android_firebase_todo_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.firebase.ui.firestore.ObservableSnapshotArray;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class VerTodosActivity extends AppCompatActivity implements ToDoRecyclerAdapter.ToDoListener, FirebaseAuth.AuthStateListener {

    ToDoRecyclerAdapter toDoRecyclerAdapter;
    RecyclerView recyclerView;
    FirestoreRecyclerOptions<TODO> options;
    Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_todo);
        spinner = findViewById(R.id.spinner);
        recyclerView = findViewById(R.id.listaTodo);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.spinner_items,
                android.R.layout.simple_spinner_item
        );

        findViewById(R.id.todoback).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);


        spinner.setAdapter(adapter);


        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

                String selectedItem = parentView.getItemAtPosition(position).toString();
                Toast.makeText(VerTodosActivity.this, "Selected: " + selectedItem, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {

            }
        });

        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        initRecycleTodoView(FirebaseAuth.getInstance().getCurrentUser());
        EditText searchEditText = findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String searchTerm = editable.toString().trim();
                initRecycleTodoView(FirebaseAuth.getInstance().getCurrentUser(), searchTerm);
                if (searchTerm.isEmpty()) {

                    initRecycleTodoView(FirebaseAuth.getInstance().getCurrentUser());
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(this);
        toDoRecyclerAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseAuth.getInstance().removeAuthStateListener(this);
        toDoRecyclerAdapter.stopListening();
    }

    private void initRecycleTodoView(FirebaseUser user) {
        Query query = FirebaseFirestore.getInstance().collection("todos")
                .whereEqualTo("user", user.getUid())
                .orderBy("completed", Query.Direction.ASCENDING)
                .orderBy("created", Query.Direction.DESCENDING);

        options = new FirestoreRecyclerOptions.Builder<TODO>().setQuery(query, TODO.class).build();
        toDoRecyclerAdapter = new ToDoRecyclerAdapter(options, this);

        recyclerView.setAdapter(toDoRecyclerAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void initRecycleTodoView(FirebaseUser user, String searchTerm) {
        Query query = FirebaseFirestore.getInstance().collection("todos")
                .whereEqualTo("user", user.getUid())
                .orderBy("completed", Query.Direction.ASCENDING)
                .orderBy("created", Query.Direction.DESCENDING);

        if (searchTerm != null && !searchTerm.isEmpty()) {
            if (spinner.getSelectedItem().equals("status")) {
                query = query.whereEqualTo("title", searchTerm);
            } else {
                query = query.whereEqualTo("completed", true);
            }
        }

        options = new FirestoreRecyclerOptions.Builder<TODO>().setQuery(query, TODO.class).build();
        toDoRecyclerAdapter.updateOptions(options);
    }

    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }
        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            ToDoRecyclerAdapter.ToDoViewHolder toDoViewHolder = (ToDoRecyclerAdapter.ToDoViewHolder) viewHolder;

            if (direction == ItemTouchHelper.LEFT) {
                toDoViewHolder.deleteToDo();
            }
            if (direction == ItemTouchHelper.RIGHT) {
                toDoViewHolder.editTodo();
            }
        }
        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    .addSwipeRightBackgroundColor(ContextCompat.getColor(VerTodosActivity.this, R.color.amberEdit))
                    .addSwipeRightActionIcon(R.drawable.ic_baseline_edit_24)
                    .addSwipeLeftBackgroundColor(ContextCompat.getColor(VerTodosActivity.this, R.color.redDelete))
                    .addSwipeLeftActionIcon(R.drawable.ic_baseline_delete_24)
                    .create()
                    .decorate();

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    };

    @Override
    public void handleCompleted(boolean completed, DocumentSnapshot snapshot) {
        snapshot.getReference().update("completed", completed).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Snackbar.make(recyclerView, "To Do updated!", Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(recyclerView, "Problem updating the To-Do!", Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void handleEdit(DocumentSnapshot snapshot) {

        TODO todo = snapshot.toObject(TODO.class);

        EditText titleEdit;
        EditText contentEdit;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.edit_todo_dialog, null);
        builder.setView(dialogView);

        titleEdit = dialogView.findViewById(R.id.editTodoTitleDialog);
        titleEdit.setText(todo.getTitle());
        titleEdit.setSelection(todo.getTitle().length());

        contentEdit = dialogView.findViewById(R.id.editTodoContentDialog);
        contentEdit.setText(todo.getContent());
        contentEdit.setSelection(todo.getContent().length());

        builder.setPositiveButton("Save", (dialog, which) -> {

            todo.setTitle(titleEdit.getText().toString());
            todo.setContent(contentEdit.getText().toString());
            todo.setUpdated(Timestamp.now());
            snapshot.getReference().set(todo).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Snackbar.make(recyclerView,  "To-Do successfully edited!", Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(recyclerView, "There was a problem editing the TO-DO!", Snackbar.LENGTH_LONG).show();
                }
            });
        }).setNegativeButton( "Cancel" , null);

        builder.show();
    }

    @Override
    public void handleDelete(DocumentSnapshot snapshot) {

        DocumentReference documentReference = snapshot.getReference();
        TODO todo = snapshot.toObject(TODO.class);
        snapshot.getReference().delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Snackbar.make(recyclerView, "To-Do successfully deleted!", Snackbar.LENGTH_LONG).setAction( "Undo", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        documentReference.set(todo);
                    }
                }).show();
            } else {
                Snackbar.make(recyclerView, "There was a problem deleting the TO-DO!" , Snackbar.LENGTH_LONG).show();
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

    public void startSignUpLoginActivity() {
        Intent intent = new Intent(this, SignUpLoginActivity.class);
        startActivity(intent);
        finish();
    }
}