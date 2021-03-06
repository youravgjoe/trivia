package com.youravgjoe.apps.trivia;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ButtonBarLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity";
    private static final String DIFFICULTY = "DIFFICULTY";
    private static final String NUMBER_OF_QUESTIONS = "NUMBER_OF_QUESTIONS";
    private static final String TOTAL_NUMBER_OF_ANSWERS = "TOTAL_NUMBER_OF_ANSWERS";
    private static final String BASE_64 = "base64";

    private List<TriviaQuestion> mTriviaQuestionList;
    private List<String> mAnswerList;

    private TextView mScoreTextView;
    private TextView mQuestionTextView;
    private RadioGroup mRadioGroup;
    private Button mSubmitButton;

    private int mCurrentQuestionIndex;

    private int mCorrect;

    private boolean mQuestionAnswered;
    private boolean mSummaryShown;

    // keep this here to subtract from the RadioButton ids. It's dumb, but this is my solution for now.
//    private int mTotalAnswerCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTriviaQuestionList = new ArrayList<>();
        mAnswerList = new ArrayList<>();
        mCurrentQuestionIndex = 0;
        mCorrect = 0;
        mQuestionAnswered = false;

        setupViews();

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
//            mTotalAnswerCount += extras.getInt(TOTAL_NUMBER_OF_ANSWERS);
            callApi(extras.getString(NUMBER_OF_QUESTIONS), extras.getString(DIFFICULTY).toLowerCase(), BASE_64);
        } else {
            Log.d(TAG, "Error getting difficulty and number of questions.");
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

//        MainActivity.mTotalNumOfAnswers += mTotalAnswerCount;
    }

    private void setupViews() {

        // todo: make a middle view at the top that says "Question 4/10",
        // todo: and then one in the top left corner with # correct and top right with # incorrect


        // todo: try to decode the whole json string before we parse it into a json object

        // todo: if that doesn't work, try specifying the encoding in the url when we call the api,
        // todo: and then decode it that specific way on the other end.

        mScoreTextView = (TextView) findViewById(R.id.score);
        mQuestionTextView = (TextView) findViewById(R.id.question);
        mRadioGroup = (RadioGroup) findViewById(R.id.radio_group);
        mSubmitButton = (Button) findViewById(R.id.submit);
        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSummaryShown) {
                    finish();
                }
                if (mQuestionAnswered) {
                    if (mCurrentQuestionIndex == mTriviaQuestionList.size() - 1) {
                        if (mSummaryShown) {

                        } else {
                            // if we've reached the end of the questions, add the last group of answers and load the summary.
//                            mTotalAnswerCount += mAnswerList.size();
                            showSummary();
                            mSummaryShown = true;
                        }
                    } else {
                        // if they've already answered and haven't reached the end of the questions, load the next question
                        mCurrentQuestionIndex++;
                        mQuestionAnswered = false;
//                        mTotalAnswerCount += mAnswerList.size();
                        mAnswerList.clear();
                        mRadioGroup.removeAllViews();
                        mRadioGroup.clearCheck();
                        mSubmitButton.setText(getResources().getString(R.string.prompt_submit));
                        loadQuestion();
                    }
                } else {
                    // if they haven't already answered, then handle the answer

                    // make sure something has actually been checked
                    final int checkedId = mRadioGroup.getCheckedRadioButtonId();
                    Log.d(TAG, "checkedId: " + checkedId);
                    if (checkedId == -1) {
                        return;
                    }

                    String correctAnswer = mTriviaQuestionList.get(mCurrentQuestionIndex).getCorrectAnswer();

                    RadioButton correctRadioButton = null;

                    for (int i = 0; i < mRadioGroup.getChildCount(); i++) {
                        Log.d(TAG, "count: " + mRadioGroup.getChildCount() + ", i: " + i + " RadioButton id: " + mRadioGroup.getChildAt(i).getId());
                        RadioButton tempButton = (RadioButton) mRadioGroup.getChildAt(i);
                        if (TextUtils.equals(tempButton.getText().toString(), correctAnswer)) {
                            correctRadioButton = tempButton;
                            break;
                        }
                    }

                    RadioButton checkedRadioButton = (RadioButton) mRadioGroup.getChildAt(checkedId);
//                    RadioButton checkedRadioButton = (RadioButton) mRadioGroup.getChildAt(checkedId - 1 - mTotalAnswerCount);
                    String answer = checkedRadioButton.getText().toString();

                    // if they chose the correct answer, give them an internet high five!
                    if (TextUtils.equals(answer, correctAnswer)) {
                        mCorrect++;
//                        Toast.makeText(v.getContext(), "Correct! :)", Toast.LENGTH_SHORT).show();
                        checkedRadioButton.setTextColor(getResources().getColor(R.color.correct));
                    } else {
                        // otherwise, tell them they were wrong, and which one was right
//                        Toast.makeText(v.getContext(), "Incorrect! :(", Toast.LENGTH_SHORT).show();
                        checkedRadioButton.setTextColor(getResources().getColor(R.color.incorrect));
                        correctRadioButton.setTextColor(getResources().getColor(R.color.correct));
                    }

                    mSubmitButton.setText(getResources().getString(R.string.prompt_next_question));
                    mScoreTextView.setText(mCorrect + "/" + mTriviaQuestionList.size());

                    mQuestionAnswered = true;
                }
            }
        });
    }

    private void loadQuestion() {
        mQuestionTextView.setText(mTriviaQuestionList.get(mCurrentQuestionIndex).getQuestion());

        mAnswerList = mTriviaQuestionList.get(mCurrentQuestionIndex).getAllAnswers();

        for (int i = 0; i < mTriviaQuestionList.get(mCurrentQuestionIndex).getNumOfAnswers(); i++) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setId(i);
            radioButton.setText(mAnswerList.get(i));
            radioButton.setTextSize(18);
            mRadioGroup.addView(radioButton);
        }
    }

    private void showSummary() {
        mRadioGroup.setVisibility(View.GONE);

        String summary;
        // if they did better than 50%, tell them good job
        if (mCorrect >= (mTriviaQuestionList.size() / 2)) {
            summary = getResources().getString(R.string.summary_good_job, String.valueOf(mCorrect), String.valueOf(mTriviaQuestionList.size()));
        } else {
            summary = getResources().getString(R.string.summary_better_luck, String.valueOf(mCorrect), String.valueOf(mTriviaQuestionList.size()));
        }

        mQuestionTextView.setText(summary);
        mSubmitButton.setText(getResources().getString(R.string.done));
    }

    private void callApi(String numOfQuestions, String difficulty, String encoding) {
        final String BASE_URL = "https://www.opentdb.com/";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MyApiEndpointInterface apiService = retrofit.create(MyApiEndpointInterface.class);

        Call<ResponseBody> result = apiService.getTriviaQuestions(numOfQuestions, difficulty, encoding);

        Log.d(TAG, result.toString());

        result.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                JSONObject responseJson;
                JSONArray questionArray;
                try {

//                    String responseString = response.body().string();
//
//                    Log.d(TAG, "responseString" + responseString);
//
//                    byte[] bytes = Base64.decode(responseString, Base64.DEFAULT);
//
//                    String decodedResponse = new String(bytes, "UTF-8");



//                    try {
//                        byte[] bytes = in.getBytes("UTF-8");
//                        return new String(bytes, "UTF-8");
//                    } catch (UnsupportedEncodingException e) {
//                        e.printStackTrace();
//                        return null;
//                    }



                    responseJson = new JSONObject(response.body().string());
                    questionArray = responseJson.getJSONArray("results");

                    for (int i = 0; i < questionArray.length(); i++) {
                        JSONObject questionJson = questionArray.getJSONObject(i);
                        TriviaQuestion question = new TriviaQuestion();
                        question.setCategory(questionJson.getString("category"));
                        question.setDifficulty(questionJson.getString("difficulty"));
                        question.setQuestion(questionJson.getString("question"));
                        question.setCorrectAnswer(questionJson.getString("correct_answer"));
                        JSONArray incorrectAnswers = questionJson.getJSONArray("incorrect_answers");
                        List<String> incorrectAnswersList = new ArrayList<>();
                        for (int j = 0; j < incorrectAnswers.length(); j++) {
                            incorrectAnswersList.add(incorrectAnswers.getString(j));
                        }
                        question.setIncorrectAnswers(incorrectAnswersList);

//                        List<String> allAnswers = incorrectAnswersList;
//                        allAnswers.add(question.getCorrectAnswer());
                        question.setAllAnswers();

                        mTriviaQuestionList.add(question);
                    }

                    mScoreTextView.setText(mCorrect + "/" + mTriviaQuestionList.size());

                    loadQuestion();
                } catch (JSONException e) {
                    Log.d(TAG, "JSONException");
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.d(TAG, "IOException");
                    e.printStackTrace();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "onFailure: " + t.toString());
            }
        });
    }

    public interface MyApiEndpointInterface {
        // Request method and URL specified in the annotation
        // Callback for the parsed response is the last parameter

        @GET("api.php")
        Call<ResponseBody> getTriviaQuestions(@Query("amount") String amount, @Query("difficulty") String difficulty, @Query("encode") String encoding);
    }
}
