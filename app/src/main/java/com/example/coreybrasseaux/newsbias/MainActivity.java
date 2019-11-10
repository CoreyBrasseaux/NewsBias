package com.example.coreybrasseaux.newsbias;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Iterator;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    EditText editUrl = null;
    TextView textArticle = null;
    TextView textScore = null;
    Button buttonSubmit = null;

    String article = "";

    //st.replaceAll("\\s+","")

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editUrl = findViewById(R.id.editUrl);
        textArticle = findViewById(R.id.textArticle);
        textScore = findViewById(R.id.textScore);
        buttonSubmit = findViewById(R.id.buttonSubmit);

        seqIfBrowserShare();
    }

    private void seqIfBrowserShare() {
        // See if we opened the app from the "Share" button.
        String url = "";
        Intent intent = getIntent();
        url = intent.getStringExtra(Intent.EXTRA_TEXT);
        Log.d("LOGGER", "The URL shared is: " + url);

        if(!editUrl.equals("")) {
            editUrl.setText(url);
            // Automatically click the button.
            onSubmit(null);
        }
    }

    /*private String getArticleTika(String sURL) {
        try {
            URL url = new URL("http://www.basicsbehind.com/stack-data-structure/");
            InputStream input = url.openStream();
            LinkContentHandler linkHandler = new LinkContentHandler();
            ContentHandler textHandler = new BodyContentHandler();
            ToHTMLContentHandler toHTMLHandler = new ToHTMLContentHandler();
            TeeContentHandler teeHandler = new TeeContentHandler(linkHandler, textHandler, toHTMLHandler);
            Metadata metadata = new Metadata();
            ParseContext parseContext = new ParseContext();
            HtmlParser parser = new HtmlParser();
            parser.parse(input, teeHandler, metadata, parseContext);
            System.out.println("title:\n" + metadata.get("title"));
            System.out.println("links:\n" + linkHandler.getLinks());
            System.out.println("text:\n" + textHandler.toString());
            System.out.println("html:\n" + toHTMLHandler.toString());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }*/

    private String getArticle(String url) {
        try {
            //Document doc = Jsoup.connect("http://www.basicsbehind.com/stack-data-structure/").get();
            Document doc = Jsoup.connect(url).get();
            //Document doc = Jsoup.connect("http://www.basicsbehind.com/tag/boilerpipe-tutorial/").get();
            // select title of the page
            //System.out.println(doc.title());
            // select text of whole page
            article = doc.text();
            return article;
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public void onSubmit(View view) {
        // Handle network activity (the OKHTTP3 request) on a seperate (non-UI) thread.
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("LOGGER", "Created main activity.");
                final String article = getArticle(editUrl.getText().toString());

                // Prepare text to be analyzed.
                String text = article.replaceAll("\\s+", "%20");
                try {
                    text = text.substring(0, 1400); // make sure we're not over max length
                }
                catch(Exception e) {
                    // ignore
                }
                Log.d("LOGGER", "Preparing URL: " + text);

                Response response = analyzeText(text);


                /*if(response == null) {
                    Log.d("LOGGER", "Response was null.");
                }*/
                Log.d("LOGGER", response.toString());
                // Update the article text view.
                try {
                    final String body = response.body().string();
                    //Log.d("LOGGER", )

                    //double biasScore = getScore(body);


                    Log.d("LOGGER", body);
                    textArticle.post(new Runnable() {
                        @Override
                        public void run() {
                            //    textArticle.setText(body);
                        }
                    });

                    textScore.post(new Runnable() {
                        @Override
                        public void run() {
                            textScore.setText("Bias: " + getBias(body) + " | Score: " + getScore(body));
                        }
                    });

                    Log.d("LOGGER", "Article: " + article);
                    textArticle.post(new Runnable() {
                        @Override
                        public void run() {
                            //textArticle.setText(article);
                            textArticle.setText(highlightText(article, body));
                        }
                    });
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
                Log.d("LOGGER", "Completed request.");
            }
        }).start();
    }


    // We need to enhance this method to take in a String.
    private Response analyzeText(String text) {
        Log.d("LOGGER", "Entering analyzeText() method.");
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                //.url("https://twinword-sentiment-analysis.p.rapidapi.com/analyze/?text=this%20is%20wonderful%20and%20great")
                .url("https://twinword-sentiment-analysis.p.rapidapi.com/analyze/?text=" + text)
                .get()
                .addHeader("x-rapidapi-host", "twinword-sentiment-analysis.p.rapidapi.com")
                .addHeader("x-rapidapi-key", "22387fa742mshee79c80124a47cbp147091jsne4150131ad0e")
                .build();

        Response response = null;
        //Log.d("LOGGER", "Beginning text analysis.");
        try {
            Call call = client.newCall(request);
            response = call.execute();
            //Log.d("LOGGER", "Was the call excecuted: " + call.isExecuted());

        }
        catch(Exception e) {
            Log.d("LOGGER", "Error while trying; to make request.");
            e.printStackTrace();
        }

        return response;
    }

    private int getScore(String text) {
        JsonObject jsonObject = new JsonParser().parse(text).getAsJsonObject();
        JsonElement jsonElement = jsonObject.get("score");
        double score = jsonElement.getAsDouble();
        int modScore = (int)(score * 100);
        return modScore;
    }

    private String getBias(String text) {
        JsonObject jsonObject = new JsonParser().parse(text).getAsJsonObject();
        JsonElement jsonElement = jsonObject.get("type");
        String type = jsonElement.getAsString();
        return type;
    }

    private Spannable highlightText(String text, String json) {

        Spannable spannable = new SpannableString(text);
        BackgroundColorSpan bgSpanPositive = new BackgroundColorSpan(Color.GREEN);
        BackgroundColorSpan bgSpanNegative = new BackgroundColorSpan(Color.RED);

        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
        JsonArray jsonArray = jsonObject.getAsJsonArray("keywords");
        Iterator<JsonElement> it = jsonArray.iterator();
        while(it.hasNext()) {
            JsonElement element = it.next();

            JsonObject jsonObject1 = element.getAsJsonObject();

            JsonElement element1 = jsonObject1.get("word");
            JsonElement element2 = jsonObject1.get("score");

            String name = element1.getAsString();
            double score = element2.getAsDouble();

            try {
                if (score > 0) {
                    spannable.setSpan(
                            CharacterStyle.wrap(bgSpanPositive),
                            text.indexOf(name),
                            text.indexOf(name) + name.length(),
                            0);
                } else {
                    spannable.setSpan(
                            CharacterStyle.wrap(bgSpanNegative),
                            text.indexOf(name),
                            text.indexOf(name) + name.length(),
                            0);
                }
            }
            catch(Exception e) {
                //ignore
            }

        }

        return spannable;
    }
}