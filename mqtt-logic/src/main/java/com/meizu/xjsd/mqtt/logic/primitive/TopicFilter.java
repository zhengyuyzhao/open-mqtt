package com.meizu.xjsd.mqtt.logic.primitive;

public class TopicFilter {

    public static boolean isMatch(String topicFilter, String topic) {
        if (topicFilter == null || topic == null) {
            return false;
        }
        if (topicFilter.equals("#")) {
            return true;
        }
        if (topicFilter.equals("+")) {
            return topic.indexOf('/') < 0;
        }
        String[] filterParts = topicFilter.split("/");
        String[] topicParts = topic.split("/");
        if (filterParts.length > topicParts.length) {
            return false;
        }
        for (int i = 0; i < filterParts.length; i++) {
            if (!filterParts[i].equals("+") && !filterParts[i].equals(topicParts[i])) {
                return false;
            }
        }
        return true;
    }
}
