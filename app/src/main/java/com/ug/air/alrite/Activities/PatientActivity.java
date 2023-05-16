package com.ug.air.alrite.Activities;

import static com.ug.air.alrite.Activities.DiagnosisActivity.DATE_2;
import static com.ug.air.alrite.Activities.DiagnosisActivity.DURATION_2;
import static com.ug.air.alrite.Fragments.Patient.Bronchodilator.DATE;
import static com.ug.air.alrite.Fragments.Patient.Bronchodilator.DURATION;
import static com.ug.air.alrite.Fragments.Patient.Bronchodilator.FILENAME;
import static com.ug.air.alrite.Fragments.Patient.Bronchodilator.USERNAME;
import static com.ug.air.alrite.Fragments.Patient.Bronchodilator.UUIDS;
import static com.ug.air.alrite.Fragments.Patient.Initials.CHILD_INITIALS;
import static com.ug.air.alrite.Fragments.Patient.Initials.INITIAL_DATE;
import static com.ug.air.alrite.Fragments.Patient.Initials.PARENT_INITIALS;
import static com.ug.air.alrite.Fragments.Patient.RRCounter.FASTBREATHING2;
import static com.ug.air.alrite.Fragments.Patient.RRCounter.INITIAL_DATE_2;
import static com.ug.air.alrite.Fragments.Patient.RRCounter.SECOND;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.makeramen.roundedimageview.BuildConfig;
import com.ug.air.alrite.APIs.ApiClient;
import com.ug.air.alrite.APIs.DecisionTreeJSON;
import com.ug.air.alrite.Fragments.Patient.ActivePatients;
import com.ug.air.alrite.Fragments.Patient.Initials;
import com.ug.air.alrite.Fragments.Patient.InitialsModified;
import com.ug.air.alrite.Fragments.Patient.MultipleChoiceFragment;
import com.ug.air.alrite.Fragments.Patient.MultipleSelectionFragment;
import com.ug.air.alrite.Fragments.Patient.OtherPatients;
import com.ug.air.alrite.Fragments.Patient.ParagraphFragment;
import com.ug.air.alrite.Fragments.Patient.TextInputFragment;
import com.ug.air.alrite.Fragments.Patient.TextInputTextFragment;
import com.ug.air.alrite.R;
import com.ug.air.alrite.Utils.Credentials;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.Response;

