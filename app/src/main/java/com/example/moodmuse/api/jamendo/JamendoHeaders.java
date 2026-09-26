package com.example.moodmuse.api.jamendo;

import com.google.gson.annotations.SerializedName;

public class JamendoHeaders {
    @SerializedName("status")
    private String status;

    @SerializedName("code")
    private int code;

    @SerializedName("error_message")
    private String errorMessage;

    @SerializedName("warnings")
    private String warnings;

    @SerializedName("results_count")
    private int resultsCount;

    public String getStatus() {
        return status;
    }

    public int getCode() {
        return code;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getWarnings() {
        return warnings;
    }

    public int getResultsCount() {
        return resultsCount;
    }
}

