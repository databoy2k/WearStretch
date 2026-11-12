package com.db2k.wearstretch.model

object DataLayerPaths {
    const val ROUTINE_LIST = "/routines_list"
    const val SETTINGS = "/settings"
    const val START_APP = "/start_app"
    const val REFRESH_ROUTINES = "/refresh_routines"

    // A completed workout is published by the watch as a persistent DataItem at
    // "$WORKOUT_RESULT_PREFIX<recordId>". The data layer delivers it to the phone
    // whenever the phone is next reachable; the phone owns the Health Connect
    // sync and acks by deleting the item.
    const val WORKOUT_RESULT_PREFIX = "/workout_result/"
}
