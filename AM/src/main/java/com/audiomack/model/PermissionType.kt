package com.audiomack.model

enum class PermissionType {

    Notification {
        override fun stringValue() = "Notification"
    },
    Location {
        override fun stringValue() = "Location"
    },
    Storage {
        override fun stringValue() = "Storage"
    },
    Camera {
        override fun stringValue() = "Camera"
    };

    abstract fun stringValue(): String
}
