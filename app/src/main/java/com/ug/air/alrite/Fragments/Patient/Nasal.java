package com.ug.air.alrite.Fragments.Patient;

import static com.ug.air.alrite.Fragments.Patient.Assess.DATE;
import static com.ug.air.alrite.Fragments.Patient.Assess.S4;
import static com.ug.air.alrite.Fragments.Patient.Assess.UUIDS;
import static com.ug.air.alrite.Fragments.Patient.Sex.AGE;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.ug.air.alrite.Activities.Dashboard;
import com.ug.air.alrite.Activities.DiagnosisActivity;
import com.ug.air.alrite.Adapters.AssessmentAdapter;
import com.ug.air.alrite.Models.Assessment;
import com.ug.air.alrite.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class Nasal extends Fragment {

    View view;
    Button back, next, btnSave, btnGrunting, btnNasal, btnContinue;
    RadioGroup radioGroup;
    RadioButton radioButton1, radioButton2;
    String value7 = "none";
    TextView txtDisease, txtDefinition, txtOk,txtDiagnosis;
    LinearLayout linearLayoutDisease, linearLayout_instruction;
    VideoView videoView;
    MediaPlayer mediaPlayer;
    Dialog dialog, dialog1;
    RecyclerView recyclerView;
    ArrayList<Assessment> assessments;
    AssessmentAdapter assessmentAdapter;
    CardView inst;
    List<Integer> messages;
    String diagnosis;
    private static final int YES = 0;
    private static final int NO = 1;
    public static final String CHOICEGN = "choiceGn";
    public static final String GNDIAGNOSIS = "gnDiagnosis";
    public static final String SHARED_PREFS = "sharedPrefs";
    SharedPreferences sharedPreferences, sharedPreferences1;
    SharedPreferences.Editor editor, editor1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_nasal, container, false);

        next = view.findViewById(R.id.next);
        back = view.findViewById(R.id.back);
        radioGroup = view.findViewById(R.id.radioGroup);
        radioButton1 = view.findViewById(R.id.yes);
        radioButton2 = view.findViewById(R.id.no);
        btnGrunting = view.findViewById(R.id.grunting);
        btnNasal = view.findViewById(R.id.nasal);

        sharedPreferences = this.requireActivity().getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        loadData();
        updateViews();

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                View radioButton = radioGroup.findViewById(checkedId);
                int index = radioGroup.indexOfChild(radioButton);

                switch (index) {
                    case YES:
                        value7 = "Yes";
                        break;
                    case NO:
                        value7 = "No";
                        break;
                    default:
                        break;
                }
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (value7.isEmpty()){
                    Toast.makeText(getActivity(), "Please select at least one of the options", Toast.LENGTH_SHORT).show();
                }else {
                    saveData();
                }
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction fr = requireActivity().getSupportFragmentManager().beginTransaction();
                fr.replace(R.id.fragment_container, new Wheezing());
                fr.commit();
            }
        });

        btnNasal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });

        btnGrunting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });

        return view;
    }

    private void saveData() {
        editor.putString(CHOICEGN, value7);
        editor.apply();

        checkIfNone();
    }

    private void loadData() {
        value7 = sharedPreferences.getString(CHOICEGN, "");
    }

    private void updateViews() {
        if (value7.equals("Yes")){
            radioButton1.setChecked(true);
        }else if (value7.equals("No")){
            radioButton2.setChecked(true);
        }else {
            radioButton1.setChecked(false);
            radioButton2.setChecked(false);
        }

    }

    private void checkIfNone() {
        if (value7.equals("No")){
            editor.remove(GNDIAGNOSIS);
            editor.apply();
            FragmentTransaction fr = requireActivity().getSupportFragmentManager().beginTransaction();
            fr.replace(R.id.fragment_container, new ChestIndrawing());
            fr.addToBackStack(null);
            fr.commit();
        }else{
            displayDialog();
        }
    }

    private void displayDialog() {
        dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.assessment_layout);
        dialog.setCancelable(true);
        Window window = dialog.getWindow();
