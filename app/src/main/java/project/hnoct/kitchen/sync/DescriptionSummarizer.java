package project.hnoct.kitchen.sync;

import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hnoct on 5/22/2017.
 */

public class DescriptionSummarizer {
    // Constants
    private static final String LOG_TAG = DescriptionSummarizer.class.getSimpleName();

    private String titleFillerRegex = "(?:[Tt]he|[Bb]est|[Ww]ith|[Ww]ithout|[Aa]nd|[Ee]asy|[Ee]asiest|[Dd]e|[Mm]eal|[Pp]rep|[Hh]ealthy?) ?";
    private String punctuationRegex = "\\.|:|;|\\?|\\!|\\\"|–|-";

    // Common cooking related words that are given a starter score
    private List<String> defaultKeyWords = new ArrayList<>(Arrays.asList("cook(?:ed)?\\b",
            "bake?\\b", "fr[yi](?:ed)?\\b", "recipe\\b", "this is(?! not)", "these are(?! not)",
            "they are(?! not)"));

    private Map<String, Double> pointMap = new HashMap<>(); // Holds the score of each word
    private Element mRecipeElement;

    // Member Variables
    Document mRecipeDocument;

    public DescriptionSummarizer() {
//        try {
//            mRecipeDocument = Jsoup.connect(url).get();
//            mRecipeElement = mRecipeDocument.select("[itemtype*=//schema.org/Recipe]").first();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    public String summarize(String recipeName, String recipeDescription) {
        // Remove unnecessary elements from the recipe title
        recipeName = recipeName.replaceAll(titleFillerRegex, "");
        recipeName = recipeName.replaceAll(punctuationRegex, "");

        // Score each word in the recipe's title utilizing a count of how many times it occurs
        // in the description
        String[] recipeNameArray = recipeName.replace("'s", "").toLowerCase().split(" ");
        String[] recipeDescriptionArray = recipeDescription.split(" ");

        // Iterate through each word in the title and ensure that it doesn't already occur in the
        // list of default words and add each word into the pointMap
        for (String nameWord : recipeNameArray) {
            boolean addScore = true;            // Boolean for whether this word occurs in the description
            for (String defaultWord : defaultKeyWords) {
                if (nameWord.toLowerCase().matches(".*" + defaultWord + ".*")) {
                    pointMap.put(nameWord.toLowerCase(), 3.0);
                    addScore = false;
                    defaultKeyWords.remove(defaultWord);
                    break;
                }
            }
            if (addScore && nameWord.length() > 1) {
                pointMap.put(nameWord.toLowerCase().replace("'s", ""), 3.0);
            } else if (addScore) {
                pointMap.put(nameWord, 0.0);
            }

        }

        for (String defaultWord : defaultKeyWords) {
            pointMap.put(defaultWord, 3.0);
        }

        // Modify the word score of each word based on how many times it occurs in the description
        for (String descriptionWord : recipeDescriptionArray) {
            int count = 0;
            for (String nameWord : recipeNameArray) {
                if (descriptionWord.toLowerCase().matches(".*" + nameWord.toLowerCase() + ".*")) {
                    pointMap.put(nameWord, pointMap.get(nameWord) * 1.05);
                    count++;
                }
            }
        }

        // Split the description into sentences based on where periods occur
        String[] descriptionSentences = recipeDescription.replaceAll("\\([\\w\\s\\d\\.\\!\\…,':.’]+\\) *", "").split("(?<!Mr|Mrs|Dr|Ms|\\b[Pp]|\\b[Ss])\\.|  ");

        // Calculate the minimum score threshold for when a sentence should be included based on
        // how many sentences the description has
        double scoreThreshold = 3 * (Math.pow(1.01, descriptionSentences.length));

        Log.d(LOG_TAG, "Score threshold: " + scoreThreshold);

        // Init StringBuilder to hold the summarized description String
        StringBuilder builder = new StringBuilder();

        // Iterate through the sentences of the description and check whether it contains one of the
        // key words
        for (String descriptionSentence : descriptionSentences) {
            // Init variable to hold sentence score based on how many key words it contains
            double sentenceScore = 0;
            for (String word : pointMap.keySet()) {
                if (descriptionSentence.toLowerCase().contains(word.toLowerCase()) ||
                        descriptionSentence.toLowerCase().matches(".*" + word.toLowerCase() + ".*") ||
                        descriptionSentence.toLowerCase().matches(".*" + word + ".*")) {
                    Log.d(LOG_TAG, word);
                    // If sentence contains a keyword, add its score to its sentence score
                    sentenceScore += pointMap.get(word);
                }
            }

            // Check if sentence score is greater than the score threshold
            if (sentenceScore > scoreThreshold) {
                Log.d(LOG_TAG, "\n"+descriptionSentence.trim() + "\nScore: " + sentenceScore);
                builder.append(descriptionSentence.replaceAll("^ *", "").replaceAll("\n", "\n\n")).append(". ");
            }

            if (descriptionSentence.trim().isEmpty()) {
                builder.append("\n\n");
            }
        }

        Log.d(LOG_TAG, "Description: " + builder.toString().trim());
        return builder.toString().trim();
    }

    private String getRecipeTitle() {
        // Array of Elements that could possibly contain the recipe author's name
        Element[] elementArray = new Element[] {
                mRecipeElement.select("[itemprop=name]").first(),
        };

        // Iterate through the Elements and attempt to retrieve author information
        for (Element recipeTitleElement : elementArray) {
            if (recipeTitleElement != null) {
                // If an Element is valid, remove the "by" tag and return the String
                return recipeTitleElement.text().replaceAll("[Bb][Yy] ", "");
            }
        }

        return null;
    }

    private String getAllDescription() {
        StringBuilder builder = new StringBuilder();

        for (String line : mRecipeDocument.toString().split("\n")) {
            if (line.matches(".*//schema.org/[Rr]ecipe.*")) {
                break;
            }

            builder.append(line)
                    .append("\n");
        }

        Document partialDocument = Jsoup.parse(builder.toString());

        Elements descriptionElements = partialDocument.select("p");

        List<String> descriptionList = new ArrayList<>();

        builder = new StringBuilder();
        for (Element descriptionElement : descriptionElements) {
            builder.append(descriptionElement.text())
                    .append("\n");
        }

        return builder.toString().trim();
    }
}
