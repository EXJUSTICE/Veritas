package com.xu.servicequalityrater.data;

import android.provider.BaseColumns;

public class EmotionListContract {

    public class EmotionListEntry implements BaseColumns {
        public static final String COLUMN_ANGER = "anger";
        public static final String COLUMN_CONTEMPT = "contempt";
        public static final String COLUMN_DISGUST = "disgust";
        public static final String COLUMN_FEAR = "fear";
        public static final String COLUMN_HAPPINESS = "happiness";
        public static final String COLUMN_NEUTRAL = "neutral";
        public static final String COLUMN_SADNESS = "sadness";
        public static final String COLUMN_SURPRISE = "surprise";

        public static final String COLUMN_TIMESTAMP = "datetime";

        //http://www.jiahaoliuliu.com/2011/09/sqlite-saving-date-as-integer.html
        public static final String TABLE_NAME = "emotionlist";
    }
}
