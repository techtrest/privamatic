package com.techtrest.privacywidget.data.model

/**
 * Types of actions that can be taken to resolve a privacy issue.
 * Each type maps to a specific Android settings intent.
 */
enum class ActionType {
    OPEN_APP_SETTINGS,
    NOTIFICATION_LISTENER,
    ACCESSIBILITY_SETTINGS,
    DEVICE_ADMIN_SETTINGS,
    DEFAULT_APPS_SETTINGS,
    PRIVACY_SETTINGS,
    NETWORK_SETTINGS,
    LOCATION_SETTINGS,
    SECURITY_SETTINGS
}