//        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

        linearLayout_instruction = dialog.findViewById(R.id.diagnosis);
        txtDiagnosis = dialog.findViewById(R.id.txtDiagnosis);
        recyclerView = dialog.findViewById(R.id.recyclerView1);
        btnSave = dialog.findViewById(R.id.btnSave);
        btnContinue = dialog.findViewById(R.id.btnContinue);

        linearLayout_instruction.setBackgroundColor(getResources().getColor(R.color.severeDiagnosisColor));
        txtDiagnosis.setText(R.string.severe);
        diagnosis = txtDiagnosis.getText().toString();
        diagnosis = diagnosis.replace("Diagnosis: ", "");

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));

        assessments = new ArrayList<>();
        assessmentAdapter = new AssessmentAdapter(assessments);

        String age = sharedPreferences.getString(AGE, "");
        String s = sharedPreferences.getString(S4, "");
        float ag = Float.parseFloat(age);
        if (ag >= 0.2 && ag < 0.4){
            if (s.contains("Convulsions")){
                messages = Arrays.asList(R.string.first_dose, R.string.ampicilin2, R.string.gentamicin2,
                        R.string.convulsions, R.string.diazepam2, R.string.convulsions1,
                        R.string.convulsions2, R.string.convulsions3, R.string.convulsions4,
                        R.string.convulsions5, R.string.other1, R.string.other2, R.string.other3,
                        R.string.other4, R.string.other5, R.string.other6, R.string.other7,
                        R.string.other8, R.string.refer_urgently);
            }else{
                messages = Arrays.asList(R.string.first_dose, R.string.ampicilin2, R.string.gentamicin2,
                        R.string.other1, R.string.other2, R.string.other3,
                        R.string.other4, R.string.other5, R.string.other6, R.string.other7,
                        R.string.other8, R.string.refer_urgently);
            }
        }else if (ag >= 0.4 && ag < 1.0){
            if (s.contains("Convulsions")){
                messages = Arrays.asList(R.string.first_dose, R.string.ampicilin4, R.string.gentamicin4,
                        R.string.convulsions, R.string.diazepam4, R.string.convulsions1,
                        R.string.convulsions2, R.string.convulsions3, R.string.convulsions4,
                        R.string.convulsions5, R.string.other1, R.string.other2, R.string.other3,
                        R.string.other4, R.string.other5, R.string.other6, R.string.other7,
                        R.string.other8, R.string.refer_urgently);
            }else{
                messages = Arrays.asList(R.string.first_dose, R.string.ampicilin4, R.string.gentamicin4,
                        R.string.other1, R.string.other2, R.string.other3,
                        R.string.other4, R.string.other5, R.string.other6, R.string.other7,
                        R.string.other8, R.string.refer_urgently);
            }
        }else if (ag >= 1.0 && ag < 3.0){
            if (s.contains("Convulsions")){
                messages = Arrays.asList(R.string.first_dose, R.string.ampicilin12, R.string.gentamicin12,
                        R.string.convulsions, R.string.diazepam12, R.string.convulsions1,
                        R.string.convulsions2, R.string.convulsions3, R.string.convulsions4,
                        R.string.convulsions5, R.string.other1, R.string.other2, R.string.other3,
                        R.string.other4, R.string.other5, R.string.other6, R.string.other7,
                        R.string.other8, R.string.refer_urgently);
            }else{
                messages = Arrays.asList(R.string.first_dose, R.string.ampicilin12, R.string.gentamicin12,
                        R.string.other1, R.string.other2, R.string.other3,
                        R.string.other4, R.string.other5, R.string.other6, R.string.other7,
                        R.string.other8, R.string.refer_urgently);
            }

        }else if (ag >= 3.0){
            if (s.contains("Convulsions")){
                messages = Arrays.asList(R.string.first_dose, R.string.ampicilin3, R.string.gentamicin3,
                        R.string.convulsions, R.string.diazepam3, R.string.convulsions1,
                        R.string.convulsions2, R.string.convulsions3, R.string.convulsions4,
                        R.string.convulsions5, R.string.other1, R.string.other2, R.string.other3,
                        R.string.other4, R.string.other5, R.string.other6, R.string.other7,
                        R.string.other8, R.string.refer_urgently);
            }else{
                messages = Arrays.asList(R.string.first_dose, R.string.ampicilin3, R.string.gentamicin3,
                        R.string.other1, R.string.other2, R.string.other3,
                        R.string.other4, R.string.other5, R.string.other6, R.string.other7,
                        R.string.other8, R.string.refer_urgently);
            }

        }

        for (int i = 0; i < messages.size(); i++){
            Assessment assessment = new Assessment(messages.get(i));
            assessments.add(assessment);
        }
        recyclerView.setAdapter(assessmentAdapter);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putString(GNDIAGNOSIS, diagnosis);
                editor.apply();
                dialog.dismiss();
                startActivity(new Intent(getActivity(), DiagnosisActivity.class));
            }
        });
        btnSave.setVisibility(View.VISIBLE);

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putString(GNDIAGNOSIS, diagnosis);
                editor.apply();
                dialog.dismiss();
                FragmentTransaction fr = requireActivity().getSupportFragmentManager().beginTransaction();
                fr.replace(R.id.fragment_container, new ChestIndrawing());
                fr.addToBackStack(null);
                fr.commit();
            }
        });

        dialog.getWindow().setLayout(700, 1300);
        dialog.getWindow().setGravity(Gravity.CENTER);
        dialog.show();
    }

    private void showDialog() {
    }
}