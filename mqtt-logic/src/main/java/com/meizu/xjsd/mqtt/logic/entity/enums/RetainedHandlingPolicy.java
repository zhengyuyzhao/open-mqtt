//package com.meizu.xjsd.domain.entity.enums;
//
//public enum RetainedHandlingPolicy {
//    SEND_AT_SUBSCRIBE(0),
//    SEND_AT_SUBSCRIBE_IF_NOT_YET_EXISTS(1),
//    DONT_SEND_AT_SUBSCRIBE(2);
//
//    private final int value;
//
//    private RetainedHandlingPolicy(int value) {
//        this.value = value;
//    }
//
//    public int value() {
//        return this.value;
//    }
//
//    public static RetainedHandlingPolicy valueOf(int value) {
//        switch (value) {
//            case 0:
//                return SEND_AT_SUBSCRIBE;
//            case 1:
//                return SEND_AT_SUBSCRIBE_IF_NOT_YET_EXISTS;
//            case 2:
//                return DONT_SEND_AT_SUBSCRIBE;
//            default:
//                throw new IllegalArgumentException("invalid RetainedHandlingPolicy: " + value);
//        }
//    }
//}