public class PatientActivity extends AppCompatActivity implements
        MultipleChoiceFragment.onGetResultListener, MultipleSelectionFragment.onGetResultListener,
        TextInputFragment.onGetResultListener, TextInputTextFragment.onGetResultListener,
        ParagraphFragment.onGetResultListener {

    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String INCOMPLETE = "incomplete";

    public static final String ASSESS_INCOMPLETE = "incomplete";
    SharedPreferences sharedPreferences, sharedPreferences1;
    SharedPreferences.Editor editor, editor1;
    int frag = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient);

        sharedPreferences = getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        Intent intent = getIntent();
        if (intent.hasExtra("Fragment")){
            frag = intent.getExtras().getInt("Fragment");
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            if (frag == 1){
//                fragmentTransaction.add(R.id.fragment_container, new Initials());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    fragmentTransaction.add(R.id.fragment_container, new InitialsModified());
                } else {
                    System.out.println("Cant read from file because it's too old of an SDK build version");
                }
            }else if (frag == 2){
                fragmentTransaction.add(R.id.fragment_container, new ActivePatients());
            }else {
                fragmentTransaction.add(R.id.fragment_container, new OtherPatients());
            }
            fragmentTransaction.commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (frag == 2 || frag == 3){
            startActivity(new Intent(PatientActivity.this, Dashboard.class));
            finish();
        }
        else {
            String pin = sharedPreferences.getString(PARENT_INITIALS, "");
            String cin = sharedPreferences.getString(CHILD_INITIALS, "");

            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.exit);
            dialog.setCancelable(true);

            Button btnYes = dialog.findViewById(R.id.yes);
            Button btnNo = dialog.findViewById(R.id.no);

            btnNo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });

            btnYes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (pin.isEmpty() && cin.isEmpty()){
                        dialog.dismiss();
                        startActivity(new Intent(PatientActivity.this, Dashboard.class));
                        finish();
                    }else{
                        Date currentTime = Calendar.getInstance().getTime();
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault());
                        String formattedDate = df.format(currentTime);

                        String file = sharedPreferences.getString(FILENAME, "");

                        Credentials credentials = new Credentials();
                        String username = credentials.creds(PatientActivity.this).getUsername();


                        if (file.isEmpty()){
                            getDuration(currentTime);

                            String uniqueID = UUID.randomUUID().toString();

                            editor.putString(DATE, formattedDate);
                            editor.putString(USERNAME, username);
                            editor.putString(UUIDS, uniqueID);
                            editor.putString(INCOMPLETE, "incomplete");
                            editor.apply();

                            String filename = formattedDate + "_" + uniqueID;
                            doLogic(filename);
                            dialog.dismiss();
                        }
                        else {
                            String fast = sharedPreferences.getString(FASTBREATHING2, "");
                            if (fast.isEmpty()){
                                dialog.dismiss();
                                startActivity(new Intent(PatientActivity.this, Dashboard.class));
                                finish();
                            }else{

                                editor.putString(DATE_2, formattedDate);
                                editor.putString(INCOMPLETE, "incomplete");
                                editor.apply();

                                getDuration2(currentTime);
                                doLogic(file);
                            }

                        }

                    }
                }
            });

            dialog.show();
        }

    }

    private void getDuration(Date currentTime) {
        String initial_date = sharedPreferences.getString(INITIAL_DATE, "");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault());
        try {
            Date d1 = format.parse(initial_date);

            long diff = currentTime.getTime() - d1.getTime();//as given

            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            String duration = String.valueOf(minutes);
            editor.putString(DURATION, duration);
            editor.apply();
            Log.d("Difference in time", "getTimeDifference: " + minutes);

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void getDuration2(Date currentTime) {
        String initial_date = sharedPreferences.getString(INITIAL_DATE_2, "");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault());
        try {
            Date d1 = format.parse(initial_date);

            long diff = currentTime.getTime() - d1.getTime();//as given

            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            String duration = String.valueOf(minutes);
            editor.putString(DURATION_2, duration);
            editor.apply();
            Log.d("Difference in time", "getTimeDifference: " + minutes);

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void doLogic(String file) {
        sharedPreferences1 = getSharedPreferences(file, Context.MODE_PRIVATE);
        editor1 = sharedPreferences1.edit();
        Map<String, ?> all = sharedPreferences.getAll();
        for (Map.Entry<String, ?> x : all.entrySet()) {
            if (x.getValue().getClass().equals(String.class))  editor1.putString(x.getKey(),  (String)x.getValue());
//            else if (x.getValue().getClass().equals(Boolean.class)) editor1.putBoolean(x.getKey(), (Boolean)x.getValue());
        }
        editor1.commit();

        String filename = sharedPreferences1.getString(SECOND, "");
        if (!filename.isEmpty()){
            filename = filename + ".xml";
            File src = new File("/data/data/" + BuildConfig.APPLICATION_ID + "/shared_prefs/" + filename);
            if (src.exists()){
                src.delete();
            }
        }

        editor.clear();
        editor.commit();

        Intent intent;
        intent = new Intent(PatientActivity.this, Dashboard.class);
        startActivity(intent);
    }


    /**---------------------------------------------------------------------------------------------
     *
     * Decision Tree creation and maintenance functions below vv
     *
     *--------------------------------------------------------------------------------------------*/

    // Strings for the JSON identifiers
    public static final String COMPONENT = "component";
    public static final String MULTISELECT = "multiselect";
    public static final String PAGE_ID = "pageID";
    public static final String PAGES = "pages";
    public static final String LABEL = "label";
    public static final String CHOICES = "choices";
    public static final String TEXT = "text";
    public static final String VALUE = "value";
    public static final String LINK = "link";
    public static final String CONTENT = "content";
    public static final String MULTIPLE_CHOICE = "MultipleChoice";
    public static final String TEXT_INPUT = "TextInput";
    public static final String DIAGNOSIS_PAGE = "Diagnosis Page";
    public static final String TITLE = "title";
    public static final String IS_DIAGNOSIS_PAGE = "isDiagnosisPage";
    public static final String NULL = "null";
    public static final String DEFAULT_LINK = "defaultLink";
    public static final String TARGET_VALUE_ID = "targetValueID";
    public static final String VALUE_ID = "valueID";
    public static final String SATISFIED_LINK = "satisfiedLink";
    public static final String NOT_SATISFIED_LINK = "notSatisfiedLink";
    public static final String TYPE = "type";
    public static final String THRESHOLD = "threshold";
    public static final String NUMERIC_TYPE = "numeric";
    public static final String ALPHANUMERIC_TYPE = "alphanumeric";
    public static final String TEXT_TYPE = "text";
    public static final String ANY_TYPE = "any";
    public static final String PARAGRAPH = "Paragraph";

     // Full JSON infos to call getNextPage
     JSONArray pages;

     // Information gotten from JSON for the current page
     // Information gotten from JSON
     String question;
     ArrayList<JSONObject> choices;
     ArrayList<String> backstack;
     // Other than question there is more information needed
     // from the JSON for text input
     String InputHint; //The preview text shown in the input bubble (enter the value here)  *optional (prefer this not to be optional)
    String InputInformation; //Extra stuff below the input bubble telling user more information (its in celcius) *optional
    String SkipInformation; //Text shown above the skip button telling user when to skip (no thermometer available) *optional
    int MinValue; //The minimum value allowed to be inputted
    int MaxValue; //The maximum value allowed to be inputted
    int diagnosisCutoff; //The minimum value to be inputted in order to get the diagnosis
    JSONObject pageID;
    String targetValue_id; // for text input
    String targetValueID; // for multiple selection
    String paragraph; //Content for paragraph fragment

    /**
     *
     * @throws JSONException
     * @throws IOException
     */
    private void implementEditableDecisionTree(JSONObject json) throws JSONException, IOException {
//        DecisionTreeJSON json = ApiClient.getClient(ApiClient.TEMP_SERV_URL)
//                                    .create(DecisionTreeJSON.class);
//        Call<String> call = DecisionTreeJSON.getJson();
//        System.out.println(json);

        backstack = new ArrayList<>();
        pages = json.getJSONArray(PAGES);
        // This is just an array because it needs to be mutable: think of it as
        // its own element
        String nextPage = pages.getJSONObject(0).getString(PAGE_ID);

        // Handling the intent

        // Continue looping through each page in sequence: depending on the component,
        // inflate a new fragment for that component type
        // If the next page is the final page, exit the loop
        getNextPage(nextPage);
    }

    /**
     * NOTE: THIS SHOULD ONLY BE USED FOR SINGLE-LAYER ARRAYS
     *
     * @param jsonArray
     * @return
     * @throws JSONException
     */
    private ArrayList<JSONObject> JSONArrayToListOfJSONObjects(JSONArray jsonArray) throws JSONException {
        ArrayList<JSONObject> ret = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            ret.add(jsonArray.getJSONObject(i));
        }
        return ret;
    }

    /**
     * This will add the given fragment to the back stack, and do a transaction
     * which replaces the current fragment with the given fragment
     *
     * @param fragment the fragment to swap to
     */
    private void completeFragmentTransaction(Fragment fragment) {
        // Begin the transaction
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        // Replace the contents of the container with the new fragment
        ft.replace(R.id.fragment_container, fragment);
        // Complete the changes added above
        ft.addToBackStack(null);
        ft.commit();
    }

    /**
     * Given a page from the json object, we create the fragment for that page and
     * populate it with the necessary information.
     *
     * @param nextPageID the ID of the next page to be displayed in the app
     * @throws JSONException if the json is mad at you for calling it improperly
     */
    private void getNextPage(String nextPageID) throws JSONException {
        // Add the new page to the backstack, if it's not already there somewhere
        if (!backstack.contains(nextPageID)) {
            backstack.add(nextPageID);
        }

        // Get all of the items that should be displayed on the page
        JSONObject nextPageJSON = pages.getJSONObject(findIndexInPagesGivenPageId(nextPageID));
        JSONArray nextPageContent = nextPageJSON.getJSONArray(CONTENT);

        // If the next page would be the final page, then the assessment is over!
        // So, start up the diagnosis activity
        if ((Boolean) nextPageJSON.get(IS_DIAGNOSIS_PAGE)) {
            exitActivity();
            return;
        }
        pageID = nextPageJSON;

        // Then, for each component that we're given within the page, display a
        // fragment for that component.
        JSONObject nextPageComponent = nextPageContent.getJSONObject(0);
        String nextPageComponentType = nextPageComponent.getString(COMPONENT);

        // Multiple choice option(s):
        if (nextPageComponentType.equals(MULTIPLE_CHOICE)) {
            Boolean isMultiSelect = nextPageComponent.getBoolean(MULTISELECT);
            if (isMultiSelect) {
                createMultiSelectFragment(nextPageComponent);
            } else {
                createMultipleChoiceFragment(nextPageComponent);
            }

        // Text input option:
        } else if (nextPageComponentType.equals(TEXT_INPUT)) {
            if (nextPageComponent.getString(TYPE).equals(NUMERIC_TYPE)) {
                createTextInputFragment(nextPageComponent);
            } else {
                createTextInputTextFragment(nextPageComponent);
            }

        // Paragraph option:
        } else if (nextPageComponentType.equals(PARAGRAPH)) {
            createParagraphFragment(nextPageComponent);

        // There was an issue with identifying the page...
        } else {
            System.out.println("should never get here lol: just go to the next page");
            getNextPage(pageID.getString(DEFAULT_LINK));
        }
    }

    /**
     * Essentially, make an asynchronous API call to get the HTTP
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void getJSONFromBackend() {
        File assessment = new File(getFilesDir(), "assessment.json");

        try {
            BufferedReader br = Files.newBufferedReader(assessment.toPath());
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            String json = sb.toString();

            br.close();

            implementEditableDecisionTree(new JSONObject(json));
        } catch (Exception e) {
            System.out.println("did a bad: " + e);
        }
    }

    /**
     * Start the Diagnosis activity that directly follows from this one, and exit
     * the activity.
     */
    private void exitActivity() {
        startActivity(new Intent(PatientActivity.this, DiagnosisActivityModified.class));
        this.finish();
    }

    private void createMultipleChoiceFragment(JSONObject nextPageComponent) throws JSONException {
        // Collect the important arguments from the component
        question = nextPageComponent.getString(LABEL);
        choices = JSONArrayToListOfJSONObjects(nextPageComponent.getJSONArray(CHOICES));

        // Get the new page's fragment, and set a listener for when the next button
        // is clicked
        MultipleChoiceFragment mc_fragment = MultipleChoiceFragment.newInstance(question, choices);

        // Replace and commit the fragment
        completeFragmentTransaction(mc_fragment);
    }
    /**
     * Creating the Fragment for Text Input
     *
     * @throws JSONException because we use json objects
     */
    private void createTextInputFragment(JSONObject page) throws JSONException {
        // Collect the important arguments from the component
        question = page.getString(LABEL);
        InputHint = "none right now";
        InputInformation = "none right now";
        SkipInformation = "none right now";
        MinValue = 0;
        MaxValue = 100;
        diagnosisCutoff = 50;
        targetValue_id = page.getString(VALUE_ID);

        // Get the new page's fragment, and set a listener for when the next button
        // is clicked
        TextInputFragment ti_fragment = TextInputFragment.newInstance(question, InputHint, InputInformation, SkipInformation,
        MinValue, MaxValue, diagnosisCutoff);

        // Replace and commit the fragment
        completeFragmentTransaction(ti_fragment);
    }

    /**
     * Create the TextInputTextFragment
     *
     * @param nextPageComponent the component to be displayed on screen
     * @throws JSONException because json
     */
    private void createTextInputTextFragment(JSONObject nextPageComponent) throws JSONException {
        question = nextPageComponent.getString(LABEL);
        InputHint = "none right now";
        InputInformation = "none right now";
        SkipInformation = "none right now";
        targetValue_id = nextPageComponent.getString(VALUE_ID);

        // Get the new page's fragment, and set a listener for when the next button
        // is clicked
        TextInputTextFragment tit_fragment = TextInputTextFragment.newInstance(question, InputHint, InputInformation, SkipInformation);

        // Replace and commit the fragment
        completeFragmentTransaction(tit_fragment);
    }

    /**
     * Fragment for multiple selection
     *
     * @throws JSONException because we use json objects
     */
    private void createMultiSelectFragment(JSONObject page) throws JSONException {
        question = page.getString(LABEL);
        choices = JSONArrayToListOfJSONObjects(page.getJSONArray(CHOICES));
        targetValueID = page.getString(VALUE_ID);

        // Get the new page's fragment
        // set a listener for when the next button is clicked
        MultipleSelectionFragment ms_fragment = MultipleSelectionFragment.newInstance(question, choices);

        // Replace and commit the fragment
        completeFragmentTransaction(ms_fragment);
    }
    /**
     * Create the ParagraphFragment
     *
     * @param nextPageComponent the component to be displayed on screen
     * @throws JSONException because json
     */
    private void createParagraphFragment(JSONObject nextPageComponent) throws JSONException {
        question = nextPageComponent.getString(LABEL);
        paragraph = nextPageComponent.getString(TEXT);
        targetValue_id = nextPageComponent.getString(VALUE_ID);

        // Get the new page's fragment, and set a listener for when the next button
        // is clicked
        ParagraphFragment p_fragment = ParagraphFragment.newInstance(question, paragraph);

        // Replace and commit the fragment
        completeFragmentTransaction(p_fragment);
    }

    /**
     * Listener for clicking the next button: we can move to the correct next
     * page, as given by the user's sent JSON. This one is specifically for the
     * multiple choice fragment.
     *
     * @param choiceIndex the radiobutton choice that the user picked
     * @throws JSONException because we use json objects
     */
    @Override
    public void getResultFromMultipleChoiceFragment(int choiceIndex) throws JSONException {
        String diagnosis = choices.get(choiceIndex - 1).getString(TEXT);
        String nextPageName = choices.get(choiceIndex - 1).getString(LINK);

        // If the link is null, then we should go to the default page
        if (nextPageName.equals(NULL)) {
            nextPageName = pageID.getString(DEFAULT_LINK);
        }
        System.out.println("NEXT PAGE NAME:" + nextPageName);

        // Enter the diagnosis into the editor
        enterSymptomIntoEditor(pageID, diagnosis);

        // Decide on the next page based on the result
        try {
            getNextPage(nextPageName);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void getResultFromMultipleSelectionFragment(ArrayList<Integer> chosenOptionIds) throws JSONException {
        // add the selected choices to the diagnosis
        String allDiagnoses = choices.get(chosenOptionIds.get(0)).getString(TEXT);
        for (int i = 1; i < chosenOptionIds.size(); i++) {
            allDiagnoses += "\n" + choices.get(i).getString(TEXT);
            // Enter the diagnosis into the editor
        }
        enterSymptomIntoEditor(pageID, allDiagnoses);

        JSONObject foundLink = getContentFromPageID(pageID, targetValueID);
        String NextPage;
        if (foundLink == null) {
            NextPage =  pageID.getString(DEFAULT_LINK);
        }
        else {
            NextPage = foundLink.getString(SATISFIED_LINK);
        }

        // Decide on the next page based on the result
        try {
            getNextPage(NextPage);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void getResultFromTextInputFragment(Float numberInputted) throws JSONException {
        String diagnosis = String.valueOf(numberInputted);
        ArrayList<JSONObject> foundLinks = getContentFromPageIDArray(pageID, targetValue_id);
        String NextPage = pageID.getString(DEFAULT_LINK);
        if(!foundLinks.isEmpty()) {
            // Replace BranchedLink with whatever the name is for the link field
            // once the branched link logic is completed in the JSON file
            for (JSONObject foundLink : foundLinks) {
                if (foundLink.get("type").equals(">")) {
                    if (numberInputted > Float.parseFloat(foundLink.getString(THRESHOLD))) {
                        NextPage = foundLink.getString(SATISFIED_LINK);
                        break;
                    }
                } else if (foundLink.get("type").equals("<")) {
                    if (numberInputted < Float.parseFloat(foundLink.getString(THRESHOLD))) {
                        NextPage = foundLink.getString(SATISFIED_LINK);
                        break;
                    }
                } else if (foundLink.get("type").equals("<")) {
                    if (numberInputted < Float.parseFloat(foundLink.getString(THRESHOLD))) {
                        NextPage = foundLink.getString(SATISFIED_LINK);
                        break;
                    }
                } else {
                    if (numberInputted == Integer.parseInt(foundLink.getString(THRESHOLD))) {
                        NextPage = foundLink.getString(SATISFIED_LINK);
                        break;
                    }
                }
            }
        }
        // Enter the diagnosis into the editor
        enterSymptomIntoEditor(pageID, diagnosis);
        try {
            getNextPage(NextPage);
        } catch(JSONException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public void getResultFromTextInputTextFragment(String textInputted) throws JSONException {
        String recordedSymptom = textInputted;
        JSONObject logicComponent = getContentFromPageID(pageID, targetValue_id);
        String nextPage = pageID.getString(DEFAULT_LINK);
        if (logicComponent != null) {
            if (logicComponent.getString(TYPE).equals("=")) {
                if (recordedSymptom.equals(logicComponent.getString(THRESHOLD))) {
                    nextPage = logicComponent.getString(SATISFIED_LINK);
                }
            }
        }

        enterSymptomIntoEditor(pageID, recordedSymptom);
        try {
            getNextPage(nextPage);
        } catch(JSONException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public void getResultFromParagraphFragment() throws JSONException {
        String nextPage = pageID.getString(DEFAULT_LINK);
        try {
            getNextPage(nextPage);
        } catch(JSONException e) {
            throw new RuntimeException();
        }
    }
    /**
     * Takes the page id and returns where we wish to go
     *
     * @param pageid the page ID we wish to use
     * @param targetValueID the valueID of the current component that we are using
     * @return
     */
    public JSONObject getContentFromPageID(JSONObject pageid, String targetValueID) throws JSONException {
        JSONArray contentVal;
        if(pageid != null) {
            try {
                // Get the content from the JSONArray
                contentVal = pageid.getJSONArray(CONTENT);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            for(int i = 0; i < contentVal.length(); i++) {
                // Check to see if the ID's match up
                // With the build in has method
                JSONObject RetrievedTargetID = ((JSONObject)contentVal.get(i));
                if ( RetrievedTargetID.has(TARGET_VALUE_ID)) {
                    if(RetrievedTargetID.get(TARGET_VALUE_ID).equals(targetValueID)) {
                        return RetrievedTargetID;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Takes the page id and returns a list of targetids
     *
     * @param pageid the page ID we wish to use
     * @param targetValueID the valueID of the current component that we are using
     * @return
     */
    public ArrayList<JSONObject> getContentFromPageIDArray(JSONObject pageid, String targetValueID) throws JSONException {
        JSONArray contentVal;
        ArrayList<JSONObject> RetrievedTargetIDs = new ArrayList<>();
        if(pageid != null) {
            try {
                // Get the content from the JSONArray
                contentVal = pageid.getJSONArray(CONTENT);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            for(int i = 0; i < contentVal.length(); i++) {
                // Check to see if the ID's match up
                // With the build in has method
                JSONObject RetrievedTargetID = ((JSONObject)contentVal.get(i));
                if ( RetrievedTargetID.has(TARGET_VALUE_ID)) {
                    if(RetrievedTargetID.get(TARGET_VALUE_ID).equals(targetValueID)) {
                        RetrievedTargetIDs.add(RetrievedTargetID);
                    }
                }
            }
        }
        return RetrievedTargetIDs;
    }

    /**
     * Listener for clicking the back button: calls up the next page based on the
     * current backstack of pages
     *
     * @throws JSONException if the json hates you
     */
    @Override
    public void getLastPage() throws JSONException {
        // TODO: exit to main activity, if it's the first page

        // Get the last page JSON object that we put on to the backstack
        backstack.remove(backstack.size() - 1);
        String backPageName = backstack.get(backstack.size() - 1);

        getNextPage(backPageName);
    }

    private int findIndexInPagesGivenPageId(String pageID) throws JSONException {
        for (int i = 0; i < pages.length(); i++) {
            JSONObject currPage = pages.getJSONObject(i);
            String pageName = currPage.getString("pageID");
            if (pageID.equals(pageName)) {
                return i;
            }
        }
        return -1;
    }

    public void enterSymptomIntoEditor(JSONObject page, String symptom) throws JSONException {
        // Enter the diagnosis into the editor
        editor.putString("?: " + page.getString(TITLE), symptom);
        editor.apply();
    }
}